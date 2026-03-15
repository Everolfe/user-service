package com.github.everolfe.userservice.integration;

import com.github.everolfe.userservice.dao.PaymentCardRepository;
import com.github.everolfe.userservice.dao.UserRepository;
import com.github.everolfe.userservice.dto.paymentcarddto.CreatePaymentCardDto;
import com.github.everolfe.userservice.dto.paymentcarddto.GetPaymentCardDto;
import com.github.everolfe.userservice.dto.userdto.CreateUserDto;
import com.github.everolfe.userservice.dto.userdto.GetUserDto;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.test.annotation.DirtiesContext;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PaymentCardIntegrationTest extends BaseIntegrationTest {

    private static final String BASE_PATH = "/api/cards";
    private static final String USERS_PATH = "/api/users";

    @Autowired
    private PaymentCardRepository paymentCardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    @LocalServerPort
    private int port;

    private Long testUserId;
    private String validExpirationDate = "12/25";

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        testUserId = createTestUser();
        RestAssured.defaultParser = io.restassured.parsing.Parser.JSON;
    }

    @AfterEach
    void cleanDatabase() {
        paymentCardRepository.deleteAll();
        userRepository.deleteAll();
        cacheManager.getCacheNames().stream()
                .forEach(cacheName -> Objects.requireNonNull(cacheManager.getCache(cacheName)).clear());
    }

    private Long createTestUser() {
        CreateUserDto userDto = new CreateUserDto();
        userDto.setName("Test");
        userDto.setSurname("User");
        userDto.setEmail("test.user." + System.currentTimeMillis() + "@example.com");
        userDto.setBirthDate(LocalDate.of(1990, 1, 1));
        userDto.setActive(true);

        return given()
                .contentType(ContentType.JSON)
                .body(userDto)
                .when()
                .post(USERS_PATH)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(GetUserDto.class)
                .getId();
    }


    @Test
    void createCard_ShouldReturnCreatedCard_WhenDataIsValid() {
        CreatePaymentCardDto createDto = createPaymentCardDto(
                "1234567890123456", "John Doe", validExpirationDate);

        GetPaymentCardDto createdCard = given()
                .contentType(ContentType.JSON)
                .body(createDto)
                .when()
                .post(BASE_PATH + "/user/{userId}", testUserId)
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .as(GetPaymentCardDto.class);

        assertThat(createdCard).isNotNull();
        assertThat(createdCard.getId()).isNotNull();
        assertThat(createdCard.getNumber()).isEqualTo("1234567890123456");
        assertThat(createdCard.getHolder()).isEqualTo("John Doe");
        assertThat(createdCard.getExpirationDate()).isEqualTo(validExpirationDate);
        assertThat(createdCard.getActive()).isTrue();
        assertThat(createdCard.getUserId()).isEqualTo(testUserId);
        assertThat(createdCard.getCreatedAt()).isNotNull();
        assertThat(createdCard.getUpdatedAt()).isNotNull();
    }

    @Test
    void createCard_ShouldReturnConflict_WhenNumberAlreadyExists() {
        String cardNumber = "1111222233334444";
        CreatePaymentCardDto createDto = createPaymentCardDto(
                cardNumber, "John Doe", validExpirationDate);

        given()
                .contentType(ContentType.JSON)
                .body(createDto)
                .when()
                .post(BASE_PATH + "/user/{userId}", testUserId)
                .then()
                .statusCode(HttpStatus.CREATED.value());

        given()
                .contentType(ContentType.JSON)
                .body(createDto)
                .when()
                .post(BASE_PATH + "/user/{userId}", testUserId)
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("message", containsString("already exists"));
    }

    @Test
    void createCard_ShouldReturnNotFound_WhenUserDoesNotExist() {
        CreatePaymentCardDto createDto = createPaymentCardDto(
                "1234567890123456", "John Doe", validExpirationDate);

        given()
                .contentType(ContentType.JSON)
                .body(createDto)
                .when()
                .post(BASE_PATH + "/user/{userId}", 99999L)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void createCard_ShouldReturnBadRequest_WhenExpirationDateIsInvalid() {
        CreatePaymentCardDto invalidDto = createPaymentCardDto(
                "1234567890123456", "John Doe", "13/25");

        given()
                .contentType(ContentType.JSON)
                .body(invalidDto)
                .when()
                .post(BASE_PATH + "/user/{userId}", testUserId)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }


    @Test
    void getCardById_ShouldReturnCard_WhenCardExists() {
        Long cardId = createTestCard(testUserId, "5555666677778888", "Jane Smith");

        GetPaymentCardDto retrievedCard = given()
                .when()
                .get(BASE_PATH + "/{id}", cardId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(GetPaymentCardDto.class);

        assertThat(retrievedCard.getId()).isEqualTo(cardId);
        assertThat(retrievedCard.getNumber()).isEqualTo("5555666677778888");
        assertThat(retrievedCard.getHolder()).isEqualTo("Jane Smith");
        assertThat(retrievedCard.getUserId()).isEqualTo(testUserId);
    }

    @Test
    void getCardById_ShouldReturnNotFound_WhenCardDoesNotExist() {
        given()
                .when()
                .get(BASE_PATH + "/{id}", 99999L)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }


    @Test
    void getAllCards_ShouldReturnPagedResults() {
        for (int i = 0; i < 5; i++) {
            createTestCard(testUserId,
                    "111122223333" + String.format("%04d", i),
                    "Holder " + i);
        }

        given()
                .param("page", 0)
                .param("size", 5)
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("content.size()", is(5))
                .body("totalElements", is(5))
                .body("totalPages", is(1))
                .body("number", is(0))
                .body("size", is(5))
                .body("first", is(true))
                .body("last", is(true));

        given()
                .param("page", 1)
                .param("size", 5)
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("content.size()", is(0))
                .body("number", is(1))
                .body("totalElements", is(5))
                .body("first", is(false))
                .body("last", is(true));
    }


    @Test
    void getUserCards_ShouldReturnAllUserCards() {
        for (int i = 0; i < 3; i++) {
            createTestCard(testUserId,
                    "999988887777" + String.format("%04d", i),
                    "User Card " + i);
        }

        List<GetPaymentCardDto> userCards = given()
                .when()
                .get(BASE_PATH + "/user/{userId}", testUserId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getList(".", GetPaymentCardDto.class);

        assertThat(userCards).hasSize(3);
        assertThat(userCards)
                .extracting(GetPaymentCardDto::getUserId)
                .allMatch(id -> id.equals(testUserId));
    }

    @Test
    void getUserCards_ShouldReturnNotFound_WhenUserDoesNotExist() {
        given()
                .when()
                .get(BASE_PATH + "/user/{userId}", 99999L)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }


    @Test
    void searchCardsByUserNameAndSurname_ShouldReturnMatchingCards() {
        Long user1Id = createUserWithName("John", "Smith", "john.smith@example.com");
        Long user2Id = createUserWithName("Jane", "Smith", "jane.smith@example.com");
        Long user3Id = createUserWithName("Bob", "Johnson", "bob.johnson@example.com");

        createTestCard(user1Id, "1111222233334444", "John's Card");
        createTestCard(user2Id, "5555666677778888", "Jane's Card");
        createTestCard(user3Id, "9999000011112222", "Bob's Card");

        given()
                .param("surname", "Smith")
                .param("page", 0)
                .param("size", 10)
                .when()
                .get(BASE_PATH + "/search")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("content.size()", is(2))
                .body("totalElements", is(2))
                .body("totalPages", is(1))
                .body("number", is(0))
                .body("size", is(10));
    }

    @Test
    void searchCardsByUserNameAndSurname_WithOnlyName_ShouldReturnMatchingCards() {
        Long user1Id = createUserWithName("John", "Doe", "john.doe@example.com");
        Long user2Id = createUserWithName("Johnny", "Deep", "johnny.deep@example.com");
        Long user3Id = createUserWithName("Jane", "Doe", "jane.doe@example.com");

        createTestCard(user1Id, "1111222233334444", "John's Card");
        createTestCard(user2Id, "5555666677778888", "Johnny's Card");
        createTestCard(user3Id, "9999000011112222", "Jane's Card");

        given()
                .param("name", "John")
                .param("page", 0)
                .param("size", 10)
                .when()
                .get(BASE_PATH + "/search")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("content.size()", is(2))
                .body("totalElements", is(2));
    }


    @Test
    void activateCard_ShouldChangeCardStatus() {
        Long cardId = createTestCard(testUserId, "4444555566667777", "Active Test");

        given()
                .when()
                .get(BASE_PATH + "/{id}", cardId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("active", is(true));

        given()
                .when()
                .patch(BASE_PATH + "/{id}/deactivate", cardId)
                .then()
                .statusCode(HttpStatus.OK.value());

        given()
                .when()
                .get(BASE_PATH + "/{id}", cardId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("active", is(false));

        given()
                .when()
                .patch(BASE_PATH + "/{id}/activate", cardId)
                .then()
                .statusCode(HttpStatus.OK.value());

        given()
                .when()
                .get(BASE_PATH + "/{id}", cardId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("active", is(true));
    }

    @Test
    void activateCard_ShouldReturnNotFound_WhenCardDoesNotExist() {
        given()
                .when()
                .patch(BASE_PATH + "/{id}/activate", 99999L)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void deactivateCard_ShouldReturnNotFound_WhenCardDoesNotExist() {
        given()
                .when()
                .patch(BASE_PATH + "/{id}/deactivate", 99999L)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }


    @Test
    void updateCard_ShouldModifyCardDetails() {
        Long cardId = createTestCard(testUserId, "8888999900001111", "Original Holder");

        CreatePaymentCardDto updateDto = new CreatePaymentCardDto();
        updateDto.setNumber("8888999900001111");
        updateDto.setHolder("Updated Holder");
        updateDto.setExpirationDate("01/26");
        updateDto.setActive(true);

        given()
                .contentType(ContentType.JSON)
                .body(updateDto)
                .when()
                .put(BASE_PATH + "/{id}", cardId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("holder", is("Updated Holder"))
                .body("expirationDate", is("01/26"));
    }

    @Test
    void updateCard_ShouldReturnConflict_WhenNumberAlreadyExists() {
        Long cardId1 = createTestCard(testUserId, "1111222233334444", "First Card");
        createTestCard(testUserId, "5555666677778888", "Second Card");

        CreatePaymentCardDto updateDto = new CreatePaymentCardDto();
        updateDto.setNumber("5555666677778888");
        updateDto.setHolder("Updated Holder");
        updateDto.setExpirationDate("12/25");
        updateDto.setActive(true);

        given()
                .contentType(ContentType.JSON)
                .body(updateDto)
                .when()
                .put(BASE_PATH + "/{id}", cardId1)
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("message", containsString("already exists"));
    }

    @Test
    void updateCard_WithSameNumber_ShouldSucceed() {
        String cardNumber = "9999888877776666";
        Long cardId = createTestCard(testUserId, cardNumber, "Original Holder");

        CreatePaymentCardDto updateDto = new CreatePaymentCardDto();
        updateDto.setNumber(cardNumber);
        updateDto.setHolder("Updated Holder");
        updateDto.setExpirationDate("12/25");
        updateDto.setActive(true);

        given()
                .contentType(ContentType.JSON)
                .body(updateDto)
                .when()
                .put(BASE_PATH + "/{id}", cardId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("holder", is("Updated Holder"));
    }


    @Test
    void deleteCard_ShouldRemoveCard() {
        Long cardId = createTestCard(testUserId, "1234123412341234", "To Delete");

        getCardById(cardId);

        given()
                .when()
                .delete(BASE_PATH + "/{id}", cardId)
                .then()
                .statusCode(HttpStatus.OK.value());

    }

    @Test
    void deleteCard_ShouldReturnNotFound_WhenCardDoesNotExist() {
        given()
                .when()
                .delete(BASE_PATH + "/{id}", 99999L)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }


    @Test
    void existsByNumber_ShouldReturnTrue_WhenCardExists() {
        String cardNumber = "9876543210987654";
        createTestCard(testUserId, cardNumber, "Exists Test");

        given()
                .when()
                .get(BASE_PATH + "/exists/{number}", cardNumber)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(is("true"));
    }

    @Test
    void existsByNumber_ShouldReturnFalse_WhenCardDoesNotExist() {
        given()
                .when()
                .get(BASE_PATH + "/exists/{number}", "0000000000000000")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body(is("false"));
    }

    @Test
    void canAddCardToUser_ShouldReturnTrue_WhenUserHasLessThan5Cards() {
        Boolean canAdd = given()
                .when()
                .get(BASE_PATH + "/user/{userId}/can-add", testUserId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(Boolean.class);

        assertThat(canAdd).isTrue();
    }

    @Test
    void canAddCardToUser_ShouldReturnFalse_WhenUserHas5Cards() {
        for (int i = 0; i < 5; i++) {
            createTestCard(testUserId,
                    "111122223333" + String.format("%04d", i),
                    "Limit Test " + i);
        }

        Boolean canAdd = given()
                .when()
                .get(BASE_PATH + "/user/{userId}/can-add", testUserId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(Boolean.class);

        assertThat(canAdd).isFalse();

    }

    @Test
    void countCardsByUserId_ShouldReturnCorrectCount() {
        Integer initialCount = given()
                .when()
                .get(BASE_PATH + "/user/{userId}/count", testUserId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(Integer.class);

        assertThat(initialCount).isZero();

        for (int i = 0; i < 3; i++) {
            createTestCard(testUserId,
                    "444455556666" + String.format("%04d", i),
                    "Count Test " + i);
        }

        Integer finalCount = given()
                .when()
                .get(BASE_PATH + "/user/{userId}/count", testUserId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(Integer.class);

        assertThat(finalCount).isEqualTo(3);
    }

    @Test
    void testCache_FullLifecycle_Get_Update_Delete() {
        String uniqueNumber = "999988887777" + System.currentTimeMillis();
        CreatePaymentCardDto createDto = createPaymentCardDto(
                uniqueNumber, "Cache Test", validExpirationDate);

        Long cardId = given()
                .contentType(ContentType.JSON)
                .body(createDto)
                .when()
                .post(BASE_PATH + "/user/{userId}", testUserId)
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .as(GetPaymentCardDto.class)
                .getId();

        assertNull(getCardCacheValue(cardId));

        GetPaymentCardDto cardFromDb = given()
                .when()
                .get(BASE_PATH + "/{id}", cardId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(GetPaymentCardDto.class);

        assertThat(cardFromDb.getHolder()).isEqualTo("Cache Test");
        assertNotNull(getCardCacheValue(cardId));

        GetPaymentCardDto cardFromCache = given()
                .when()
                .get(BASE_PATH + "/{id}", cardId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(GetPaymentCardDto.class);

        assertThat(cardFromCache.getHolder()).isEqualTo("Cache Test");

        CreatePaymentCardDto updateDto = new CreatePaymentCardDto();
        updateDto.setNumber(uniqueNumber);
        updateDto.setHolder("Updated Cache Test");
        updateDto.setExpirationDate(validExpirationDate);
        updateDto.setActive(true);

        GetPaymentCardDto updatedCard = given()
                .contentType(ContentType.JSON)
                .body(updateDto)
                .when()
                .put(BASE_PATH + "/{id}", cardId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(GetPaymentCardDto.class);

        assertThat(updatedCard.getHolder()).isEqualTo("Updated Cache Test");
        assertNotNull(getCardCacheValue(cardId));

        GetPaymentCardDto cardAfterUpdate = given()
                .when()
                .get(BASE_PATH + "/{id}", cardId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(GetPaymentCardDto.class);

        assertThat(cardAfterUpdate.getHolder()).isEqualTo("Updated Cache Test");

        given()
                .when()
                .delete(BASE_PATH + "/{id}", cardId)
                .then()
                .statusCode(HttpStatus.OK.value());

        assertNull(getCardCacheValue(cardId));

    }


    private CreatePaymentCardDto createPaymentCardDto(String number, String holder, String expirationDate) {
        CreatePaymentCardDto dto = new CreatePaymentCardDto();
        dto.setNumber(number);
        dto.setHolder(holder);
        dto.setExpirationDate(expirationDate);
        dto.setActive(true);
        return dto;
    }

    private Long createTestCard(Long userId, String number, String holder) {
        CreatePaymentCardDto createDto = createPaymentCardDto(number, holder, validExpirationDate);

        return given()
                .contentType(ContentType.JSON)
                .body(createDto)
                .when()
                .post(BASE_PATH + "/user/{userId}", userId)
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .as(GetPaymentCardDto.class)
                .getId();
    }

    private Long createUserWithName(String name, String surname, String email) {
        CreateUserDto userDto = new CreateUserDto();
        userDto.setName(name);
        userDto.setSurname(surname);
        userDto.setEmail(email);
        userDto.setBirthDate(LocalDate.of(1990, 1, 1));
        userDto.setActive(true);

        return given()
                .contentType(ContentType.JSON)
                .body(userDto)
                .when()
                .post(USERS_PATH)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(GetUserDto.class)
                .getId();
    }

    private GetPaymentCardDto getCardById(Long id) {
        return given()
                .when()
                .get(BASE_PATH + "/{id}", id)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(GetPaymentCardDto.class);
    }

    private Object getCardCacheValue(Long id) {
        Cache cardsCache = cacheManager.getCache("cards");
        if (cardsCache == null) {
            return null;
        }
        Cache.ValueWrapper valueWrapper = cardsCache.get(id);
        return (valueWrapper != null) ? valueWrapper.get() : null;
    }
}