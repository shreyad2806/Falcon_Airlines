package com.falcon.airlines.repository;

import com.falcon.airlines.common.BaseIntegrationTest;
import com.falcon.airlines.entity.Airport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class AirportRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AirportRepository airportRepository;

    @Test
    void seedDataLoaded() {
        Optional<Airport> jfk = airportRepository.findByIataCode("JFK");
        assertThat(jfk).isPresent();
        assertThat(jfk.get().getCity()).isEqualTo("New York");
    }

    @Test
    void saveAndFindAirport() {
        Airport airport = new Airport();
        airport.setIataCode("NEW");
        airport.setIcaoCode("KNEW");
        airport.setName("New Test Airport");
        airport.setCity("Testville");
        airport.setCountry("US");
        airport.setTimeZone("America/New_York");
        airport.setLatitude(new BigDecimal("12.3456"));
        airport.setLongitude(new BigDecimal("-65.4321"));
        airport.setIsActive(true);

        Airport saved = airportRepository.save(airport);
        assertThat(saved.getId()).isNotNull();

        Optional<Airport> found = airportRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getIataCode()).isEqualTo("NEW");
    }

    @Test
    void findByIataCodeAndIcaoCode() {
        Optional<Airport> byIata = airportRepository.findByIataCode("JFK");
        Optional<Airport> byIcao = airportRepository.findByIcaoCode("KJFK");

        assertThat(byIata).isPresent();
        assertThat(byIcao).isPresent();
        assertThat(byIata.get().getId()).isEqualTo(byIcao.get().getId());
    }

    @Test
    void findByIataCodeAndIdNot() {
        Airport jfk = airportRepository.findByIataCode("JFK").orElseThrow();
        Optional<Airport> other = airportRepository.findByIataCodeAndIdNot("JFK", jfk.getId());
        assertThat(other).isEmpty();
    }

    @Test
    void findByIcaoCodeAndIdNot() {
        Airport jfk = airportRepository.findByIcaoCode("KJFK").orElseThrow();
        Optional<Airport> other = airportRepository.findByIcaoCodeAndIdNot("KJFK", jfk.getId());
        assertThat(other).isEmpty();
    }

    @Test
    void paginationAndSorting() {
        Page<Airport> page = airportRepository.findAll(
                Specification.where((root, query, cb) -> cb.equal(root.get("isDeleted"), false)),
                PageRequest.of(0, 3, Sort.by("iataCode").ascending()));

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(6);
        assertThat(page.getNumberOfElements()).isLessThanOrEqualTo(3);
        assertThat(page.getContent().get(0).getIataCode()).isNotNull();
    }

    @Test
    void searchByNameAndCity() {
        Specification<Airport> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("isDeleted"), false),
                cb.or(
                        cb.like(cb.lower(root.get("name")), "%heathrow%"),
                        cb.like(cb.lower(root.get("city")), "%london%")));

        Page<Airport> page = airportRepository.findAll(spec, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void softDeleteHidesFromQueries() {
        Airport airport = airportRepository.findByIataCode("JFK").orElseThrow();
        airport.setDeleted(true);
        airport.setIsActive(false);
        airportRepository.save(airport);

        Optional<Airport> after = airportRepository.findByIataCode("JFK");
        assertThat(after).isEmpty();
    }
}
