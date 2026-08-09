package com.falcon.airlines.service;

import com.falcon.airlines.dto.request.AircraftRequest;
import com.falcon.airlines.dto.response.AircraftResponse;
import com.falcon.airlines.entity.Aircraft;
import com.falcon.airlines.exception.BaseException;
import com.falcon.airlines.mapper.AircraftMapper;
import com.falcon.airlines.repository.AircraftRepository;
import com.falcon.airlines.repository.FlightRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Application service for aircraft inventory management.
 */
@Service
@Transactional
public class AircraftService {

    private final AircraftRepository aircraftRepository;
    private final FlightRepository flightRepository;
    private final AircraftMapper aircraftMapper;

    public AircraftService(AircraftRepository aircraftRepository,
                           FlightRepository flightRepository,
                           AircraftMapper aircraftMapper) {
        this.aircraftRepository = aircraftRepository;
        this.flightRepository = flightRepository;
        this.aircraftMapper = aircraftMapper;
    }

    public AircraftResponse createAircraft(AircraftRequest request) {
        validateUniqueness(request, null);

        Aircraft aircraft = aircraftMapper.toEntity(request);
        Aircraft saved = aircraftRepository.save(aircraft);
        return aircraftMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AircraftResponse getAircraftById(Long id) {
        Aircraft aircraft = aircraftRepository.findById(id)
                .orElseThrow(() -> new BaseException("Aircraft not found", HttpStatus.NOT_FOUND, "AIRCRAFT_NOT_FOUND"));
        return aircraftMapper.toResponse(aircraft);
    }

    public AircraftResponse updateAircraft(Long id, AircraftRequest request) {
        Aircraft existing = aircraftRepository.findById(id)
                .orElseThrow(() -> new BaseException("Aircraft not found", HttpStatus.NOT_FOUND, "AIRCRAFT_NOT_FOUND"));

        validateUniqueness(request, id);

        existing.setRegistrationNumber(request.getRegistrationNumber());
        existing.setType(request.getType());
        existing.setModel(request.getModel());
        existing.setManufacturer(request.getManufacturer());
        existing.setTotalCapacity(request.getTotalCapacity());
        existing.setConfiguration(request.getConfiguration());

        Aircraft updated = aircraftRepository.save(existing);
        return aircraftMapper.toResponse(updated);
    }

    public void deleteAircraft(Long id) {
        Aircraft aircraft = aircraftRepository.findById(id)
                .orElseThrow(() -> new BaseException("Aircraft not found", HttpStatus.NOT_FOUND, "AIRCRAFT_NOT_FOUND"));

        if (flightRepository.existsByAircraftIdAndIsActiveTrue(id)) {
            throw new BaseException("Aircraft is referenced by active flights", HttpStatus.CONFLICT, "AIRCRAFT_IN_USE");
        }

        aircraft.setDeleted(true);
        aircraft.setDeletedAt(Instant.now());
        aircraftRepository.save(aircraft);
    }

    @Transactional(readOnly = true)
    public Page<AircraftResponse> listAircraft(Pageable pageable) {
        return searchAircraft(null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AircraftResponse> searchAircraft(String type,
                                                 String manufacturer,
                                                 String registration,
                                                 Pageable pageable) {
        Specification<Aircraft> spec = buildSpecification(type, manufacturer, registration);
        return aircraftRepository.findAll(spec, pageable).map(aircraftMapper::toResponse);
    }

    private void validateUniqueness(AircraftRequest request, Long excludeId) {
        if (excludeId == null) {
            aircraftRepository.findByRegistrationNumber(request.getRegistrationNumber()).ifPresent(a -> {
                throw new BaseException("Registration number already in use: " + request.getRegistrationNumber(), HttpStatus.CONFLICT, "DUPLICATE_REGISTRATION_NUMBER");
            });
        } else {
            aircraftRepository.findByRegistrationNumberAndIdNot(request.getRegistrationNumber(), excludeId).ifPresent(a -> {
                throw new BaseException("Registration number already in use: " + request.getRegistrationNumber(), HttpStatus.CONFLICT, "DUPLICATE_REGISTRATION_NUMBER");
            });
        }
    }

    private Specification<Aircraft> buildSpecification(String type, String manufacturer, String registration) {
        Specification<Aircraft> spec = (root, query, cb) -> cb.equal(root.get("isDeleted"), false);

        if (type != null && !type.isBlank()) {
            String like = "%" + type.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("type")), like));
        }

        if (manufacturer != null && !manufacturer.isBlank()) {
            String like = "%" + manufacturer.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("manufacturer")), like));
        }

        if (registration != null && !registration.isBlank()) {
            String like = "%" + registration.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("registrationNumber")), like));
        }

        return spec;
    }
}
