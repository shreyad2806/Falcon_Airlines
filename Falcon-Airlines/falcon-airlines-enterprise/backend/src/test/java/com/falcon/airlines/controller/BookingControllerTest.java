package com.falcon.airlines.controller;

import com.falcon.airlines.config.SecurityConfig;
import com.falcon.airlines.dto.request.BookingRequest;
import com.falcon.airlines.dto.request.CancelBookingRequest;
import com.falcon.airlines.dto.response.BookingResponse;
import com.falcon.airlines.dto.response.BookingHistoryResponse;
import com.falcon.airlines.dto.response.SeatAvailabilityResponse;
import com.falcon.airlines.exception.BaseException;
import com.falcon.airlines.security.jwt.JwtAuthenticationFilter;
import com.falcon.airlines.security.jwt.JwtService;
import com.falcon.airlines.security.jwt.JwtTokenUtil;
import com.falcon.airlines.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BookingController.class)
@Import({SecurityConfig.class, JwtTokenUtil.class, JwtService.class, JwtAuthenticationFilter.class})
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private UserDetailsService userDetailsService;

    private BookingRequest buildBookingRequest() {
        BookingRequest request = new BookingRequest();
        request.setCustomerId(1L);
        request.setFlightId(1L);
        request.setRequestedSeats(List.of("1A", "1B"));
        
        BookingRequest.BookingPassengerRequest passenger1 = new BookingRequest.BookingPassengerRequest();
        passenger1.setPassengerId(1L);
        passenger1.setFareClass("ECONOMY");
        
        BookingRequest.BookingPassengerRequest passenger2 = new BookingRequest.BookingPassengerRequest();
        passenger2.setPassengerId(2L);
        passenger2.setFareClass("ECONOMY");
        
        request.setPassengers(List.of(passenger1, passenger2));
        return request;
    }

    private BookingResponse buildBookingResponse() {
        BookingResponse response = new BookingResponse();
        response.setId(1L);
        response.setBookingReference("BK123456");
        response.setCustomerId(1L);
        response.setFlightId(1L);
        response.setFlightNumber("FL001");
        response.setTotalAmount(BigDecimal.valueOf(200.00));
        response.setCurrency("USD");
        response.setBookingDate(Instant.now());
        return response;
    }

    private CancelBookingRequest buildCancelRequest() {
        CancelBookingRequest request = new CancelBookingRequest();
        request.setCancellationReason("Customer request");
        return request;
    }

    private SeatAvailabilityResponse buildSeatAvailabilityResponse() {
        SeatAvailabilityResponse response = new SeatAvailabilityResponse();
        response.setFlightId(1L);
        response.setFlightNumber("FL001");
        response.setTotalSeats(100);
        response.setAvailableSeats(50);
        return response;
    }

    private BookingHistoryResponse buildBookingHistoryResponse() {
        BookingHistoryResponse response = new BookingHistoryResponse();
        response.setCustomerId(1L);
        response.setCustomerUsername("customer");
        response.setTotalBookings(5);
        response.setBookings(Collections.emptyList());
        return response;
    }

    @Test
    void createBookingSuccessfully() throws Exception {
        when(bookingService.createBooking(any(BookingRequest.class))).thenReturn(buildBookingResponse());

        mockMvc.perform(post("/api/bookings")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("BOOKING_WRITE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildBookingRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.bookingReference").value("BK123456"));
    }

    @Test
    void createBookingUnauthorized() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildBookingRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createBookingForbidden() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .with(user("user").authorities(new SimpleGrantedAuthority("PASSENGER_READ")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildBookingRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createBookingWithInvalidData() throws Exception {
        BookingRequest request = new BookingRequest();
        request.setCustomerId(1L);
        request.setFlightId(1L);
        request.setRequestedSeats(Collections.emptyList());
        request.setPassengers(Collections.emptyList());

        mockMvc.perform(post("/api/bookings")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("BOOKING_WRITE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBookingConflict() throws Exception {
        when(bookingService.createBooking(any(BookingRequest.class)))
                .thenThrow(new BaseException("Seat already allocated", HttpStatus.CONFLICT, "SEAT_ALREADY_ALLOCATED"));

        mockMvc.perform(post("/api/bookings")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("BOOKING_WRITE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildBookingRequest())))
                .andExpect(status().isConflict());
    }

    @Test
    void getBookingById() throws Exception {
        when(bookingService.getBooking(1L)).thenReturn(buildBookingResponse());

        mockMvc.perform(get("/api/bookings/1")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("BOOKING_READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.bookingReference").value("BK123456"));
    }

    @Test
    void getBookingByIdNotFound() throws Exception {
        when(bookingService.getBooking(99L))
                .thenThrow(new BaseException("Booking not found", HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND"));

        mockMvc.perform(get("/api/bookings/99")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("BOOKING_READ"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getBookingByReference() throws Exception {
        when(bookingService.getBookingByReference("BK123456")).thenReturn(buildBookingResponse());

        mockMvc.perform(get("/api/bookings/reference/BK123456")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("BOOKING_READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingReference").value("BK123456"));
    }

    @Test
    void updateBooking() throws Exception {
        when(bookingService.updateBooking(eq(1L), any(BookingRequest.class))).thenReturn(buildBookingResponse());

        mockMvc.perform(put("/api/bookings/1")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("BOOKING_WRITE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildBookingRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void updateBookingNotFound() throws Exception {
        when(bookingService.updateBooking(eq(99L), any(BookingRequest.class)))
                .thenThrow(new BaseException("Booking not found", HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND"));

        mockMvc.perform(put("/api/bookings/99")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("BOOKING_WRITE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildBookingRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelBooking() throws Exception {
        mockMvc.perform(post("/api/bookings/1/cancel")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("BOOKING_WRITE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCancelRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void cancelBookingNotFound() throws Exception {
        doThrow(new BaseException("Booking not found", HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND"))
                .when(bookingService).cancelBooking(eq(99L), anyString());

        mockMvc.perform(post("/api/bookings/99/cancel")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("BOOKING_WRITE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCancelRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelBookingAlreadyCancelled() throws Exception {
        doThrow(new BaseException("Booking already cancelled", HttpStatus.BAD_REQUEST, "BOOKING_ALREADY_CANCELLED"))
                .when(bookingService).cancelBooking(eq(1L), anyString());

        mockMvc.perform(post("/api/bookings/1/cancel")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("BOOKING_WRITE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCancelRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getBookingHistory() throws Exception {
        when(bookingService.getBookingHistory(eq(1L), eq(0), eq(10))).thenReturn(buildBookingHistoryResponse());

        mockMvc.perform(get("/api/bookings/history")
                        .param("customerId", "1")
                        .param("page", "0")
                        .param("size", "10")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("BOOKING_READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerId").value(1))
                .andExpect(jsonPath("$.data.totalBookings").value(5));
    }

    @Test
    void getBookingHistoryWithPagination() throws Exception {
        when(bookingService.getBookingHistory(eq(1L), eq(1), eq(20))).thenReturn(buildBookingHistoryResponse());

        mockMvc.perform(get("/api/bookings/history")
                        .param("customerId", "1")
                        .param("page", "1")
                        .param("size", "20")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("BOOKING_READ"))))
                .andExpect(status().isOk());
    }

    @Test
    void checkSeatAvailability() throws Exception {
        when(bookingService.checkSeatAvailability(1L)).thenReturn(buildSeatAvailabilityResponse());

        mockMvc.perform(get("/api/bookings/seats/availability")
                        .param("flightId", "1")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("BOOKING_READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.flightId").value(1))
                .andExpect(jsonPath("$.data.totalSeats").value(100))
                .andExpect(jsonPath("$.data.availableSeats").value(50));
    }

    @Test
    void checkSeatAvailabilityFlightNotFound() throws Exception {
        when(bookingService.checkSeatAvailability(99L))
                .thenThrow(new BaseException("Flight not found", HttpStatus.NOT_FOUND, "FLIGHT_NOT_FOUND"));

        mockMvc.perform(get("/api/bookings/seats/availability")
                        .param("flightId", "99")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("BOOKING_READ"))))
                .andExpect(status().isNotFound());
    }
}
