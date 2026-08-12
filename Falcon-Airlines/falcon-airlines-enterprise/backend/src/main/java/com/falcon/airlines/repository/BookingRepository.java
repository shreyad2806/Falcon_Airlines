package com.falcon.airlines.repository;

import com.falcon.airlines.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    Optional<Booking> findByBookingReference(String bookingReference);

    Page<Booking> findByCustomerId(Long customerId, Pageable pageable);

    Page<Booking> findByStatus(com.falcon.airlines.enums.BookingStatus status, Pageable pageable);

    boolean existsByBookingReference(String bookingReference);
}
