package com.noam.fleetcommand.events;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TravelerMediaStorageService {

    private final Path storageDirectory;

    public TravelerMediaStorageService(@Value("${app.traveler-media.storage-dir:uploads/traveler-media}") String storageDir) {
        this.storageDirectory = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageDirectory);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to create traveler media storage directory", exception);
        }
    }

    public List<EventMedia> store(List<MultipartFile> files) {
        List<EventMedia> media = new ArrayList<>();
        if (files == null) {
            return media;
        }

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
            String mediaType = contentType.startsWith("video/") ? "VIDEO" : "IMAGE";
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "attachment" : file.getOriginalFilename());
            String extension = originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                    : "";
            String storedName = UUID.randomUUID() + extension;
            Path target = storageDirectory.resolve(storedName);

            try {
                Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                throw new UncheckedIOException("Failed to store traveler media", exception);
            }

            media.add(new EventMedia("/api/public/media/" + storedName, mediaType, originalFilename));
        }

        return media;
    }

    public Resource loadAsResource(String fileName) {
        try {
            Path filePath = storageDirectory.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                throw new IllegalArgumentException("Media file not found");
            }
            return resource;
        } catch (MalformedURLException exception) {
            throw new IllegalArgumentException("Media file not found");
        }
    }
}
