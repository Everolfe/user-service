package com.github.everolfe.userservice.mapper.paymentcardmapper;

import com.github.everolfe.userservice.dto.paymentcarddto.GetPaymentCardDto;
import com.github.everolfe.userservice.entity.PaymentCard;
import com.github.everolfe.userservice.mapper.BaseMapper;
import java.time.LocalDate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(config = BaseMapper.class)
public interface GetPaymentCardMapper extends
        BaseMapper<PaymentCard, GetPaymentCardDto>, ExpirationDateMapper {

    @Override
    @Mapping(source = "expirationDate", target = "expirationDate", qualifiedByName = "localDateToString")
    @Mapping(source = "user.id", target = "userId")
    GetPaymentCardDto toDto(PaymentCard paymentCard);

    @Override
    @Mapping(source = "expirationDate", target = "expirationDate", qualifiedByName = "stringToLocalDate")
    @Mapping(target = "user", ignore = true)
    PaymentCard toEntity(GetPaymentCardDto dto);

}
