package com.falcon.airlines.service;

import com.falcon.airlines.common.BaseUnitTest;
import com.falcon.airlines.dto.request.PassengerRequest;
import com.falcon.airlines.dto.response.PassengerResponse;
import com.falcon.airlines.entity.BookingPassenger;
import com.falcon.airlines.entity.Passenger;
import com.falcon.airlines.entity.Ticket;
import com.falcon.airlines.entity.User;
import com.falcon.airlines.enums.Gender;
import com.falcon.airlines.exception.BaseException;
import com.falcon.airlines.mapper.PassengerMapper;
import com.falcon.airlines.repository.BookingPassengerRepository;
import com.falcon.airlines.repository.PassengerRepository;
import com.falcon.airlines.repository.TicketRepository;
import com.falcon.airlines.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PassengerServiceTest extends BaseUnitTest {

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private BookingPassengerRepository bookingPassengerRepository;

    @Mock
    private PassengerMapper passengerMapper;

    @InjectMocks
    private PassengerService passengerService;

    private PassengerRequest buildRequest() {
        PassengerRequest request = new PassengerRequest();
        request.setUserId(1L);
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setEmail("john.doe@example.com");
        request.setPhone("+1234567890");
        request.setPassportNumber("AB1234567");
        request.setNationality("USA");
        request.setGender(Gender.M);
        request.setRedressNumber("123456789");
        return request;
    }

    private Passenger buildPassenger() {
        Passenger passenger = new Passenger();
        passenger.setId(1L);
        passenger.setFirstName("John");
        passenger.setLastName("Doe");
        passenger.setDateOfBirth(LocalDate.of(1990, 1, 1));
        passenger.setEmail("john.doe@example.com");
        passenger.setPhone("+1234567890");
        passenger.setPassportNumber("AB1234567");
        passenger.setNationality("USA");
        passenger.setGender(Gender.M);
        passenger.setRedressNumber("123456789");
        return passenger;
    }

    @Test
    void createPassenger_success() {
        PassengerRequest request = buildRequest();
        Passenger passenger = buildPassenger();
        User user = new User();
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passengerRepository.findByPassportNumber("AB1234567")).thenReturn(Optional.empty());
        when(passengerRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.empty());
        when(passengerMapper.toEntity(request)).thenReturn(passenger);
        when(passengerRepository.save(any(Passenger.class))).thenReturn(passenger);
        when(passengerMapper.toResponse(passenger)).thenReturn(new PassengerResponse());

        PassengerResponse response = passengerService.createPassenger(request);

        assertThat(response).isNotNull();
        verify(passengerRepository).save(any(Passenger.class));
    }

    @Test
    void createPassenger_userNotFound() {
        PassengerRequest request = buildRequest();

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passengerService.createPassenger(request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "USER_NOT_FOUND");
    }

    @Test
    void createPassenger_duplicatePassport() {
        PassengerRequest request = buildRequest();
        User user = new User();
        user.setId(1L);
        Passenger existing = buildPassenger();

        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        lenient().when(passengerRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.empty());
        when(passengerRepository.findByPassportNumber("AB1234567")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> passengerService.createPassenger(request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasFieldOrPropertyWithValue("errorCode", "DUPLICATE_PASSPORT");
    }

    @Test
    void createPassenger_duplicateEmail() {
        PassengerRequest request = buildRequest();
        User user = new User();
        user.setId(1L);
        Passenger existing = buildPassenger();

        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passengerRepository.findByPassportNumber("AB1234567")).thenReturn(Optional.empty());
        when(passengerRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> passengerService.createPassenger(request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasFieldOrPropertyWithValue("errorCode", "DUPLICATE_EMAIL");
    }

    @Test
    void getPassengerById_success() {
        Passenger passenger = buildPassenger();
        PassengerResponse response = new PassengerResponse();

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(passengerMapper.toResponse(passenger)).thenReturn(response);

        PassengerResponse result = passengerService.getPassengerById(1L);

        assertThat(result).isNotNull();
        verify(passengerRepository).findById(1L);
    }

    @Test
    void getPassengerById_notFound() {
        when(passengerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passengerService.getPassengerById(1L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "PASSENGER_NOT_FOUND");
    }

    @Test
    void updatePassenger_success() {
        PassengerRequest request = buildRequest();
        Passenger existing = buildPassenger();
        User user = new User();
        user.setId(1L);

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passengerRepository.findByPassportNumberAndIdNot("AB1234567", 1L)).thenReturn(Optional.empty());
        when(passengerRepository.findByEmailAndIdNot("john.doe@example.com", 1L)).thenReturn(Optional.empty());
        when(passengerRepository.save(any(Passenger.class))).thenReturn(existing);
        when(passengerMapper.toResponse(existing)).thenReturn(new PassengerResponse());

        PassengerResponse response = passengerService.updatePassenger(1L, request);

        assertThat(response).isNotNull();
        verify(passengerRepository).save(any(Passenger.class));
    }

    @Test
    void updatePassenger_notFound() {
        PassengerRequest request = buildRequest();

        when(passengerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passengerService.updatePassenger(1L, request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "PASSENGER_NOT_FOUND");
    }

    @Test
    void updatePassenger_duplicatePassport() {
        PassengerRequest request = buildRequest();
        Passenger existing = buildPassenger();
        Passenger other = buildPassenger();
        other.setId(2L);

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passengerRepository.findByPassportNumberAndIdNot("AB1234567", 1L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> passengerService.updatePassenger(1L, request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasFieldOrPropertyWithValue("errorCode", "DUPLICATE_PASSPORT");
    }

    @Test
    void deletePassenger_success() {
        Passenger passenger = buildPassenger();

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(ticketRepository.existsByPassengerId(1L)).thenReturn(false);
        when(bookingPassengerRepository.existsByPassengerId(1L)).thenReturn(false);
        when(passengerRepository.save(any(Passenger.class))).thenReturn(passenger);

        passengerService.deletePassenger(1L);

        ArgumentCaptor<Passenger> captor = ArgumentCaptor.forClass(Passenger.class);
        verify(passengerRepository).save(captor.capture());
        assertThat(captor.getValue().isDeleted()).isTrue();
    }

    @Test
    void deletePassenger_notFound() {
        when(passengerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passengerService.deletePassenger(1L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "PASSENGER_NOT_FOUND");
    }

    @Test
    void deletePassenger_inUse() {
        Passenger passenger = buildPassenger();

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(ticketRepository.existsByPassengerId(1L)).thenReturn(true);

        assertThatThrownBy(() -> passengerService.deletePassenger(1L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasFieldOrPropertyWithValue("errorCode", "PASSENGER_IN_USE");
    }

    @Test
    void listPassengers_success() {
        Page<Passenger> page = new PageImpl<>(List.of(buildPassenger()));
        Pageable pageable = PageRequest.of(0, 10);

        when(passengerRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable))).thenReturn(page);
        when(passengerMapper.toResponse(any(Passenger.class))).thenReturn(new PassengerResponse());

        Page<PassengerResponse> result = passengerService.listPassengers(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void searchPassengers_byFirstName() {
        Page<Passenger> page = new PageImpl<>(List.of(buildPassenger()));
        Pageable pageable = PageRequest.of(0, 10);

        when(passengerRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable))).thenReturn(page);
        when(passengerMapper.toResponse(any(Passenger.class))).thenReturn(new PassengerResponse());

        Page<PassengerResponse> result = passengerService.searchPassengers("John", null, null, null, null, null, pageable);

        assertThat(result).isNotNull();
        verify(passengerRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable));
    }

    @Test
    void searchPassengers_byLastName() {
        Page<Passenger> page = new PageImpl<>(List.of(buildPassenger()));
        Pageable pageable = PageRequest.of(0, 10);

        when(passengerRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable))).thenReturn(page);
        when(passengerMapper.toResponse(any(Passenger.class))).thenReturn(new PassengerResponse());

        Page<PassengerResponse> result = passengerService.searchPassengers(null, "Doe", null, null, null, null, pageable);

        assertThat(result).isNotNull();
    }

    @Test
    void searchPassengers_byEmail() {
        Page<Passenger> page = new PageImpl<>(List.of(buildPassenger()));
        Pageable pageable = PageRequest.of(0, 10);

        when(passengerRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable))).thenReturn(page);
        when(passengerMapper.toResponse(any(Passenger.class))).thenReturn(new PassengerResponse());

        Page<PassengerResponse> result = passengerService.searchPassengers(null, null, "john.doe@example.com", null, null, null, pageable);

        assertThat(result).isNotNull();
    }

    @Test
    void searchPassengers_byPassportNumber() {
        Page<Passenger> page = new PageImpl<>(List.of(buildPassenger()));
        Pageable pageable = PageRequest.of(0, 10);

        when(passengerRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable))).thenReturn(page);
        when(passengerMapper.toResponse(any(Passenger.class))).thenReturn(new PassengerResponse());

        Page<PassengerResponse> result = passengerService.searchPassengers(null, null, null, "AB1234567", null, null, pageable);

        assertThat(result).isNotNull();
    }

    @Test
    void searchPassengers_byUserId() {
        Page<Passenger> page = new PageImpl<>(List.of(buildPassenger()));
        Pageable pageable = PageRequest.of(0, 10);

        when(passengerRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable))).thenReturn(page);
        when(passengerMapper.toResponse(any(Passenger.class))).thenReturn(new PassengerResponse());

        Page<PassengerResponse> result = passengerService.searchPassengers(null, null, null, null, 1L, null, pageable);

        assertThat(result).isNotNull();
    }

    @Test
    void searchPassengers_byFullName() {
        Page<Passenger> page = new PageImpl<>(List.of(buildPassenger()));
        Pageable pageable = PageRequest.of(0, 10);

        when(passengerRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable))).thenReturn(page);
        when(passengerMapper.toResponse(any(Passenger.class))).thenReturn(new PassengerResponse());

        Page<PassengerResponse> result = passengerService.searchPassengers(null, null, null, null, null, "John Doe", pageable);

        assertThat(result).isNotNull();
    }

    @Test
    void getPassengerHistory_success() {
        Passenger passenger = buildPassenger();
        PassengerResponse response = new PassengerResponse();
        List<Ticket> tickets = List.of(new Ticket(), new Ticket());
        List<BookingPassenger> bookingPassengers = List.of(new BookingPassenger());

        when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger));
        when(passengerMapper.toResponse(passenger)).thenReturn(response);
        when(ticketRepository.findByPassengerId(1L)).thenReturn(tickets);
        when(bookingPassengerRepository.findByPassengerId(1L)).thenReturn(bookingPassengers);

        PassengerResponse result = passengerService.getPassengerHistory(1L);

        assertThat(result).isNotNull();
        assertThat(result.getTicketCount()).isEqualTo(2);
        assertThat(result.getBookingCount()).isEqualTo(1);
    }

    @Test
    void getPassengerHistory_notFound() {
        when(passengerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passengerService.getPassengerHistory(1L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "PASSENGER_NOT_FOUND");
    }
}
