package com.falcon.airlines.repository;

import com.falcon.airlines.entity.BoardingPass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardingPassRepository extends JpaRepository<BoardingPass, Long> {
    
    Optional<BoardingPass> findByBoardingPassNumber(String boardingPassNumber);
    
    List<BoardingPass> findByTicketId(Long ticketId);
    
    List<BoardingPass> findByBookingId(Long bookingId);
    
    List<BoardingPass> findByPassengerId(Long passengerId);
    
    List<BoardingPass> findByFlightId(Long flightId);
}
