package com.noam.fleetcommand;

import com.noam.fleetcommand.admin.AdminService;
import com.noam.fleetcommand.admin.dto.AdminAssignReserveDto;
import com.noam.fleetcommand.admin.dto.AdminReserveSummaryDto;
import com.noam.fleetcommand.reserves.Reserve;
import com.noam.fleetcommand.reserves.ReserveRepository;
import com.noam.fleetcommand.reserves.ReserveService;
import com.noam.fleetcommand.security.UserPrincipal;
import com.noam.fleetcommand.users.User;
import com.noam.fleetcommand.users.UserRepository;
import com.noam.fleetcommand.users.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class FleetcommandApplicationTests {

    @Autowired
    private ReserveRepository reserveRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReserveService reserveService;

    @Autowired
    private AdminService adminService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void seedsStaticReserveCatalogOnStartup() {
        assertEquals(30, reserveRepository.count(), "Expected exactly 30 seeded nature reserves");
        assertTrue(
                reserveRepository.findAll().stream().allMatch(reserve -> reserve.getArea() != null),
                "Expected seeded reserves to include geographic area data"
        );
    }

    @Test
    void adminUserIsSeededAfterReset() {
        User admin = userRepository.findByEmail("admin@reserve.local").orElseThrow();
        assertEquals(UserRole.ADMIN, admin.getRole());
        assertEquals("System Admin", admin.getName());
        assertEquals("ChangeMe123!", admin.getPasswordHash());
    }

    @Test
    @Transactional
    void publicReservesOnlyIncludeAssignedCatalogEntries() {
        int baselinePublicReserves = reserveService.getPublicReserves().size();

        User manager = new User();
        manager.setName("Manager One");
        manager.setEmail("manager.one@example.com");
        manager.setPasswordHash("secret");
        manager.setRole(UserRole.MANAGER);
        User savedManager = userRepository.save(manager);

        Reserve reserve = reserveRepository.findAllByOrderByNameAsc().stream()
                .filter(item -> item.getManager() == null)
                .findFirst()
                .orElseThrow();
        reserve.setManager(savedManager);
        reserveRepository.save(reserve);

        assertEquals(baselinePublicReserves + 1, reserveService.getPublicReserves().size());
        assertTrue(
                reserveService.getPublicReserves().stream().anyMatch(item -> item.getId().equals(reserve.getId()) && item.getManagerUserId() != null),
                "Expected the assigned reserve to become visible in the public reserve list"
        );
    }

    @Test
    void seededCatalogEntriesIncludeRealAreaBounds() {
        Reserve reserve = reserveRepository.findAllByOrderByNameAsc().stream().findFirst().orElseThrow();

        assertNotNull(reserve.getArea());
        assertTrue(reserve.getArea().getMinLatitude() < reserve.getArea().getMaxLatitude());
        assertTrue(reserve.getArea().getMinLongitude() < reserve.getArea().getMaxLongitude());
        assertNotNull(reserve.getCenterLatitude());
        assertNotNull(reserve.getCenterLongitude());
        assertNotNull(reserve.getRegion());
    }

    @Test
    @Transactional
    void adminCanFilterAndAssignStaticReserveInventory() {
        User admin = userRepository.findByEmail("admin@reserve.local").orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new UserPrincipal(admin.getId(), admin.getEmail(), admin.getRole().name()),
                        null,
                        java.util.List.of()
                )
        );

        User manager = new User();
        manager.setName("Manager Two");
        manager.setEmail("manager.two@example.com");
        manager.setPasswordHash("secret");
        manager.setRole(UserRole.MANAGER);
        User savedManager = userRepository.save(manager);

        Reserve reserve = reserveRepository.findByNameIgnoreCase("Ein Gedi Nature Reserve").orElseThrow();
        AdminReserveSummaryDto assigned = adminService.assignReserve(
                reserve.getId(),
                new AdminAssignReserveDto(savedManager.getId(), null)
        );

        assertEquals(savedManager.getId(), assigned.managerUserId());
        assertTrue(assigned.active());

        assertEquals(1, adminService.getReserveSummaries(null, savedManager.getId(), null, null, true).size());
        assertFalse(adminService.getReserveSummaries(null, null, "Dead Sea", null, false).isEmpty());
    }
}
