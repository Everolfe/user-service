package com.github.everolfe.userservice.dto.paymentcarddto;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GetPaymentCardDto implements
        Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String number;

    private String holder;

    private String expirationDate;

    private Boolean active;

    private Long userId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
