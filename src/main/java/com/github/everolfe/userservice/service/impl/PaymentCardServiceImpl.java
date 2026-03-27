package com.github.everolfe.userservice.service.impl;

import com.github.everolfe.userservice.dao.PaymentCardRepository;
import com.github.everolfe.userservice.dao.PaymentCardSpecification;
import com.github.everolfe.userservice.dao.UserRepository;
import com.github.everolfe.userservice.dto.paymentcarddto.CreatePaymentCardDto;
import com.github.everolfe.userservice.dto.paymentcarddto.GetPaymentCardDto;
import com.github.everolfe.userservice.entity.PaymentCard;
import com.github.everolfe.userservice.entity.User;
import com.github.everolfe.userservice.exception.CardsOutOfBoundsException;
import com.github.everolfe.userservice.exception.DuplicateResourceException;
import com.github.everolfe.userservice.exception.ResourceNotFoundException;
import com.github.everolfe.userservice.mapper.paymentcardmapper.CreatePaymentCardMapper;
import com.github.everolfe.userservice.mapper.paymentcardmapper.GetPaymentCardMapper;
import com.github.everolfe.userservice.service.PaymentCardService;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import static com.github.everolfe.userservice.dao.PaymentCardSpecification.isActive;

@Service
@AllArgsConstructor
public class PaymentCardServiceImpl implements PaymentCardService {

    private final PaymentCardRepository paymentCardRepository;
    private final UserRepository userRepository;
    private final GetPaymentCardMapper getPaymentCardMapper;
    private final CreatePaymentCardMapper createPaymentCardMapper;

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public GetPaymentCardDto create(CreatePaymentCardDto createPaymentCardDto, Long userId) {
        if (paymentCardRepository.existsByNumber(createPaymentCardDto.getNumber())) {
            throw new DuplicateResourceException(
                    "Card with number " + createPaymentCardDto.getNumber() + " already exists");
        }
        User user = userRepository.findByIdWithPessimisticLock(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No User with id" + userId));
        long activeCardsCount = user.getPaymentCards().stream()
                .filter(PaymentCard::getActive)
                .count();
        if (activeCardsCount >= 5) {
            throw new CardsOutOfBoundsException("User already has 5 active cards");
        }
        PaymentCard paymentCard = createPaymentCardMapper.toEntity(createPaymentCardDto);
        paymentCard.setUser(user);
        if (paymentCard.getActive() == null) {
            paymentCard.setActive(true);
        }

        PaymentCard savedCard = paymentCardRepository.save(paymentCard);
        return getPaymentCardMapper.toDto(savedCard);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "cards", key = "#cardId")
    public GetPaymentCardDto getPaymentCardById(Long cardId){
        Optional<PaymentCard> paymentCardOpt = paymentCardRepository.findById(cardId);
        if (paymentCardOpt.isEmpty()) {
            throw new ResourceNotFoundException("Payment card not found with id: " + cardId);
        } else if (!paymentCardOpt.get().getActive()) {
            throw new ResourceNotFoundException("Card is deactivated: " + cardId);
        } else {
            return getPaymentCardMapper.toDto(paymentCardOpt.get());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GetPaymentCardDto> getAllPaymentCards(Pageable pageable) {
        Page<PaymentCard> paymentCards = paymentCardRepository.findAll(isActive(),pageable);
        return paymentCards.map(getPaymentCardMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GetPaymentCardDto> getPaymentCardsByUserId(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        if(user.get().getActive()) {
            return paymentCardRepository.findAllCardsByUserId(userId)
                    .stream()
                    .filter(PaymentCard::getActive)
                    .map(getPaymentCardMapper::toDto)
                    .toList();
        }else {
            throw new ResourceNotFoundException("User is deactivated");
        }

    }

    @Override
    @Transactional(readOnly = true)
    public Page<GetPaymentCardDto> searchCardsByUserNameAndSurname(
            String name, String surname, Pageable pageable) {

        Specification<PaymentCard> spec = PaymentCardSpecification
                .byUserNameAndSurname(name, surname).and(isActive());

        return paymentCardRepository.findAll(spec, pageable)
                .map(getPaymentCardMapper::toDto);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "cards", key = "#cardId"),
            @CacheEvict(value = "users", key = "#result.userId")
    })
    public GetPaymentCardDto activateCard(Long cardId){
        PaymentCard card = paymentCardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("No cards with id: " + cardId));
        card.setActive(true);
        PaymentCard savedCard = paymentCardRepository.save(card);

        return getPaymentCardMapper.toDto(savedCard);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "cards", key = "#cardId"),
            @CacheEvict(value = "users", key = "#result.userId")
    })
    public GetPaymentCardDto deactivateCard(Long cardId){
        PaymentCard card = paymentCardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("No cards with id: " + cardId));
        card.setActive(false);
        PaymentCard savedCard = paymentCardRepository.save(card);

