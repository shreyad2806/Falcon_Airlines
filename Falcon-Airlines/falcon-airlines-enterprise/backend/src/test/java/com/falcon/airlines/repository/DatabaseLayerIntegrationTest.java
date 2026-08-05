package com.falcon.airlines.repository;

import com.falcon.airlines.common.BaseIntegrationTest;
import com.falcon.airlines.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class DatabaseLayerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AirportRepository airportRepository;
    @Autowired
    private AircraftRepository aircraftRepository;
    @Autowired
    private FlightRepository flightRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRoleRepository userRoleRepository;
    @Autowired
    private PermissionRepository permissionRepository;
    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Test
    void seedDataIsPresent() {
        assertThat(airportRepository.count()).isEqualTo(6);
        assertThat(aircraftRepository.count()).isEqualTo(3);
        assertThat(flightRepository.count()).isEqualTo(6);
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(roleRepository.count()).isEqualTo(3);
        assertThat(permissionRepository.count()).isEqualTo(15);
        assertThat(userRoleRepository.count()).isEqualTo(1);
        assertThat(rolePermissionRepository.count()).isEqualTo(15);
    }

    @Test
    void flightHasLazyAirportAndAircraft() {
        Flight flight = flightRepository.findAll().getFirst();

        assertThat(flight.getFlightNumber()).isNotBlank();
        assertThat(flight.getOriginAirport().getIataCode()).isNotBlank();
        assertThat(flight.getDestinationAirport().getIataCode()).isNotBlank();
        assertThat(flight.getAircraft().getRegistrationNumber()).isNotBlank();
    }

    @Test
    void adminUserHasRoleAndPermissions() {
        User admin = userRepository.findAll().getFirst();

        assertThat(admin.getEmail()).isEqualTo("admin@falconairlines.com");

        List<UserRole> userRoles = userRoleRepository.findAll().stream()
                .filter(ur -> ur.getUser().getId().equals(admin.getId()))
                .toList();
        assertThat(userRoles).isNotEmpty();

        Role adminRole = userRoles.getFirst().getRole();
        assertThat(adminRole.getName()).isEqualTo("ADMIN");

        List<RolePermission> rolePermissions = rolePermissionRepository.findAll().stream()
                .filter(rp -> rp.getRole().getId().equals(adminRole.getId()))
                .toList();
        assertThat(rolePermissions).hasSize(15);
        assertThat(rolePermissions.getFirst().getPermission().getCode()).isNotBlank();
    }

    @Test
    void airportHasCompositeIndexAndUniqueIata() {
        Airport jfk = airportRepository.findAll().stream()
                .filter(a -> "JFK".equals(a.getIataCode()))
                .findFirst()
                .orElseThrow();

        assertThat(jfk.getCity()).isEqualTo("New York");
    }
}
