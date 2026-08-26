package com.falcon.airlines.repository;

import com.falcon.airlines.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByBookingId(Long bookingId);

    Optional<Payment> findByBookingIdAndStatus(Long bookingId, String status);
}
