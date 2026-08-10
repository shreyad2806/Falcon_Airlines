package com.falcon.airlines.dto.request;

import com.falcon.airlines.common.BaseUnitTest;
import com.falcon.airlines.enums.Gender;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PassengerRequestValidationTest extends BaseUnitTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private PassengerRequest buildValidRequest() {
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

    @Test
    void validRequest_shouldPass() {
        PassengerRequest request = buildValidRequest();
        Set<ConstraintViolation<PassengerRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void blankFirstName_shouldFail() {
        PassengerRequest request = buildValidRequest();
        request.setFirstName("");
        Set<ConstraintViolation<PassengerRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("firstName"));
    }

    @Test
    void firstNameTooLong_shouldFail() {
        PassengerRequest request = buildValidRequest();
        request.setFirstName("A".repeat(101));
        Set<ConstraintViolation<PassengerRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("firstName"));
    }

    @Test
    void blankLastName_shouldFail() {
        PassengerRequest request = buildValidRequest();
        request.setLastName("");
        Set<ConstraintViolation<PassengerRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("lastName"));
    }

    @Test
    void lastNameTooLong_shouldFail() {
        PassengerRequest request = buildValidRequest();
        request.setLastName("A".repeat(101));
        Set<ConstraintViolation<PassengerRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("lastName"));
    }

    @Test
    void nullDateOfBirth_shouldFail() {
        PassengerRequest request = buildValidRequest();
        request.setDateOfBirth(null);
        Set<ConstraintViolation<PassengerRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("dateOfBirth"));
    }

    @Test
    void futureDateOfBirth_shouldFail() {
        PassengerRequest request = buildValidRequest();
        request.setDateOfBirth(LocalDate.now().plusDays(1));
        Set<ConstraintViolation<PassengerRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("dateOfBirth"));
    }

    @Test
    void invalidEmail_shouldFail() {
        PassengerRequest request = buildValidRequest();
        request.setEmail("invalid-email");
        Set<ConstraintViolation<PassengerRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    void emailTooLong_shouldFail() {
        PassengerRequest request = buildValidRequest();
        request.setEmail("a".repeat(250) + "@example.com");
        Set<ConstraintViolation<PassengerRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    void invalidPhone_shouldFail() {
        PassengerRequest request = buildValidRequest();
        request.setPhone("abc");
        Set<ConstraintViolation<PassengerRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("phone"));
    }

    @Test
    void phoneTooLong_shouldFail() {
        PassengerRequest request = buildValidRequest();
        request.setPhone("+1-234-567-8901-23456");
        Set<ConstraintViolation<PassengerRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("phone"));
    }

    @Test
    void validPhoneFormats_shouldPass() {
        PassengerRequest request = buildValidRequest();
        
        request.setPhone("+1234567890");
        assertThat(validator.validate(request)).isEmpty();
        
        request.setPhone("+1-234-567-8901");
        assertThat(validator.validate(request)).isEmpty();
        
        request.setPhone("(123) 456-7890");
        assertThat(validator.validate(request)).isEmpty();
        
        request.setPhone("1234567890");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void invalidPassportNumber_shouldFail() {
        PassengerRequest request = buildValidRequest();
        request.setPassportNumber("abc@123");
        Set<ConstraintViolation<PassengerRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("passportNumber"));
    }

    @Test
    void passportNumberTooLong_shouldFail() {
        PassengerRequest request = buildValidRequest();
        request.setPassportNumber("A".repeat(51));
        Set<ConstraintViolation<PassengerRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("passportNumber"));
    }

    @Test
    void validPassportNumberFormats_shouldPass() {
        PassengerRequest request = buildValidRequest();
        
        request.setPassportNumber("AB1234567");
        assertThat(validator.validate(request)).isEmpty();
        
        request.setPassportNumber("A12<3456<7");
        assertThat(validator.validate(request)).isEmpty();
        
        request.setPassportNumber("123456789");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void invalidNationality_shouldFail() {
        PassengerRequest request = buildValidRequest();
        request.setNationality("US");
        Set<ConstraintViolation<PassengerRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("nationality"));
    }

    @Test
    void nationalityTooLong_shouldFail() {
        PassengerRequest request = buildValidRequest();
        request.setNationality("USA1");
        Set<ConstraintViolation<PassengerRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("nationality"));
    }

    @Test
    void validNationalityFormats_shouldPass() {
        PassengerRequest request = buildValidRequest();
        
        request.setNationality("USA");
        assertThat(validator.validate(request)).isEmpty();
        
        request.setNationality("GBR");
        assertThat(validator.validate(request)).isEmpty();
        
        request.setNationality("FRA");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void nullGender_shouldFail() {
        PassengerRequest request = buildValidRequest();
        request.setGender(null);
        Set<ConstraintViolation<PassengerRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("gender"));
    }

    @Test
    void redressNumberTooLong_shouldFail() {
        PassengerRequest request = buildValidRequest();
        request.setRedressNumber("A".repeat(21));
        Set<ConstraintViolation<PassengerRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("redressNumber"));
    }

    @Test
    void allOptionalFieldsNull_shouldPass() {
        PassengerRequest request = new PassengerRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setGender(Gender.M);
        Set<ConstraintViolation<PassengerRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }
}
