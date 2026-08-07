package com.falcon.airlines.controller;

import com.falcon.airlines.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example administration endpoints demonstrating method-level RBAC.
 * <p>
 * The {@code @PreAuthorize} expressions use Spring Security's {@code hasRole} and
 * {@code hasAnyAuthority} SpEL functions. {@code hasRole} is hierarchy-aware because
 * a {@link org.springframework.security.access.hierarchicalroles.RoleHierarchy} bean
 * has been configured.
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> dashboard() {
        return ResponseEntity.ok(ApiResponse.ok("Admin dashboard"));
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasAnyAuthority('BOOKING_READ', 'BOOKING_WRITE')")
    public ResponseEntity<ApiResponse<String>> bookings() {
        return ResponseEntity.ok(ApiResponse.ok("Bookings area"));
    }

    @GetMapping("/customer-area")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<String>> customerArea() {
        return ResponseEntity.ok(ApiResponse.ok("Customer area"));
    }
}
