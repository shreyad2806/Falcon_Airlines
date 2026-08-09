package com.falcon.airlines.service;

import com.falcon.airlines.dto.request.FlightRequest;
import com.falcon.airlines.dto.response.FlightResponse;
import com.falcon.airlines.entity.Aircraft;
import com.falcon.airlines.entity.Airport;
import com.falcon.airlines.entity.Flight;
import com.falcon.airlines.enums.FlightStatus;
import com.falcon.airlines.exception.BaseException;
import com.falcon.airlines.mapper.FlightMapper;
import com.falcon.airlines.repository.AircraftRepository;
import com.falcon.airlines.repository.AirportRepository;
import com.falcon.airlines.repository.FlightRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Application service for flight management.
 */
@Service
@Transactional
public class FlightService {

    private final FlightRepository flightRepository;
    private final AirportRepository airportRepository;
    private final AircraftRepository aircraftRepository;
    private final FlightMapper flightMapper;

    public FlightService(FlightRepository flightRepository,
                         AirportRepository airportRepository,
                         AircraftRepository aircraftRepository,
                         FlightMapper flightMapper) {
        this.flightRepository = flightRepository;
        this.airportRepository = airportRepository;
        this.aircraftRepository = aircraftRepository;
        this.flightMapper = flightMapper;
    }

