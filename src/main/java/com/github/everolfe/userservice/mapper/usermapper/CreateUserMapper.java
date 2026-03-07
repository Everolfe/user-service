package com.github.everolfe.userservice.mapper.usermapper;

import com.github.everolfe.userservice.dto.userdto.CreateUserDto;
import com.github.everolfe.userservice.mapper.BaseMapper;
import com.github.everolfe.userservice.entity.User;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapper.class)
public interface CreateUserMapper extends BaseMapper<User, CreateUserDto> {
}
