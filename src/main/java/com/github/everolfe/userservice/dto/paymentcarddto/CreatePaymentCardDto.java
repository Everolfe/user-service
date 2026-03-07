package com.github.everolfe.userservice.dto.paymentcarddto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreatePaymentCardDto {

    @NotBlank
    private String number;

    @NotBlank
    private String holder;

    @NotBlank
    @Pattern(regexp = "^(0[1-9]|1[0-2])/([0-9]{2})$", message = "Invalid format. Must be MM/YY")
    private String expirationDate;

    private Boolean active;

}
