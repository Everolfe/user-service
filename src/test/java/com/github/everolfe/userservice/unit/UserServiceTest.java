package com.github.everolfe.userservice.unit;

import com.github.everolfe.userservice.dao.UserRepository;
import com.github.everolfe.userservice.dto.userdto.CreateUserDto;
import com.github.everolfe.userservice.dto.userdto.GetUserDto;
import com.github.everolfe.userservice.entity.PaymentCard;
import com.github.everolfe.userservice.entity.User;
import com.github.everolfe.userservice.exception.DuplicateResourceException;
import com.github.everolfe.userservice.exception.ResourceNotFoundException;
import com.github.everolfe.userservice.mapper.usermapper.CreateUserMapper;
import com.github.everolfe.userservice.mapper.usermapper.GetUserMapper;
import com.github.everolfe.userservice.service.UserService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CreateUserMapper createUserMapper;

    @Mock
    private GetUserMapper getUserMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void testCreateUserSuccess() {
        CreateUserDto createUserDto = new CreateUserDto();
        createUserDto.setEmail("testNew@test.com");
        User userToSave = new User();
        userToSave.setEmail("testNew@email.com");
        User savedUser = new User();
        savedUser.setEmail("testNew@test.com");
        GetUserDto getUserDto = new GetUserDto();
        getUserDto.setEmail("testNew@test.com");

        when(userRepository.existsByEmail(createUserDto.getEmail())).thenReturn(false);
        when(createUserMapper.toEntity(createUserDto)).thenReturn(userToSave);
        when(userRepository.save(userToSave)).thenReturn(savedUser);
        when(getUserMapper.toDto(savedUser)).thenReturn(getUserDto);

        GetUserDto result = userService.createUser(createUserDto);

        assertNotNull(result);
        assertEquals(savedUser.getEmail(), result.getEmail());

        verify(userRepository, times(1)).existsByEmail(createUserDto.getEmail());
        verify(createUserMapper, times(1)).toEntity(createUserDto);
        verify(getUserMapper, times(1)).toDto(savedUser);

    }

    @Test
    void testCreateUserWithExistingEmail() {
        CreateUserDto  createUserDto = new CreateUserDto();
        createUserDto.setEmail("email");
        when(userRepository.existsByEmail(createUserDto.getEmail())).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> userService.createUser(createUserDto));
    }

    @Test
    void testGetUserByIdSuccess() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        GetUserDto getUserDto = new GetUserDto();
        getUserDto.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(getUserMapper.toDto(user)).thenReturn(getUserDto);

        GetUserDto result = userService.getUserById(userId);

        assertNotNull(result);
        assertEquals(userId, result.getId());
    }

    @Test
    void testGetUserByIdWithIncorrectId() {
        assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserById(1L));
    }

    @Test
    void testGetAllUsersSuccess() {
        Pageable pageable = Pageable.unpaged();
        User user = new User();

        Page<User> page = new PageImpl<User>(List.of(user));

        when(userRepository.findAll(pageable)).thenReturn(page);

        Page<GetUserDto> result = userService.getAllUsers(pageable);

        assertNotNull(result);
        assertEquals(page.getTotalElements(), result.getTotalElements());
        verify(getUserMapper,times(1)).toDto(user);
    }

    @Test
    void testGetUsersByNameOrSurnameSuccess(){
        String name = "John";
        String surname = "Doe";
        Pageable pageable = Pageable.unpaged();
        User user1 = new User();
        user1.setId(1L);
        user1.setName("John");
        user1.setSurname("Doe");
        user1.setEmail("john.doe@example.com");

        User user2 = new User();
        user2.setId(2L);
        user2.setName("John");
        user2.setSurname("Doe");
        user2.setEmail("john.doe2@example.com");

        List<User> users = List.of(user1, user2);
        Page<User> userPage = new PageImpl<>(users, pageable, users.size());

        GetUserDto dto1 = new GetUserDto();
        dto1.setId(1L);
        dto1.setName("John");
        dto1.setSurname("Doe");
        dto1.setEmail("john.doe@example.com");

        GetUserDto dto2 = new GetUserDto();
        dto2.setId(2L);
        dto2.setName("John");
        dto2.setSurname("Doe");
        dto2.setEmail("john.doe2@example.com");

        when(userRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(userPage);
        when(getUserMapper.toDto(user1)).thenReturn(dto1);
        when(getUserMapper.toDto(user2)).thenReturn(dto2);

        Page<GetUserDto> result = userService.getUsersByNameOrSurname(name, surname, pageable);


        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(2, result.getTotalElements()),
                () -> assertEquals(2, result.getContent().size()),
                () -> assertEquals(1L, result.getContent().get(0).getId()),
                () -> assertEquals(2L, result.getContent().get(1).getId()),
                () -> assertEquals("John", result.getContent().get(0).getName()),
                () -> assertEquals("Doe", result.getContent().get(0).getSurname()),
                () -> assertEquals("john.doe@example.com", result.getContent().get(0).getEmail())
        );

        verify(userRepository, times(1))
                .findAll(any(Specification.class), eq(pageable));
        verify(getUserMapper, times(1)).toDto(user1);
        verify(getUserMapper, times(1)).toDto(user2);
    }

    @Test
    void testSearchUsersOnlyByNameOrSurnameSuccess(){
        String searchTerm = "john";
        Pageable pageable = Pageable.unpaged();

        User user1 = new User();
        user1.setId(1L);
        user1.setName("John");
        user1.setSurname("Doe");
        user1.setEmail("john.doe@example.com");

        User user2 = new User();
        user2.setId(2L);
        user2.setName("Jane");
        user2.setSurname("Johnson");
        user2.setEmail("jane.johnson@example.com");

        User user3 = new User();
        user3.setId(3L);
        user3.setName("Bob");
        user3.setSurname("Smith");
        user3.setEmail("bob.smith@example.com");

        List<User> matchingUsers = List.of(user1, user2);
        Page<User> userPage = new PageImpl<>(matchingUsers, pageable, matchingUsers.size());

        GetUserDto dto1 = new GetUserDto();
        dto1.setId(1L);
        dto1.setName("John");
        dto1.setSurname("Doe");
        dto1.setEmail("john.doe@example.com");

        GetUserDto dto2 = new GetUserDto();
        dto2.setId(2L);
        dto2.setName("Jane");
        dto2.setSurname("Johnson");
        dto2.setEmail("jane.johnson@example.com");

        when(userRepository.findByNameContainingIgnoreCaseOrSurnameContainingIgnoreCase(
                searchTerm, searchTerm, pageable))
                .thenReturn(userPage);
        when(getUserMapper.toDto(user1)).thenReturn(dto1);
        when(getUserMapper.toDto(user2)).thenReturn(dto2);

        Page<GetUserDto> result = userService.searchUsersOnlyByNameOrSurname(searchTerm, pageable);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(2, result.getTotalElements()),
                () -> assertEquals(2, result.getContent().size()),
                () -> assertEquals(1L, result.getContent().get(0).getId()),
                () -> assertEquals(2L, result.getContent().get(1).getId()),
                () -> assertTrue(result.getContent().get(0).getName()
                        .toLowerCase().contains(searchTerm) ||
                        result.getContent().get(0).getSurname()
                                .toLowerCase().contains(searchTerm)),
                () -> assertTrue(result.getContent().get(1).getName()
                        .toLowerCase().contains(searchTerm) ||
                        result.getContent().get(1).getSurname().toLowerCase().contains(searchTerm))
        );
        verify(userRepository, times(1))
                .findByNameContainingIgnoreCaseOrSurnameContainingIgnoreCase(
                        searchTerm, searchTerm, pageable);
        verify(getUserMapper, times(1)).toDto(user1);
        verify(getUserMapper, times(1)).toDto(user2);
    }

    @Test
    void testActivateUserSuccess() {
        when(userRepository.activateUserNative(1l))
                .thenReturn(1);
        userService.activateUser(1l);
        verify(userRepository, times(1)).activateUserNative(1l);
    }

    @Test
    void testActivateUserWithIncorrectId() {
        assertThrows(ResourceNotFoundException.class, () ->
                userService.activateUser(1l));
    }

    @Test
    void testDeactivateUserSuccess() {
        when(userRepository.deactivateUserNative(1l))
                .thenReturn(1);
        userService.deactivateUser(1l);
        verify(userRepository, times(1))
                .deactivateUserNative(1l);
    }

    @Test
    void testDeactivateUserWithIncorrectId() {
        assertThrows(ResourceNotFoundException.class,
                () -> userService.deactivateUser(1L));
    }

    @Test
    void testUpdateUserSuccess() {
        Long userId = 1L;
        String newEmail = "new.email@example.com";
        String newName = "Updated Name";
        String newSurname = "Updated Surname";

        CreateUserDto updateDto = new CreateUserDto();
        updateDto.setEmail(newEmail);
        updateDto.setName(newName);
        updateDto.setSurname(newSurname);
        updateDto.setBirthDate(LocalDate.of(1990, 1, 1));
        updateDto.setActive(true);

        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setEmail("old.email@example.com");
        existingUser.setName("Old Name");
        existingUser.setSurname("Old Surname");

        User updatedUser = new User();
        updatedUser.setId(userId);
        updatedUser.setEmail(newEmail);
        updatedUser.setName(newName);
        updatedUser.setSurname(newSurname);

        GetUserDto expectedDto = new GetUserDto();
        expectedDto.setId(userId);
        expectedDto.setEmail(newEmail);
        expectedDto.setName(newName);
        expectedDto.setSurname(newSurname);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail(newEmail)).thenReturn(false);
        when(createUserMapper.toEntity(updateDto)).thenReturn(updatedUser);
        when(userRepository.updateUserDynamic(updatedUser)).thenReturn(1);
        when(getUserMapper.toDto(updatedUser)).thenReturn(expectedDto);
        GetUserDto result = userService.updateUser(userId, updateDto);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(userId, result.getId()),
                () -> assertEquals(newEmail, result.getEmail()),
                () -> assertEquals(newName, result.getName()),
                () -> assertEquals(newSurname, result.getSurname())
        );

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).existsByEmail(newEmail);
        verify(createUserMapper, times(1)).toEntity(updateDto);
        verify(userRepository, times(1)).updateUserDynamic(updatedUser);
        verify(getUserMapper, times(1)).toDto(updatedUser);
    }

    @Test
    void testUpdateUserWithIncorrectId() {
        CreateUserDto createUserDto = new CreateUserDto();
        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateUser(1l, createUserDto));
    }

    @Test
    void testUpdateUserWithExistingEmail() {
        Long userId = 1L;
        CreateUserDto createUserDto = new CreateUserDto();
        createUserDto.setEmail("test@test.com");
        User user = new User();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail(createUserDto.getEmail())).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> userService.updateUser(userId, createUserDto));

        verify(userRepository, times(1)).existsByEmail(createUserDto.getEmail());
        verify(userRepository, times(1)).existsByEmail(any());
    }

    @Test
    void testDeleteUserSuccess() {
        Long userId = 1L;

        when(userRepository.existsById(userId)).thenReturn(true);

        userService.deleteUser(userId);

        verify(userRepository, times(1)).deleteById(userId);
        verify(userRepository, times(1)).deleteById(userId);
    }

    @Test
    void testDeleteUserWithIncorrectId() {
        assertThrows(ResourceNotFoundException.class,
                ()-> userService.deleteUser(1L));
    }

    @Test
    void testCanAddMoreCardsSuccess(){
        Long userId = 1L;
        User user = new User();
        PaymentCard card1 = new PaymentCard();
        card1.setUser(user);
        PaymentCard card2 = new PaymentCard();
        card2.setUser(user);
        user.setPaymentCards( List.of(card1,card2));

        when(userRepository.existsById(userId)).thenReturn(true);
        when(userRepository.canAddMoreCards(userId)).thenReturn(true);

        boolean result = userService.canAddMoreCards(userId);
        assertEquals(true, result);

        verify(userRepository, times(1)).canAddMoreCards(userId);
        verify(userRepository, times(1)).existsById(userId);
    }

    @Test
    void testCanAddMoreCardsWithIncorrectId(){
        assertThrows(ResourceNotFoundException.class,
                () -> userService.canAddMoreCards(1L));
    }

    @Test
    void testGetCardCountByUserIdSuccess(){
        Long userId = 1L;
        User user = new User();
        PaymentCard card1 = new PaymentCard();
        card1.setUser(user);
        PaymentCard card2 = new PaymentCard();
        card2.setUser(user);
        user.setPaymentCards( List.of(card1,card2));

        when(userRepository.existsById(userId)).thenReturn(true);
        when(userRepository.getCardCountByUserId(userId)).thenReturn(user.getPaymentCards().size());

        int result = userService.getCardCountByUserId(userId);

        assertEquals(2, result);

        verify(userRepository,times(1)).existsById(any());
        verify(userRepository,times(1)).getCardCountByUserId(any());
    }

    @Test
    void testGetCardCountByUserIdWithIncorrectId(){
        assertThrows(ResourceNotFoundException.class,
                () -> userService.getCardCountByUserId(1L));
    }
}
