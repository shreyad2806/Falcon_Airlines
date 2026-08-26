package com.falcon.airlines.service;

import com.falcon.airlines.provider.FlightDataProvider;
import com.falcon.airlines.provider.FlightSearchRequest;
import com.falcon.airlines.provider.NormalizedFlight;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Flight search service with primary provider + database fallback.
 *
 * Flow:
 *   1. Try primary provider (Aviationstack or Mock)
 *   2. On success → return live data
 *   3. On failure → fall back to database provider
 *   4. Cache results to reduce API calls
 *
 * The database provider is always available as a safety net.
 */
@Slf4j
@Service
public class FlightSearchService {

    private final FlightDataProvider primaryProvider;
    private final FlightDataProvider databaseProvider;

    // In-memory cache with configurable TTL
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final Duration SEARCH_CACHE_TTL = Duration.ofSeconds(300); // 5 minutes for search
    private static final Duration STATUS_CACHE_TTL = Duration.ofSeconds(30);  // 30 seconds for live status

    public FlightSearchService(
            FlightDataProvider primaryProvider,
            @Qualifier("databaseFlightProvider") FlightDataProvider databaseProvider) {
        this.primaryProvider = primaryProvider;
        this.databaseProvider = databaseProvider;
        log.info("FlightSearchService: primary={}, fallback=Database",
                primaryProvider.getProviderName());
    }

    /**
     * Search flights with fallback logic.
     *
     * TRY primary provider → SUCCESS → return LIVE data
     *                           ↓ FAIL
     *                      log warning → search DATABASE → return fallback data
     */
    public List<NormalizedFlight> searchFlights(String origin, String destination,
                                                  LocalDate departureDate, String flightNumber,
                                                  String status, int page, int size) {
        String cacheKey = buildCacheKey("search", origin, destination, departureDate, flightNumber, status, page, size);

        // Check cache
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Cache hit for flight search: {}", cacheKey);
            return cached.flights;
        }

        FlightSearchRequest request = FlightSearchRequest.builder()
                .origin(origin)
                .destination(destination)
                .departureDate(departureDate)
                .flightNumber(flightNumber)
                .status(status)
                .page(page)
                .size(size)
                .build();

        // STEP 1: Try primary provider
        try {
            List<NormalizedFlight> liveFlights = primaryProvider.searchFlights(request);

            if (!liveFlights.isEmpty()) {
                // Mark as live data
                liveFlights.forEach(f -> {
                    f.setDataSource("LIVE");
                    f.setLastUpdated(Instant.now());
                });

                cache.put(cacheKey, new CacheEntry(liveFlights, Instant.now()));
                evictExpired();
                log.info("Returning {} live flights from {}", liveFlights.size(), primaryProvider.getProviderName());
                return liveFlights;
            }

            // Primary returned empty — try database as well
            log.debug("Primary provider returned empty results, trying database fallback");

        } catch (Exception ex) {
            log.warn("Primary flight provider unavailable ({}). Falling back to database. Error: {}",
                    primaryProvider.getProviderName(), ex.getMessage());
        }

        // STEP 2: Database fallback
        try {
            List<NormalizedFlight> fallbackFlights = databaseProvider.searchFlights(request);

            fallbackFlights.forEach(f -> {
                f.setDataSource("DATABASE_FALLBACK");
                f.setLastUpdated(Instant.now());
            });

            cache.put(cacheKey, new CacheEntry(fallbackFlights, Instant.now()));
            evictExpired();

            if (!fallbackFlights.isEmpty()) {
                log.info("Returning {} fallback flights from database", fallbackFlights.size());
            } else {
                log.info("No flights found in database fallback either");
            }
            return fallbackFlights;

        } catch (Exception ex) {
            log.error("Database fallback also failed: {}", ex.getMessage(), ex);
            return List.of();
        }
    }

    /**
     * Get live flight status with fallback.
     */
    public Optional<NormalizedFlight> getFlightStatus(String flightNumber, LocalDate date) {
        String cacheKey = buildCacheKey("status", null, null, date, flightNumber, null, 0, 0);

        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired() && !cached.flights.isEmpty()) {
            return Optional.of(cached.flights.get(0));
        }

        // Try primary
        try {
            Optional<NormalizedFlight> live = primaryProvider.getFlightStatus(flightNumber, date);
            if (live.isPresent()) {
                NormalizedFlight f = live.get();
                f.setDataSource("LIVE");
                f.setLastUpdated(Instant.now());
                cache.put(cacheKey, new CacheEntry(List.of(f), Instant.now()));
                return live;
            }
        } catch (Exception ex) {
            log.warn("Primary provider failed for status check on {}: {}", flightNumber, ex.getMessage());
        }

        // Fallback to database
        try {
            Optional<NormalizedFlight> db = databaseProvider.getFlightStatus(flightNumber, date);
            db.ifPresent(f -> {
                f.setDataSource("DATABASE_FALLBACK");
                f.setLastUpdated(Instant.now());
            });
            return db;
        } catch (Exception ex) {
            log.error("Database fallback failed for status check: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Force refresh a flight's status.
     */
    public Optional<NormalizedFlight> refreshFlightStatus(String flightNumber) {
        cache.keySet().removeIf(key -> key.contains(flightNumber));

        try {
            Optional<NormalizedFlight> live = primaryProvider.getFlightStatus(flightNumber, LocalDate.now());
            if (live.isPresent()) {
                live.get().setDataSource("LIVE");
                live.get().setLastUpdated(Instant.now());
                return live;
            }
        } catch (Exception ex) {
            log.warn("Refresh failed for {}: {}", flightNumber, ex.getMessage());
        }

        return databaseProvider.getFlightStatus(flightNumber, LocalDate.now());
    }

    public boolean isProviderAvailable() {
        return primaryProvider.isAvailable();
    }

    public String getProviderName() {
        return primaryProvider.getProviderName();
    }

    // ---- Private helpers ----

    private String buildCacheKey(String type, String origin, String dest, LocalDate date,
                                  String flightNum, String status, int page, int size) {
        return String.format("%s:%s:%s:%s:%s:%s:%d:%d",
                type,
                origin != null ? origin : "",
                dest != null ? dest : "",
                date != null ? date.toString() : "",
                flightNum != null ? flightNum : "",
                status != null ? status : "",
                page, size);
    }

    private void evictExpired() {
        cache.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    private record CacheEntry(List<NormalizedFlight> flights, Instant cachedAt) {
        boolean isExpired() {
            return Instant.now().isAfter(cachedAt.plus(SEARCH_CACHE_TTL));
        }
    }
}
