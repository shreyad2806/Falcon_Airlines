package com.falcon.airlines.repository;

import com.falcon.airlines.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long>, JpaSpecificationExecutor<Seat> {

    List<Seat> findByAircraftId(Long aircraftId);

    List<Seat> findByAircraftIdAndIsActiveTrue(Long aircraftId);

    Optional<Seat> findByAircraftIdAndSeatNumber(Long aircraftId, String seatNumber);

    @Query("SELECT s FROM Seat s WHERE s.aircraft.id = :aircraftId AND s.isActive = true AND s.id NOT IN " +
           "(SELECT sa.seat.id FROM SeatAllocation sa WHERE sa.flight.id = :flightId AND sa.isDeleted = false)")
    List<Seat> findAvailableSeatsForFlight(@Param("aircraftId") Long aircraftId, @Param("flightId") Long flightId);

    @Query("SELECT COUNT(sa) > 0 FROM SeatAllocation sa WHERE sa.seat.id = :seatId AND sa.flight.id = :flightId AND sa.isDeleted = false")
    boolean isSeatAllocatedForFlight(@Param("seatId") Long seatId, @Param("flightId") Long flightId);
}
