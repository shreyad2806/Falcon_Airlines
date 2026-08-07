package com.falcon.airlines.security.service;

import com.falcon.airlines.entity.*;
import com.falcon.airlines.enums.UserStatus;
import com.falcon.airlines.repository.RolePermissionRepository;
import com.falcon.airlines.repository.UserRepository;
import com.falcon.airlines.repository.UserRoleRepository;
import com.falcon.airlines.security.principal.UserPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Loads a {@link UserDetails} instance from the database by username or e-mail.
 * <p>
 * Resolves the active roles and permissions for the user and returns a
 * {@link UserPrincipal} with the appropriate {@link GrantedAuthority} set.
 */
@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public CustomUserDetailsService(UserRepository userRepository,
                                    UserRoleRepository userRoleRepository,
                                    RolePermissionRepository rolePermissionRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(login)
                .or(() -> userRepository.findByEmail(login))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + login));
        return new UserPrincipal(user, resolveAuthorities(user));
    }

    private Collection<? extends GrantedAuthority> resolveAuthorities(User user) {
        if (!isActive(user)) {
            return Collections.emptyList();
        }

        Instant now = Instant.now();
        List<Role> activeRoles = userRoleRepository.findByUser(user).stream()
                .filter(ur -> !ur.getValidFrom().isAfter(now))
                .filter(ur -> ur.getValidUntil() == null || !ur.getValidUntil().isBefore(now))
                .map(UserRole::getRole)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Set<GrantedAuthority> authorities = new HashSet<>();
        for (Role role : activeRoles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName().toUpperCase()));
        }

        if (!activeRoles.isEmpty()) {
            List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleIn(activeRoles);
            for (RolePermission rolePermission : rolePermissions) {
                Permission permission = rolePermission.getPermission();
                if (permission != null && permission.getCode() != null) {
                    authorities.add(new SimpleGrantedAuthority(permission.getCode().toUpperCase()));
                }
            }
        }

        return authorities;
    }

    private boolean isActive(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            return false;
        }
        Instant lockedUntil = user.getLockedUntil();
        return lockedUntil == null || !Instant.now().isBefore(lockedUntil);
    }
}
