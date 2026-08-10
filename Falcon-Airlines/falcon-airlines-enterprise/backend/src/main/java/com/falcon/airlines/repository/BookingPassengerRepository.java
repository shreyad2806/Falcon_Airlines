package com.falcon.airlines.repository;

import com.falcon.airlines.entity.BookingPassenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface BookingPassengerRepository extends JpaRepository<BookingPassenger, Long>, JpaSpecificationExecutor<BookingPassenger> {

    List<BookingPassenger> findByPassengerId(Long passengerId);

    boolean existsByPassengerId(Long passengerId);
}