    public FlightResponse createFlight(FlightRequest request) {
        validateFlightSchedule(request);

        Airport origin = resolveAirport(request.getOriginAirportId(), "Origin");
        Airport destination = resolveAirport(request.getDestinationAirportId(), "Destination");
        Aircraft aircraft = resolveAircraft(request.getAircraftId());

        Flight flight = flightMapper.toEntity(request);
        flight.setOriginAirport(origin);
        flight.setDestinationAirport(destination);
        flight.setAircraft(aircraft);
        flight.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        checkDuplicateFlight(flight.getFlightNumber(), flight.getScheduledDeparture(), null);
        checkAircraftOverlap(request.getAircraftId(), flight.getScheduledDeparture(),
                flight.getScheduledArrival(), null);

        Flight saved = flightRepository.save(flight);
        return flightMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public FlightResponse getFlightById(Long id) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new BaseException("Flight not found", HttpStatus.NOT_FOUND, "FLIGHT_NOT_FOUND"));
        return flightMapper.toResponse(flight);
    }

    public FlightResponse updateFlight(Long id, FlightRequest request) {
        Flight existing = flightRepository.findById(id)
                .orElseThrow(() -> new BaseException("Flight not found", HttpStatus.NOT_FOUND, "FLIGHT_NOT_FOUND"));

        validateFlightSchedule(request);

        resolveAirport(request.getOriginAirportId(), "Origin");
        resolveAirport(request.getDestinationAirportId(), "Destination");
        resolveAircraft(request.getAircraftId());

        Flight updated = flightMapper.toEntity(request);
        updated.setId(existing.getId());
        updated.setIsActive(request.getIsActive() != null ? request.getIsActive() : existing.getIsActive());

        checkDuplicateFlight(updated.getFlightNumber(), updated.getScheduledDeparture(), id);
        checkAircraftOverlap(request.getAircraftId(), updated.getScheduledDeparture(),
                updated.getScheduledArrival(), id);

        Flight saved = flightRepository.save(updated);
        return flightMapper.toResponse(saved);
    }

    public void deleteFlight(Long id) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new BaseException("Flight not found", HttpStatus.NOT_FOUND, "FLIGHT_NOT_FOUND"));

        flight.setIsActive(false);
        flight.setDeleted(true);
        flight.setDeletedAt(Instant.now());
        flightRepository.save(flight);
    }

    @Transactional(readOnly = true)
    public Page<FlightResponse> listFlights(Pageable pageable) {
        return searchFlights(null, null, null, null, null, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<FlightResponse> searchFlights(String flightNumber,
                                              String originAirport,
                                              String destinationAirport,
                                              String aircraft,
                                              FlightStatus status,
                                              Instant departureFrom,
                                              Instant departureTo,
                                              Boolean active,
                                              Pageable pageable) {
        Specification<Flight> spec = buildSpecification(flightNumber, originAirport, destinationAirport,
                aircraft, status, departureFrom, departureTo, active);
        return flightRepository.findAll(spec, pageable).map(flightMapper::toResponse);
    }

    private void validateFlightSchedule(FlightRequest request) {
        if (request.getOriginAirportId().equals(request.getDestinationAirportId())) {
            throw new BaseException("Departure and arrival airports must be different", HttpStatus.BAD_REQUEST, "FLIGHT_SAME_AIRPORT");
        }

        if (request.getScheduledDeparture().equals(request.getScheduledArrival())) {
            throw new BaseException("Departure and arrival times cannot be the same", HttpStatus.BAD_REQUEST, "FLIGHT_SAME_TIME");
        }

        if (request.getScheduledDeparture().isAfter(request.getScheduledArrival())) {
            throw new BaseException("Departure time must be before arrival time", HttpStatus.BAD_REQUEST, "FLIGHT_INVALID_SCHEDULE");
        }
    }

    private Airport resolveAirport(Long id, String label) {
        return airportRepository.findById(id)
                .orElseThrow(() -> new BaseException(label + " airport not found", HttpStatus.NOT_FOUND, "AIRPORT_NOT_FOUND"));
    }

    private Aircraft resolveAircraft(Long id) {
        return aircraftRepository.findById(id)
                .orElseThrow(() -> new BaseException("Aircraft not found", HttpStatus.NOT_FOUND, "AIRCRAFT_NOT_FOUND"));
    }

    private void checkDuplicateFlight(String flightNumber, Instant scheduledDeparture, Long excludeId) {
        Optional<Flight> duplicate;
        if (excludeId == null) {
            duplicate = flightRepository.findByFlightNumberAndScheduledDeparture(flightNumber, scheduledDeparture);
        } else {
            duplicate = flightRepository.findByFlightNumberAndScheduledDepartureAndIdNot(flightNumber, scheduledDeparture, excludeId);
        }

        duplicate.ifPresent(f -> {
            throw new BaseException("Duplicate flight: a flight with number " + flightNumber + " and the same departure time already exists",
                    HttpStatus.CONFLICT, "FLIGHT_DUPLICATE");
        });
    }

    private void checkAircraftOverlap(Long aircraftId, Instant newDeparture, Instant newArrival, Long excludeId) {
        List<Flight> overlapping;
        if (excludeId == null) {
            overlapping = flightRepository.findByAircraftIdAndScheduledArrivalGreaterThanAndScheduledDepartureLessThanAndIsActiveTrue(
                    aircraftId, newDeparture, newArrival);
        } else {
            overlapping = flightRepository.findByAircraftIdAndScheduledArrivalGreaterThanAndScheduledDepartureLessThanAndIsActiveTrueAndIdNot(
                    aircraftId, newDeparture, newArrival, excludeId);
        }

        if (!overlapping.isEmpty()) {
            throw new BaseException("Aircraft is already assigned to an overlapping flight", HttpStatus.CONFLICT, "AIRCRAFT_SCHEDULE_CONFLICT");
        }
    }

    private Specification<Flight> buildSpecification(String flightNumber,
                                                     String originAirport,
                                                     String destinationAirport,
                                                     String aircraft,
                                                     FlightStatus status,
                                                     Instant departureFrom,
                                                     Instant departureTo,
                                                     Boolean active) {
        Specification<Flight> spec = (root, query, cb) -> cb.equal(root.get("isDeleted"), false);

        if (flightNumber != null && !flightNumber.isBlank()) {
            String like = "%" + flightNumber.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("flightNumber")), like));
        }

        if (originAirport != null && !originAirport.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("originAirport").get("iataCode"), originAirport));
        }

        if (destinationAirport != null && !destinationAirport.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("destinationAirport").get("iataCode"), destinationAirport));
        }

        if (aircraft != null && !aircraft.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("aircraft").get("registrationNumber"), aircraft));
        }

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        if (departureFrom != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("scheduledDeparture"), departureFrom));
        }

        if (departureTo != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("scheduledDeparture"), departureTo));
        }

        if (active != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isActive"), active));
        }

        return spec;
    }
}
