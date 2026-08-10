package com.falcon.airlines.repository;

import com.falcon.airlines.common.BaseIntegrationTest;
import com.falcon.airlines.entity.Passenger;
import com.falcon.airlines.enums.Gender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class PassengerRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PassengerRepository passengerRepository;

    private Passenger buildPassenger(String firstName, String lastName, String email, String passport) {
        Passenger passenger = new Passenger();
        passenger.setFirstName(firstName);
        passenger.setLastName(lastName);
        passenger.setDateOfBirth(LocalDate.of(1990, 1, 1));
        passenger.setEmail(email);
        passenger.setPhone("+1234567890");
        passenger.setPassportNumber(passport);
        passenger.setNationality("USA");
        passenger.setGender(Gender.M);
        return passenger;
    }

    @Test
    void saveAndFindPassenger() {
        Passenger passenger = buildPassenger("John", "Doe", "john.doe@example.com", "AB1234567");
        
        Passenger saved = passengerRepository.save(passenger);
        assertThat(saved.getId()).isNotNull();

        Optional<Passenger> found = passengerRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("John");
        assertThat(found.get().getLastName()).isEqualTo("Doe");
    }

    @Test
    void findById_notFound() {
        Optional<Passenger> found = passengerRepository.findById(999999L);
        assertThat(found).isEmpty();
    }

    @Test
    void findByPassportNumber() {
        Passenger passenger = buildPassenger("Jane", "Smith", "jane@example.com", "CD9876543");
        passengerRepository.save(passenger);

        Optional<Passenger> found = passengerRepository.findByPassportNumber("CD9876543");
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Jane");
    }

    @Test
    void findByPassportNumber_notFound() {
        Optional<Passenger> found = passengerRepository.findByPassportNumber("NOTFOUND");
        assertThat(found).isEmpty();
    }

    @Test
    void findByPassportNumberAndIdNot() {
        Passenger passenger = buildPassenger("Bob", "Johnson", "bob@example.com", "EF1234567");
        Passenger saved = passengerRepository.save(passenger);

        Optional<Passenger> found = passengerRepository.findByPassportNumberAndIdNot("EF1234567", saved.getId());
        assertThat(found).isEmpty();

        Optional<Passenger> same = passengerRepository.findByPassportNumberAndIdNot("EF1234567", 999999L);
        assertThat(same).isPresent();
        assertThat(same.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void findByEmail() {
        Passenger passenger = buildPassenger("Alice", "Williams", "alice@example.com", "GH7654321");
        passengerRepository.save(passenger);

        Optional<Passenger> found = passengerRepository.findByEmail("alice@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Alice");
    }

    @Test
    void findByEmail_notFound() {
        Optional<Passenger> found = passengerRepository.findByEmail("notfound@example.com");
        assertThat(found).isEmpty();
    }

    @Test
    void findByEmailAndIdNot() {
        Passenger passenger = buildPassenger("Charlie", "Brown", "charlie@example.com", "IJ8765432");
        Passenger saved = passengerRepository.save(passenger);

        Optional<Passenger> found = passengerRepository.findByEmailAndIdNot("charlie@example.com", saved.getId());
        assertThat(found).isEmpty();

        Optional<Passenger> same = passengerRepository.findByEmailAndIdNot("charlie@example.com", 999999L);
        assertThat(same).isPresent();
        assertThat(same.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void existsByUserId() {
        Passenger passenger = buildPassenger("David", "Miller", "david@example.com", "KL5432109");
        passengerRepository.save(passenger);

        boolean exists = passengerRepository.existsByUserId(null);
        assertThat(exists).isFalse();
    }

    @Test
    void paginationAndSorting() {
        passengerRepository.save(buildPassenger("Zoe", "Anderson", "zoe@example.com", "MN9876543"));
        passengerRepository.save(buildPassenger("Adam", "Baker", "adam@example.com", "OP1234567"));
        passengerRepository.save(buildPassenger("Mary", "Clark", "mary@example.com", "QR7654321"));

        Page<Passenger> page = passengerRepository.findAll(
                Specification.where((root, query, cb) -> cb.equal(root.get("isDeleted"), false)),
                PageRequest.of(0, 2, Sort.by("firstName").ascending()));

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(3);
        assertThat(page.getNumberOfElements()).isLessThanOrEqualTo(2);
        assertThat(page.getContent()).isNotEmpty();
    }

    @Test
    void searchByFirstName() {
        passengerRepository.save(buildPassenger("Test", "Search", "test@example.com", "ST1234567"));

        Specification<Passenger> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("isDeleted"), false),
                cb.like(cb.lower(root.get("firstName")), "%test%"));

        Page<Passenger> page = passengerRepository.findAll(spec, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void searchByLastName() {
        passengerRepository.save(buildPassenger("Search", "Test", "search@example.com", "TS7654321"));

        Specification<Passenger> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("isDeleted"), false),
                cb.like(cb.lower(root.get("lastName")), "%test%"));

        Page<Passenger> page = passengerRepository.findAll(spec, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void searchByEmail() {
        passengerRepository.save(buildPassenger("Email", "Test", "emailtest@example.com", "ET5432109"));

        Specification<Passenger> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("isDeleted"), false),
                cb.like(cb.lower(root.get("email")), "%emailtest%"));

        Page<Passenger> page = passengerRepository.findAll(spec, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void searchByPassportNumber() {
        passengerRepository.save(buildPassenger("Passport", "Test", "passport@example.com", "PT9876543"));

        Specification<Passenger> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("isDeleted"), false),
                cb.like(cb.lower(root.get("passportNumber")), "%pt9876543%"));

        Page<Passenger> page = passengerRepository.findAll(spec, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void searchByFullName() {
        passengerRepository.save(buildPassenger("Full", "Name", "fullname@example.com", "FN1234567"));

        Specification<Passenger> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("isDeleted"), false),
                cb.or(
                        cb.like(cb.lower(root.get("firstName")), "%full%"),
                        cb.like(cb.lower(root.get("lastName")), "%name%")));

        Page<Passenger> page = passengerRepository.findAll(spec, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void softDeleteHidesFromQueries() {
        Passenger passenger = buildPassenger("Delete", "Test", "delete@example.com", "DL7654321");
        Passenger saved = passengerRepository.save(passenger);

        passengerRepository.delete(saved);

        Optional<Passenger> after = passengerRepository.findById(saved.getId());
        assertThat(after).isEmpty();

        Optional<Passenger> byPassport = passengerRepository.findByPassportNumber("DL7654321");
        assertThat(byPassport).isEmpty();
    }

    @Test
    void softDeleteWithSpecification() {
        Passenger passenger = buildPassenger("Soft", "Delete", "soft@example.com", "SD5432109");
        Passenger saved = passengerRepository.save(passenger);

        passengerRepository.delete(saved);

        Specification<Passenger> spec = (root, query, cb) -> cb.equal(root.get("isDeleted"), false);
        Page<Passenger> page = passengerRepository.findAll(spec, PageRequest.of(0, 10));
        
        assertThat(page.getContent()).noneMatch(p -> p.getId().equals(saved.getId()));
    }

    @Test
    void updatePassenger() {
        Passenger passenger = buildPassenger("Update", "Test", "update@example.com", "UT9876543");
        Passenger saved = passengerRepository.save(passenger);

        saved.setFirstName("Updated");
        saved.setEmail("updated@example.com");
        Passenger updated = passengerRepository.save(saved);

        Optional<Passenger> found = passengerRepository.findById(updated.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Updated");
        assertThat(found.get().getEmail()).isEqualTo("updated@example.com");
    }
}
