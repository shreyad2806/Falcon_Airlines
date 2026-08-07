package com.falcon.airlines.mapper;

import com.falcon.airlines.dto.request.RegisterRequest;
import com.falcon.airlines.dto.request.UserRequest;
import com.falcon.airlines.dto.response.UserResponse;
import com.falcon.airlines.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "passwordHash", source = "password")
    User toEntity(UserRequest dto);

    @Mapping(target = "passwordHash", source = "password")
    User toEntity(RegisterRequest dto);

    UserResponse toResponse(User entity);
}
