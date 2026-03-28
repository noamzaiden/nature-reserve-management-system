package com.noam.fleetcommand.security;

import com.noam.fleetcommand.users.User;
import com.noam.fleetcommand.users.UserRepository;
import com.noam.fleetcommand.users.UserRole;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getRequiredUser() {
        UserPrincipal principal = getRequiredPrincipal();
        return userRepository.findById(principal.id())
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Authenticated user not found"));
    }

    public User getRequiredAdmin() {
        User user = getRequiredUser();
        if (user.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Administrator access is required");
        }
        return user;
    }

    public User getRequiredManager() {
        User user = getRequiredUser();
        if (user.getRole() != UserRole.MANAGER) {
            throw new AccessDeniedException("Manager access is required");
        }
        return user;
    }

    public UserPrincipal getRequiredPrincipal() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal;
        }
        throw new AuthenticationCredentialsNotFoundException("Authentication is required");
    }
}
