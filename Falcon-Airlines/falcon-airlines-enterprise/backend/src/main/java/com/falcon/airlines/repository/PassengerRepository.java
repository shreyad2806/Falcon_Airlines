package com.falcon.airlines.repository;

import com.falcon.airlines.entity.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PassengerRepository extends JpaRepository<Passenger, Long>, JpaSpecificationExecutor<Passenger> {

    Optional<Passenger> findByPassportNumber(String passportNumber);

    Optional<Passenger> findByPassportNumberAndIdNot(String passportNumber, Long id);

    Optional<Passenger> findByEmail(String email);

    Optional<Passenger> findByEmailAndIdNot(String email, Long id);

    boolean existsByUserId(Long userId);
}
