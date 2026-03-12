package com.github.everolfe.userservice.integration;

import com.github.everolfe.userservice.dao.UserRepository;
import com.github.everolfe.userservice.dto.userdto.CreateUserDto;
import com.github.everolfe.userservice.dto.userdto.GetUserDto;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import org.springframework.test.annotation.DirtiesContext;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class UserIntegrationTest extends BaseIntegrationTest {

    private static final String BASE_PATH = "/api/users";


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.defaultParser = io.restassured.parsing.Parser.JSON;
    }

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
        cacheManager.getCacheNames().stream()
                .forEach(cacheName -> Objects.requireNonNull(cacheManager.getCache(cacheName)).clear());
    }


    @Test
    void createUser_ShouldReturnCreatedUser_WhenDataIsValid() {
        CreateUserDto createDto = createUserDto(
                "john.doe@example.com", "John", "Doe");

        GetUserDto createdUser = given()
                .contentType(ContentType.JSON)
                .body(createDto)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(GetUserDto.class);

        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getId()).isNotNull();
        assertThat(createdUser.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(createdUser.getName()).isEqualTo("John");
        assertThat(createdUser.getSurname()).isEqualTo("Doe");
        assertThat(createdUser.getActive()).isTrue();
        assertThat(createdUser.getCreatedAt()).isNotNull();
        assertThat(createdUser.getUpdatedAt()).isNotNull();
    }

    @Test
    void createUser_ShouldReturnConflict_WhenEmailAlreadyExists() {
        String email = "duplicate@example.com";
        CreateUserDto createDto = createUserDto(email, "John", "Doe");

        given()
                .contentType(ContentType.JSON)
                .body(createDto)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(HttpStatus.OK.value());

        given()
                .contentType(ContentType.JSON)
                .body(createDto)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("message", containsString("already exists"));
    }

    @Test
    void getUserById_ShouldReturnUser_WhenUserExists() {
        Long userId = createUserAndGetId(createUserDto(
                "get.by.id@example.com", "Get", "User"));

        GetUserDto retrievedUser = given()
                .when()
                .get(BASE_PATH + "/{id}", userId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(GetUserDto.class);

        assertThat(retrievedUser.getId()).isEqualTo(userId);
        assertThat(retrievedUser.getEmail()).isEqualTo("get.by.id@example.com");
        assertThat(retrievedUser.getName()).isEqualTo("Get");
        assertThat(retrievedUser.getSurname()).isEqualTo("User");
    }

    @Test
    void getUserById_ShouldReturnNotFound_WhenUserDoesNotExist() {
        given()
                .when()
                .get(BASE_PATH + "/{id}", 99999L)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void getAllUsers_ShouldReturnPagedResults() {
        for (int i = 0; i < 15; i++) {
            CreateUserDto createDto = createUserDto(
                    "user" + i + "@example.com",
                    "FirstName" + i,
                    "LastName" + i
            );
            createUser(createDto);
        }

        given()
                .param("page", 0)
                .param("size", 10)
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("content.size()", is(10))
                .body("totalElements", greaterThanOrEqualTo(15))
                .body("totalPages", greaterThanOrEqualTo(2))
                .body("number", is(0))
                .body("size", is(10));

        given()
                .param("page", 1)
                .param("size", 10)
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("content.size()", is(5))
                .body("number", is(1));
    }

    @Test
    void getUsersByNameAndSurname_ShouldReturnMatchingUsers() {
        CreateUserDto johnDto = createUserDto(
                "john.smith@example.com", "John", "Smith");
        CreateUserDto janeDto = createUserDto(
                "jane.smith@example.com", "Jane", "Smith");
        CreateUserDto bobDto = createUserDto(
                "bob.johnson@example.com", "Bob", "Johnson");
        CreateUserDto aliceDto = createUserDto(
                "alice.wonder@example.com", "Alice", "Wonder");

        createUser(johnDto);
        createUser(janeDto);
        createUser(bobDto);
        createUser(aliceDto);

        given()
                .param("surname", "Smith")
                .param("page", 0)
                .param("size", 10)
                .when()
                .get(BASE_PATH + "/search")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("content.size()", is(2))
                .body("content[0].surname", is("Smith"))
                .body("content[1].surname", is("Smith"))
                .body("content.name", hasItems("John", "Jane"))
                .body("totalElements", is(2))
                .body("totalPages", is(1))
                .body("number", is(0))
                .body("size", is(10));
    }

    @Test
    void getUsersByNameAndSurname_WithOnlyName_ShouldReturnMatchingUsers() {
        CreateUserDto johnDto = createUserDto(
                "john.doe@example.com", "John", "Doe");
        CreateUserDto johnnyDto = createUserDto(
                "johnny.deep@example.com", "Johnny", "Deep");
        CreateUserDto janeDto = createUserDto(
                "jane.doe@example.com", "Jane", "Doe");

        createUser(johnDto);
        createUser(johnnyDto);
        createUser(janeDto);

        given()
                .param("name", "John")
                .param("page", 0)
                .param("size", 10)
                .when()
                .get(BASE_PATH + "/search")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("content.size()", is(2))
                .body("content[0].name", anyOf(is("John"), is("Johnny")))
                .body("content[1].name", anyOf(is("John"), is("Johnny")))
                .body("totalElements", is(2))
                .body("totalPages", is(1))
                .body("number", is(0))
                .body("size", is(10));
    }

    @Test
    void searchUsersBySearchTerm_ShouldReturnMatchingUsers() {
        CreateUserDto aliceDto = createUserDto(
                "alice@example.com", "Alice", "Wonder");
        CreateUserDto bobDto = createUserDto(
                "bob@example.com", "Bob", "Alice");
        CreateUserDto charlieDto = createUserDto(
                "charlie@example.com", "Charlie", "Brown");
        CreateUserDto danielDto = createUserDto(
                "daniel@example.com", "Daniel", "ALICE");

        createUser(aliceDto);
        createUser(bobDto);
        createUser(charlieDto);
        createUser(danielDto);

        given()
                .param("searchTerm", "Alice")
                .param("page", 0)
                .param("size", 10)
                .when()
                .get(BASE_PATH + "/search/term")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("content.size()", is(3))
                .body("content.name", containsInAnyOrder("Alice", "Bob", "Daniel"))
                .body("content.surname", containsInAnyOrder("Wonder", "Alice", "ALICE"))
                .body("content.email", containsInAnyOrder(
                        "alice@example.com",
                        "bob@example.com",
                        "daniel@example.com"
                ))
                .body("totalElements", is(3))
                .body("totalPages", is(1))
                .body("number", is(0))
                .body("size", is(10))
                .body("first", is(true))
                .body("last", is(true));
    }

    @Test
    void searchUsersBySearchTerm_WithEmptyTerm_ShouldReturnAllUsers() {

        for (int i = 0; i < 5; i++) {
            createUser(createUserDto(
                    "user" + i + "@example.com", "User" + i, "Test" + i));
        }


        given()
                .param("searchTerm", "")
                .param("page", 0)
                .param("size", 20)
                .when()
                .get(BASE_PATH + "/search/term")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("content.size()", is(5))
                .body("totalElements", is(5))
                .body("totalPages", is(1))
                .body("number", is(0))
                .body("size", is(20))
                .body("first", is(true))
                .body("last", is(true));
    }

    @Test
    void activateUser_ShouldChangeUserStatus() {
        Long userId = createUserAndGetId(createUserDto(
                "activate@example.com", "Activate", "User"));


        given()
                .when()
                .get(BASE_PATH + "/{id}", userId)
                .then()
                .log().all()
                .statusCode(HttpStatus.OK.value())
                .body("active", is(true));

        given()
                .when()
                .patch(BASE_PATH + "/{id}/deactivate", userId)
                .then()
                .log().all()
                .statusCode(HttpStatus.NO_CONTENT.value());
        given()
                .when()
                .patch(BASE_PATH + "/{id}/activate", userId)
                .then()
                .log().all()
                .statusCode(HttpStatus.NO_CONTENT.value());
        given()
                .when()
                .get(BASE_PATH + "/{id}", userId)
                .then()
                .log().all()
                .statusCode(HttpStatus.OK.value())
                .body("active", is(true));
    }

    @Test
    void activateUser_ShouldReturnNotFound_WhenUserDoesNotExist() {
        given()
                .when()
                .patch(BASE_PATH + "/{id}/activate", 99999L)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void deactivateUser_ShouldReturnNotFound_WhenUserDoesNotExist() {
        given()
                .when()
                .patch(BASE_PATH + "/{id}/deactivate", 99999L)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void updateUser_ShouldModifyUserDetails() {
        Long userId = createUserAndGetId(createUserDto(
                "update@example.com", "Old", "Name"));

        CreateUserDto updateDto = new CreateUserDto();
        updateDto.setName("NewName");
        updateDto.setSurname("NewSurname");
        updateDto.setEmail("new@example.com");
        updateDto.setBirthDate(LocalDate.of(1995, 5, 5));
        updateDto.setActive(true);

        given()
                .contentType(ContentType.JSON)
                .body(updateDto)
                .when()
                .put(BASE_PATH + "/{id}", userId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("name", is("NewName"))
                .body("surname", is("NewSurname"))
                .body("email", is("new@example.com"));
    }

    @Test
    void updateUser_ShouldReturnConflict_WhenEmailAlreadyExists() {
        Long userId1 = createUserAndGetId(createUserDto(
                "first@example.com", "First", "User"));
        createUserAndGetId(createUserDto("second@example.com", "Second", "User"));

        CreateUserDto updateDto = new CreateUserDto();
        updateDto.setEmail("second@example.com");
        updateDto.setName("First");
        updateDto.setSurname("User");
        updateDto.setBirthDate(LocalDate.of(1990, 1, 1));
        updateDto.setActive(true);
        updateDto.setEmail("second@example.com");

        given()
                .contentType(ContentType.JSON)
                .body(updateDto)
                .when()
                .put(BASE_PATH + "/{id}", userId1)
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("message", containsString("already exists"))
                .body("status", is(409))
                .body("timestamp", notNullValue());
    }

    @Test
    void deleteUser_ShouldRemoveUser() {
        Long userId = createUserAndGetId(createUserDto("delete@example.com", "Delete", "User"));

        GetUserDto user = getUserById(userId);
        assertThat(user).isNotNull();

        given()
                .when()
                .delete(BASE_PATH + "/{id}", userId)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        given()
                .when()
                .get(BASE_PATH + "/{id}", userId)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void deleteUser_ShouldReturnNotFound_WhenUserDoesNotExist() {
        given()
                .when()
                .delete(BASE_PATH + "/{id}", 99999L)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void canAddMoreCards_ShouldReturnTrue_WhenUserHasLessThan5Cards() {
        Long userId = createUserAndGetId(createUserDto("cards.test@example.com", "Card", "Test"));

        Boolean canAdd = given()
                .when()
                .get(BASE_PATH + "/{userId}/can-add-cards", userId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(Boolean.class);

        assertThat(canAdd).isTrue();
    }

    @Test
    void canAddMoreCards_ShouldReturnNotFound_WhenUserDoesNotExist() {
        given()
                .when()
                .get(BASE_PATH + "/{userId}/can-add-cards", 99999L)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void getCardCountByUserId_ShouldReturnZero_WhenUserHasNoCards() {
        Long userId = createUserAndGetId(createUserDto("card.count@example.com", "Card", "Count"));

        Integer count = given()
                .when()
                .get(BASE_PATH + "/{userId}/cards-count", userId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(Integer.class);

        assertThat(count).isZero();
    }

    @Test
    void getCardCountByUserId_ShouldReturnNotFound_WhenUserDoesNotExist() {
        given()
                .when()
                .get(BASE_PATH + "/{userId}/cards-count", 99999L)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }



    @Test
    void testCache_FullLifecycle_Get_Update_Delete() {
        String uniqueEmail = "cache.test." + System.currentTimeMillis() + "@example.com";
        CreateUserDto createDto = createUserDto(uniqueEmail, "Ivan", "Ivanov");

        Long userId = given()
                .contentType(ContentType.JSON)
                .body(createDto)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(GetUserDto.class)
                .getId();

        assertNull(getCacheValue(userId));

        GetUserDto userFromDb = given()
                .when()
                .get(BASE_PATH + "/{id}", userId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(GetUserDto.class);

        assertThat(userFromDb.getName()).isEqualTo("Ivan");
        assertNotNull(getCacheValue(userId));

        GetUserDto userFromCache = given()
                .when()
                .get(BASE_PATH + "/{id}", userId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(GetUserDto.class);

        assertThat(userFromCache.getName()).isEqualTo("Ivan");

        CreateUserDto updateDto = new CreateUserDto();
        updateDto.setName("Petr");
        updateDto.setSurname("Ivanov");
        updateDto.setEmail(uniqueEmail);
        updateDto.setBirthDate(LocalDate.of(1990, 1, 1));
        updateDto.setActive(true);

        GetUserDto updatedUser = given()
                .contentType(ContentType.JSON)
                .body(updateDto)
                .when()
                .put(BASE_PATH + "/{id}", userId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(GetUserDto.class);

        assertThat(updatedUser.getName()).isEqualTo("Petr");
        assertNotNull(getCacheValue(userId));

        GetUserDto userAfterUpdate = given()
                .when()
                .get(BASE_PATH + "/{id}", userId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(GetUserDto.class);

        assertThat(userAfterUpdate.getName()).isEqualTo("Petr");

        given()
                .when()
                .delete(BASE_PATH + "/{id}", userId)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        assertNull(getCacheValue(userId));

        given()
                .when()
                .get(BASE_PATH + "/{id}", userId)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }



    private CreateUserDto createUserDto(String email, String name, String surname) {
        CreateUserDto dto = new CreateUserDto();
        dto.setName(name);
        dto.setSurname(surname);
        dto.setEmail(email);
        dto.setBirthDate(LocalDate.of(1990, 1, 1));
        dto.setActive(true);
        return dto;
    }

    private Object getCacheValue(Long id) {
        Cache usersCache = cacheManager.getCache("users");
        if (usersCache == null) {
            return null;
        }
        Cache.ValueWrapper valueWrapper = usersCache.get(id);
        return (valueWrapper != null) ? valueWrapper.get() : null;
    }

    private Long createUserAndGetId(CreateUserDto createDto) {
        return given()
                .contentType(ContentType.JSON)
                .body(createDto)
                .when()
                .post(BASE_PATH)
                .then()
                .extract()
                .as(GetUserDto.class)
                .getId();
    }

    private void createUser(CreateUserDto createDto) {
        given()
                .contentType(ContentType.JSON)
                .body(createDto)
                .when()
                .post(BASE_PATH);
    }

    private GetUserDto getUserById(Long id) {
        return given()
                .when()
                .get(BASE_PATH + "/{id}", id)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(GetUserDto.class);
    }
}