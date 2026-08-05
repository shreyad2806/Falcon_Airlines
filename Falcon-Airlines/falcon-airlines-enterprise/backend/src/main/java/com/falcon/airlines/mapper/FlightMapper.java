package com.falcon.airlines.mapper;

import com.falcon.airlines.dto.request.FlightRequest;
import com.falcon.airlines.dto.response.FlightResponse;
import com.falcon.airlines.entity.Flight;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FlightMapper {

    @Mapping(target = "originAirport.id", source = "originAirportId")
    @Mapping(target = "destinationAirport.id", source = "destinationAirportId")
    @Mapping(target = "aircraft.id", source = "aircraftId")
    Flight toEntity(FlightRequest dto);

    @Mapping(target = "originAirportId", source = "originAirport.id")
    @Mapping(target = "originAirportIataCode", source = "originAirport.iataCode")
    @Mapping(target = "destinationAirportId", source = "destinationAirport.id")
    @Mapping(target = "destinationAirportIataCode", source = "destinationAirport.iataCode")
    @Mapping(target = "aircraftId", source = "aircraft.id")
    @Mapping(target = "aircraftRegistrationNumber", source = "aircraft.registrationNumber")
    FlightResponse toResponse(Flight entity);
}
