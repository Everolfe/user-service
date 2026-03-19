package com.github.everolfe.userservice.mapper.paymentcardmapper;

import com.github.everolfe.userservice.dto.paymentcarddto.CreatePaymentCardDto;
import com.github.everolfe.userservice.entity.PaymentCard;
import com.github.everolfe.userservice.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = BaseMapper.class)
public interface CreatePaymentCardMapper extends
        BaseMapper<PaymentCard, CreatePaymentCardDto>, ExpirationDateMapper{

    @Override
    @Mapping(source = "expirationDate", target = "expirationDate",
            qualifiedByName = "stringToLocalDate")
    PaymentCard toEntity(CreatePaymentCardDto createPaymentCardDto);

    @Override
    @Mapping(source = "expirationDate", target = "expirationDate",
            qualifiedByName = "localDateToString")
    CreatePaymentCardDto toDto(PaymentCard paymentCard);
}
