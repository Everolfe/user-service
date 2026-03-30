package com.github.everolfe.userservice.dao;

import com.github.everolfe.userservice.entity.User;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long>,
        JpaSpecificationExecutor<User> {

    @Modifying
    @Query("UPDATE User u SET " +
            "u.name = COALESCE(:#{#user.name}, u.name), " +
            "u.surname = COALESCE(:#{#user.surname}, u.surname), " +
            "u.birthDate = COALESCE(:#{#user.birthDate}, u.birthDate), " +
            "u.email = COALESCE(:#{#user.email}, u.email), " +
            "u.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE u.id = :#{#user.id}")
    int updateUserDynamic(@Param("user") User user);

    Page<User> findByNameContainingIgnoreCaseOrSurnameContainingIgnoreCase(
            String name, String surname, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(c) < 5 THEN true ELSE false END FROM PaymentCard c WHERE c.user.id = :userId")
    boolean canAddMoreCards(@Param("userId") Long userId);

    @Query("SELECT SIZE(u.paymentCards) FROM User u WHERE u.id = :id")
    int getCardCountByUserId(@Param("id") Long id);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = {"paymentCards"})
    Page<User> findAll(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.paymentCards WHERE u.id = :userId")
    Optional<User> findByIdWithPessimisticLock(@Param("userId") Long userId);

    @Query("SELECT u.email FROM User u WHERE u.email IN :emails")
    List<String> findExistingEmails(@Param("emails") Set<String> emails);

    Optional<User> findBySub(UUID sub);

    boolean existsBySub(UUID sub);
}
