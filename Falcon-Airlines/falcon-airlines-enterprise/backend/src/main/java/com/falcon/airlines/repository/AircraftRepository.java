package com.falcon.airlines.repository;

import com.falcon.airlines.entity.Aircraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AircraftRepository extends JpaRepository<Aircraft, Long>, JpaSpecificationExecutor<Aircraft> {

    Optional<Aircraft> findByRegistrationNumber(String registrationNumber);

    Optional<Aircraft> findByRegistrationNumberAndIdNot(String registrationNumber, Long id);
}
