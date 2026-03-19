package com.github.everolfe.userservice.mapper.paymentcardmapper;

import java.time.LocalDate;
import org.mapstruct.Named;

public interface ExpirationDateMapper {

    @Named("stringToLocalDate")
    default LocalDate stringToLocalDate(String expirationDate) {
        if (expirationDate == null || expirationDate.isBlank()) {
            return null;
        }
        try {
            String[] parts = expirationDate.split("/");
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt("20" + parts[1]);
            return LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid expiration date format: " + expirationDate);
        }
    }

    @Named("localDateToString")
    default String localDateToString(LocalDate date) {
        if (date == null) {
            return null;
        }
        return String.format("%02d/%02d", date.getMonthValue(), date.getYear() % 100);
    }
}