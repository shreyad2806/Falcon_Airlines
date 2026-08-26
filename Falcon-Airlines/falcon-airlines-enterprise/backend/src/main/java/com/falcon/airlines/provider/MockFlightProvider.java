package com.falcon.airlines.provider;

import com.falcon.airlines.entity.Flight;
import com.falcon.airlines.repository.FlightRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Default flight provider backed by the internal database.
 * Replace with ExternalFlightProvider for live flight data.
 */
@Slf4j
@Component
@Profile("!external")
public class MockFlightProvider implements FlightProvider {

    private final FlightRepository flightRepository;

    public MockFlightProvider(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    @Override
    public List<Flight> searchFlights(String origin, String destination, String date) {
        log.debug("MockFlightProvider: searching flights {} -> {} on {}", origin, destination, date);
        // Delegates to repository - actual search is handled by FlightService
        return List.of();
    }

    @Override
    public Optional<Flight> getFlight(Long id) {
        return flightRepository.findById(id);
    }

    @Override
    public BigDecimal getBasePrice(Long flightId, String currency) {
        return flightRepository.findById(flightId)
                .map(f -> f.getBasePrice() != null ? f.getBasePrice() : BigDecimal.valueOf(35000))
                .orElse(BigDecimal.valueOf(35000));
    }

    @Override
    public BigDecimal getSeatFee(String seatCategory, String currency) {
        return switch (seatCategory) {
            case "WINDOW" -> BigDecimal.valueOf(500);
            case "AISLE" -> BigDecimal.valueOf(400);
            case "EXTRA_LEGROOM" -> BigDecimal.valueOf(1500);
            case "EXIT_ROW" -> BigDecimal.valueOf(2000);
            default -> BigDecimal.ZERO; // STANDARD
        };
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
