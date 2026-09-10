package com.securebank.bms.security;

import com.securebank.bms.entity.UserAccount;
import com.securebank.bms.repository.UserAccountRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserAccountRepository users;

    public DatabaseUserDetailsService(UserAccountRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount user = users.findByUsernameIgnoreCase(username)
                .or(() -> users.findByEmailIgnoreCase(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode()))
                .toList();
        boolean enabled = user.getStatus().name().equals("ACTIVE");
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .disabled(!enabled)
                .accountLocked(user.getLockedUntil() != null && user.getLockedUntil().isAfter(java.time.Instant.now()))
                .build();
    }
}
