package com.github.everolfe.userservice.dao;

import com.github.everolfe.userservice.entity.PaymentCard;
import com.github.everolfe.userservice.entity.User;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class PaymentCardSpecification {

    public static Specification<PaymentCard> byUserNameAndSurname(String name, String surname) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<PaymentCard, User> userJoin = root.join("user");

            if (name != null && !name.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(userJoin.get("name")),
                        "%" + name.toLowerCase() + "%"
                ));
            }
            if (surname != null && !surname.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(userJoin.get("surname")),
                        "%" + surname.toLowerCase() + "%"
                ));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}