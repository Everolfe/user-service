package com.github.everolfe.userservice.dao;

import com.github.everolfe.userservice.entity.User;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class UserSpecification {

    private UserSpecification() {}

    public static Specification<User> filterByNameAndSurname(String name, String surname) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (name != null && !name.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + name.toLowerCase() + "%"
                ));
            }

            if (surname != null && !surname.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("surname")),
                        "%" + surname.toLowerCase() + "%"
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<User> hasExactName(String name) {
        return (root, query, criteriaBuilder) ->
                name == null ? criteriaBuilder.conjunction() :
                        criteriaBuilder.equal(criteriaBuilder
                                .lower(root.get("name")), name.toLowerCase());
    }

    public static Specification<User> hasExactSurname(String surname) {
        return (root, query, criteriaBuilder) ->
                surname == null ? criteriaBuilder.conjunction() :
                        criteriaBuilder.equal(criteriaBuilder
                                .lower(root.get("surname")), surname.toLowerCase());
    }

    public static Specification<User> nameStartsWith(String prefix) {
        return (root, query, criteriaBuilder) ->
                prefix == null ? criteriaBuilder.conjunction() :
                        criteriaBuilder.like(criteriaBuilder
                                .lower(root.get("name")), prefix.toLowerCase() + "%");
    }

    public static Specification<User> surnameStartsWith(String prefix) {
        return (root, query, criteriaBuilder) ->
                prefix == null ? criteriaBuilder.conjunction() :
                        criteriaBuilder.like(criteriaBuilder
                                .lower(root.get("surname")), prefix.toLowerCase() + "%");
    }
}