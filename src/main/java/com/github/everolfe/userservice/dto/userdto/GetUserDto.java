package com.github.everolfe.userservice.dto.userdto;

import com.github.everolfe.userservice.dto.paymentcarddto.GetPaymentCardDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GetUserDto {

    private Long id;

    private String name;

    private String surname;

    private LocalDate birthDate;

    private String email;

    private Boolean active;

    private List<GetPaymentCardDto> paymentCards = new ArrayList<>();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
