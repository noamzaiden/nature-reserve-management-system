package com.noam.fleetcommand.reserves;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class IsraeliNatureReserveLookupService {

    private final Map<String, CatalogReserve> reservesByNormalizedName;
    private final List<CatalogReserve> catalog;

    public IsraeliNatureReserveLookupService() {
        this(new ObjectMapper(), new ClassPathResource("israeli-nature-reserves.json"));
    }

    IsraeliNatureReserveLookupService(ObjectMapper objectMapper, Resource reserveCatalogResource) {
        LoadedCatalog loadedCatalog = loadCatalog(objectMapper, reserveCatalogResource);
        this.catalog = loadedCatalog.catalog();
        this.reservesByNormalizedName = loadedCatalog.reservesByNormalizedName();
    }

    public ResolvedNatureReserve resolveByName(String requestedName) {
        String normalizedRequestedName = normalizeInputName(requestedName);
        CatalogReserve catalogReserve = reservesByNormalizedName.get(normalizedRequestedName);
        if (catalogReserve == null) {
            List<CatalogCandidate> candidates = findBestCandidates(normalizedRequestedName);
            if (!candidates.isEmpty()) {
                CatalogCandidate bestCandidate = candidates.get(0);
                if (shouldAutoResolve(candidates)) {
                    return toResolvedReserve(bestCandidate.reserve());
                }

                String suggestions = candidates.stream()
                        .map(candidate -> candidate.reserve().officialName())
                        .distinct()
                        .limit(5)
                        .collect(Collectors.joining(", "));
                throw new IllegalArgumentException(
                        "This name is not recognized as an official nature reserve in Israel. Possible matches: " + suggestions
                );
            }

            throw new IllegalArgumentException("This name is not recognized as an official nature reserve in Israel");
        }

        return toResolvedReserve(catalogReserve);
    }

    public List<String> searchSuggestions(String requestedName) {
        String trimmed = requestedName == null ? "" : requestedName.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }

        String normalizedRequestedName = normalizeName(trimmed);
        return findBestCandidates(normalizedRequestedName).stream()
                .map(candidate -> candidate.reserve().officialName())
                .distinct()
                .limit(5)
                .toList();
    }

    public List<ResolvedNatureReserve> getCatalog() {
        return catalog.stream()
                .map(this::toResolvedReserve)
                .toList();
    }

    private LoadedCatalog loadCatalog(ObjectMapper objectMapper, Resource reserveCatalogResource) {
        try (InputStream inputStream = reserveCatalogResource.getInputStream()) {
            List<CatalogReserve> catalog = objectMapper.readValue(inputStream, new TypeReference<>() {});
            Map<String, CatalogReserve> normalizedCatalog = new HashMap<>();

            for (CatalogReserve reserve : catalog) {
                registerName(normalizedCatalog, reserve, reserve.officialName());
                for (String alias : reserve.aliases()) {
                    registerName(normalizedCatalog, reserve, alias);
                }
            }

            return new LoadedCatalog(List.copyOf(catalog), Map.copyOf(normalizedCatalog));
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to load Israeli nature reserve catalog", exception);
        }
    }

    private ResolvedNatureReserve toResolvedReserve(CatalogReserve catalogReserve) {
        AreaBounds area = catalogReserve.area();
        return new ResolvedNatureReserve(
                catalogReserve.officialName(),
                catalogReserve.region(),
                new Area(area.minLatitude(), area.maxLatitude(), area.minLongitude(), area.maxLongitude())
        );
    }

    private List<CatalogCandidate> findBestCandidates(String normalizedRequestedName) {
        return catalog.stream()
                .map(reserve -> new CatalogCandidate(reserve, scoreCandidate(normalizedRequestedName, reserve)))
                .filter(candidate -> candidate.score() >= 0.45d)
                .sorted(Comparator.comparingDouble(CatalogCandidate::score).reversed())
                .limit(5)
                .toList();
    }

    private boolean shouldAutoResolve(List<CatalogCandidate> candidates) {
        if (candidates.isEmpty()) {
            return false;
        }

        CatalogCandidate bestCandidate = candidates.get(0);
        double gap = candidates.size() > 1 ? bestCandidate.score() - candidates.get(1).score() : bestCandidate.score();
        return bestCandidate.score() >= 0.92d || (bestCandidate.score() >= 0.84d && gap >= 0.08d);
    }

    private double scoreCandidate(String normalizedRequestedName, CatalogReserve reserve) {
        double bestScore = similarity(normalizedRequestedName, normalizeName(reserve.officialName()));
        for (String alias : reserve.aliases()) {
            bestScore = Math.max(bestScore, similarity(normalizedRequestedName, normalizeName(alias)));
        }
        return bestScore;
    }

    private void registerName(Map<String, CatalogReserve> normalizedCatalog, CatalogReserve reserve, String rawName) {
        String normalizedName = normalizeName(rawName);
        CatalogReserve previous = normalizedCatalog.putIfAbsent(normalizedName, reserve);
        if (previous != null && !previous.officialName().equals(reserve.officialName())) {
            throw new IllegalStateException("Duplicate normalized reserve name in catalog: " + rawName);
        }
    }

    private String normalizeInputName(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Reserve name is required");
        }
        return normalizeName(trimmed);
    }

    private String normalizeName(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replace("&", " and ")
                .replace("'", "")
                .replace("-", " ")
                .replaceAll("\\bnature reserve\\b", " ")
                .replaceAll("\\breserve\\b", " ")
                .replaceAll("\\bnational park\\b", " ")
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private double similarity(String left, String right) {
        if (left.equals(right)) {
            return 1.0d;
        }
        if (left.isBlank() || right.isBlank()) {
            return 0.0d;
        }

        Set<String> leftTokens = tokenize(left);
        Set<String> rightTokens = tokenize(right);
        long sharedTokens = leftTokens.stream().filter(rightTokens::contains).count();
        double tokenScore = (2.0d * sharedTokens) / (leftTokens.size() + rightTokens.size());

        double textScore;
        if (left.contains(right) || right.contains(left)) {
            textScore = 0.95d;
        } else {
            int longestLength = Math.max(left.length(), right.length());
            int distance = levenshteinDistance(left, right);
            textScore = 1.0d - ((double) distance / longestLength);
        }

        return Math.max(textScore, (textScore * 0.65d) + (tokenScore * 0.35d));
    }

    private Set<String> tokenize(String value) {
        Set<String> tokens = new HashSet<>();
        for (String token : value.split(" ")) {
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private int levenshteinDistance(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];

        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }

        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost
                );
            }

            int[] swap = previous;
            previous = current;
            current = swap;
        }

        return previous[right.length()];
    }

    private record CatalogReserve(
            String officialName,
            String region,
            List<String> aliases,
            AreaBounds area
    ) {
        private CatalogReserve {
            aliases = aliases == null ? List.of() : List.copyOf(aliases);
        }
    }

    private record AreaBounds(
            double minLatitude,
            double maxLatitude,
            double minLongitude,
            double maxLongitude
    ) {
    }

    private record LoadedCatalog(List<CatalogReserve> catalog, Map<String, CatalogReserve> reservesByNormalizedName) {
    }

    private record CatalogCandidate(CatalogReserve reserve, double score) {
    }

    public record ResolvedNatureReserve(String officialName, String region, Area area) {
    }
}
