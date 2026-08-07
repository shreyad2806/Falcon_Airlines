package com.falcon.airlines.config;

import com.falcon.airlines.controller.AdminController;
import com.falcon.airlines.security.jwt.JwtAuthenticationFilter;
import com.falcon.airlines.security.jwt.JwtService;
import com.falcon.airlines.security.jwt.JwtTokenUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link SecurityConfig}.
 * <p>
 * Verifies that the required security beans are wired and that the
 * {@link org.springframework.security.web.SecurityFilterChain} permits the
 * public endpoints while requiring authentication for all others.
 */
@WebMvcTest(controllers = {TestSecurityController.class, AdminController.class})
@Import({SecurityConfig.class, JwtTokenUtil.class, JwtService.class, JwtAuthenticationFilter.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void bCryptPasswordEncoderBeanIsAvailable() {
        assertThat(bCryptPasswordEncoder).isNotNull();
        assertThat(bCryptPasswordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void authenticationManagerBeanIsAvailable() {
        assertThat(authenticationManager).isNotNull();
    }

    @Test
    void authEndpointPermitsAnonymous() throws Exception {
        mockMvc.perform(get("/auth/ping")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerUiEndpointPermitsAnonymous() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void apiDocsEndpointPermitsAnonymous() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorHealthEndpointPermitsAnonymous() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpointRejectsAnonymous() throws Exception {
        mockMvc.perform(get("/api/flights"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpointAllowsAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/flights")
                        .with(user("alice").roles("USER")))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void protectedEndpointAllowsMockAdmin() throws Exception {
        mockMvc.perform(get("/api/flights"))
                .andExpect(status().isOk());
    }

    @Test
    void adminDashboardAllowsAdmin() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void adminDashboardRejectsCustomer() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                        .with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminBookingsAllowsBookingWriteAuthority() throws Exception {
        mockMvc.perform(get("/admin/bookings")
                        .with(user("agent").authorities(new SimpleGrantedAuthority("BOOKING_WRITE"))))
                .andExpect(status().isOk());
    }

    @Test
    void adminBookingsRejectsCustomer() throws Exception {
        mockMvc.perform(get("/admin/bookings")
                        .with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerAreaAllowsCustomer() throws Exception {
        mockMvc.perform(get("/admin/customer-area")
                        .with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isOk());
    }

    @Test
    void customerAreaAllowsAdminDueToRoleHierarchy() throws Exception {
        mockMvc.perform(get("/admin/customer-area")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void customerAreaRejectsAnonymous() throws Exception {
        mockMvc.perform(get("/admin/customer-area"))
                .andExpect(status().isForbidden());
    }
}
