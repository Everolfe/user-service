package com.github.everolfe.userservice.mapper.usermapper;

import com.github.everolfe.userservice.entity.User;
import com.github.everolfe.userservice.dto.userdto.GetUserDto;
import com.github.everolfe.userservice.mapper.BaseMapper;
import com.github.everolfe.userservice.mapper.paymentcardmapper.GetPaymentCardMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapper.class, uses = GetPaymentCardMapper.class )
public interface GetUserMapper extends BaseMapper<User, GetUserDto> {
}
