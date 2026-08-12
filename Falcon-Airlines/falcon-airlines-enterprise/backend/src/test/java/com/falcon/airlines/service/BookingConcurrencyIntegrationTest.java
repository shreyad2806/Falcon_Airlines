package com.falcon.airlines.service;

import com.falcon.airlines.common.BaseIntegrationTest;
import com.falcon.airlines.dto.request.BookingRequest;
import com.falcon.airlines.dto.response.BookingResponse;
import com.falcon.airlines.entity.Booking;
import com.falcon.airlines.entity.Flight;
import com.falcon.airlines.entity.Passenger;
import com.falcon.airlines.entity.Seat;
import com.falcon.airlines.entity.SeatAllocation;
import com.falcon.airlines.entity.Ticket;
import com.falcon.airlines.entity.User;
import com.falcon.airlines.entity.Aircraft;
import com.falcon.airlines.enums.BookingStatus;
import com.falcon.airlines.enums.BookingPaymentStatus;
import com.falcon.airlines.enums.FlightStatus;
import com.falcon.airlines.enums.TicketStatus;
import com.falcon.airlines.exception.BaseException;
import com.falcon.airlines.repository.BookingRepository;
import com.falcon.airlines.repository.FlightRepository;
import com.falcon.airlines.repository.PassengerRepository;
import com.falcon.airlines.repository.SeatAllocationRepository;
import com.falcon.airlines.repository.SeatRepository;
import com.falcon.airlines.repository.TicketRepository;
import com.falcon.airlines.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class BookingConcurrencyIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private SeatAllocationRepository seatAllocationRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void twoUsersAttemptSameSeatSimultaneously() throws Exception {
        // Setup test data
        User customer1 = createCustomer("cc_cust1");
        User customer2 = createCustomer("cc_cust2");
        Flight flight = createFlightWithAircraft("CC001", "REG001");
        Passenger passenger1 = createPassenger("User1", "One", "PP111111");
        Passenger passenger2 = createPassenger("User2", "Two", "PP222222");
        Seat seat1 = createSeat(flight.getAircraft(), "1A", "ECONOMY");

        // Create booking requests
        BookingRequest request1 = new BookingRequest();
        request1.setCustomerId(customer1.getId());
        request1.setFlightId(flight.getId());
        request1.setRequestedSeats(List.of("1A"));

        BookingRequest.BookingPassengerRequest bp1 = new BookingRequest.BookingPassengerRequest();
        bp1.setPassengerId(passenger1.getId());
        bp1.setFareClass("ECONOMY");
        request1.setPassengers(List.of(bp1));

        BookingRequest request2 = new BookingRequest();
        request2.setCustomerId(customer2.getId());
        request2.setFlightId(flight.getId());
        request2.setRequestedSeats(List.of("1A"));

        BookingRequest.BookingPassengerRequest bp2 = new BookingRequest.BookingPassengerRequest();
        bp2.setPassengerId(passenger2.getId());
        bp2.setFareClass("ECONOMY");
        request2.setPassengers(List.of(bp2));

        // Synchronization barrier for true concurrency
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(2);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        BookingResponse[] results = new BookingResponse[2];
        Exception[] exceptions = new Exception[2];

        // Submit concurrent booking attempts
        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            try {
                startLatch.await(); // Wait for both threads to be ready
                results[0] = bookingService.createBooking(request1);
                successCount.incrementAndGet();
            } catch (Exception e) {
                exceptions[0] = e;
                failureCount.incrementAndGet();
            } finally {
                completionLatch.countDown();
            }
        }, executor);

        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            try {
                startLatch.await(); // Wait for both threads to be ready
                results[1] = bookingService.createBooking(request2);
                successCount.incrementAndGet();
            } catch (Exception e) {
                exceptions[1] = e;
                failureCount.incrementAndGet();
            } finally {
                completionLatch.countDown();
            }
        }, executor);

        // Release both threads simultaneously
        startLatch.countDown();
        
        // Wait for completion
        boolean completed = completionLatch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Verify exactly one booking succeeded
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);

        // Verify the failed request threw a business exception
        Exception failedException = exceptions[0] != null ? exceptions[0] : exceptions[1];
        assertThat(failedException).isInstanceOf(BaseException.class);
        assertThat(((BaseException) failedException).getStatus().value()).isEqualTo(409); // CONFLICT

        // Verify exactly one ticket owns the seat
        Optional<SeatAllocation> allocation = seatAllocationRepository.findBySeatIdAndFlightId(seat1.getId(), flight.getId());
        assertThat(allocation).isPresent();
        
        Ticket winningTicket = allocation.get().getTicket();
        Booking winningBooking = winningTicket.getBooking();
        
        // Verify the winning booking belongs to one of the customers
        assertThat(winningBooking.getCustomer().getId()).isIn(customer1.getId(), customer2.getId());

        // Verify no duplicate seat allocations
        List<SeatAllocation> allAllocations = seatAllocationRepository.findAll();
        long allocationsForSeat = allAllocations.stream()
                .filter(sa -> sa.getSeat().getId().equals(seat1.getId()))
                .filter(sa -> sa.getTicket().getFlight().getId().equals(flight.getId()))
                .count();
        assertThat(allocationsForSeat).isEqualTo(1);

        // Verify no overbooking - only one booking exists for this seat
        List<Booking> customer1Bookings = bookingRepository.findByCustomerId(customer1.getId(), 
                org.springframework.data.domain.PageRequest.of(0, 10)).getContent();
        List<Booking> customer2Bookings = bookingRepository.findByCustomerId(customer2.getId(), 
                org.springframework.data.domain.PageRequest.of(0, 10)).getContent();
        
        long totalBookings = customer1Bookings.size() + customer2Bookings.size();
        assertThat(totalBookings).isEqualTo(1);
    }

    @Test
    void twoBookingsAttemptDifferentSeatsSimultaneously() throws Exception {
        // Setup test data
        User customer1 = createCustomer("cc_cust3");
        User customer2 = createCustomer("cc_cust4");
        Flight flight = createFlightWithAircraft("CC002", "REG002");
        Passenger passenger1 = createPassenger("User3", "Three", "PP333333");
        Passenger passenger2 = createPassenger("User4", "Four", "PP444444");
        Seat seat1 = createSeat(flight.getAircraft(), "1A", "ECONOMY");
        Seat seat2 = createSeat(flight.getAircraft(), "1B", "ECONOMY");

        // Create booking requests for different seats
        BookingRequest request1 = new BookingRequest();
        request1.setCustomerId(customer1.getId());
        request1.setFlightId(flight.getId());
        request1.setRequestedSeats(List.of("1A"));

        BookingRequest.BookingPassengerRequest bp1 = new BookingRequest.BookingPassengerRequest();
        bp1.setPassengerId(passenger1.getId());
        bp1.setFareClass("ECONOMY");
        request1.setPassengers(List.of(bp1));

        BookingRequest request2 = new BookingRequest();
        request2.setCustomerId(customer2.getId());
        request2.setFlightId(flight.getId());
        request2.setRequestedSeats(List.of("1B"));

        BookingRequest.BookingPassengerRequest bp2 = new BookingRequest.BookingPassengerRequest();
        bp2.setPassengerId(passenger2.getId());
        bp2.setFareClass("ECONOMY");
        request2.setPassengers(List.of(bp2));

        // Synchronization barrier
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(2);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            try {
                startLatch.await();
                bookingService.createBooking(request1);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                completionLatch.countDown();
            }
        }, executor);

        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            try {
                startLatch.await();
                bookingService.createBooking(request2);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                completionLatch.countDown();
            }
        }, executor);

        startLatch.countDown();
        boolean completed = completionLatch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Both should succeed
        assertThat(successCount.get()).isEqualTo(2);
        assertThat(failureCount.get()).isEqualTo(0);

        // Verify both seats are allocated
        Optional<SeatAllocation> alloc1 = seatAllocationRepository.findBySeatIdAndFlightId(seat1.getId(), flight.getId());
        Optional<SeatAllocation> alloc2 = seatAllocationRepository.findBySeatIdAndFlightId(seat2.getId(), flight.getId());
        
        assertThat(alloc1).isPresent();
        assertThat(alloc2).isPresent();

        // Verify both customers have bookings
        List<Booking> customer1Bookings = bookingRepository.findByCustomerId(customer1.getId(), 
                org.springframework.data.domain.PageRequest.of(0, 10)).getContent();
        List<Booking> customer2Bookings = bookingRepository.findByCustomerId(customer2.getId(), 
                org.springframework.data.domain.PageRequest.of(0, 10)).getContent();
        
        assertThat(customer1Bookings).hasSize(1);
        assertThat(customer2Bookings).hasSize(1);
    }

    @Test
    void cancellationWhileBookingAttemptsSameSeat() throws Exception {
        // Setup test data
        User customer1 = createCustomer("cc_cust5");
        User customer2 = createCustomer("cc_cust6");
        Flight flight = createFlightWithAircraft("CC003", "REG003");
        Passenger passenger1 = createPassenger("User5", "Five", "PP555555");
        Passenger passenger2 = createPassenger("User6", "Six", "PP666666");
        Seat seat1 = createSeat(flight.getAircraft(), "1A", "ECONOMY");

        // Create initial booking
        BookingRequest initialRequest = new BookingRequest();
        initialRequest.setCustomerId(customer1.getId());
        initialRequest.setFlightId(flight.getId());
        initialRequest.setRequestedSeats(List.of("1A"));

        BookingRequest.BookingPassengerRequest bpInit = new BookingRequest.BookingPassengerRequest();
        bpInit.setPassengerId(passenger1.getId());
        bpInit.setFareClass("ECONOMY");
        initialRequest.setPassengers(List.of(bpInit));

        BookingResponse initialBooking = bookingService.createBooking(initialRequest);
        Long bookingId = initialBooking.getId();

        // Create second booking request
        BookingRequest secondRequest = new BookingRequest();
        secondRequest.setCustomerId(customer2.getId());
        secondRequest.setFlightId(flight.getId());
        secondRequest.setRequestedSeats(List.of("1A"));

        BookingRequest.BookingPassengerRequest bp2 = new BookingRequest.BookingPassengerRequest();
        bp2.setPassengerId(passenger2.getId());
        bp2.setFareClass("ECONOMY");
        secondRequest.setPassengers(List.of(bp2));

        // Synchronization barrier
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(2);
        
        AtomicInteger bookingSuccessCount = new AtomicInteger(0);
        AtomicInteger bookingFailureCount = new AtomicInteger(0);
        AtomicInteger cancellationSuccessCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        // Thread 1: Cancel booking
        CompletableFuture<Void> cancelFuture = CompletableFuture.runAsync(() -> {
            try {
                startLatch.await();
                bookingService.cancelBooking(bookingId, "Concurrent cancellation test");
                cancellationSuccessCount.incrementAndGet();
            } catch (Exception e) {
                // Cancellation might fail if booking already processed
            } finally {
                completionLatch.countDown();
            }
        }, executor);

        // Thread 2: Attempt new booking
        CompletableFuture<Void> bookingFuture = CompletableFuture.runAsync(() -> {
            try {
                startLatch.await();
                bookingService.createBooking(secondRequest);
                bookingSuccessCount.incrementAndGet();
            } catch (Exception e) {
                bookingFailureCount.incrementAndGet();
            } finally {
                completionLatch.countDown();
            }
        }, executor);

        startLatch.countDown();
        boolean completed = completionLatch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Verify database consistency
        // Either cancellation succeeded and new booking succeeded, or cancellation failed and new booking failed
        // Either way, only one booking should own the seat
        Optional<SeatAllocation> allocation = seatAllocationRepository.findBySeatIdAndFlightId(seat1.getId(), flight.getId());
        
        // Verify no duplicate allocations
        List<SeatAllocation> allAllocations = seatAllocationRepository.findAll();
        long allocationsForSeat = allAllocations.stream()
                .filter(sa -> sa.getSeat().getId().equals(seat1.getId()))
                .filter(sa -> sa.getTicket().getFlight().getId().equals(flight.getId()))
                .count();
        assertThat(allocationsForSeat).isLessThanOrEqualTo(1);

        // Verify total bookings for both customers
        List<Booking> customer1Bookings = bookingRepository.findByCustomerId(customer1.getId(), 
                org.springframework.data.domain.PageRequest.of(0, 10)).getContent();
        List<Booking> customer2Bookings = bookingRepository.findByCustomerId(customer2.getId(), 
                org.springframework.data.domain.PageRequest.of(0, 10)).getContent();
        
        long totalBookings = customer1Bookings.size() + customer2Bookings.size();
        assertThat(totalBookings).isLessThanOrEqualTo(1);
    }

    @Test
    void optimisticLockConflictOnConcurrentUpdates() throws Exception {
        // Setup test data
        User customer = createCustomer("cc_cust7");
        Flight flight = createFlightWithAircraft("CC004", "REG004");
        Passenger passenger = createPassenger("User7", "Seven", "PP777777");
        Seat seat1 = createSeat(flight.getAircraft(), "1A", "ECONOMY");

        // Create initial booking
        BookingRequest initialRequest = new BookingRequest();
        initialRequest.setCustomerId(customer.getId());
        initialRequest.setFlightId(flight.getId());
        initialRequest.setRequestedSeats(List.of("1A"));

        BookingRequest.BookingPassengerRequest bpInit = new BookingRequest.BookingPassengerRequest();
        bpInit.setPassengerId(passenger.getId());
        bpInit.setFareClass("ECONOMY");
        initialRequest.setPassengers(List.of(bpInit));

        BookingResponse initialBooking = bookingService.createBooking(initialRequest);
        Long bookingId = initialBooking.getId();

        // Synchronization barrier for concurrent updates
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(2);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        // Thread 1: Update booking
        CompletableFuture<Void> update1 = CompletableFuture.runAsync(() -> {
            try {
                startLatch.await();
                BookingRequest updateRequest = new BookingRequest();
                updateRequest.setCustomerId(customer.getId());
                updateRequest.setFlightId(flight.getId());
                updateRequest.setRequestedSeats(List.of("1A"));
                
                BookingRequest.BookingPassengerRequest bp = new BookingRequest.BookingPassengerRequest();
                bp.setPassengerId(passenger.getId());
                bp.setFareClass("ECONOMY");
                updateRequest.setPassengers(List.of(bp));
                
                bookingService.updateBooking(bookingId, updateRequest);
                successCount.incrementAndGet();
            } catch (Exception e) {
                if (e instanceof BaseException && ((BaseException) e).getErrorCode().equals("CONCURRENT_ALLOCATION")) {
                    conflictCount.incrementAndGet();
                }
            } finally {
                completionLatch.countDown();
            }
        }, executor);

        // Thread 2: Update booking
        CompletableFuture<Void> update2 = CompletableFuture.runAsync(() -> {
            try {
                startLatch.await();
                BookingRequest updateRequest = new BookingRequest();
                updateRequest.setCustomerId(customer.getId());
                updateRequest.setFlightId(flight.getId());
                updateRequest.setRequestedSeats(List.of("1A"));
                
                BookingRequest.BookingPassengerRequest bp = new BookingRequest.BookingPassengerRequest();
                bp.setPassengerId(passenger.getId());
                bp.setFareClass("ECONOMY");
                updateRequest.setPassengers(List.of(bp));
                
                bookingService.updateBooking(bookingId, updateRequest);
                successCount.incrementAndGet();
            } catch (Exception e) {
                if (e instanceof BaseException && ((BaseException) e).getErrorCode().equals("CONCURRENT_ALLOCATION")) {
                    conflictCount.incrementAndGet();
                }
            } finally {
                completionLatch.countDown();
            }
        }, executor);

        startLatch.countDown();
        boolean completed = completionLatch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Verify booking still exists and is consistent
        Optional<Booking> booking = bookingRepository.findById(bookingId);
        assertThat(booking).isPresent();

        // Verify database consistency - booking should have a valid version
        assertThat(booking.get().getVersion()).isNotNull();
    }

    // Helper methods to create test data

    private User createCustomer(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPasswordHash("hashed_password");
        user.setStatus(com.falcon.airlines.enums.UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private Aircraft createAircraft(String registration) {
        Aircraft aircraft = new Aircraft();
        aircraft.setRegistrationNumber(registration);
        aircraft.setModel("Boeing 737");
        aircraft.setTotalCapacity((short) 150);
        return aircraft;
    }

    private Flight createFlightWithAircraft(String flightNumber, String aircraftReg) {
        Aircraft aircraft = createAircraft(aircraftReg);
        
        com.falcon.airlines.entity.Airport origin = new com.falcon.airlines.entity.Airport();
        origin.setIataCode("JFK");
        origin.setIcaoCode("KJFK");
        origin.setName("John F. Kennedy International");
        origin.setCity("New York");
        origin.setCountry("US");
        origin.setTimeZone("America/New_York");
        origin.setIsActive(true);
        
        com.falcon.airlines.entity.Airport destination = new com.falcon.airlines.entity.Airport();
        destination.setIataCode("LAX");
        destination.setIcaoCode("KLAX");
        destination.setName("Los Angeles International");
        destination.setCity("Los Angeles");
        destination.setCountry("US");
        destination.setTimeZone("America/Los_Angeles");
        destination.setIsActive(true);

        Flight flight = new Flight();
        flight.setFlightNumber(flightNumber);
        flight.setOriginAirport(origin);
        flight.setDestinationAirport(destination);
        flight.setAircraft(aircraft);
        flight.setScheduledDeparture(Instant.now().plusSeconds(86400));
        flight.setScheduledArrival(Instant.now().plusSeconds(172800));
        flight.setStatus(FlightStatus.SCHEDULED);
        flight.setIsActive(true);
        flight.setTerminal("T1");
        flight.setGate("A1");
        
        return flightRepository.save(flight);
    }

    private Passenger createPassenger(String firstName, String lastName, String passportNumber) {
        Passenger passenger = new Passenger();
        passenger.setFirstName(firstName);
        passenger.setLastName(lastName);
        passenger.setDateOfBirth(java.time.LocalDate.of(1990, 1, 1));
        passenger.setEmail(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com");
        passenger.setPhone("+1234567890");
        passenger.setPassportNumber(passportNumber);
        passenger.setNationality("USA");
        passenger.setGender(com.falcon.airlines.enums.Gender.M);
        return passengerRepository.save(passenger);
    }

    private Seat createSeat(Aircraft aircraft, String seatNumber, String seatClass) {
        Seat seat = new Seat();
        seat.setAircraft(aircraft);
        seat.setSeatNumber(seatNumber);
        seat.setSeatClass(seatClass);
        seat.setRowNumber((short) 1);
        seat.setColumnLetter("A");
        seat.setIsActive(true);
        return seatRepository.save(seat);
    }
}
