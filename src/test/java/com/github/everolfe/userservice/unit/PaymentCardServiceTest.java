package com.github.everolfe.userservice.unit;


import com.github.everolfe.userservice.dao.PaymentCardRepository;
import com.github.everolfe.userservice.dao.UserRepository;
import com.github.everolfe.userservice.dto.paymentcarddto.CreatePaymentCardDto;
import com.github.everolfe.userservice.dto.paymentcarddto.GetPaymentCardDto;
import com.github.everolfe.userservice.entity.PaymentCard;
import com.github.everolfe.userservice.entity.User;
import com.github.everolfe.userservice.exception.DuplicateResourceException;
import com.github.everolfe.userservice.exception.ResourceNotFoundException;
import com.github.everolfe.userservice.exception.CardsOutOfBoundsException;
import com.github.everolfe.userservice.mapper.paymentcardmapper.CreatePaymentCardMapper;
import com.github.everolfe.userservice.mapper.paymentcardmapper.GetPaymentCardMapper;
import com.github.everolfe.userservice.service.PaymentCardService;
import java.util.List;
import java.util.Set;
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

import static com.github.everolfe.userservice.dao.PaymentCardSpecification.isActive;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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

        when(paymentCardRepository.existsByNumber(createPaymentCardDto.getNumber())).thenReturn(false);
        when(userRepository.findByIdWithPessimisticLock(userId)).thenReturn(Optional.of(user));
        when(createPaymentCardMapper.toEntity(createPaymentCardDto)).thenReturn(cardToSave);
        when(paymentCardRepository.save(cardToSave)).thenReturn(savedCard);
        when(getPaymentCardMapper.toDto(savedCard)).thenReturn(getPaymentCardDto);

        GetPaymentCardDto result = paymentCardService.create(createPaymentCardDto,userId);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());

        verify(userRepository,times(1)).findByIdWithPessimisticLock(userId);
        verify(paymentCardRepository,times(1)).save(cardToSave);
    }

    @Test
    void testCreatePaymentCardWithoutUserId() {
        Long userID = 99L;
        CreatePaymentCardDto createPaymentCardDto = new CreatePaymentCardDto();
        createPaymentCardDto.setNumber("123456789");
        when(paymentCardRepository.existsByNumber(createPaymentCardDto.getNumber())).thenReturn(false);
        when(userRepository.findByIdWithPessimisticLock(userID)).thenReturn(Optional.empty());

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

        when(paymentCardRepository.findAll(isActive(),pageable)).thenReturn(page);

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

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(paymentCardRepository.findAllCardsByUserId(1L)).thenReturn(userCards);
        when(getPaymentCardMapper.toDto(paymentCardEntity)).thenReturn(getPaymentCardDto);

        List<GetPaymentCardDto> result = paymentCardService.getPaymentCardsByUserId(1L);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10L, result.getFirst().getId());
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
                () -> assertEquals(10L, result.getContent().getFirst().getId()),
                () -> assertEquals(11L, result.getContent().get(1).getId()),
                () -> assertEquals("1111222233334444", result.getContent().getFirst().getNumber()),
                () -> assertEquals(1L, result.getContent().getFirst().getUserId())
        );

        verify(paymentCardRepository, times(1))
                .findAll(any(Specification.class), eq(pageable));
        verify(getPaymentCardMapper, times(1)).toDto(card1);
        verify(getPaymentCardMapper, times(1)).toDto(card2);
    }

    @Test
    void testActivateCardSuccess() {
        PaymentCard card = new PaymentCard();
        when(paymentCardRepository.findById(10L)).thenReturn(Optional.of(card));
        when(paymentCardRepository.save(card)).thenReturn(card);
        paymentCardService.activateCard(10L);
        verify(paymentCardRepository,times(1)).findById(10L);
    }

    @Test
    void testActivateCardWithIncorrectId() {
        assertThrows(ResourceNotFoundException.class,
                () -> paymentCardService.activateCard(10L));
    }


    @Test
    void testDeactivateCardSuccess() {
        PaymentCard card = new PaymentCard();
        when(paymentCardRepository.findById(10L)).thenReturn(Optional.of(card));
        when(paymentCardRepository.save(card)).thenReturn(card);
        paymentCardService.deactivateCard(10L);
        verify(paymentCardRepository,times(1)).findById(10L);
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

        when(paymentCardRepository.findById(10L)).thenReturn(Optional.of(paymentCardEntity));
        when(paymentCardRepository.existsByNumber(createPaymentCardDto.getNumber())).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> paymentCardService.updatePaymentCard(10L,createPaymentCardDto));

    }

    @Test
    void testDeletePaymentCardSuccess() {
        PaymentCard card = new PaymentCard();
        when(paymentCardRepository.findById(10L)).thenReturn(Optional.of(card));
        when(paymentCardRepository.save(card)).thenReturn(card);
        paymentCardService.deleteCard(10L);
        verify(paymentCardRepository,times(1)).save(card);
        verify(paymentCardRepository,times(1)).findById(10L);
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

        assertTrue(result);

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

        assertTrue(result);

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

    @Test
    void testCreateMultipleCardsSuccess() {
        Long userId = 1L;

        CreatePaymentCardDto dto1 = new CreatePaymentCardDto();
        dto1.setNumber("1111222233334444");
        dto1.setHolder("John Doe");
        dto1.setExpirationDate("12/25");

        CreatePaymentCardDto dto2 = new CreatePaymentCardDto();
        dto2.setNumber("5555666677778888");
        dto2.setHolder("Jane Smith");
        dto2.setExpirationDate("01/26");

        List<CreatePaymentCardDto> dtos = List.of(dto1, dto2);

        User user = new User();
        user.setId(userId);

        PaymentCard card1 = new PaymentCard();
        card1.setNumber("1111222233334444");

        PaymentCard card2 = new PaymentCard();
        card2.setNumber("5555666677778888");

        List<PaymentCard> cardsToSave = List.of(card1, card2);

        PaymentCard savedCard1 = new PaymentCard();
        savedCard1.setId(10L);
        savedCard1.setNumber("1111222233334444");
        savedCard1.setUser(user);
        savedCard1.setActive(true);

        PaymentCard savedCard2 = new PaymentCard();
        savedCard2.setId(11L);
        savedCard2.setNumber("5555666677778888");
        savedCard2.setUser(user);
        savedCard2.setActive(true);

        List<PaymentCard> savedCards = List.of(savedCard1, savedCard2);

        GetPaymentCardDto getDto1 = new GetPaymentCardDto();
        getDto1.setId(10L);
        getDto1.setNumber("1111222233334444");
        getDto1.setUserId(userId);

        GetPaymentCardDto getDto2 = new GetPaymentCardDto();
        getDto2.setId(11L);
        getDto2.setNumber("5555666677778888");
        getDto2.setUserId(userId);

        List<GetPaymentCardDto> expectedDtos = List.of(getDto1, getDto2);

        when(userRepository.findByIdWithPessimisticLock(userId)).thenReturn(Optional.of(user));
        when(createPaymentCardMapper.toEntities(dtos)).thenReturn(cardsToSave);
        when(paymentCardRepository.findExistingNumbers(Set.of("1111222233334444", "5555666677778888")))
                .thenReturn(List.of());
        when(paymentCardRepository.saveAll(cardsToSave)).thenReturn(savedCards);
        when(getPaymentCardMapper.toDtos(savedCards)).thenReturn(expectedDtos);

        // Выполнение
        List<GetPaymentCardDto> result = paymentCardService.createMultiple(dtos, userId);

        // Проверка
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(2, result.size()),
                () -> assertEquals(10L, result.getFirst().getId()),
                () -> assertEquals("1111222233334444", result.getFirst().getNumber()),
                () -> assertEquals(userId, result.getFirst().getUserId()),
                () -> assertEquals(11L, result.get(1).getId()),
                () -> assertEquals("5555666677778888", result.get(1).getNumber()),
                () -> assertEquals(userId, result.get(1).getUserId())
        );

        verify(userRepository, times(1)).findByIdWithPessimisticLock(userId);
        verify(createPaymentCardMapper, times(1)).toEntities(dtos);
        verify(paymentCardRepository, times(1)).findExistingNumbers(any());
        verify(paymentCardRepository, times(1)).saveAll(cardsToSave);
        verify(getPaymentCardMapper, times(1)).toDtos(savedCards);
    }

    @Test
    void testCreateMultipleCardsUserNotFound() {
        // Подготовка
        Long userId = 99L;
        List<CreatePaymentCardDto> dtos = List.of(new CreatePaymentCardDto());

        when(userRepository.findByIdWithPessimisticLock(userId)).thenReturn(Optional.empty());

        // Выполнение и проверка
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> paymentCardService.createMultiple(dtos, userId)
        );

        assertEquals("No User with id" + userId, exception.getMessage());

        verify(createPaymentCardMapper, never()).toEntities(any());
        verify(paymentCardRepository, never()).findExistingNumbers(any());
        verify(paymentCardRepository, never()).saveAll(any());
    }

    @Test
    void testCreateMultipleCardsExceedsLimit() {
        // Подготовка
        Long userId = 1L;

        User user = new User();
        user.setId(userId);

        // У пользователя уже есть 4 активные карты
        PaymentCard existingCard1 = new PaymentCard();
        existingCard1.setActive(true);
        PaymentCard existingCard2 = new PaymentCard();
        existingCard2.setActive(true);
        PaymentCard existingCard3 = new PaymentCard();
        existingCard3.setActive(true);
        PaymentCard existingCard4 = new PaymentCard();
        existingCard4.setActive(true);

        user.setPaymentCards(List.of(existingCard1, existingCard2, existingCard3, existingCard4));

        // Пытаемся добавить еще 2 карты (будет 6 > 5)
        CreatePaymentCardDto dto1 = new CreatePaymentCardDto();
        CreatePaymentCardDto dto2 = new CreatePaymentCardDto();
        List<CreatePaymentCardDto> dtos = List.of(dto1, dto2);

        when(userRepository.findByIdWithPessimisticLock(userId)).thenReturn(Optional.of(user));

        CardsOutOfBoundsException exception = assertThrows(
                CardsOutOfBoundsException.class,
                () -> paymentCardService.createMultiple(dtos, userId)
        );

        assertTrue(exception.getMessage().contains("Cannot add 2 cards"));
        assertTrue(exception.getMessage().contains("already has 4 cards"));

        verify(createPaymentCardMapper, never()).toEntities(any());
        verify(paymentCardRepository, never()).findExistingNumbers(any());
        verify(paymentCardRepository, never()).saveAll(any());
    }

    @Test
    void testCreateMultipleCardsWithDuplicateNumbersInRequest() {

        Long userId = 1L;

        CreatePaymentCardDto dto1 = new CreatePaymentCardDto();
        dto1.setNumber("1111222233334444");

        CreatePaymentCardDto dto2 = new CreatePaymentCardDto();
        dto2.setNumber("1111222233334444");

        List<CreatePaymentCardDto> dtos = List.of(dto1, dto2);

        User user = new User();
        user.setId(userId);
        user.setPaymentCards(List.of());

        when(userRepository.findByIdWithPessimisticLock(userId)).thenReturn(Optional.of(user));


        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> paymentCardService.createMultiple(dtos, userId)
        );

        assertEquals("Duplicate card numbers in request", exception.getMessage());

        verify(createPaymentCardMapper, never()).toEntities(any());
        verify(paymentCardRepository, never()).findExistingNumbers(any());
        verify(paymentCardRepository, never()).saveAll(any());
    }

    @Test
    void testCreateMultipleCardsWithExistingNumbersInDatabase() {
        Long userId = 1L;

        CreatePaymentCardDto dto1 = new CreatePaymentCardDto();
        dto1.setNumber("1111222233334444");

        CreatePaymentCardDto dto2 = new CreatePaymentCardDto();
        dto2.setNumber("5555666677778888");

        List<CreatePaymentCardDto> dtos = List.of(dto1, dto2);

        User user = new User();
        user.setId(userId);
        user.setPaymentCards(List.of());

        Set<String> numbers = Set.of("1111222233334444", "5555666677778888");
        List<String> existingNumbers = List.of("1111222233334444");

        when(userRepository.findByIdWithPessimisticLock(userId)).thenReturn(Optional.of(user));
        when(paymentCardRepository.findExistingNumbers(numbers)).thenReturn(existingNumbers);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> paymentCardService.createMultiple(dtos, userId)
        );

        assertTrue(exception.getMessage().contains("Cards with numbers already exist"));
        assertTrue(exception.getMessage().contains("1111222233334444"));

        verify(createPaymentCardMapper, never()).toEntities(any());
        verify(paymentCardRepository, times(1)).findExistingNumbers(numbers);
        verify(paymentCardRepository, never()).saveAll(any());
    }

    @Test
    void testCreateMultipleCardsSetsActiveToTrueWhenNull() {
        Long userId = 1L;

        CreatePaymentCardDto dto1 = new CreatePaymentCardDto();
        dto1.setNumber("1111222233334444");
        dto1.setActive(null);

        CreatePaymentCardDto dto2 = new CreatePaymentCardDto();
        dto2.setNumber("5555666677778888");
        dto2.setActive(null);

        List<CreatePaymentCardDto> dtos = List.of(dto1, dto2);

        User user = new User();
        user.setId(userId);
        user.setPaymentCards(List.of());

        PaymentCard card1 = new PaymentCard();
        card1.setNumber("1111222233334444");
        card1.setActive(null);

        PaymentCard card2 = new PaymentCard();
        card2.setNumber("5555666677778888");
        card2.setActive(null);

        List<PaymentCard> cardsToSave = List.of(card1, card2);

        PaymentCard savedCard1 = new PaymentCard();
        savedCard1.setId(10L);
        savedCard1.setNumber("1111222233334444");
        savedCard1.setUser(user);
        savedCard1.setActive(true);

        PaymentCard savedCard2 = new PaymentCard();
        savedCard2.setId(11L);
        savedCard2.setNumber("5555666677778888");
        savedCard2.setUser(user);
        savedCard2.setActive(true);

        List<PaymentCard> savedCards = List.of(savedCard1, savedCard2);

        when(userRepository.findByIdWithPessimisticLock(userId)).thenReturn(Optional.of(user));
        when(createPaymentCardMapper.toEntities(dtos)).thenReturn(cardsToSave);
        when(paymentCardRepository.findExistingNumbers(Set.of("1111222233334444", "5555666677778888")))
                .thenReturn(List.of());
        when(paymentCardRepository.saveAll(cardsToSave)).thenReturn(savedCards);
        when(getPaymentCardMapper.toDtos(savedCards)).thenReturn(List.of(new GetPaymentCardDto(), new GetPaymentCardDto()));

        paymentCardService.createMultiple(dtos, userId);

        verify(paymentCardRepository).saveAll(argThat(cards -> {
            List<PaymentCard> cardList = (List<PaymentCard>) cards;
            return cardList.stream().allMatch(c -> c.getActive() != null && c.getActive());
        }));
    }

    @Test
    void testCreateMultipleCardsWithEmptyList() {
        Long userId = 1L;
        List<CreatePaymentCardDto> emptyList = List.of();

        User user = new User();
        user.setId(userId);
        user.setPaymentCards(List.of());

        when(userRepository.findByIdWithPessimisticLock(userId)).thenReturn(Optional.of(user));
        when(createPaymentCardMapper.toEntities(emptyList)).thenReturn(List.of());
        when(paymentCardRepository.findExistingNumbers(Set.of())).thenReturn(List.of());
        when(paymentCardRepository.saveAll(List.of())).thenReturn(List.of());
        when(getPaymentCardMapper.toDtos(List.of())).thenReturn(List.of());

        List<GetPaymentCardDto> result = paymentCardService.createMultiple(emptyList, userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(createPaymentCardMapper, times(1)).toEntities(emptyList);
        verify(paymentCardRepository, times(1)).findExistingNumbers(Set.of());
        verify(paymentCardRepository, times(1)).saveAll(List.of());
        verify(getPaymentCardMapper, times(1)).toDtos(List.of());
    }
}
