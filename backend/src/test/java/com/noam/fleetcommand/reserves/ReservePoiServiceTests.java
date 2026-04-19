package com.noam.fleetcommand.reserves;

import com.noam.fleetcommand.reserves.dto.ReservePoiRequestDto;
import com.noam.fleetcommand.reserves.dto.ReservePoiTypeRequestDto;
import com.noam.fleetcommand.security.CurrentUserService;
import com.noam.fleetcommand.users.User;
import com.noam.fleetcommand.users.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservePoiServiceTests {

    @Mock
    private ReserveRepository reserveRepository;

    @Mock
    private ReservePoiRepository reservePoiRepository;

    @Mock
    private ReservePoiTypeRepository reservePoiTypeRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private ReservePoiService reservePoiService;

    private User manager;
    private Reserve reserve;

    @BeforeEach
    void setUp() {
        manager = new User();
        manager.setId(7L);
        manager.setRole(UserRole.MANAGER);

        reserve = new Reserve("Yatir Forest", "South", new Area(31.2, 31.4, 35.0, 35.2));
        reserve.setId(12L);
        reserve.setManager(manager);
    }

    @Test
    void getReservePoiTypes_seedsDefaultTypesWhenMissing() {
        when(currentUserService.getRequiredManager()).thenReturn(manager);
        when(reserveRepository.findById(reserve.getId())).thenReturn(Optional.of(reserve));
        when(reservePoiTypeRepository.existsByReserveId(reserve.getId())).thenReturn(false);

        ReservePoiType entrance = new ReservePoiType(reserve, "Entrance", true);
        entrance.setId(1L);
        when(reservePoiTypeRepository.findByReserveIdOrderBySystemDefaultDescNameAsc(reserve.getId()))
                .thenReturn(List.of(entrance));

        reservePoiService.getReservePoiTypes(reserve.getId());

        verify(reservePoiTypeRepository).saveAll(any());
        verify(reservePoiTypeRepository).findByReserveIdOrderBySystemDefaultDescNameAsc(reserve.getId());
    }

    @Test
    void createReservePoi_rejectsCoordinatesOutsideReserve() {
        ReservePoiType type = new ReservePoiType(reserve, "Entrance", true);
        type.setId(33L);

        ReservePoiRequestDto request = new ReservePoiRequestDto();
        request.setTypeId(type.getId());
        request.setName("Forest Gate");
        request.setLatitude(40.0);
        request.setLongitude(35.1);

        when(currentUserService.getRequiredManager()).thenReturn(manager);
        when(reserveRepository.findById(reserve.getId())).thenReturn(Optional.of(reserve));
        when(reservePoiTypeRepository.existsByReserveId(reserve.getId())).thenReturn(true);
        when(reservePoiTypeRepository.findByIdAndReserveId(type.getId(), reserve.getId())).thenReturn(Optional.of(type));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> reservePoiService.createReservePoi(reserve.getId(), request));

        assertEquals("POI location is outside reserve boundaries", error.getMessage());
        verify(reservePoiRepository, never()).save(any());
    }

    @Test
    void createReservePoiType_savesCustomTypeForReserve() {
        ReservePoiTypeRequestDto request = new ReservePoiTypeRequestDto();
        request.setName("Shuttle Stop");

        when(currentUserService.getRequiredManager()).thenReturn(manager);
        when(reserveRepository.findById(reserve.getId())).thenReturn(Optional.of(reserve));
        when(reservePoiTypeRepository.existsByReserveId(reserve.getId())).thenReturn(true);
        when(reservePoiTypeRepository.findByReserveIdAndNameIgnoreCase(reserve.getId(), "Shuttle Stop")).thenReturn(Optional.empty());
        when(reservePoiTypeRepository.save(any(ReservePoiType.class))).thenAnswer(invocation -> {
            ReservePoiType saved = invocation.getArgument(0);
            saved.setId(88L);
            return saved;
        });

        reservePoiService.createReservePoiType(reserve.getId(), request);

        ArgumentCaptor<ReservePoiType> captor = ArgumentCaptor.forClass(ReservePoiType.class);
        verify(reservePoiTypeRepository).save(captor.capture());
        assertEquals("Shuttle Stop", captor.getValue().getName());
        assertEquals(reserve.getId(), captor.getValue().getReserve().getId());
    }

    @Test
    void deleteReservePoiType_rejectsDefaultTypeDeletion() {
        ReservePoiType defaultType = new ReservePoiType(reserve, "Entrance", true);
        defaultType.setId(5L);

        when(currentUserService.getRequiredManager()).thenReturn(manager);
        when(reserveRepository.findById(reserve.getId())).thenReturn(Optional.of(reserve));
        when(reservePoiTypeRepository.existsByReserveId(reserve.getId())).thenReturn(true);
        when(reservePoiTypeRepository.findByIdAndReserveId(defaultType.getId(), reserve.getId())).thenReturn(Optional.of(defaultType));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> reservePoiService.deleteReservePoiType(reserve.getId(), defaultType.getId()));

        assertEquals("Default POI types cannot be deleted", error.getMessage());
    }
}
