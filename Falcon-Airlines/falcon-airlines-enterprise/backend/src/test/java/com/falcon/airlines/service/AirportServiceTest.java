package com.falcon.airlines.service;

import com.falcon.airlines.common.BaseUnitTest;
import com.falcon.airlines.dto.request.AirportRequest;
import com.falcon.airlines.dto.response.AirportResponse;
import com.falcon.airlines.entity.Airport;
import com.falcon.airlines.exception.BaseException;
import com.falcon.airlines.mapper.AirportMapper;
import com.falcon.airlines.repository.AirportRepository;
import com.falcon.airlines.repository.FlightRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AirportServiceTest extends BaseUnitTest {

    @Mock
    private AirportRepository airportRepository;

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private AirportMapper airportMapper;

    @InjectMocks
    private AirportService airportService;

    private AirportRequest buildRequest() {
        AirportRequest request = new AirportRequest();
        request.setIataCode("NYC");
        request.setIcaoCode("KNYC");
        request.setName("New York Airport");
        request.setCity("New York");
        request.setCountry("US");
        request.setTimeZone("America/New_York");
        request.setLatitude(new BigDecimal("40.7128"));
        request.setLongitude(new BigDecimal("-74.0060"));
        request.setIsActive(true);
        return request;
    }

    private Airport buildAirport(Long id) {
        Airport airport = new Airport();
        airport.setId(id);
        airport.setIataCode("NYC");
        airport.setIcaoCode("KNYC");
        airport.setName("New York Airport");
        airport.setCity("New York");
        airport.setCountry("US");
        airport.setTimeZone("America/New_York");
        airport.setLatitude(new BigDecimal("40.7128"));
        airport.setLongitude(new BigDecimal("-74.0060"));
        airport.setIsActive(true);
        return airport;
    }

    @Test
    void createAirportSuccessfully() {
        AirportRequest request = buildRequest();
        Airport saved = buildAirport(1L);
        AirportResponse response = new AirportResponse();
        response.setId(1L);

        when(airportMapper.toEntity(request)).thenReturn(buildAirport(null));
        when(airportRepository.save(any(Airport.class))).thenReturn(saved);
        when(airportMapper.toResponse(saved)).thenReturn(response);

        AirportResponse result = airportService.createAirport(request);

        assertThat(result.getId()).isEqualTo(1L);
        ArgumentCaptor<Airport> captor = ArgumentCaptor.forClass(Airport.class);
        verify(airportRepository).save(captor.capture());
        assertThat(captor.getValue().getIsActive()).isTrue();
    }

    @Test
    void createAirportDefaultsActiveToTrue() {
        AirportRequest request = buildRequest();
        request.setIsActive(null);
        Airport toSave = buildAirport(null);
        toSave.setIsActive(null);
        Airport saved = buildAirport(1L);
        AirportResponse response = new AirportResponse();
        response.setId(1L);

        when(airportMapper.toEntity(request)).thenReturn(toSave);
        when(airportRepository.save(any(Airport.class))).thenReturn(saved);
        when(airportMapper.toResponse(saved)).thenReturn(response);

        airportService.createAirport(request);

        ArgumentCaptor<Airport> captor = ArgumentCaptor.forClass(Airport.class);
        verify(airportRepository).save(captor.capture());
        assertThat(captor.getValue().getIsActive()).isTrue();
    }

    @Test
    void duplicateIataCodeThrowsConflict() {
        AirportRequest request = buildRequest();
        when(airportRepository.findByIataCode("NYC")).thenReturn(Optional.of(buildAirport(5L)));

        assertThatThrownBy(() -> airportService.createAirport(request))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void duplicateIcaoCodeThrowsConflict() {
        AirportRequest request = buildRequest();
        when(airportRepository.findByIataCode("NYC")).thenReturn(Optional.empty());
        when(airportRepository.findByIcaoCode("KNYC")).thenReturn(Optional.of(buildAirport(5L)));

        assertThatThrownBy(() -> airportService.createAirport(request))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void getAirportByIdSuccessfully() {
        Airport airport = buildAirport(1L);
        AirportResponse response = new AirportResponse();
        response.setId(1L);

        when(airportRepository.findById(1L)).thenReturn(Optional.of(airport));
        when(airportMapper.toResponse(airport)).thenReturn(response);

        AirportResponse result = airportService.getAirportById(1L);
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getAirportByIdNotFound() {
        when(airportRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> airportService.getAirportById(99L))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void updateAirportSuccessfully() {
        AirportRequest request = buildRequest();
        request.setIataCode("LAX");
        request.setIcaoCode("KLAX");
        Airport existing = buildAirport(1L);
        AirportResponse response = new AirportResponse();
        response.setId(1L);

        when(airportRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(airportRepository.findByIataCodeAndIdNot("LAX", 1L)).thenReturn(Optional.empty());
        when(airportRepository.findByIcaoCodeAndIdNot("KLAX", 1L)).thenReturn(Optional.empty());
        when(airportRepository.save(existing)).thenReturn(existing);
        when(airportMapper.toResponse(existing)).thenReturn(response);

        AirportResponse result = airportService.updateAirport(1L, request);
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(existing.getIataCode()).isEqualTo("LAX");
    }

    @Test
    void updateNonexistentAirportThrowsNotFound() {
        AirportRequest request = buildRequest();
        when(airportRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> airportService.updateAirport(99L, request))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void deleteAirportSuccessfully() {
        Airport airport = buildAirport(1L);
        when(airportRepository.findById(1L)).thenReturn(Optional.of(airport));
        when(flightRepository.existsByOriginAirportIdAndIsActiveTrue(1L)).thenReturn(false);
        when(flightRepository.existsByDestinationAirportIdAndIsActiveTrue(1L)).thenReturn(false);

        airportService.deleteAirport(1L);

        assertThat(airport.getIsActive()).isFalse();
        assertThat(airport.isDeleted()).isTrue();
        assertThat(airport.getDeletedAt()).isNotNull();
        verify(airportRepository).save(airport);
    }

    @Test
    void deleteAirportInUseThrowsConflict() {
        Airport airport = buildAirport(1L);
        when(airportRepository.findById(1L)).thenReturn(Optional.of(airport));
        when(flightRepository.existsByOriginAirportIdAndIsActiveTrue(1L)).thenReturn(true);

        assertThatThrownBy(() -> airportService.deleteAirport(1L))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void searchAirportsByCode() {
        Airport airport = buildAirport(1L);
        AirportResponse response = new AirportResponse();
        response.setId(1L);
        Page<Airport> page = new PageImpl<>(List.of(airport), PageRequest.of(0, 10), 1);

        when(airportRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(airportMapper.toResponse(airport)).thenReturn(response);

        Page<AirportResponse> result = airportService.searchAirports("NYC", null, null, null, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listAirportsPaginatedAndSorted() {
        Airport airport = buildAirport(1L);
        AirportResponse response = new AirportResponse();
        response.setId(1L);
        Page<Airport> page = new PageImpl<>(List.of(airport), PageRequest.of(0, 5), 1);

        when(airportRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(airportMapper.toResponse(airport)).thenReturn(response);

        Page<AirportResponse> result = airportService.listAirports(PageRequest.of(0, 5, org.springframework.data.domain.Sort.by("name")));
        assertThat(result.getSize()).isEqualTo(5);
    }
}
