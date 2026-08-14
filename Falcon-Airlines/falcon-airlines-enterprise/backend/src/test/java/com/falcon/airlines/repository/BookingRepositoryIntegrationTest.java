package com.falcon.airlines.repository;

import com.falcon.airlines.common.BaseIntegrationTest;
import com.falcon.airlines.entity.Booking;
import com.falcon.airlines.entity.User;
import com.falcon.airlines.enums.BookingStatus;
import com.falcon.airlines.enums.BookingPaymentStatus;
import com.falcon.airlines.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class BookingRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveAndFindBooking() {
        User customer = createUser();
        Booking booking = createBooking(customer, "REF001");
        
        Booking saved = bookingRepository.save(booking);
        assertThat(saved.getId()).isNotNull();

        Optional<Booking> found = bookingRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getBookingReference()).isEqualTo("REF001");
    }

    @Test
    void findByBookingReference() {
        User customer = createUser();
        Booking booking = createBooking(customer, "REF002");
        bookingRepository.save(booking);

        Optional<Booking> found = bookingRepository.findByBookingReference("REF002");
        assertThat(found).isPresent();
        assertThat(found.get().getCustomer().getId()).isEqualTo(customer.getId());
    }

    @Test
    void findByBookingReference_notFound() {
        Optional<Booking> found = bookingRepository.findByBookingReference("NOTFOUND");
        assertThat(found).isEmpty();
    }

    @Test
    void findByCustomerId() {
        User customer = createUser();
        Booking booking1 = createBooking(customer, "REF003");
        Booking booking2 = createBooking(customer, "REF004");
        bookingRepository.save(booking1);
        bookingRepository.save(booking2);

        Page<Booking> bookings = bookingRepository.findByCustomerId(customer.getId(), PageRequest.of(0, 10));
        assertThat(bookings.getContent()).hasSize(2);
    }

    @Test
    void findByCustomerId_paginated() {
        User customer = createUser();
        for (int i = 0; i < 15; i++) {
            Booking booking = createBooking(customer, "REF" + String.format("%03d", i + 10));
            bookingRepository.save(booking);
        }

        Page<Booking> firstPage = bookingRepository.findByCustomerId(customer.getId(), PageRequest.of(0, 10));
        assertThat(firstPage.getContent()).hasSize(10);
        assertThat(firstPage.getTotalElements()).isEqualTo(15);

        Page<Booking> secondPage = bookingRepository.findByCustomerId(customer.getId(), PageRequest.of(1, 10));
        assertThat(secondPage.getContent()).hasSize(5);
    }

    @Test
    void findByStatus() {
        User customer = createUser();
        Booking booking1 = createBooking(customer, "REF005");
        booking1.setStatus(BookingStatus.CONFIRMED);
        Booking booking2 = createBooking(customer, "REF006");
        booking2.setStatus(BookingStatus.CONFIRMED);
        Booking booking3 = createBooking(customer, "REF007");
        booking3.setStatus(BookingStatus.PENDING);
        bookingRepository.save(booking1);
        bookingRepository.save(booking2);
        bookingRepository.save(booking3);

        Page<Booking> confirmedBookings = bookingRepository.findByStatus(BookingStatus.CONFIRMED, PageRequest.of(0, 10));
        assertThat(confirmedBookings.getContent()).hasSize(2);

        Page<Booking> pendingBookings = bookingRepository.findByStatus(BookingStatus.PENDING, PageRequest.of(0, 10));
        assertThat(pendingBookings.getContent()).hasSize(1);
    }

    @Test
    void existsByBookingReference() {
        User customer = createUser();
        Booking booking = createBooking(customer, "REF008");
        bookingRepository.save(booking);

        boolean exists = bookingRepository.existsByBookingReference("REF008");
        assertThat(exists).isTrue();

        boolean notExists = bookingRepository.existsByBookingReference("NOTFOUND");
        assertThat(notExists).isFalse();
    }

    @Test
    void versionFieldPresent() {
        User customer = createUser();
        Booking booking = createBooking(customer, "REF009");
        Booking saved = bookingRepository.save(booking);
        
        assertThat(saved.getVersion()).isNotNull();
        assertThat(saved.getVersion()).isEqualTo(0L);
    }

    @Test
    void versionIncrementedOnUpdate() {
        User customer = createUser();
        Booking booking = createBooking(customer, "REF010");
        Booking saved = bookingRepository.save(booking);
        Long initialVersion = saved.getVersion();
        
        saved.setStatus(BookingStatus.CONFIRMED);
        Booking updated = bookingRepository.save(saved);
        bookingRepository.flush();
        
        assertThat(updated.getVersion()).isGreaterThan(initialVersion);
    }

    private User createUser() {
        User user = new User();
        user.setUsername("cust_test");
        user.setEmail("cust_test@example.com");
        user.setPasswordHash("hashed_password");
        user.setStatus(UserStatus.ACTIVE);
        user.setMfaEnabled(false);
        user.setEmailVerified(false);
        return userRepository.save(user);
    }

    private Booking createBooking(User customer, String reference) {
        Booking booking = new Booking();
        booking.setBookingReference(reference);
        booking.setCustomer(customer);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(BigDecimal.valueOf(100.00));
        booking.setCurrency("USD");
        booking.setBookingDate(Instant.now());
        booking.setPaymentStatus(BookingPaymentStatus.PENDING);
        return booking;
    }
}
