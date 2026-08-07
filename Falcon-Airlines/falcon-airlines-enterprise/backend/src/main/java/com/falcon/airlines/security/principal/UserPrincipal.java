package com.falcon.airlines.security.principal;

import com.falcon.airlines.entity.User;
import com.falcon.airlines.enums.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security {@link UserDetails} wrapper around the application {@link User} entity.
 * <p>
 * Provides the user's identity, credentials, and a resolved authority set made up of
 * roles (prefixed with {@code ROLE_}) and fine-grained permissions.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String email;
    private final String password;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final Collection<? extends GrantedAuthority> authorities;

    private final User user;

    public UserPrincipal(User user, Collection<? extends GrantedAuthority> authorities) {
        this.user = user;
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.authorities = Collections.unmodifiableCollection(authorities);
        this.accountNonLocked = isNotLocked(user);
        this.enabled = isNotLocked(user) && user.getStatus() == UserStatus.ACTIVE;
    }

    private static boolean isNotLocked(User user) {
        if (user.getStatus() == UserStatus.LOCKED) {
            return false;
        }
        Instant lockedUntil = user.getLockedUntil();
        return lockedUntil == null || !Instant.now().isBefore(lockedUntil);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
