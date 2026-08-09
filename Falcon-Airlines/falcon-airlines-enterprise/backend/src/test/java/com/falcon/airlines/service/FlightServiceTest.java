package com.falcon.airlines.service;

import com.falcon.airlines.common.BaseUnitTest;
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
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FlightServiceTest extends BaseUnitTest {

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private AirportRepository airportRepository;

    @Mock
    private AircraftRepository aircraftRepository;

    @Mock
    private FlightMapper flightMapper;

    @InjectMocks
    private FlightService flightService;

    private static final Instant NOW = Instant.parse("2026-08-10T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-08-10T14:00:00Z");
    private static final Long ORIGIN_ID = 1L;
    private static final Long DEST_ID = 2L;
    private static final Long AIRCRAFT_ID = 3L;

    private FlightRequest buildValidRequest() {
        FlightRequest r = new FlightRequest();
        r.setFlightNumber("F101");
        r.setOriginAirportId(ORIGIN_ID);
        r.setDestinationAirportId(DEST_ID);
        r.setAircraftId(AIRCRAFT_ID);
        r.setScheduledDeparture(NOW);
        r.setScheduledArrival(LATER);
        r.setStatus(FlightStatus.SCHEDULED);
        return r;
    }

    private Airport airport(Long id) {
        Airport a = new Airport();
        a.setId(id);
        a.setIataCode("AAA" + id);
        a.setIsActive(true);
        return a;
    }

    private Aircraft aircraft() {
        Aircraft a = new Aircraft();
        a.setId(AIRCRAFT_ID);
        a.setRegistrationNumber("REG-1");
        return a;
    }

    private Flight mappedFlight(FlightRequest r) {
        Flight f = new Flight();
        f.setFlightNumber(r.getFlightNumber());
        f.setScheduledDeparture(r.getScheduledDeparture());
        f.setScheduledArrival(r.getScheduledArrival());
        f.setStatus(r.getStatus());
        f.setIsActive(true);
        return f;
    }

    private FlightResponse buildResponse() {
        FlightResponse r = new FlightResponse();
        r.setId(100L);
        r.setFlightNumber("F101");
        return r;
    }

    @Test
    void createFlight_success() {
        FlightRequest r = buildValidRequest();
        when(airportRepository.findById(ORIGIN_ID)).thenReturn(Optional.of(airport(ORIGIN_ID)));
        when(airportRepository.findById(DEST_ID)).thenReturn(Optional.of(airport(DEST_ID)));
        when(aircraftRepository.findById(AIRCRAFT_ID)).thenReturn(Optional.of(aircraft()));
        when(flightMapper.toEntity(r)).thenReturn(mappedFlight(r));
        when(flightRepository.findByFlightNumberAndScheduledDeparture(r.getFlightNumber(), r.getScheduledDeparture()))
                .thenReturn(Optional.empty());
        when(flightRepository.findByAircraftIdAndScheduledArrivalGreaterThanAndScheduledDepartureLessThanAndIsActiveTrue(
                eq(AIRCRAFT_ID), any(Instant.class), any(Instant.class))).thenReturn(List.of());
        when(flightRepository.save(any(Flight.class))).thenAnswer(i -> i.getArgument(0));
        when(flightMapper.toResponse(any(Flight.class))).thenReturn(buildResponse());

        FlightResponse response = flightService.createFlight(r);

        assertNotNull(response);
        assertEquals("F101", response.getFlightNumber());
    }

    @Test
    void createFlight_originAirportNotFound() {
        FlightRequest r = buildValidRequest();
        when(airportRepository.findById(ORIGIN_ID)).thenReturn(Optional.empty());

        BaseException ex = assertThrows(BaseException.class, () -> flightService.createFlight(r));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("AIRPORT_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    void createFlight_destinationAirportNotFound() {
        FlightRequest r = buildValidRequest();
        when(airportRepository.findById(ORIGIN_ID)).thenReturn(Optional.of(airport(ORIGIN_ID)));
        when(airportRepository.findById(DEST_ID)).thenReturn(Optional.empty());

        BaseException ex = assertThrows(BaseException.class, () -> flightService.createFlight(r));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("AIRPORT_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    void createFlight_aircraftNotFound() {
        FlightRequest r = buildValidRequest();
        when(airportRepository.findById(ORIGIN_ID)).thenReturn(Optional.of(airport(ORIGIN_ID)));
        when(airportRepository.findById(DEST_ID)).thenReturn(Optional.of(airport(DEST_ID)));
        when(aircraftRepository.findById(AIRCRAFT_ID)).thenReturn(Optional.empty());

        BaseException ex = assertThrows(BaseException.class, () -> flightService.createFlight(r));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("AIRCRAFT_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    void createFlight_sameOriginAndDestination() {
        FlightRequest r = buildValidRequest();
        r.setDestinationAirportId(ORIGIN_ID);

        BaseException ex = assertThrows(BaseException.class, () -> flightService.createFlight(r));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("FLIGHT_SAME_AIRPORT", ex.getErrorCode());
    }

    @Test
    void createFlight_departureAfterArrival() {
        FlightRequest r = buildValidRequest();
        r.setScheduledDeparture(LATER);
        r.setScheduledArrival(NOW);

        BaseException ex = assertThrows(BaseException.class, () -> flightService.createFlight(r));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("FLIGHT_INVALID_SCHEDULE", ex.getErrorCode());
    }

    @Test
    void createFlight_departureEqualsArrival() {
        FlightRequest r = buildValidRequest();
        r.setScheduledArrival(NOW);

        BaseException ex = assertThrows(BaseException.class, () -> flightService.createFlight(r));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("FLIGHT_SAME_TIME", ex.getErrorCode());
    }

    @Test
    void createFlight_duplicateFlight() {
        FlightRequest r = buildValidRequest();
        when(airportRepository.findById(ORIGIN_ID)).thenReturn(Optional.of(airport(ORIGIN_ID)));
        when(airportRepository.findById(DEST_ID)).thenReturn(Optional.of(airport(DEST_ID)));
        when(aircraftRepository.findById(AIRCRAFT_ID)).thenReturn(Optional.of(aircraft()));
        when(flightMapper.toEntity(r)).thenReturn(mappedFlight(r));
        when(flightRepository.findByFlightNumberAndScheduledDeparture(r.getFlightNumber(), r.getScheduledDeparture()))
                .thenReturn(Optional.of(new Flight()));

        BaseException ex = assertThrows(BaseException.class, () -> flightService.createFlight(r));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("FLIGHT_DUPLICATE", ex.getErrorCode());
    }

    @Test
    void createFlight_aircraftOverlap() {
        FlightRequest r = buildValidRequest();
        when(airportRepository.findById(ORIGIN_ID)).thenReturn(Optional.of(airport(ORIGIN_ID)));
        when(airportRepository.findById(DEST_ID)).thenReturn(Optional.of(airport(DEST_ID)));
        when(aircraftRepository.findById(AIRCRAFT_ID)).thenReturn(Optional.of(aircraft()));
        when(flightMapper.toEntity(r)).thenReturn(mappedFlight(r));
        when(flightRepository.findByFlightNumberAndScheduledDeparture(r.getFlightNumber(), r.getScheduledDeparture()))
                .thenReturn(Optional.empty());
        when(flightRepository.findByAircraftIdAndScheduledArrivalGreaterThanAndScheduledDepartureLessThanAndIsActiveTrue(
                eq(AIRCRAFT_ID), any(Instant.class), any(Instant.class))).thenReturn(List.of(new Flight()));

        BaseException ex = assertThrows(BaseException.class, () -> flightService.createFlight(r));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("AIRCRAFT_SCHEDULE_CONFLICT", ex.getErrorCode());
    }

    @Test
    void getFlightById_notFound() {
        when(flightRepository.findById(99L)).thenReturn(Optional.empty());

        BaseException ex = assertThrows(BaseException.class, () -> flightService.getFlightById(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("FLIGHT_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    void getFlightById_success() {
        Flight f = new Flight();
        f.setId(99L);
        when(flightRepository.findById(99L)).thenReturn(Optional.of(f));
        when(flightMapper.toResponse(f)).thenReturn(buildResponse());

        FlightResponse response = flightService.getFlightById(99L);

        assertNotNull(response);
        assertEquals("F101", response.getFlightNumber());
    }

    @Test
    void updateFlight_success() {
        FlightRequest r = buildValidRequest();
        Flight existing = new Flight();
        existing.setId(1L);
        existing.setIsActive(true);

        when(flightRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(airportRepository.findById(ORIGIN_ID)).thenReturn(Optional.of(airport(ORIGIN_ID)));
        when(airportRepository.findById(DEST_ID)).thenReturn(Optional.of(airport(DEST_ID)));
        when(aircraftRepository.findById(AIRCRAFT_ID)).thenReturn(Optional.of(aircraft()));
        when(flightMapper.toEntity(r)).thenReturn(mappedFlight(r));
        when(flightRepository.findByFlightNumberAndScheduledDepartureAndIdNot(r.getFlightNumber(), r.getScheduledDeparture(), 1L))
                .thenReturn(Optional.empty());
        when(flightRepository.findByAircraftIdAndScheduledArrivalGreaterThanAndScheduledDepartureLessThanAndIsActiveTrueAndIdNot(
                eq(AIRCRAFT_ID), any(Instant.class), any(Instant.class), eq(1L))).thenReturn(List.of());
        when(flightRepository.save(any(Flight.class))).thenAnswer(i -> i.getArgument(0));
        when(flightMapper.toResponse(any(Flight.class))).thenReturn(buildResponse());

        FlightResponse response = flightService.updateFlight(1L, r);

        assertNotNull(response);
        verify(flightRepository).save(any(Flight.class));
    }

    @Test
    void updateFlight_invalidUpdate_sameAirport() {
        FlightRequest r = buildValidRequest();
        r.setDestinationAirportId(ORIGIN_ID);
        Flight existing = new Flight();
        existing.setId(1L);

        when(flightRepository.findById(1L)).thenReturn(Optional.of(existing));

        BaseException ex = assertThrows(BaseException.class, () -> flightService.updateFlight(1L, r));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void deleteFlight_success() {
        Flight existing = new Flight();
        existing.setId(1L);
        existing.setIsActive(true);
        when(flightRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(flightRepository.save(any(Flight.class))).thenAnswer(i -> i.getArgument(0));

        flightService.deleteFlight(1L);

        assertTrue(existing.isDeleted());
        assertFalse(existing.getIsActive());
        assertNotNull(existing.getDeletedAt());
    }

    @Test
    void searchFlights_filteringAndPagination() {
        Flight f = new Flight();
        f.setId(1L);
        Page<Flight> page = new PageImpl<>(List.of(f), PageRequest.of(0, 10), 1);

        when(flightRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
        when(flightMapper.toResponse(f)).thenReturn(buildResponse());

        Page<FlightResponse> result = flightService.searchFlights("F101", "DEL", "BOM",
                null, FlightStatus.SCHEDULED, NOW, LATER, true, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }
}
