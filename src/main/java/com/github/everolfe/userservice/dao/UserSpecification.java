package com.github.everolfe.userservice.dao;

import com.github.everolfe.userservice.entity.User;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.Optional;
import java.util.List;

public class UserSpecification {

    private UserSpecification() {}

    public static Specification<User> filterByNameAndSurname(String name, String surname) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = Stream.of(
                            createLikePredicate(criteriaBuilder, root.get("name"), name),
                            createLikePredicate(criteriaBuilder, root.get("surname"), surname)
                    ).filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Optional<Predicate> createLikePredicate(
            CriteriaBuilder criteriaBuilder,
            Path<String> field,
            String value) {

        return Optional.ofNullable(value)
                .filter(v -> !v.trim().isEmpty())
                .map(v -> criteriaBuilder.like(
                        criteriaBuilder.lower(field),
                        "%" + v.toLowerCase() + "%"
                ));
    }
}