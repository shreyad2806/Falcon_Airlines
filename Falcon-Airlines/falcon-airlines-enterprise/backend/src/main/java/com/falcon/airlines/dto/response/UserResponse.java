package com.falcon.airlines.dto.response;

import com.falcon.airlines.enums.UserStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String mobileNumber;
    private UserStatus status;
    private Boolean mfaEnabled;
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant updatedAt;
}
