package com.noam.fleetcommand.reserves;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IsraeliNatureReserveLookupServiceTests {

    private final IsraeliNatureReserveLookupService lookupService =
            new IsraeliNatureReserveLookupService(new ObjectMapper(), new ClassPathResource("israeli-nature-reserves.json"));

    @Test
    void resolvesKnownReserveAliasToOfficialReserve() {
        IsraeliNatureReserveLookupService.ResolvedNatureReserve reserve = lookupService.resolveByName("Tel Dan");

        assertEquals("Tel Dan Nature Reserve", reserve.officialName());
        assertEquals("Upper Galilee", reserve.region());
        assertEquals(33.2380, reserve.area().getMinLatitude());
        assertEquals(35.6740, reserve.area().getMaxLongitude());
    }

    @Test
    void rejectsUnknownReserveName() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> lookupService.resolveByName("Definitely Not A Real Reserve")
        );

        assertEquals("This name is not recognized as an official nature reserve in Israel", exception.getMessage());
    }

    @Test
    void autoResolvesCloseMatchToOfficialReserve() {
        IsraeliNatureReserveLookupService.ResolvedNatureReserve reserve = lookupService.resolveByName("ein gedi");

        assertEquals("Ein Gedi Nature Reserve", reserve.officialName());
    }

    @Test
    void returnsSuggestionsForPartialInput() {
        assertFalse(lookupService.searchSuggestions("ged").isEmpty());
        assertEquals("Ein Gedi Nature Reserve", lookupService.searchSuggestions("ged").get(0));
    }
}
