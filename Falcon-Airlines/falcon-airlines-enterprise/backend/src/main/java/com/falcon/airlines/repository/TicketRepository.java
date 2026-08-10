package com.falcon.airlines.repository;

import com.falcon.airlines.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    List<Ticket> findByBookingId(Long bookingId);

    List<Ticket> findByPassengerId(Long passengerId);

    boolean existsByPassengerId(Long passengerId);
}
