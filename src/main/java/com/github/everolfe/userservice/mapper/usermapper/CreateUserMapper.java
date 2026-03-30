package com.github.everolfe.userservice.mapper.usermapper;

import com.github.everolfe.userservice.dto.userdto.CreateUserDto;
import com.github.everolfe.userservice.mapper.BaseMapper;
import com.github.everolfe.userservice.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = BaseMapper.class)
public interface CreateUserMapper extends BaseMapper<User, CreateUserDto> {
    @Override
    @Mapping(target = "active", defaultExpression = "java(true)")
    User toEntity(CreateUserDto createUserDto);
}
