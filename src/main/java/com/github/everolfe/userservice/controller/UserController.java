package com.github.everolfe.userservice.controller;


import com.github.everolfe.userservice.dto.userdto.CreateUserDto;
import com.github.everolfe.userservice.dto.userdto.GetUserDto;
import com.github.everolfe.userservice.service.UserService;
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
 * REST controller for managing users.
 * Provides endpoints for CRUD operations on users with role-based access control.
 */
@AllArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userServiceImpl;

    /**
     * Internal endpoint for user registration.
     * This endpoint is intended for internal use only and does not require authentication.
     *
     * @param dto the DTO containing user creation data
     */
    @PostMapping("/internal/register")
    public ResponseEntity<GetUserDto> internalCreateUser(@Valid @RequestBody
                                                         CreateUserDto dto) {
        GetUserDto createdUser = userServiceImpl.createUser(dto);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    /**
     * Creates a new user.
     * Restricted to ADMIN role only.
     *
     * @param createUserDto the DTO containing user creation data
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GetUserDto> createUser(@Valid @RequestBody
                                                 CreateUserDto createUserDto) {
        GetUserDto getUserDto = userServiceImpl.createUser(createUserDto);
        return new ResponseEntity<>(getUserDto, HttpStatus.OK);
    }

    /**
     * Creates multiple users in a batch operation.
     * Restricted to ADMIN role only.
     *
     * @param createUserDtos the list of DTOs containing user creation data
     */
    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GetUserDto>> createMultipleUsers(@Valid @RequestBody
                                                                List<CreateUserDto> createUserDtos) {
        List<GetUserDto> createdUsers = userServiceImpl
                .createMultipleUsers(createUserDtos);
        return new ResponseEntity<>(createdUsers, HttpStatus.CREATED);
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param id the ID of the user
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityHelper.isOwner(#id)")
    public ResponseEntity<GetUserDto> getUser(@PathVariable Long id) {
        GetUserDto user = userServiceImpl.getUserById(id);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    /**
     * Retrieves all active users with pagination.
     * Restricted to ADMIN role only.
     *
     * @param pageable pagination information (page number, size, sort)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<GetUserDto>> getUsers(Pageable pageable) {
        Page<GetUserDto> users = userServiceImpl.getAllUsers(pageable);
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    /**
     * Searches for users by name and/or surname.
     * Restricted to ADMIN role only.
     *
     * @param name the first name to search for (optional)
     * @param surname the last name to search for (optional)
     * @param pageable pagination information
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<GetUserDto>> getUsersByNameAndSurname(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String surname,
            Pageable pageable) {
        Page<GetUserDto> users = userServiceImpl.getUsersByNameOrSurname(name, surname, pageable);

        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    /**
     * Searches for users by a search term that matches either name or surname.
     * Restricted to ADMIN role only.
     *
     * @param searchTerm the term to search for in name or surname
     * @param pageable pagination information (default size 20, sorted by ID ascending)
     */
    @GetMapping("/search/term")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<GetUserDto>> searchUsersByNameOrSurname(
            @RequestParam String searchTerm,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<GetUserDto> users = userServiceImpl.searchUsersOnlyByNameOrSurname(searchTerm, pageable);
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    /**
     * Activates a user.
     * Restricted to ADMIN role only.
     *
     * @param id the ID of the user to activate
     */
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GetUserDto> activateUser(@PathVariable Long id) {
        GetUserDto result = userServiceImpl.activateUser(id);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * Deactivates a user and automatically deactivates all their associated payment cards.
     * Restricted to ADMIN role only.
     *
     * @param id the ID of the user to deactivate
     */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GetUserDto> deactivateUser(@PathVariable Long id) {
        GetUserDto result = userServiceImpl.deactivateUser(id);
        return new ResponseEntity<>(result,HttpStatus.OK);
    }

    /**
     * Updates an existing user.
     *
     * @param id the ID of the user to update
     * @param createUserDto the DTO containing updated user data
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityHelper.isOwner(#id)")
    public ResponseEntity<GetUserDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody CreateUserDto createUserDto) {
        GetUserDto updatedUser = userServiceImpl.updateUser(id, createUserDto);
        return new ResponseEntity<>(updatedUser, HttpStatus.OK);
    }

    /**
     * Soft deletes a user by deactivating them and all their associated payment cards.
     *
     * @param id the ID of the user to delete
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityHelper.isOwner(#id)")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userServiceImpl.deleteUser(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Checks if a user can add more active payment cards (maximum 5).
     *
     * @param userId the ID of the user
     */
    @GetMapping("/{userId}/can-add-cards")
    @PreAuthorize("hasRole('ADMIN') or @securityHelper.isOwner(#userId)")
    public ResponseEntity<Boolean> canAddMoreCards(@PathVariable Long userId) {
        boolean canAdd = userServiceImpl.canAddMoreCards(userId);
        return new ResponseEntity<>(canAdd, HttpStatus.OK);
    }

    /**
     * Gets the count of payment cards associated with a user.
     *
     * @param userId the ID of the user
     */
    @GetMapping("/{userId}/cards-count")
    @PreAuthorize("hasRole('ADMIN') or @securityHelper.isOwner(#userId)")
    public ResponseEntity<Integer> getCardCountByUserId(@PathVariable Long userId) {
        int count = userServiceImpl.getCardCountByUserId(userId);
        return new ResponseEntity<>(count, HttpStatus.OK);
    }

    @PostMapping("/batch/id")
    @PreAuthorize("hasRole('ADMIN') or @securityHelper.isOwner(#id)")
    public ResponseEntity<List<GetUserDto>> getUsersByIds(@RequestBody List<Long> ids) {
        List<GetUserDto> users = userServiceImpl.getUserByIds(ids);
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("/email")
    @PreAuthorize("hasRole('ADMIN') or @securityHelper.isEmailOwner(#email)")
    public ResponseEntity<GetUserDto> getUserByEmail(@RequestParam String email) {
        GetUserDto user = userServiceImpl.getUserByEmail(email);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }


}