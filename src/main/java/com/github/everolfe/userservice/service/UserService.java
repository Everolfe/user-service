package com.github.everolfe.userservice.service;

import com.github.everolfe.userservice.dto.userdto.CreateUserDto;
import com.github.everolfe.userservice.dto.userdto.GetUserDto;
import com.github.everolfe.userservice.exception.DuplicateResourceException;
import com.github.everolfe.userservice.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing users.
 * Provides operations for creating, retrieving, updating, and deleting users,
 * as well as managing user activation status and card limits.
 */
public interface UserService {

    /**
     * Creates a new user.
     *
     * @param createUserDto the DTO containing user creation data
     * @return the DTO of the created user
     * @throws DuplicateResourceException if a user with the same email already exists
     */
    GetUserDto createUser(CreateUserDto createUserDto);

    /**
     * Retrieves a user by their ID.
     *
     * @param id the ID of the user
     * @return the DTO of the user
     * @throws ResourceNotFoundException if the user is not found or is deactivated
     */
    GetUserDto getUserById(Long id);

    /**
     * Retrieves all active users with pagination.
     *
     * @param pageable pagination information
     * @return a page of user DTOs
     */
    Page<GetUserDto> getAllUsers(Pageable pageable);

    /**
     * Searches for active users by name or surname.
     *
     * @param name the first name to search for
     * @param surname the last name to search for
     * @param pageable pagination information
     * @return a page of user DTOs matching the search criteria
     */
    Page<GetUserDto> getUsersByNameOrSurname(String name, String surname, Pageable pageable);

    /**
     * Searches for active users by a search term that matches either name or surname.
     *
     * @param searchTerm the term to search for in name or surname
     * @param pageable pagination information
     * @return a page of user DTOs matching the search criteria
     */
    Page<GetUserDto> searchUsersOnlyByNameOrSurname(String searchTerm, Pageable pageable);

    /**
     * Activates a user.
     *
     * @param id the ID of the user to activate
     * @return the DTO of the activated user
     * @throws ResourceNotFoundException if the user is not found
     */
    GetUserDto activateUser(Long id);

    /**
     * Deactivates a user and automatically deactivates all their associated payment cards.
     *
     * @param id the ID of the user to deactivate
     * @return the DTO of the deactivated user
     * @throws ResourceNotFoundException if the user is not found
     */
    GetUserDto deactivateUser(Long id);

    /**
     * Updates an existing user with new data.
     *
     * @param id the ID of the user to update
     * @param createUserDto the DTO containing updated user data
     * @return the DTO of the updated user
     * @throws ResourceNotFoundException if the user is not found
     * @throws DuplicateResourceException if the new email already exists
     */
    GetUserDto updateUser(Long id, CreateUserDto createUserDto);

    /**
     * Soft deletes a user by deactivating them and all their associated payment cards.
     *
     * @param id the ID of the user to delete
     * @throws ResourceNotFoundException if the user is not found
     */
    void deleteUser(Long id);

    /**
     * Creates multiple users in a batch operation.
     *
     * @param createUserDtos the list of DTOs containing user creation data
     * @return a list of DTOs of the created users
     * @throws DuplicateResourceException if duplicate emails exist in request or database
     */
    List<GetUserDto> createMultipleUsers(List<CreateUserDto> createUserDtos);

    /**
     * Checks if a user can add more active payment cards (maximum 5).
     *
     * @param userId the ID of the user
     * @return true if the user can add more cards, false otherwise
     * @throws ResourceNotFoundException if the user is not found
     */
    boolean canAddMoreCards(Long userId);

    /**
     * Gets the count of payment cards associated with a user.
     *
     * @param userId the ID of the user
     * @return the number of cards for the user
     * @throws ResourceNotFoundException if the user is not found
     */
    int getCardCountByUserId(Long userId);

    List<GetUserDto> getUserByIds(List<Long> ids);

    GetUserDto getUserByEmail(String email);
}