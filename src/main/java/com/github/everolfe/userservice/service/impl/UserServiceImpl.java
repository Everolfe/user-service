package com.github.everolfe.userservice.service.impl;

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
import com.github.everolfe.userservice.service.UserService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.github.everolfe.userservice.dao.UserSpecification.isActive;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final CreateUserMapper createUserMapper;
    private final GetUserMapper getUserMapper;
    private final PaymentCardRepository paymentCardRepository;
    private final CacheManager cacheManager;

    @Override
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

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#id")
    public GetUserDto getUserById(Long id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isEmpty()) {
            throw new ResourceNotFoundException("User not found with id" + id);
        } else if (Boolean.FALSE.equals(user.get().getActive())) {
            throw new ResourceNotFoundException("User is deactivated");
        } else {
            return getUserMapper.toDto(user.get());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GetUserDto> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(isActive(), pageable);
        return users.map(getUserMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GetUserDto> getUsersByNameOrSurname(String name, String surname, Pageable pageable) {
        Specification<User> spec = UserSpecification
                .filterByNameAndSurname(name, surname)
                .and(isActive());
        return userRepository
                .findAll(spec, pageable)
                .map(getUserMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GetUserDto> searchUsersOnlyByNameOrSurname(String searchTerm, Pageable pageable) {
        return userRepository.findByNameContainingIgnoreCaseOrSurnameContainingIgnoreCase(
                        searchTerm, searchTerm, pageable)
                .map(getUserMapper::toDto);
    }

    @Override
    @Transactional
    @CachePut(value = "users", key = "#id")
    public GetUserDto activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setActive(true);
        userRepository.save(user);
        return getUserMapper.toDto(user);
    }

    @Override
    @Transactional
    @CachePut(value = "users", key = "#id")
    public GetUserDto  deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setActive(false);
        userRepository.save(user);
        deactivateUserCards(id);
        return getUserMapper.toDto(user);
    }

    @Override
    @Transactional
    @CachePut(value = "users", key = "#id")
    public GetUserDto updateUser(Long id, CreateUserDto createUserDto) {
        User user = userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No user with id: " + id));

        if (createUserDto.getEmail() != null &&
                !createUserDto.getEmail().equals(user.getEmail()) &&
                userRepository.existsByEmail(createUserDto.getEmail())) {
            throw new DuplicateResourceException(
                    "User with email " + createUserDto.getEmail() + " already exists");
        }

        boolean isDeactivating = Boolean.TRUE.equals(user.getActive()) &&
                Boolean.FALSE.equals(createUserDto.getActive());

        User userToUpdate = createUserMapper.toEntity(createUserDto);
        userToUpdate.setId(id);

        int updatedCount = userRepository.updateUserDynamic(userToUpdate);
        if (updatedCount == 0) {
            throw new ResourceNotFoundException("Failed to update user with id: " + id);
        }

        if (isDeactivating) {
            deactivateUserCards(id);
        }

        return getUserMapper.toDto(userToUpdate);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "users", key = "#id"),
            @CacheEvict(value = "cards", allEntries = true)
    })
    public void deleteUser(Long id) {
        User user = userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No user with id" + id));
        user.setActive(false);
        userRepository.save(user);
        deactivateUserCards(id);
    }

    @Override
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


    @Override
    @Transactional(readOnly = true)
    public boolean canAddMoreCards(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return userRepository.canAddMoreCards(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public int getCardCountByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return userRepository.getCardCountByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GetUserDto> getUserByIds(List<Long> ids) {
        List<User> existingUsers = userRepository.findAllById(ids);
        return getUserMapper.toDtos(existingUsers);
    }

    @Override
    @Transactional(readOnly = true)
    public GetUserDto getUserByEmail(String email) {
        User user = userRepository
                .findByEmail(email)
                .filter(u -> u.getActive() == Boolean.TRUE)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return getUserMapper.toDto(user);
    }


    private void deactivateUserCards(Long userId) {
        paymentCardRepository.deactivateAllCardsByUserId(userId);

        Cache cardsCache = cacheManager.getCache("cards");
        if (cardsCache != null) {
            List<Long> cardIds = paymentCardRepository.findCardIdsByUserId(userId);
            cardIds.forEach(cardsCache::evict);
        }
    }

}
