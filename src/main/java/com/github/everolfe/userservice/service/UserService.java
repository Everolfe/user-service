package com.github.everolfe.userservice.service;

import com.github.everolfe.userservice.dao.PaymentCardRepository;
import com.github.everolfe.userservice.dao.UserRepository;
import com.github.everolfe.userservice.dao.UserSpecification;
import com.github.everolfe.userservice.dto.userdto.CreateUserDto;
import com.github.everolfe.userservice.dto.userdto.GetUserDto;
import com.github.everolfe.userservice.entity.User;
import com.github.everolfe.userservice.exception.DuplicateResourceException;
import com.github.everolfe.userservice.exception.ResourceNotFoundException;
import com.github.everolfe.userservice.mapper.usermapper.CreateUserMapper;
import com.github.everolfe.userservice.mapper.usermapper.GetUserMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final CreateUserMapper createUserMapper;
    private final GetUserMapper getUserMapper;

    @Transactional
    public GetUserDto createUser(CreateUserDto createUserDto) {
        if (userRepository.existsByEmail(createUserDto.getEmail())) {
            throw new DuplicateResourceException(
                    "User with email " + createUserDto.getEmail() + " already exists");
        }
        User user = createUserMapper.toEntity(createUserDto);
        User savedUser = userRepository.save(user);
        return getUserMapper.toDto(savedUser);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#id")
    public GetUserDto getUserById(Long id) {
        Optional<User> user = userRepository.findById(id);
        if (!user.isPresent()) {
            throw new ResourceNotFoundException("User not found with id" + id);
        } else {
            return getUserMapper.toDto(user.get());
        }
    }

    @Transactional(readOnly = true)
    public Page<GetUserDto> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return users.map(getUserMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<GetUserDto> getUsersByNameOrSurname(String name, String surname, Pageable pageable) {
        Specification<User> spec = UserSpecification.filterByNameAndSurname(name, surname);
        return userRepository.findAll(spec, pageable)
                .map(getUserMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<GetUserDto> searchUsersOnlyByNameOrSurname(String searchTerm, Pageable pageable) {
        return userRepository.findByNameContainingIgnoreCaseOrSurnameContainingIgnoreCase(
                        searchTerm, searchTerm, pageable)
                .map(getUserMapper::toDto);
    }

    @Transactional
    @CachePut(value = "users", key = "#id")
    public GetUserDto activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setActive(true);
        userRepository.save(user);
        return getUserMapper.toDto(user);
    }

    @Transactional
    @CachePut(value = "users", key = "#id")
    public GetUserDto  deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setActive(false);
        userRepository.save(user);
        return getUserMapper.toDto(user);
    }

    @Transactional
    @CachePut(value = "users", key = "#id")
    public GetUserDto updateUser(Long id, CreateUserDto createUserDto) {
        User user = userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No user with id" + id));

        if (createUserDto.getEmail() != null &&
                !createUserDto.getEmail().equals(user.getEmail()) &&
                userRepository.existsByEmail(createUserDto.getEmail())) {
            throw new DuplicateResourceException(
                    "User with email " + createUserDto.getEmail() + " already exists");
        }
        User updatedUser = createUserMapper.toEntity(createUserDto);
        updatedUser.setId(id);

        int updatedCount = userRepository.updateUserDynamic(updatedUser);
        if (updatedCount == 0) {
            throw new ResourceNotFoundException("Failed to update user with id" + id);
        }
        return getUserMapper.toDto(updatedUser);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void deleteUser(Long id) {
        User user = userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No user with id" + id));
        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public List<GetUserDto> createMultipleUsers(List<CreateUserDto> createUserDtos) {
        Set<String> emailsInRequest = createUserDtos.stream()
                .map(CreateUserDto::getEmail)
                .collect(Collectors.toSet());

        if (emailsInRequest.size() != createUserDtos.size()) {
            throw new DuplicateResourceException("Duplicate emails in request");
        }

        List<String> existingEmails = userRepository.findExistingEmails(emailsInRequest);
        if (!existingEmails.isEmpty()) {
            throw new DuplicateResourceException(
                    "Users with emails already exist: " + String.join(", ", existingEmails));
        }

        List<User> users = createUserMapper.toEntities(createUserDtos);

        users.forEach(user -> {
            if (user.getActive() == null) {
                user.setActive(true);
            }
        });

        List<User> savedUsers = userRepository.saveAll(users);

        return getUserMapper.toDtos(savedUsers);
    }


    @Transactional(readOnly = true)
    public boolean canAddMoreCards(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return userRepository.canAddMoreCards(userId);
    }

    @Transactional(readOnly = true)
    public int getCardCountByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return userRepository.getCardCountByUserId(userId);
    }

}
