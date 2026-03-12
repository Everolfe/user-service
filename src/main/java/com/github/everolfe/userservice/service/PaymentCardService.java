package com.github.everolfe.userservice.service;

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
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PaymentCardService {

    private final PaymentCardRepository paymentCardRepository;
    private final UserRepository userRepository;
    private final GetPaymentCardMapper getPaymentCardMapper;
    private final CreatePaymentCardMapper createPaymentCardMapper;

    @Transactional
    public GetPaymentCardDto create(CreatePaymentCardDto createPaymentCardDto, Long userId) {
        PaymentCard paymentCard = createPaymentCardMapper.toEntity(createPaymentCardDto);
        if (paymentCardRepository.existsByNumber(createPaymentCardDto.getNumber())) {
            throw new DuplicateResourceException(
                    "Card with number " + createPaymentCardDto.getNumber() + " already exists");
        }
        Optional<User> user = userRepository.findById(userId);
        paymentCard.setUser(user
                .orElseThrow(() -> new ResourceNotFoundException("No User with id" + userId)));
        PaymentCard savedCard = paymentCardRepository.save(paymentCard);
        return getPaymentCardMapper.toDto(savedCard);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "cards", key = "#cardId")
    public GetPaymentCardDto getPaymentCardById(Long cardId){
        Optional<PaymentCard> paymentCardOpt = paymentCardRepository.findById(cardId);
        if (paymentCardOpt.isPresent()) {
            return getPaymentCardMapper.toDto(paymentCardOpt.get());
        } else {
            throw new ResourceNotFoundException("Payment card not found with id: " + cardId);
        }
    }

    @Transactional(readOnly = true)
    public Page<GetPaymentCardDto> getAllPaymentCards(Pageable pageable) {
        Page<PaymentCard> paymentCards = paymentCardRepository.findAll(pageable);
        return paymentCards.map(getPaymentCardMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<GetPaymentCardDto> getPaymentCardsByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return paymentCardRepository.findAllCardsByUserId(userId)
                .stream()
                .map(getPaymentCardMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<GetPaymentCardDto> searchCardsByUserNameAndSurname(
            String name, String surname, Pageable pageable) {

        Specification<PaymentCard> spec = PaymentCardSpecification.byUserNameAndSurname(name, surname);

        return paymentCardRepository.findAll(spec, pageable)
                .map(getPaymentCardMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = "cards", key = "#cardId")
    public void activateCard(Long cardId){
        int activated = paymentCardRepository.activateCard(cardId);
        if(activated == 0){
            throw new ResourceNotFoundException("No cards with id: " + cardId);
        }
    }

    @Transactional
    @CacheEvict(value = "cards", key = "#cardId")
    public void deactivateCard(Long cardId){
        int deactivated = paymentCardRepository.deactivateCard(cardId);
        if(deactivated == 0){
            throw new ResourceNotFoundException("No cards with id: " + cardId);
        }
    }

    @Transactional
    @CachePut(value = "cards", key = "#cardId")
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

        int updatedCount = paymentCardRepository.updateCardDynamic(updatedPaymentCard);

        if (updatedCount == 0) {
            throw new ResourceNotFoundException("Failed to update payment card with id: " + cardId);
        }

        return getPaymentCardMapper.toDto(updatedPaymentCard);
    }

    @Transactional
    @CacheEvict(value = "cards", key = "#cardId")
    public void deleteCard(Long cardId){
        if (!paymentCardRepository.existsById(cardId)) {
            throw new ResourceNotFoundException("Payment card not found with id: " + cardId);
        }
        paymentCardRepository.deleteById(cardId);
    }

    @Transactional(readOnly = true)
    public boolean existsByNumber(String number) {
        return paymentCardRepository.existsByNumber(number);
    }

    @Transactional(readOnly = true)
    public boolean canAddCardToUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return paymentCardRepository.canAddCardToUser(userId);
    }

    @Transactional(readOnly = true)
    public int countCardsByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return paymentCardRepository.countCardsByUserId(userId);
    }

}
