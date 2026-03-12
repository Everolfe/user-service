package com.github.everolfe.userservice.unit;


import com.github.everolfe.userservice.dao.PaymentCardRepository;
import com.github.everolfe.userservice.dao.PaymentCardSpecification;
import com.github.everolfe.userservice.dao.UserRepository;
import com.github.everolfe.userservice.dto.paymentcarddto.CreatePaymentCardDto;
import com.github.everolfe.userservice.dto.paymentcarddto.GetPaymentCardDto;
import com.github.everolfe.userservice.entity.PaymentCard;
import com.github.everolfe.userservice.entity.User;
import com.github.everolfe.userservice.exception.DuplicateResourceException;
import com.github.everolfe.userservice.exception.ResourceNotFoundException;
import com.github.everolfe.userservice.mapper.paymentcardmapper.CreatePaymentCardMapper;
import com.github.everolfe.userservice.mapper.paymentcardmapper.GetPaymentCardMapper;
import com.github.everolfe.userservice.service.PaymentCardService;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class PaymentCardServiceTest {

    @Mock
    private PaymentCardRepository paymentCardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GetPaymentCardMapper getPaymentCardMapper;

    @Mock
    private CreatePaymentCardMapper createPaymentCardMapper;

    @InjectMocks
    private PaymentCardService paymentCardService;

    @Test
    void testCreatePaymentCardSuccess() {
        Long userId = 1L;
        CreatePaymentCardDto createPaymentCardDto = new CreatePaymentCardDto();
        User user = new User();
        user.setId(userId);

        PaymentCard cardToSave = new PaymentCard();
        PaymentCard savedCard = new PaymentCard();
        savedCard.setId(10L);
        savedCard.setUser(user);

        GetPaymentCardDto getPaymentCardDto = new GetPaymentCardDto();
        getPaymentCardDto.setId(10L);
        getPaymentCardDto.setUserId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(createPaymentCardMapper.toEntity(createPaymentCardDto)).thenReturn(cardToSave);
        when(paymentCardRepository.save(cardToSave)).thenReturn(savedCard);
        when(getPaymentCardMapper.toDto(savedCard)).thenReturn(getPaymentCardDto);

        GetPaymentCardDto result = paymentCardService.create(createPaymentCardDto,userId);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());

        verify(userRepository,times(1)).findById(userId);
        verify(paymentCardRepository,times(1)).save(cardToSave);
    }

    @Test
    void testCreatePaymentCardWithoutUserId() {
        Long userID = 99L;
        CreatePaymentCardDto createPaymentCardDto = new CreatePaymentCardDto();

        when(userRepository.findById(userID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentCardService.create(createPaymentCardDto,userID));

        verify(paymentCardRepository,never()).save(any());
    }

    @Test
    void testCreateCardWithExistingNumber(){
        CreatePaymentCardDto createPaymentCardDto = new CreatePaymentCardDto();
        createPaymentCardDto.setNumber("1234567890");
        when(paymentCardRepository.existsByNumber(createPaymentCardDto.getNumber())).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> paymentCardService.create(createPaymentCardDto,1L));
    }

    @Test
    void testGetPaymentCardByIdSuccess() {
        PaymentCard cardEntity = new PaymentCard();
        cardEntity.setId(10L);
        GetPaymentCardDto getPaymentCardDto = new GetPaymentCardDto();
        getPaymentCardDto.setId(10L);

        when(paymentCardRepository.findById(10L)).thenReturn(Optional.of(cardEntity));
        when(getPaymentCardMapper.toDto(cardEntity)).thenReturn(getPaymentCardDto);

        GetPaymentCardDto result = paymentCardService.getPaymentCardById(10L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
    }

    @Test
    void testGetPaymentCardByIdWithIncorrectId() {
        assertThrows(ResourceNotFoundException.class,
                () -> paymentCardService.getPaymentCardById(10L));
    }


    @Test
    void testGetAllPaymentCardSuccess() {
        Pageable pageable = Pageable.unpaged();
        PaymentCard paymentCardEntity = new PaymentCard();

        Page<PaymentCard> page = new PageImpl<>(List.of(paymentCardEntity));

        when(paymentCardRepository.findAll(pageable)).thenReturn(page);

        Page<GetPaymentCardDto> result = paymentCardService.getAllPaymentCards(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(getPaymentCardMapper,times(1)).toDto(paymentCardEntity);
    }

    @Test
    void testPaymentCardWithUserIdSuccess() {
        PaymentCard paymentCardEntity = new PaymentCard();
        paymentCardEntity.setId(10L);

        User user = new User();
        user.setId(1L);
        paymentCardEntity.setUser(user);

        GetPaymentCardDto getPaymentCardDto = new GetPaymentCardDto();
        getPaymentCardDto.setId(10L);
        getPaymentCardDto.setUserId(1L);

        List<PaymentCard> userCards = List.of(paymentCardEntity);

        when(userRepository.existsById(1L)).thenReturn(true);
        when(paymentCardRepository.findAllCardsByUserId(1L)).thenReturn(userCards);
        when(getPaymentCardMapper.toDto(paymentCardEntity)).thenReturn(getPaymentCardDto);

        List<GetPaymentCardDto> result = paymentCardService.getPaymentCardsByUserId(1L);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
    }

    @Test
    void testPaymentCardWithIncorrectUserId() {
        assertThrows(ResourceNotFoundException.class,
                () -> paymentCardService.getPaymentCardsByUserId(990L));
    }

    @Test
    void testSearchCardsByUserNameAndSurname() {
        String name = "John";
        String surname = "Doe";
        Pageable pageable = Pageable.unpaged();

        User user = new User();
        user.setId(1L);
        user.setName(name);
        user.setSurname(surname);

        PaymentCard card1 = new PaymentCard();
        card1.setId(10L);
        card1.setNumber("1111222233334444");
        card1.setUser(user);

        PaymentCard card2 = new PaymentCard();
        card2.setId(11L);
        card2.setNumber("5555666677778888");
        card2.setUser(user);

        List<PaymentCard> cards = List.of(card1, card2);
        Page<PaymentCard> cardPage = new PageImpl<>(cards, pageable, cards.size());

        GetPaymentCardDto dto1 = new GetPaymentCardDto();
        dto1.setId(10L);
        dto1.setNumber("1111222233334444");
        dto1.setUserId(1L);

        GetPaymentCardDto dto2 = new GetPaymentCardDto();
        dto2.setId(11L);
        dto2.setNumber("5555666677778888");
        dto2.setUserId(1L);

        Specification<PaymentCard> expectedSpec =
                PaymentCardSpecification.byUserNameAndSurname(name, surname);

        when(paymentCardRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(cardPage);
        when(getPaymentCardMapper.toDto(card1)).thenReturn(dto1);
        when(getPaymentCardMapper.toDto(card2)).thenReturn(dto2);
        Page<GetPaymentCardDto> result = paymentCardService
                .searchCardsByUserNameAndSurname(name, surname, pageable);
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(2, result.getTotalElements()),
                () -> assertEquals(2, result.getContent().size()),
                () -> assertEquals(10L, result.getContent().get(0).getId()),
                () -> assertEquals(11L, result.getContent().get(1).getId()),
                () -> assertEquals("1111222233334444", result.getContent().get(0).getNumber()),
                () -> assertEquals(1L, result.getContent().get(0).getUserId())
        );

        verify(paymentCardRepository, times(1))
                .findAll(any(Specification.class), eq(pageable));
        verify(getPaymentCardMapper, times(1)).toDto(card1);
        verify(getPaymentCardMapper, times(1)).toDto(card2);
    }

    @Test
    void testActivateCardSuccess() {
        when(paymentCardRepository.activateCard(10L)).thenReturn(1);
        paymentCardService.activateCard(10L);
        verify(paymentCardRepository,times(1)).activateCard(10L);
    }

    @Test
    void testActivateCardWithIncorrectId() {
        assertThrows(ResourceNotFoundException.class,
                () -> paymentCardService.activateCard(10L));
    }


    @Test
    void testDeactivateCardSuccess() {
        when(paymentCardRepository.deactivateCard(10L)).thenReturn(1);
        paymentCardService.deactivateCard(10L);
        verify(paymentCardRepository,times(1)).deactivateCard(10L);
    }

    @Test
    void testDeactivateCardWithIncorrectId() {
        assertThrows(ResourceNotFoundException.class,
                () -> paymentCardService.deactivateCard(10L));
    }

    @Test
    void testUpdatePaymentCardSuccess() {
        Long cardId = 10L;
        String newNumber = "9999888877776666";
        String newHolder = "Updated Holder";

        CreatePaymentCardDto updateDto = new CreatePaymentCardDto();
        updateDto.setNumber(newNumber);
        updateDto.setHolder(newHolder);
        updateDto.setExpirationDate("12/25");
        updateDto.setActive(true);

        PaymentCard existingCard = new PaymentCard();
        existingCard.setId(cardId);
        existingCard.setNumber("1111222233334444");
        existingCard.setHolder("Old Holder");

        PaymentCard updatedCard = new PaymentCard();
        updatedCard.setId(cardId);
        updatedCard.setNumber(newNumber);
        updatedCard.setHolder(newHolder);

        GetPaymentCardDto expectedDto = new GetPaymentCardDto();
        expectedDto.setId(cardId);
        expectedDto.setNumber(newNumber);
        expectedDto.setHolder(newHolder);
        expectedDto.setUserId(1L);

        when(paymentCardRepository.findById(cardId)).thenReturn(Optional.of(existingCard));
        when(paymentCardRepository.existsByNumber(newNumber)).thenReturn(false);
        when(createPaymentCardMapper.toEntity(updateDto)).thenReturn(updatedCard);
        when(paymentCardRepository.updateCardDynamic(updatedCard)).thenReturn(1);
        when(getPaymentCardMapper.toDto(updatedCard)).thenReturn(expectedDto);

        GetPaymentCardDto result = paymentCardService.updatePaymentCard(cardId, updateDto);
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(cardId, result.getId()),
                () -> assertEquals(newNumber, result.getNumber()),
                () -> assertEquals(newHolder, result.getHolder())
        );
        verify(paymentCardRepository, times(1)).findById(cardId);
        verify(paymentCardRepository, times(1)).existsByNumber(newNumber);
        verify(createPaymentCardMapper, times(1)).toEntity(updateDto);
        verify(paymentCardRepository, times(1)).updateCardDynamic(updatedCard);
        verify(getPaymentCardMapper, times(1)).toDto(updatedCard);
    }

    @Test
    void testUpdatePaymentCardWithIncorrectId() {
        CreatePaymentCardDto createPaymentCardDto = new CreatePaymentCardDto();

        assertThrows(ResourceNotFoundException.class,
                () -> paymentCardService.updatePaymentCard(10L, createPaymentCardDto));
    }

    @Test
    void testUpdatePaymentCardWithExistingFields() {
        CreatePaymentCardDto createPaymentCardDto = new CreatePaymentCardDto();
        createPaymentCardDto.setNumber("123456789");
        PaymentCard paymentCardEntity = new PaymentCard();

        when(paymentCardRepository.findById(10l)).thenReturn(Optional.of(paymentCardEntity));
        when(paymentCardRepository.existsByNumber(createPaymentCardDto.getNumber())).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> paymentCardService.updatePaymentCard(10l,createPaymentCardDto));

    }

    @Test
    void testDeletePaymentCardSuccess() {
        when(paymentCardRepository.existsById(10L)).thenReturn(true);

        paymentCardService.deleteCard(10L);
        verify(paymentCardRepository,times(1)).deleteById(10L);
        verify(paymentCardRepository,times(1)).existsById(10L);
    }

    @Test
    void testDeletePaymentCardWithIncorrectId() {
        assertThrows(ResourceNotFoundException.class,
                () -> paymentCardService.deleteCard(10L));
    }

    @Test
    void testExistByNumberSuccess() {
        when(paymentCardRepository.existsByNumber("123")).thenReturn(true);
        boolean result = paymentCardService.existsByNumber("123");

        assertEquals(true, result);

        verify(paymentCardRepository,times(1)).existsByNumber("123");
    }

    @Test
    void testCanAddPaymentCardSuccess() {
        Long userId = 1L;

        PaymentCard card = new PaymentCard();
        card.setId(10L);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(paymentCardRepository.canAddCardToUser(userId)).thenReturn(true);

        boolean result = paymentCardService.canAddCardToUser(userId);

        assertEquals(true, result);

        verify(userRepository,times(1)).existsById(userId);
        verify(paymentCardRepository,times(1)).canAddCardToUser(userId);
    }

    @Test
    void testCanAddPaymentCardWithIncorrectUserId() {
        assertThrows(ResourceNotFoundException.class,
                () -> paymentCardService.canAddCardToUser(1L));
    }

    @Test
    void testCountCardsByUserIdSuccess() {
        Long userId = 1L;

        PaymentCard card = new PaymentCard();
        card.setId(10L);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(paymentCardRepository.countCardsByUserId(userId)).thenReturn(1);
        int result = paymentCardService.countCardsByUserId(userId);

        assertEquals(1, result);

        verify(userRepository, times(1)).existsById(userId);
        verify(paymentCardRepository, times(1)).countCardsByUserId(userId);
    }

    @Test
    void testCountCardsByUserIdWithIncorrectUserId() {
        assertThrows(ResourceNotFoundException.class,
                () -> paymentCardService.countCardsByUserId(1L));
    }
}
