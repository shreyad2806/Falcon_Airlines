package com.falcon.airlines.mapper;

import com.falcon.airlines.dto.request.PassengerRequest;
import com.falcon.airlines.dto.response.PassengerResponse;
import com.falcon.airlines.entity.Passenger;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PassengerMapper {

    @Mapping(target = "user.id", source = "userId")
    Passenger toEntity(PassengerRequest dto);

    @Mapping(target = "userId", source = "user.id")
    PassengerResponse toResponse(Passenger entity);
}
