package com.falcon.airlines.repository;

import com.falcon.airlines.entity.SeatHold;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeatHoldRepository extends JpaRepository<SeatHold, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sh FROM SeatHold sh WHERE sh.seat.id = :seatId AND sh.flight.id = :flightId AND sh.status = 'HELD' AND sh.isDeleted = false")
    Optional<SeatHold> findActiveHoldForSeat(@Param("seatId") Long seatId, @Param("flightId") Long flightId);

    List<SeatHold> findByFlightIdAndStatusAndIsDeletedFalse(Long flightId, String status);

    List<SeatHold> findByBookingIdAndStatusAndIsDeletedFalse(Long bookingId, String status);

    @Query("SELECT sh FROM SeatHold sh WHERE sh.holdExpiresAt < CURRENT_TIMESTAMP AND sh.status = 'HELD' AND sh.isDeleted = false")
    List<SeatHold> findExpiredHolds();

    boolean existsBySeatIdAndFlightIdAndStatusAndIsDeletedFalse(Long seatId, Long flightId, String status);
}
