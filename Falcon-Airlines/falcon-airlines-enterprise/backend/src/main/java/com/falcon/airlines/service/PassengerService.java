package com.falcon.airlines.service;

import com.falcon.airlines.dto.request.PassengerRequest;
import com.falcon.airlines.dto.response.PassengerResponse;
import com.falcon.airlines.entity.BookingPassenger;
import com.falcon.airlines.entity.Passenger;
import com.falcon.airlines.entity.Ticket;
import com.falcon.airlines.entity.User;
import com.falcon.airlines.exception.BaseException;
import com.falcon.airlines.mapper.PassengerMapper;
import com.falcon.airlines.repository.BookingPassengerRepository;
import com.falcon.airlines.repository.PassengerRepository;
import com.falcon.airlines.repository.TicketRepository;
import com.falcon.airlines.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Application service for passenger management.
 */
@Service
@Transactional
public class PassengerService {

    private final PassengerRepository passengerRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final BookingPassengerRepository bookingPassengerRepository;
    private final PassengerMapper passengerMapper;

    public PassengerService(PassengerRepository passengerRepository,
                           UserRepository userRepository,
                           TicketRepository ticketRepository,
                           BookingPassengerRepository bookingPassengerRepository,
                           PassengerMapper passengerMapper) {
        this.passengerRepository = passengerRepository;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.bookingPassengerRepository = bookingPassengerRepository;
        this.passengerMapper = passengerMapper;
    }

    public PassengerResponse createPassenger(PassengerRequest request) {
        validateUniqueness(request, null);

        Passenger passenger = passengerMapper.toEntity(request);

        if (request.getUserId() != null) {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new BaseException("User not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
            passenger.setUser(user);
        }

        Passenger saved = passengerRepository.save(passenger);
        return passengerMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PassengerResponse getPassengerById(Long id) {
        Passenger passenger = passengerRepository.findById(id)
                .orElseThrow(() -> new BaseException("Passenger not found", HttpStatus.NOT_FOUND, "PASSENGER_NOT_FOUND"));
        return passengerMapper.toResponse(passenger);
    }

    public PassengerResponse updatePassenger(Long id, PassengerRequest request) {
        Passenger existing = passengerRepository.findById(id)
                .orElseThrow(() -> new BaseException("Passenger not found", HttpStatus.NOT_FOUND, "PASSENGER_NOT_FOUND"));

        validateUniqueness(request, id);

        if (request.getUserId() != null) {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new BaseException("User not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
            existing.setUser(user);
        }

        existing.setFirstName(request.getFirstName());
        existing.setLastName(request.getLastName());
        existing.setDateOfBirth(request.getDateOfBirth());
        existing.setEmail(request.getEmail());
        existing.setPhone(request.getPhone());
        existing.setPassportNumber(request.getPassportNumber());
        existing.setNationality(request.getNationality());
        existing.setGender(request.getGender());
        existing.setRedressNumber(request.getRedressNumber());

        Passenger updated = passengerRepository.save(existing);
        return passengerMapper.toResponse(updated);
    }

    public void deletePassenger(Long id) {
        Passenger passenger = passengerRepository.findById(id)
                .orElseThrow(() -> new BaseException("Passenger not found", HttpStatus.NOT_FOUND, "PASSENGER_NOT_FOUND"));

        if (ticketRepository.existsByPassengerId(id) || bookingPassengerRepository.existsByPassengerId(id)) {
            throw new BaseException("Passenger is referenced by active bookings or tickets", HttpStatus.CONFLICT, "PASSENGER_IN_USE");
        }

        passenger.setDeleted(true);
        passenger.setDeletedAt(Instant.now());
        passengerRepository.save(passenger);
    }

    @Transactional(readOnly = true)
    public Page<PassengerResponse> listPassengers(Pageable pageable) {
        return searchPassengers(null, null, null, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PassengerResponse> searchPassengers(String firstName,
                                                     String lastName,
                                                     String email,
                                                     String passportNumber,
                                                     Long userId,
                                                     String fullName,
                                                     Pageable pageable) {
        Specification<Passenger> spec = buildSpecification(firstName, lastName, email, passportNumber, userId, fullName);
        return passengerRepository.findAll(spec, pageable).map(passengerMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PassengerResponse getPassengerHistory(Long id) {
        Passenger passenger = passengerRepository.findById(id)
                .orElseThrow(() -> new BaseException("Passenger not found", HttpStatus.NOT_FOUND, "PASSENGER_NOT_FOUND"));

        PassengerResponse response = passengerMapper.toResponse(passenger);

        List<Ticket> tickets = ticketRepository.findByPassengerId(id);
        List<BookingPassenger> bookingPassengers = bookingPassengerRepository.findByPassengerId(id);

        response.setTicketCount(tickets.size());
        response.setBookingCount(bookingPassengers.size());

        return response;
    }

    private void validateUniqueness(PassengerRequest request, Long excludeId) {
        if (request.getPassportNumber() != null && !request.getPassportNumber().isBlank()) {
            if (excludeId == null) {
                passengerRepository.findByPassportNumber(request.getPassportNumber()).ifPresent(p -> {
                    throw new BaseException("Passport number already in use: " + request.getPassportNumber(),
                            HttpStatus.CONFLICT, "DUPLICATE_PASSPORT");
                });
            } else {
                passengerRepository.findByPassportNumberAndIdNot(request.getPassportNumber(), excludeId).ifPresent(p -> {
                    throw new BaseException("Passport number already in use: " + request.getPassportNumber(),
                            HttpStatus.CONFLICT, "DUPLICATE_PASSPORT");
                });
            }
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (excludeId == null) {
                passengerRepository.findByEmail(request.getEmail()).ifPresent(p -> {
                    throw new BaseException("Email already in use: " + request.getEmail(),
                            HttpStatus.CONFLICT, "DUPLICATE_EMAIL");
                });
            } else {
                passengerRepository.findByEmailAndIdNot(request.getEmail(), excludeId).ifPresent(p -> {
                    throw new BaseException("Email already in use: " + request.getEmail(),
                            HttpStatus.CONFLICT, "DUPLICATE_EMAIL");
                });
            }
        }
    }

    private Specification<Passenger> buildSpecification(String firstName,
                                                         String lastName,
                                                         String email,
                                                         String passportNumber,
                                                         Long userId,
                                                         String fullName) {
        Specification<Passenger> spec = (root, query, cb) -> cb.equal(root.get("isDeleted"), false);

        if (firstName != null && !firstName.isBlank()) {
            String like = "%" + firstName.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("firstName")), like));
        }

        if (lastName != null && !lastName.isBlank()) {
            String like = "%" + lastName.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("lastName")), like));
        }

        if (email != null && !email.isBlank()) {
            String like = "%" + email.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("email")), like));
        }

        if (passportNumber != null && !passportNumber.isBlank()) {
            String like = "%" + passportNumber.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("passportNumber")), like));
        }

        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("user").get("id"), userId));
        }

        if (fullName != null && !fullName.isBlank()) {
            String like = "%" + fullName.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("firstName")), like),
                    cb.like(cb.lower(root.get("lastName")), like)));
        }

        return spec;
    }
}
