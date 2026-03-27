package com.github.everolfe.userservice.service;

import com.github.everolfe.userservice.dto.paymentcarddto.CreatePaymentCardDto;
import com.github.everolfe.userservice.dto.paymentcarddto.GetPaymentCardDto;
import com.github.everolfe.userservice.exception.CardsOutOfBoundsException;
import com.github.everolfe.userservice.exception.DuplicateResourceException;
import com.github.everolfe.userservice.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing payment cards.
 * Provides operations for creating, retrieving, updating, and deleting payment cards,
 * as well as managing card activation status and user-card relationships.
 */
public interface PaymentCardService {

    /**
     * Creates a new payment card for the specified user.
     *
     * @param createPaymentCardDto the DTO containing card creation data
     * @param userId the ID of the user to associate the card with
     * @return the DTO of the created payment card
     * @throws DuplicateResourceException if a card with the same number already exists
     * @throws ResourceNotFoundException if the user is not found
     * @throws CardsOutOfBoundsException if the user already has 5 active cards
     */
    GetPaymentCardDto create(
            CreatePaymentCardDto createPaymentCardDto, Long userId
    );

    /**
     * Retrieves a payment card by its ID.
     *
     * @param cardId the ID of the payment card
     * @return the DTO of the payment card
     * @throws ResourceNotFoundException if the card is not found or is deactivated
     */
    GetPaymentCardDto getPaymentCardById(Long cardId);

    /**
     * Retrieves all active payment cards with pagination.
     *
     * @param pageable pagination information
     * @return a page of payment card DTOs
     */
    Page<GetPaymentCardDto> getAllPaymentCards(Pageable pageable);

    /**
     * Retrieves all active payment cards associated with a specific user.
     *
     * @param userId the ID of the user
     * @return a list of payment card DTOs for the user
     * @throws ResourceNotFoundException if the user is not found or is deactivated
     */
    List<GetPaymentCardDto> getPaymentCardsByUserId(Long userId);

    /**
     * Searches for active payment cards by the cardholder's name and surname.
     *
     * @param username the first name of the cardholder
     * @param surname the last name of the cardholder
     * @param pageable pagination information
     * @return a page of payment card DTOs matching the search criteria
     */
    Page<GetPaymentCardDto> searchCardsByUserNameAndSurname(
            String username, String surname, Pageable pageable
    );

    /**
     * Activates a payment card.
     *
     * @param cardId the ID of the payment card to activate
     * @return the DTO of the activated payment card
     * @throws ResourceNotFoundException if the card is not found
     */
    GetPaymentCardDto activateCard(Long cardId);

    /**
     * Deactivates a payment card.
     *
     * @param cardId the ID of the payment card to deactivate
     * @return the DTO of the deactivated payment card
     * @throws ResourceNotFoundException if the card is not found
     */
    GetPaymentCardDto deactivateCard(Long cardId);

    /**
     * Updates an existing payment card with new data.
     *
     * @param cardId the ID of the payment card to update
     * @param createPaymentCardDto the DTO containing updated card data
     * @return the DTO of the updated payment card
     * @throws ResourceNotFoundException if the card is not found
     * @throws DuplicateResourceException if the new card number already exists
     */
    GetPaymentCardDto updatePaymentCard(
            Long cardId, CreatePaymentCardDto createPaymentCardDto
    );

    /**
     * Soft deletes a payment card by deactivating it.
     *
     * @param cardId the ID of the payment card to delete
     * @return the DTO of the deactivated payment card
     * @throws ResourceNotFoundException if the card is not found
     */
    GetPaymentCardDto deleteCard(Long cardId);

    /**
     * Creates multiple payment cards for a user in a batch operation.
     *
     * @param cardDtos the list of DTOs containing card creation data
     * @param userId the ID of the user to associate the cards with
     * @return a list of DTOs of the created payment cards
     * @throws ResourceNotFoundException if the user is not found
     * @throws DuplicateResourceException if duplicate card numbers exist in request or database
     * @throws CardsOutOfBoundsException if adding the cards would exceed the 5 active card limit
     */
    List<GetPaymentCardDto> createMultiple(
            List<CreatePaymentCardDto> cardDtos, Long userId
    );

    /**
     * Checks if a payment card with the given number already exists.
     *
     * @param number the card number to check
     * @return true if a card with the number exists, false otherwise
     */
    boolean existsByNumber(String number);

    /**
     * Checks if a user can add more active payment cards (maximum 5).
     *
     * @param userId the ID of the user
     * @return true if the user can add more cards, false otherwise
     * @throws ResourceNotFoundException if the user is not found
     */
    boolean canAddCardToUser(Long userId);

    /**
     * Counts the number of payment cards associated with a user.
     *
     * @param userId the ID of the user
     * @return the number of cards for the user
     * @throws ResourceNotFoundException if the user is not found
     */
    int countCardsByUserId(Long userId);
}