package com.falcon.airlines.provider;

import com.falcon.airlines.entity.Flight;
import com.falcon.airlines.entity.Seat;
import com.falcon.airlines.enums.FlightStatus;
import com.falcon.airlines.repository.FlightRepository;
import com.falcon.airlines.repository.SeatAllocationRepository;
import com.falcon.airlines.repository.SeatRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Database-backed flight provider. Always available as fallback.
 * Provides Falcon's own flight records with real seat inventory.
 */
@Slf4j
public class DatabaseFlightDataProvider implements FlightDataProvider {

    private final FlightRepository flightRepository;
    private final SeatRepository seatRepository;
    private final SeatAllocationRepository seatAllocationRepository;

    public DatabaseFlightDataProvider(FlightRepository flightRepository,
                                       SeatRepository seatRepository,
                                       SeatAllocationRepository seatAllocationRepository) {
        this.flightRepository = flightRepository;
        this.seatRepository = seatRepository;
        this.seatAllocationRepository = seatAllocationRepository;
    }

    @Override
    public List<NormalizedFlight> searchFlights(FlightSearchRequest request) {
        log.debug("DatabaseFlightProvider: searching flights {} -> {} on {}",
                request.getOrigin(), request.getDestination(), request.getDepartureDate());

        Specification<Flight> spec = buildSearchSpec(request);
        List<Flight> flights = flightRepository.findAll(spec);

        List<NormalizedFlight> results = new ArrayList<>();
        for (Flight f : flights) {
            results.add(normalize(f));
        }
        return results;
    }

    @Override
    public Optional<NormalizedFlight> getFlightStatus(String flightNumber, LocalDate date) {
        return flightRepository.findByFlightNumber(flightNumber)
                .map(this::normalize);
    }

    @Override
    public boolean isAvailable() {
        return true;  // Database is always available
    }

    @Override
    public String getProviderName() {
        return "Falcon Database";
    }

    /**
     * Convert a Falcon Flight entity into a NormalizedFlight.
     */
    private NormalizedFlight normalize(Flight flight) {
        int totalSeats = 0;
        int availableSeats = 0;

        try {
            if (flight.getAircraft() != null) {
                List<Seat> allSeats = seatRepository.findByAircraftIdAndIsActiveTrue(flight.getAircraft().getId());
                totalSeats = allSeats.size();
                List<Seat> available = seatRepository.findAvailableSeatsForFlight(flight.getAircraft().getId(), flight.getId());
                availableSeats = available.size();
            }
        } catch (Exception e) {
            log.warn("Could not calculate seat inventory for flight {}: {}", flight.getFlightNumber(), e.getMessage());
        }

        boolean departed = flight.getScheduledDeparture().isBefore(Instant.now());
        boolean cancelled = flight.getStatus() == FlightStatus.CANCELLED;
        boolean soldOut = availableSeats == 0 && totalSeats > 0;
        boolean bookable = !departed && !cancelled && !soldOut && flight.getIsActive();

        String reason = null;
        if (departed) reason = "This flight has already departed";
        else if (cancelled) reason = "This flight has been cancelled";
        else if (soldOut) reason = "No seats available";

        Duration duration = Duration.between(flight.getScheduledDeparture(), flight.getScheduledArrival());
        String status = mapStatus(flight.getStatus(), departed);

        return NormalizedFlight.builder()
                .internalId(flight.getId())
                .flightNumber(flight.getFlightNumber())
                .airlineName("Falcon Airlines")
                .airlineCode("FA")
                .originCode(flight.getOriginAirport().getIataCode())
                .originName(flight.getOriginAirport().getName())
                .originCity(flight.getOriginAirport().getCity())
                .destinationCode(flight.getDestinationAirport().getIataCode())
                .destinationName(flight.getDestinationAirport().getName())
                .destinationCity(flight.getDestinationAirport().getCity())
                .scheduledDeparture(flight.getScheduledDeparture())
                .scheduledArrival(flight.getScheduledArrival())
                .status(status)
                .duration(duration)
                .aircraftType(flight.getAircraft() != null ? flight.getAircraft().getModel() : null)
                .aircraftRegistration(flight.getAircraft() != null ? flight.getAircraft().getRegistrationNumber() : null)
                .inventoryManagedByFalcon(true)
                .totalSeats(totalSeats)
                .availableSeats(availableSeats)
                .basePrice(flight.getBasePrice())
                .currency(flight.getCurrency() != null ? flight.getCurrency() : "INR")
                .bookable(bookable)
                .bookingUnavailableReason(reason)
                .terminal(flight.getTerminal())
                .gate(flight.getGate())
                .build();
    }

    private String mapStatus(FlightStatus fs, boolean departed) {
        if (fs == null) return "UNKNOWN";
        return switch (fs) {
            case SCHEDULED -> departed ? "DEPARTED" : "SCHEDULED";
            case DELAYED -> "DELAYED";
            case CANCELLED -> "CANCELLED";
            case BOARDING -> "BOARDING";
            case DEPARTED -> "DEPARTED";
            case ARRIVED -> "LANDED";
        };
    }

    private Specification<Flight> buildSearchSpec(FlightSearchRequest request) {
        Specification<Flight> spec = (root, query, cb) -> cb.equal(root.get("isDeleted"), false);

        if (request.getFlightNumber() != null && !request.getFlightNumber().isBlank()) {
            String like = "%" + request.getFlightNumber().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("flightNumber")), like));
        }

        if (request.getOrigin() != null && !request.getOrigin().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("originAirport").get("iataCode"), request.getOrigin().toUpperCase()));
        }

        if (request.getDestination() != null && !request.getDestination().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("destinationAirport").get("iataCode"), request.getDestination().toUpperCase()));
        }

        if (request.getDepartureDate() != null) {
            Instant dayStart = request.getDepartureDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant dayEnd = request.getDepartureDate().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            spec = spec.and((root, query, cb) ->
                    cb.between(root.get("scheduledDeparture"), dayStart, dayEnd));
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            try {
                FlightStatus fs = FlightStatus.valueOf(request.getStatus().toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), fs));
            } catch (IllegalArgumentException ignored) {}
        }

        return spec;
    }
}
