package com.github.everolfe.userservice.dao;

import com.github.everolfe.userservice.entity.PaymentCard;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long>,
        JpaSpecificationExecutor<PaymentCard> {

    @Modifying
    @Query("UPDATE PaymentCard pc SET " +
            "pc.number = COALESCE(:#{#card.number}, pc.number), " +
            "pc.holder = COALESCE(:#{#card.holder}, pc.holder), " +
            "pc.expirationDate = COALESCE(:#{#card.expirationDate}, pc.expirationDate), " +
            "pc.updatedAt = CURRENT_TIMESTAMP, " +
            "pc.active = COALESCE(:#{#card.active}, pc.active) " +
            "WHERE pc.id = :#{#card.id}")
    int updateCardDynamic(@Param("card") PaymentCard card);

    @Query("SELECT pc FROM PaymentCard pc WHERE pc.user.id = :userId")
    List<PaymentCard> findAllCardsByUserId(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(pc) < 5 THEN true ELSE false END FROM PaymentCard pc WHERE pc.user.id = :userId")
    boolean canAddCardToUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(pc) FROM PaymentCard pc WHERE pc.user.id = :userId")
    int countCardsByUserId(@Param("userId") Long userId);

    boolean existsByNumber(String number);

    @Query("SELECT pc.number FROM PaymentCard pc WHERE pc.number IN :numbers")
    List<String> findExistingNumbers(@Param("numbers") Set<String> numbers);

    @Modifying
    @Query("UPDATE PaymentCard c SET c.active = false WHERE c.user.id = :userId AND c.active = true")
    int deactivateAllCardsByUserId(Long userId);

    @Query("SELECT c.id FROM PaymentCard c WHERE c.user.id = :userId")
    List<Long> findCardIdsByUserId(Long userId);
}
