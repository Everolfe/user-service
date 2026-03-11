package com.github.everolfe.userservice.controller;


import com.github.everolfe.userservice.dto.userdto.CreateUserDto;
import com.github.everolfe.userservice.dto.userdto.GetUserDto;
import com.github.everolfe.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<GetUserDto> createUser(@RequestBody CreateUserDto createUserDto) {
        GetUserDto getUserDto = userService.createUser(createUserDto);
        return new ResponseEntity<>(getUserDto, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GetUserDto> getUser(@PathVariable Long id) {
        GetUserDto user = userService.getUserById(id);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<Page<GetUserDto>> getUsers(Pageable pageable) {
        Page<GetUserDto> users = userService.getAllUsers(pageable);
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<GetUserDto>> getUsersByNameAndSurname(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String surname,
            Pageable pageable) {
        Page<GetUserDto> users = userService.getUsersByNameOrSurname(name, surname, pageable);

        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("/search/term")
    public ResponseEntity<Page<GetUserDto>> searchUsersByNameOrSurname(
            @RequestParam String searchTerm,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<GetUserDto> users = userService.searchUsersOnlyByNameOrSurname(searchTerm, pageable);
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateUser(@PathVariable Long id) {
        userService.activateUser(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GetUserDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody CreateUserDto createUserDto) {
        GetUserDto updatedUser = userService.updateUser(id, createUserDto);
        return new ResponseEntity<>(updatedUser, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{userId}/can-add-cards")
    public ResponseEntity<Boolean> canAddMoreCards(@PathVariable Long userId) {
        boolean canAdd = userService.canAddMoreCards(userId);
        return new ResponseEntity<>(canAdd, HttpStatus.OK);
    }

    @GetMapping("/{userId}/cards-count")
    public ResponseEntity<Integer> getCardCountByUserId(@PathVariable Long userId) {
        int count = userService.getCardCountByUserId(userId);
        return new ResponseEntity<>(count, HttpStatus.OK);
    }
}
