package com.securebank.bms.security;

import com.securebank.bms.entity.UserAccount;
import com.securebank.bms.exception.ResourceNotFoundException;
import com.securebank.bms.repository.UserAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserService {

    private final UserAccountRepository users;

    public CurrentUserService(UserAccountRepository users) {
        this.users = users;
    }

    public UserAccount requireUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResourceNotFoundException("Not authenticated");
        }
        return users.findByUsernameIgnoreCase(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
