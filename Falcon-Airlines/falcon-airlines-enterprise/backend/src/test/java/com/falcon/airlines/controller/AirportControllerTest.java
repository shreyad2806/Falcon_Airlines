package com.falcon.airlines.controller;

import com.falcon.airlines.config.SecurityConfig;
import com.falcon.airlines.dto.request.AirportRequest;
import com.falcon.airlines.dto.response.AirportResponse;
import com.falcon.airlines.exception.BaseException;
import com.falcon.airlines.response.ApiErrorResponse;
import com.falcon.airlines.security.jwt.JwtAuthenticationFilter;
import com.falcon.airlines.security.jwt.JwtService;
import com.falcon.airlines.security.jwt.JwtTokenUtil;
import com.falcon.airlines.service.AirportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AirportController.class)
@Import({SecurityConfig.class, JwtTokenUtil.class, JwtService.class, JwtAuthenticationFilter.class})
class AirportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AirportService airportService;

    @MockBean
    private UserDetailsService userDetailsService;

    private AirportRequest buildRequest() {
        AirportRequest request = new AirportRequest();
        request.setIataCode("NYC");
        request.setIcaoCode("KNYC");
        request.setName("New York Airport");
        request.setCity("New York");
        request.setCountry("US");
        request.setTimeZone("America/New_York");
        request.setLatitude(new BigDecimal("40.7128"));
        request.setLongitude(new BigDecimal("-74.0060"));
        request.setIsActive(true);
        return request;
    }

    private AirportResponse buildResponse() {
        AirportResponse response = new AirportResponse();
        response.setId(1L);
        response.setIataCode("NYC");
        response.setName("New York Airport");
        return response;
    }

    @Test
    void createAirportSuccessfully() throws Exception {
        when(airportService.createAirport(any(AirportRequest.class))).thenReturn(buildResponse());

        mockMvc.perform(post("/api/airports")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void createAirportWithInvalidData() throws Exception {
        AirportRequest request = buildRequest();
        request.setIataCode("AB");

        mockMvc.perform(post("/api/airports")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAirportDuplicateCode() throws Exception {
        when(airportService.createAirport(any(AirportRequest.class)))
                .thenThrow(new BaseException("Duplicate IATA code", HttpStatus.CONFLICT, "DUPLICATE_IATA_CODE"));

        mockMvc.perform(post("/api/airports")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isConflict());
    }

    @Test
    void getAirportById() throws Exception {
        when(airportService.getAirportById(1L)).thenReturn(buildResponse());

        mockMvc.perform(get("/api/airports/1")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("FLIGHT_READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getAirportNotFound() throws Exception {
        when(airportService.getAirportById(99L))
                .thenThrow(new BaseException("Airport not found", HttpStatus.NOT_FOUND, "AIRPORT_NOT_FOUND"));

        mockMvc.perform(get("/api/airports/99")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("FLIGHT_READ"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAirport() throws Exception {
        when(airportService.updateAirport(eq(1L), any(AirportRequest.class))).thenReturn(buildResponse());

        mockMvc.perform(put("/api/airports/1")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void updateNonexistentAirport() throws Exception {
        when(airportService.updateAirport(eq(99L), any(AirportRequest.class)))
                .thenThrow(new BaseException("Airport not found", HttpStatus.NOT_FOUND, "AIRPORT_NOT_FOUND"));

        mockMvc.perform(put("/api/airports/99")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAirport() throws Exception {
        mockMvc.perform(delete("/api/airports/1")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void searchAirports() throws Exception {
        Page<AirportResponse> page = new PageImpl<>(List.of(buildResponse()));
        when(airportService.searchAirports(any(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/airports")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("FLIGHT_READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void searchAirportsWithFilter() throws Exception {
        Page<AirportResponse> page = new PageImpl<>(Collections.emptyList());
        when(airportService.searchAirports(eq("JFK"), isNull(), isNull(), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/airports")
                        .param("code", "JFK")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("FLIGHT_READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void searchAirportsPaginated() throws Exception {
        Page<AirportResponse> page = new PageImpl<>(List.of(buildResponse()), org.springframework.data.domain.PageRequest.of(0, 2), 5);
        when(airportService.searchAirports(any(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/airports")
                        .param("page", "0")
                        .param("size", "2")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("FLIGHT_READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(2));
    }

    @Test
    void searchAirportsSorted() throws Exception {
        Page<AirportResponse> page = new PageImpl<>(List.of(buildResponse()));
        when(airportService.searchAirports(any(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/airports")
                        .param("sort", "name,asc")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("FLIGHT_READ"))))
                .andExpect(status().isOk());
    }
}
