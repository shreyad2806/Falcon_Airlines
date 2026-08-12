package com.falcon.airlines.repository;

import com.falcon.airlines.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    List<Ticket> findByBookingId(Long bookingId);

    List<Ticket> findByPassengerId(Long passengerId);

    List<Ticket> findByFlightId(Long flightId);

    boolean existsByPassengerId(Long passengerId);

    @Query("SELECT sa FROM SeatAllocation sa WHERE sa.ticket.id = :ticketId")
    Optional<com.falcon.airlines.entity.SeatAllocation> findSeatAllocationByTicketId(@Param("ticketId") Long ticketId);
}
