package com.falcon.airlines.provider.aviationstack;

import com.falcon.airlines.provider.FlightDataProvider;
import com.falcon.airlines.provider.FlightSearchRequest;
import com.falcon.airlines.provider.NormalizedFlight;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Aviationstack-backed flight data provider.
 * Normalizes external API responses into Falcon's internal model.
 *
 * Activate with: flight.provider=aviationstack
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "flight.provider", havingValue = "aviationstack")
public class AviationstackFlightDataProvider implements FlightDataProvider {

    private final AviationstackClient client;

    public AviationstackFlightDataProvider(AviationstackClient client) {
        this.client = client;
    }

    @Override
    public List<NormalizedFlight> searchFlights(FlightSearchRequest request) {
        String date = request.getDepartureDate() != null ? request.getDepartureDate().toString() : null;

        List<AviationstackClient.AviationstackFlight> raw =
                client.searchFlights(request.getOrigin(), request.getDestination(),
                        date, request.getFlightNumber());

        List<NormalizedFlight> results = new ArrayList<>();
        for (AviationstackClient.AviationstackFlight flight : raw) {
            NormalizedFlight normalized = normalize(flight);
            if (normalized != null) {
                // Apply search filters that Aviationstack may not fully support
                if (matchesFilters(normalized, request)) {
                    results.add(normalized);
                }
            }
        }
        return results;
    }

    @Override
    public Optional<NormalizedFlight> getFlightStatus(String flightNumber, LocalDate date) {
        String dateStr = date != null ? date.toString() : null;
        List<AviationstackClient.AviationstackFlight> raw =
                client.getFlightStatus(flightNumber, dateStr);

        return raw.stream()
                .map(this::normalize)
                .filter(f -> f != null && flightNumber.equalsIgnoreCase(f.getFlightNumber()))
                .findFirst();
    }

    @Override
    public boolean isAvailable() {
        return client.isConfigured();
    }

    @Override
    public String getProviderName() {
        return "Aviationstack";
    }

    /**
     * Normalize Aviationstack flight data into Falcon's internal model.
     */
    private NormalizedFlight normalize(AviationstackClient.AviationstackFlight raw) {
        if (raw == null || raw.getFlight() == null) return null;

        try {
            String flightNumber = raw.getFlight().getIata();
            if (flightNumber == null || flightNumber.isBlank()) return null;

            String airlineName = raw.getAirline() != null ? raw.getAirline().getName() : "Unknown Airline";
            String airlineCode = raw.getAirline() != null ? raw.getAirline().getIata() : null;

            // Route
            String originCode = raw.getDeparture() != null ? raw.getDeparture().getIata() : null;
            String originName = raw.getDeparture() != null ? raw.getDeparture().getAirport() : null;
            String destCode = raw.getArrival() != null ? raw.getArrival().getIata() : null;
            String destName = raw.getArrival() != null ? raw.getArrival().getAirport() : null;

            // Times
            Instant scheduledDep = parseInstant(raw.getDeparture() != null ? raw.getDeparture().getScheduled() : null);
            Instant scheduledArr = parseInstant(raw.getArrival() != null ? raw.getArrival().getScheduled() : null);
            Instant estimatedDep = parseInstant(raw.getDeparture() != null ? raw.getDeparture().getEstimated() : null);
            Instant estimatedArr = parseInstant(raw.getArrival() != null ? raw.getArrival().getEstimated() : null);
            Instant actualDep = parseInstant(raw.getDeparture() != null ? raw.getDeparture().getActual() : null);
            Instant actualArr = parseInstant(raw.getArrival() != null ? raw.getArrival().getActual() : null);

            // Status
            String status = mapAviationstackStatus(raw.getFlightStatus());

            // Duration
            Duration duration = (scheduledDep != null && scheduledArr != null)
                    ? Duration.between(scheduledDep, scheduledArr) : null;

            // Aircraft
            String aircraftType = raw.getAircraft() != null ? raw.getAircraft().getIata() : null;

            // Terminal/gate
            String terminal = raw.getDeparture() != null ? raw.getDeparture().getTerminal() : null;
            String gate = raw.getDeparture() != null ? raw.getDeparture().getGate() : null;

            // Bookability: only SCHEDULED and DELAYED flights are bookable
            boolean departed = scheduledDep != null && scheduledDep.isBefore(Instant.now());
            boolean cancelled = "CANCELLED".equals(status);
            boolean bookable = !departed && !cancelled && scheduledDep != null && scheduledDep.isAfter(Instant.now());

            return NormalizedFlight.builder()
                    .flightNumber(flightNumber)
                    .airlineName(airlineName)
                    .airlineCode(airlineCode)
                    .originCode(originCode)
                    .originName(originName)
                    .destinationCode(destCode)
                    .destinationName(destName)
                    .scheduledDeparture(scheduledDep)
                    .scheduledArrival(scheduledArr)
                    .estimatedDeparture(estimatedDep)
                    .estimatedArrival(estimatedArr)
                    .actualDeparture(actualDep)
                    .actualArrival(actualArr)
                    .status(status)
                    .duration(duration)
                    .aircraftType(aircraftType)
                    .inventoryManagedByFalcon(false)  // External data — no seat inventory
                    .totalSeats(0)
                    .availableSeats(0)
                    .basePrice(null)  // No pricing from external API
                    .currency("INR")
                    .bookable(bookable)
                    .bookingUnavailableReason(bookable ? null : "External flight — not available for direct booking")
                    .terminal(terminal)
                    .gate(gate)
                    .dataSource("LIVE")
                    .lastUpdated(Instant.now())
                    .build();

        } catch (Exception e) {
            log.warn("Failed to normalize Aviationstack flight: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parse Aviationstack time string to Instant.
     * Supports both UTC and local time formats.
     */
    private Instant parseInstant(AviationstackClient.AviationstackTime time) {
        if (time == null) return null;

        // Try UTC first
        if (time.getUtc() != null && !time.getUtc().isBlank()) {
            try {
                return Instant.parse(time.getUtc());
            } catch (DateTimeParseException ignored) {}
            // Try without Z suffix
            try {
                return Instant.parse(time.getUtc() + "Z");
            } catch (DateTimeParseException ignored) {}
            // Try common format
            try {
                LocalDateTime ldt = LocalDateTime.parse(time.getUtc(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                return ldt.toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {}
        }

        // Try local time
        if (time.getLocal() != null && !time.getLocal().isBlank()) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(time.getLocal(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                return ldt.toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {}
        }

        return null;
    }

    /**
     * Map Aviationstack flight_status to Falcon's normalized status.
     * Never produces contradictory states.
     */
    private String mapAviationstackStatus(String apiStatus) {
        if (apiStatus == null) return "UNKNOWN";
        return switch (apiStatus.toLowerCase()) {
            case "scheduled" -> "SCHEDULED";
            case "active" -> "ACTIVE";
            case "landed" -> "LANDED";
            case "cancelled" -> "CANCELLED";
            case "diverted", "incident" -> "DELAYED";
            case "delayed" -> "DELAYED";
            case "unknown" -> "UNKNOWN";
            default -> "UNKNOWN";
        };
    }

    /**
     * Apply search filters that Aviationstack may not fully support server-side.
     */
    private boolean matchesFilters(NormalizedFlight flight, FlightSearchRequest request) {
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            if (!request.getStatus().equalsIgnoreCase(flight.getStatus())) {
                return false;
            }
        }
        return true;
    }
}
