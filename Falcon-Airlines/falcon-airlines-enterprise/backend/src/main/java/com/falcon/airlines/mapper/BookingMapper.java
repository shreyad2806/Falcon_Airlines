package com.falcon.airlines.mapper;

import com.falcon.airlines.dto.request.BookingRequest;
import com.falcon.airlines.dto.response.BookingResponse;
import com.falcon.airlines.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BookingMapper {

    @Mapping(target = "customer.id", source = "customerId")
    Booking toEntity(BookingRequest dto);

    @Mapping(target = "customerId", source = "customer.id")
    BookingResponse toResponse(Booking entity);
}
