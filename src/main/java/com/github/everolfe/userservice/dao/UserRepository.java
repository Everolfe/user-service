package com.github.everolfe.userservice.dao;

import com.github.everolfe.userservice.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long>,
        JpaSpecificationExecutor<User> {

    @Modifying
    @Query(value = "UPDATE users SET active = true WHERE id = :id", nativeQuery = true)
    int activateUserNative(@Param("id") Long id);

    @Modifying
    @Query(value = "UPDATE users SET active = false WHERE id = :id", nativeQuery = true)
    int deactivateUserNative(@Param("id") Long id);

    @Modifying
    @Query("UPDATE User u SET u.name = COALESCE(:#{#user.name}, u.name), " +
            "u.surname = COALESCE(:#{#user.surname}, u.surname), " +
            "u.birthDate = COALESCE(:#{#user.birthDate}, u.birthDate), " +
            "u.email = COALESCE(:#{#user.email}, u.email) " +
            "WHERE u.id = :#{#user.id}")
    int updateUserDynamic(@Param("user") User user);

    Page<User> findByNameContainingIgnoreCaseOrSurnameContainingIgnoreCase(
            String name, String surname, Pageable pageable);

    Page<User> findByNameIgnoreCaseAndSurnameIgnoreCase(
            String name, String surname, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(c) < 5 THEN true ELSE false END FROM PaymentCard c WHERE c.user.id = :userId")
    boolean canAddMoreCards(@Param("userId") Long userId);

    @Query("SELECT SIZE(u.paymentCards) FROM User u WHERE u.id = :id")
    int getCardCountByUserId(@Param("id") Long id);

    boolean existsByEmail(String email);
}
