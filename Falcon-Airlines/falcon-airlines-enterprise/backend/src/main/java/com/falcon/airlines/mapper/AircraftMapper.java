package com.falcon.airlines.mapper;

import com.falcon.airlines.dto.request.AircraftRequest;
import com.falcon.airlines.dto.response.AircraftResponse;
import com.falcon.airlines.entity.Aircraft;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AircraftMapper {

    Aircraft toEntity(AircraftRequest dto);
    AircraftResponse toResponse(Aircraft entity);
}
