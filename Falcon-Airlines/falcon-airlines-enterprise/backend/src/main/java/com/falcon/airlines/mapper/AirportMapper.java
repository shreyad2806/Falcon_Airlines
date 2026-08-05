package com.falcon.airlines.mapper;

import com.falcon.airlines.dto.request.AirportRequest;
import com.falcon.airlines.dto.response.AirportResponse;
import com.falcon.airlines.entity.Airport;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AirportMapper {

    Airport toEntity(AirportRequest dto);
    AirportResponse toResponse(Airport entity);
}
