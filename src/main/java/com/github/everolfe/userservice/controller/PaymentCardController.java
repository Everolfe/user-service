package com.github.everolfe.userservice.controller;

import com.github.everolfe.userservice.dto.paymentcarddto.CreatePaymentCardDto;
import com.github.everolfe.userservice.dto.paymentcarddto.GetPaymentCardDto;
import com.github.everolfe.userservice.service.PaymentCardService;
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

@AllArgsConstructor
@RestController
@RequestMapping("/api/cards")
public class PaymentCardController {
    private final PaymentCardService paymentCardService;

    @PostMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @securityHelper.isOwner(#userId)")
    public ResponseEntity<GetPaymentCardDto> create(@PathVariable Long userId,
                                                    @Valid @RequestBody CreatePaymentCardDto paymentCardDto) {
        GetPaymentCardDto createdCard = paymentCardService.create(paymentCardDto, userId);
        return new ResponseEntity<>(createdCard, HttpStatus.CREATED);
    }

    @PostMapping("/user/{userId}/batch")
    @PreAuthorize("hasRole('ADMIN') or @securityHelper.isOwner(#userId)")
    public ResponseEntity<List<GetPaymentCardDto>> createMultiple(
            @PathVariable Long userId,
            @Valid @RequestBody List<CreatePaymentCardDto> paymentCardDtos) {
        List<GetPaymentCardDto> createdCards = paymentCardService
                .createMultiple(paymentCardDtos, userId);
        return new ResponseEntity<>(createdCards, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityHelper.isCardOwner(#id)")
    public ResponseEntity<GetPaymentCardDto> get(@PathVariable Long id) {
        GetPaymentCardDto card = paymentCardService.getPaymentCardById(id);
        return new ResponseEntity<>(card, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<Page<GetPaymentCardDto>> getAll(Pageable pageable) {
        Page<GetPaymentCardDto> cards = paymentCardService.getAllPaymentCards(pageable);
        return new ResponseEntity<>(cards, HttpStatus.OK);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GetPaymentCardDto>> getUserPaymentCards(@PathVariable Long userId) {
        List<GetPaymentCardDto> userCards = paymentCardService.getPaymentCardsByUserId(userId);
        return new ResponseEntity<>(userCards, HttpStatus.OK);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<GetPaymentCardDto>> searchCardsByUserNameOrSurname(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String surname,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<GetPaymentCardDto> cards = paymentCardService
                .searchCardsByUserNameAndSurname(name, surname, pageable);
        return new ResponseEntity<>(cards, HttpStatus.OK);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GetPaymentCardDto> activate(@PathVariable Long id) {
        GetPaymentCardDto result = paymentCardService.activateCard(id);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GetPaymentCardDto> deactivate(@PathVariable Long id) {
        GetPaymentCardDto result = paymentCardService.deactivateCard(id);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityHelper.isCardOwner(#id)")
    public ResponseEntity<GetPaymentCardDto> updateCard(
            @PathVariable Long id,
            @Valid @RequestBody CreatePaymentCardDto paymentCardDto){
        GetPaymentCardDto updatedCard = paymentCardService.updatePaymentCard(id,paymentCardDto);
        return new ResponseEntity<>(updatedCard, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityHelper.isCardOwner(#id)")
    public ResponseEntity<GetPaymentCardDto> delete(@PathVariable Long id) {
        GetPaymentCardDto result = paymentCardService.deleteCard(id);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/exists/{number}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Boolean> existsByNumber(@PathVariable String number) {
        boolean exists = paymentCardService.existsByNumber(number);
        return new ResponseEntity<>(exists,HttpStatus.OK);
    }

    @GetMapping("/user/{userId}/can-add")
    @PreAuthorize("hasRole('ADMIN') or @securityHelper.isOwner(#userId)")
    public ResponseEntity<Boolean> canAddCardToUser(@PathVariable Long userId) {
        boolean canAdd = paymentCardService.canAddCardToUser(userId);
        return new ResponseEntity<>(canAdd,HttpStatus.OK);
    }

    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Integer> countCardsByUserId(@PathVariable Long userId) {
        int count = paymentCardService.countCardsByUserId(userId);
        return new ResponseEntity<>(count,HttpStatus.OK);
    }
}
