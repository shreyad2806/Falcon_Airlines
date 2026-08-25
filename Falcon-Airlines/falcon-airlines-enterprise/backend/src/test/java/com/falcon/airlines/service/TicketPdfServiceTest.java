package com.falcon.airlines.service;

import com.falcon.airlines.entity.Airport;
import com.falcon.airlines.entity.Booking;
import com.falcon.airlines.entity.Flight;
import com.falcon.airlines.entity.Passenger;
import com.falcon.airlines.entity.Seat;
import com.falcon.airlines.entity.SeatAllocation;
import com.falcon.airlines.entity.Ticket;
import com.falcon.airlines.enums.FlightStatus;
import com.falcon.airlines.enums.TicketStatus;
import com.falcon.airlines.repository.SeatAllocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketPdfServiceTest {

    @Mock
    private SeatAllocationRepository seatAllocationRepository;

    private TicketPdfService ticketPdfService;

    @BeforeEach
    void setUp() {
        ticketPdfService = new TicketPdfService(seatAllocationRepository);
    }

    @Test
    void generateTicketPdf_success() {
        // Arrange
        Ticket ticket = createTestTicket();
        when(seatAllocationRepository.findByTicketId(anyLong())).thenReturn(Optional.empty());

        // Act
        byte[] pdfBytes = ticketPdfService.generateTicketPdf(ticket);

        // Assert
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes).isNotEmpty();
        // PDF files start with %PDF
        assertThat(new String(pdfBytes, 0, 4)).startsWith("%PDF");
    }

    @Test
    void generateTicketPdf_withSeatAllocation() {
        // Arrange
        Ticket ticket = createTestTicket();
        SeatAllocation seatAllocation = createTestSeatAllocation();
        when(seatAllocationRepository.findByTicketId(anyLong())).thenReturn(Optional.of(seatAllocation));

        // Act
        byte[] pdfBytes = ticketPdfService.generateTicketPdf(ticket);

        // Assert
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes).isNotEmpty();
        assertThat(new String(pdfBytes, 0, 4)).startsWith("%PDF");
    }

    @Test
    void generateTicketPdf_withNullPassenger() {
        // Arrange
        Ticket ticket = createTestTicket();
        ticket.setPassenger(null);
        when(seatAllocationRepository.findByTicketId(anyLong())).thenReturn(Optional.empty());

        // Act
        byte[] pdfBytes = ticketPdfService.generateTicketPdf(ticket);

        // Assert
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes).isNotEmpty();
    }

    @Test
    void generateTicketPdf_withNullFlight() {
        // Arrange
        Ticket ticket = createTestTicket();
        ticket.setFlight(null);
        when(seatAllocationRepository.findByTicketId(anyLong())).thenReturn(Optional.empty());

        // Act
        byte[] pdfBytes = ticketPdfService.generateTicketPdf(ticket);

        // Assert
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes).isNotEmpty();
    }

    @Test
    void generateTicketPdf_withNullBooking() {
        // Arrange
        Ticket ticket = createTestTicket();
        ticket.setBooking(null);
        when(seatAllocationRepository.findByTicketId(anyLong())).thenReturn(Optional.empty());

        // Act
        byte[] pdfBytes = ticketPdfService.generateTicketPdf(ticket);

        // Assert
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes).isNotEmpty();
    }

    private Ticket createTestTicket() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setTicketNumber("TKT123456");
        ticket.setFareBasis("ECONOMY");
        ticket.setFare(new BigDecimal("299.99"));
        ticket.setTaxes(new BigDecimal("45.50"));
        ticket.setStatus(TicketStatus.ACTIVE);
        ticket.setIssuedAt(Instant.now());

        // Passenger
        Passenger passenger = new Passenger();
        passenger.setId(1L);
        passenger.setFirstName("John");
        passenger.setLastName("Doe");
        ticket.setPassenger(passenger);

        // Booking
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setBookingReference("BK12345");
        ticket.setBooking(booking);

        // Flight
        Flight flight = new Flight();
        flight.setId(1L);
        flight.setFlightNumber("FA123");
        flight.setStatus(FlightStatus.SCHEDULED);
        flight.setScheduledDeparture(Instant.now().plusSeconds(86400));
        flight.setScheduledArrival(Instant.now().plusSeconds(172800));
        flight.setTerminal("T1");
        flight.setGate("A12");

        // Origin Airport
        Airport origin = new Airport();
        origin.setIataCode("JFK");
        origin.setName("John F. Kennedy International");
        origin.setCity("New York");
        flight.setOriginAirport(origin);

        // Destination Airport
        Airport destination = new Airport();
        destination.setIataCode("LAX");
        destination.setName("Los Angeles International");
        destination.setCity("Los Angeles");
        flight.setDestinationAirport(destination);

        ticket.setFlight(flight);

        return ticket;
    }

    private SeatAllocation createTestSeatAllocation() {
        SeatAllocation allocation = new SeatAllocation();
        allocation.setId(1L);

        Seat seat = new Seat();
        seat.setId(1L);
        seat.setSeatNumber("12A");
        seat.setSeatClass("ECONOMY");
        allocation.setSeat(seat);

        return allocation;
    }
}
