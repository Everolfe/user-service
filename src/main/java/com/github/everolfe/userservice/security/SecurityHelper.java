package com.github.everolfe.userservice.security;

import com.github.everolfe.userservice.dao.PaymentCardRepository;
import com.github.everolfe.userservice.dao.UserRepository;
import com.github.everolfe.userservice.entity.PaymentCard;
import com.github.everolfe.userservice.entity.User;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;


@Component("securityHelper")
@RequiredArgsConstructor
public class SecurityHelper {

    private final UserRepository userRepository;
    private final PaymentCardRepository paymentCardRepository;

    public boolean isOwner(Long userId) {
        UUID subFromToken = getSubFromToken();
        if (subFromToken == null) {
            return false;
        }
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return false;
        }
        return user.getSub() != null && user.getSub().equals(subFromToken);
    }

    public boolean isCardOwner(Long cardId) {
        UUID subFromToken = getSubFromToken();
        if (subFromToken == null) return false;

        PaymentCard card = paymentCardRepository.findById(cardId).orElse(null);
        if (card == null) return false;

        User owner = card.getUser();
        if (owner == null || owner.getSub() == null) return false;

        return owner.getSub().equals(subFromToken);
    }

    public boolean isEmailOwner(String email) {
        UUID subFromToken = getSubFromToken();
        if (subFromToken == null) return false;

        User owner = userRepository.findByEmail(email).orElse(null);
        if (owner == null) return false;
        return owner.getSub() != null && owner.getSub().equals(subFromToken);
    }

    private UUID getSubFromToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            String sub = token.getToken().getSubject();
            if (sub != null) {
                return UUID.fromString(sub);
            }
        }
        return null;
    }
}