        return getPaymentCardMapper.toDto(savedCard);
    }

    @Override
    @Transactional
    @Caching(
            put = {@CachePut(value = "cards", key = "#cardId")},
            evict = {@CacheEvict(value = "users", key = "#result.userId")}
    )
    public GetPaymentCardDto updatePaymentCard(
            Long cardId, CreatePaymentCardDto createPaymentCardDto) {
        PaymentCard paymentCard = paymentCardRepository
                .findById(cardId).orElseThrow(() -> new ResourceNotFoundException("No card with id: " + cardId));

        if (createPaymentCardDto.getNumber() != null &&
                !createPaymentCardDto.getNumber().equals(paymentCard.getNumber()) &&
                paymentCardRepository.existsByNumber(createPaymentCardDto.getNumber())) {
            throw new DuplicateResourceException(
                    "Card with number " + createPaymentCardDto.getNumber() + " already exists");
        }
        PaymentCard updatedPaymentCard = createPaymentCardMapper.toEntity(createPaymentCardDto);
        updatedPaymentCard.setId(cardId);
        updatedPaymentCard.setUser(paymentCard.getUser());
        int updatedCount = paymentCardRepository.updateCardDynamic(updatedPaymentCard);

        if (updatedCount == 0) {
            throw new ResourceNotFoundException("Failed to update payment card with id: " + cardId);
        }

        return getPaymentCardMapper.toDto(updatedPaymentCard);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "cards", key = "#cardId"),
            @CacheEvict(value = "users", key = "#result.userId")
    })
    public GetPaymentCardDto deleteCard(Long cardId){
        PaymentCard card = paymentCardRepository
                .findById(cardId)
                .orElseThrow(()
                        -> new ResourceNotFoundException("Payment card not found with id: " + cardId));
        card.setActive(false);
        PaymentCard savedCard = paymentCardRepository.save(card);
        return getPaymentCardMapper.toDto(savedCard);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public List<GetPaymentCardDto> createMultiple(
            List<CreatePaymentCardDto> cardDtos, Long userId
    ) {
        User user = userRepository.findByIdWithPessimisticLock(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No User with id" + userId));

        long existingCards = user.getPaymentCards().stream()
                .filter(PaymentCard::getActive)
                .count();

        if (existingCards + cardDtos.size() > 5) {
            throw new CardsOutOfBoundsException(
                    String.format("Cannot add %d cards. User already has %d cards (max 5)",
                            cardDtos.size(), existingCards));
        }

        Set<String> newNumbers = cardDtos.stream()
                .map(CreatePaymentCardDto::getNumber)
                .collect(Collectors.toSet());

        if (newNumbers.size() != cardDtos.size()) {
            throw new DuplicateResourceException("Duplicate card numbers in request");
        }

        List<String> existingNumbers = paymentCardRepository.findExistingNumbers(newNumbers);
        if (!existingNumbers.isEmpty()) {
            throw new DuplicateResourceException(
                    "Cards with numbers already exist: " + String.join(", ", existingNumbers));
        }

        List<PaymentCard> cards = createPaymentCardMapper.toEntities(cardDtos);
        cards.forEach(card -> {
            card.setUser(user);
            if (card.getActive() == null) {
                card.setActive(true);
            }
        });

        List<PaymentCard> savedCards = paymentCardRepository.saveAll(cards);
        return getPaymentCardMapper.toDtos(savedCards);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByNumber(String number) {
        return paymentCardRepository.existsByNumber(number);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAddCardToUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return paymentCardRepository.canAddCardToUser(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public int countCardsByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return paymentCardRepository.countCardsByUserId(userId);
    }

}
