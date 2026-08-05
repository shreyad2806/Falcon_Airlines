package com.falcon.airlines.repository;

import com.falcon.airlines.entity.BookingPassenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BookingPassengerRepository extends JpaRepository<BookingPassenger, Long>, JpaSpecificationExecutor<BookingPassenger> {
}
