package com.falcon.airlines.repository;

import com.falcon.airlines.entity.SeatAllocation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SeatAllocationRepository extends JpaRepository<SeatAllocation, Long>, JpaSpecificationExecutor<SeatAllocation> {

    Optional<SeatAllocation> findBySeatIdAndFlightId(Long seatId, Long flightId);

    Optional<SeatAllocation> findByTicketId(Long ticketId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sa FROM SeatAllocation sa WHERE sa.seat.id = :seatId AND sa.flight.id = :flightId AND sa.isDeleted = false")
    Optional<SeatAllocation> findSeatAllocationForUpdate(@Param("seatId") Long seatId, @Param("flightId") Long flightId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sa FROM SeatAllocation sa WHERE sa.ticket.id = :ticketId AND sa.isDeleted = false")
    Optional<SeatAllocation> findSeatAllocationByTicketIdForUpdate(@Param("ticketId") Long ticketId);

    boolean existsBySeatIdAndFlightId(Long seatId, Long flightId);

    boolean existsByTicketId(Long ticketId);

    long countByFlightId(Long flightId);
}
