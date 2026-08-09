package com.falcon.airlines.service;

import com.falcon.airlines.dto.request.AirportRequest;
import com.falcon.airlines.dto.response.AirportResponse;
import com.falcon.airlines.entity.Airport;
import com.falcon.airlines.exception.BaseException;
import com.falcon.airlines.mapper.AirportMapper;
import com.falcon.airlines.repository.AirportRepository;
import com.falcon.airlines.repository.FlightRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Application service for airport inventory management.
 */
@Service
@Transactional
public class AirportService {

    private final AirportRepository airportRepository;
    private final FlightRepository flightRepository;
    private final AirportMapper airportMapper;

    public AirportService(AirportRepository airportRepository,
                          FlightRepository flightRepository,
                          AirportMapper airportMapper) {
        this.airportRepository = airportRepository;
        this.flightRepository = flightRepository;
        this.airportMapper = airportMapper;
    }

    public AirportResponse createAirport(AirportRequest request) {
        validateUniqueness(request, null);

        Airport airport = airportMapper.toEntity(request);
        airport.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        Airport saved = airportRepository.save(airport);
        return airportMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AirportResponse getAirportById(Long id) {
        Airport airport = airportRepository.findById(id)
                .orElseThrow(() -> new BaseException("Airport not found", HttpStatus.NOT_FOUND, "AIRPORT_NOT_FOUND"));
        return airportMapper.toResponse(airport);
    }

    public AirportResponse updateAirport(Long id, AirportRequest request) {
        Airport existing = airportRepository.findById(id)
                .orElseThrow(() -> new BaseException("Airport not found", HttpStatus.NOT_FOUND, "AIRPORT_NOT_FOUND"));

        validateUniqueness(request, id);

        existing.setIataCode(request.getIataCode());
        existing.setIcaoCode(request.getIcaoCode());
        existing.setName(request.getName());
        existing.setCity(request.getCity());
        existing.setCountry(request.getCountry());
        existing.setTimeZone(request.getTimeZone());
        existing.setLatitude(request.getLatitude());
        existing.setLongitude(request.getLongitude());
        existing.setIsActive(request.getIsActive() != null ? request.getIsActive() : existing.getIsActive());

        Airport updated = airportRepository.save(existing);
        return airportMapper.toResponse(updated);
    }

    public void deleteAirport(Long id) {
        Airport airport = airportRepository.findById(id)
                .orElseThrow(() -> new BaseException("Airport not found", HttpStatus.NOT_FOUND, "AIRPORT_NOT_FOUND"));

        if (flightRepository.existsByOriginAirportIdAndIsActiveTrue(id)
                || flightRepository.existsByDestinationAirportIdAndIsActiveTrue(id)) {
            throw new BaseException("Airport is referenced by active flights", HttpStatus.CONFLICT, "AIRPORT_IN_USE");
        }

        airport.setIsActive(false);
        airport.setDeleted(true);
        airport.setDeletedAt(Instant.now());
        airportRepository.save(airport);
    }

    @Transactional(readOnly = true)
    public Page<AirportResponse> listAirports(Pageable pageable) {
        return searchAirports(null, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AirportResponse> searchAirports(String code,
                                                String name,
                                                String city,
                                                Boolean isActive,
                                                Pageable pageable) {
        Specification<Airport> spec = buildSpecification(code, name, city, isActive);
        return airportRepository.findAll(spec, pageable).map(airportMapper::toResponse);
    }

    private void validateUniqueness(AirportRequest request, Long excludeId) {
        if (excludeId == null) {
            airportRepository.findByIataCode(request.getIataCode()).ifPresent(a -> {
                throw new BaseException("IATA code already in use: " + request.getIataCode(), HttpStatus.CONFLICT, "DUPLICATE_IATA_CODE");
            });

            if (request.getIcaoCode() != null && !request.getIcaoCode().isBlank()) {
                airportRepository.findByIcaoCode(request.getIcaoCode()).ifPresent(a -> {
                    throw new BaseException("ICAO code already in use: " + request.getIcaoCode(), HttpStatus.CONFLICT, "DUPLICATE_ICAO_CODE");
                });
            }
        } else {
            airportRepository.findByIataCodeAndIdNot(request.getIataCode(), excludeId).ifPresent(a -> {
                throw new BaseException("IATA code already in use: " + request.getIataCode(), HttpStatus.CONFLICT, "DUPLICATE_IATA_CODE");
            });

            if (request.getIcaoCode() != null && !request.getIcaoCode().isBlank()) {
                airportRepository.findByIcaoCodeAndIdNot(request.getIcaoCode(), excludeId).ifPresent(a -> {
                    throw new BaseException("ICAO code already in use: " + request.getIcaoCode(), HttpStatus.CONFLICT, "DUPLICATE_ICAO_CODE");
                });
            }
        }
    }

    private Specification<Airport> buildSpecification(String code, String name, String city, Boolean isActive) {
        Specification<Airport> spec = (root, query, cb) -> cb.equal(root.get("isDeleted"), false);

        if (code != null && !code.isBlank()) {
            String like = "%" + code.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("iataCode")), like),
                    cb.like(cb.lower(root.get("icaoCode")), like)));
        }

        if (name != null && !name.isBlank()) {
            String like = "%" + name.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), like));
        }

        if (city != null && !city.isBlank()) {
            String like = "%" + city.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("city")), like));
        }

        if (isActive != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isActive"), isActive));
        }

        return spec;
    }
}
