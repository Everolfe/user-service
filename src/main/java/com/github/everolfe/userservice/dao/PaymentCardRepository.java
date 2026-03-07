package com.github.everolfe.userservice.dao;

import com.github.everolfe.userservice.entity.PaymentCard;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long>,
        JpaSpecificationExecutor<PaymentCard> {

    @Modifying
    @Query("UPDATE PaymentCard pc SET pc.active = true WHERE pc.id = :id")
    int activateCard(@Param("id") Long id);

    @Modifying
    @Query("UPDATE PaymentCard pc SET pc.active = false WHERE pc.id = :id")
    int deactivateCard(@Param("id") Long id);

    @Modifying
    @Query("UPDATE PaymentCard pc SET " +
            "pc.number = COALESCE(:#{#card.number}, pc.number), " +
            "pc.holder = COALESCE(:#{#card.holder}, pc.holder), " +
            "pc.expirationDate = COALESCE(:#{#card.expirationDate}, pc.expirationDate), " +
            "pc.active = COALESCE(:#{#card.active}, pc.active) " +
            "WHERE pc.id = :#{#card.id}")
    int updateCardDynamic(@Param("card") PaymentCard card);

    @Query("SELECT pc FROM PaymentCard pc WHERE pc.user.id = :userId")
    List<PaymentCard> findAllCardsByUserId(@Param("userId") Long userId);

    @Query("SELECT pc FROM PaymentCard pc WHERE pc.user.name LIKE %:name% AND pc.user.surname LIKE %:surname%")
    Page<PaymentCard> findCardsByUserNameAndSurname(@Param("name") String name,
                                                    @Param("surname") String surname,
                                                    Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(pc) < 5 THEN true ELSE false END FROM PaymentCard pc WHERE pc.user.id = :userId")
    boolean canAddCardToUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(pc) FROM PaymentCard pc WHERE pc.user.id = :userId")
    int countCardsByUserId(@Param("userId") Long userId);

    boolean existsByNumber(String number);
}
