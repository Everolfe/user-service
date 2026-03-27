package com.github.everolfe.userservice.controller;

import com.github.everolfe.userservice.dto.paymentcarddto.CreatePaymentCardDto;
import com.github.everolfe.userservice.dto.paymentcarddto.GetPaymentCardDto;
import com.github.everolfe.userservice.service.impl.PaymentCardServiceImpl;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing payment cards.
 * Provides endpoints for CRUD operations on payment cards with role-based access control.
 */
@AllArgsConstructor
@RestController
@RequestMapping("/api/cards")
public class PaymentCardController {
    private final PaymentCardServiceImpl paymentCardServiceImpl;

    /**
     * Creates a new payment card for a specific user.
     *
     * @param userId the ID of the user to associate the card with
     * @param paymentCardDto the DTO containing card creation data
     */
    @PostMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @securityHelper.isOwner(#userId)")
    public ResponseEntity<GetPaymentCardDto> create(@PathVariable Long userId,
                                                    @Valid @RequestBody CreatePaymentCardDto paymentCardDto) {
        GetPaymentCardDto createdCard = paymentCardServiceImpl.create(paymentCardDto, userId);
        return new ResponseEntity<>(createdCard, HttpStatus.CREATED);
    }

    /**
     * Creates multiple payment cards for a user in a batch operation.
     *
     * @param userId the ID of the user to associate the cards with
     * @param paymentCardDtos the list of DTOs containing card creation data
     */
    @PostMapping("/user/{userId}/batch")
    @PreAuthorize("hasRole('ADMIN') or @securityHelper.isOwner(#userId)")
    public ResponseEntity<List<GetPaymentCardDto>> createMultiple(
            @PathVariable Long userId,
            @Valid @RequestBody List<CreatePaymentCardDto> paymentCardDtos) {
        List<GetPaymentCardDto> createdCards = paymentCardServiceImpl
                .createMultiple(paymentCardDtos, userId);
        return new ResponseEntity<>(createdCards, HttpStatus.CREATED);
    }

    /**
     * Retrieves a payment card by its ID.
     *
     * @param id the ID of the payment card
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityHelper.isCardOwner(#id)")
    public ResponseEntity<GetPaymentCardDto> get(@PathVariable Long id) {
        GetPaymentCardDto card = paymentCardServiceImpl.getPaymentCardById(id);
        return new ResponseEntity<>(card, HttpStatus.OK);
    }

    /**
     * Retrieves all active payment cards with pagination.
     *
     * @param pageable pagination information (page number, size, sort)
     */
    @GetMapping
    public ResponseEntity<Page<GetPaymentCardDto>> getAll(Pageable pageable) {
        Page<GetPaymentCardDto> cards = paymentCardServiceImpl.getAllPaymentCards(pageable);
        return new ResponseEntity<>(cards, HttpStatus.OK);
    }

    /**
     * Retrieves all payment cards for a specific user.
     * Restricted to ADMIN role only.
     *
     * @param userId the ID of the user
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GetPaymentCardDto>> getUserPaymentCards(@PathVariable Long userId) {
        List<GetPaymentCardDto> userCards = paymentCardServiceImpl.getPaymentCardsByUserId(userId);
        return new ResponseEntity<>(userCards, HttpStatus.OK);
    }

    /**
     * Searches for payment cards by cardholder name and surname.
     * Restricted to ADMIN role only.
     *
     * @param name the first name to search for (optional)
     * @param surname the last name to search for (optional)
     * @param pageable pagination information (default size 20, sorted by ID ascending)
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<GetPaymentCardDto>> searchCardsByUserNameOrSurname(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String surname,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<GetPaymentCardDto> cards = paymentCardServiceImpl
                .searchCardsByUserNameAndSurname(name, surname, pageable);
        return new ResponseEntity<>(cards, HttpStatus.OK);
    }

    /**
     * Activates a payment card.
     * Restricted to ADMIN role only.
     *
     * @param id the ID of the payment card to activate
     */
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GetPaymentCardDto> activate(@PathVariable Long id) {
        GetPaymentCardDto result = paymentCardServiceImpl.activateCard(id);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * Deactivates a payment card.
     * Restricted to ADMIN role only.
     *
     * @param id the ID of the payment card to deactivate
     */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GetPaymentCardDto> deactivate(@PathVariable Long id) {
        GetPaymentCardDto result = paymentCardServiceImpl.deactivateCard(id);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * Updates an existing payment card.
     *
     * @param id the ID of the payment card to update
     * @param paymentCardDto the DTO containing updated card data
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityHelper.isCardOwner(#id)")
    public ResponseEntity<GetPaymentCardDto> updateCard(
            @PathVariable Long id,
            @Valid @RequestBody CreatePaymentCardDto paymentCardDto){
        GetPaymentCardDto updatedCard = paymentCardServiceImpl.updatePaymentCard(id,paymentCardDto);
        return new ResponseEntity<>(updatedCard, HttpStatus.OK);
    }

    /**
     * Soft deletes a payment card by deactivating it.
     *
     * @param id the ID of the payment card to delete
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityHelper.isCardOwner(#id)")
    public ResponseEntity<GetPaymentCardDto> delete(@PathVariable Long id) {
        GetPaymentCardDto result = paymentCardServiceImpl.deleteCard(id);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * Checks if a payment card with the given number exists.
     * Restricted to ADMIN role only.
     *
     * @param number the card number to check
     */
    @GetMapping("/exists/{number}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Boolean> existsByNumber(@PathVariable String number) {
        boolean exists = paymentCardServiceImpl.existsByNumber(number);
        return new ResponseEntity<>(exists,HttpStatus.OK);
    }

    /**
     * Checks if a user can add more active payment cards (maximum 5).
     *
     * @param userId the ID of the user
     */
    @GetMapping("/user/{userId}/can-add")
    @PreAuthorize("hasRole('ADMIN') or @securityHelper.isOwner(#userId)")
    public ResponseEntity<Boolean> canAddCardToUser(@PathVariable Long userId) {
        boolean canAdd = paymentCardServiceImpl.canAddCardToUser(userId);
        return new ResponseEntity<>(canAdd,HttpStatus.OK);
    }

    /**
     * Gets the count of payment cards associated with a user.
     *
     * @param userId the ID of the user
     */
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Integer> countCardsByUserId(@PathVariable Long userId) {
        int count = paymentCardServiceImpl.countCardsByUserId(userId);
        return new ResponseEntity<>(count,HttpStatus.OK);
    }
}