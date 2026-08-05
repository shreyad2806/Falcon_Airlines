package com.falcon.airlines.repository;

import com.falcon.airlines.entity.Aircraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AircraftRepository extends JpaRepository<Aircraft, Long>, JpaSpecificationExecutor<Aircraft> {
}
