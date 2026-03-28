package com.noam.fleetcommand.auth;

import com.noam.fleetcommand.auth.dto.LoginRequestDto;
import com.noam.fleetcommand.auth.dto.LoginResponseDto;
import com.noam.fleetcommand.auth.dto.SignupRequestDto;
import com.noam.fleetcommand.auth.dto.UserProfileDto;
import com.noam.fleetcommand.common.errors.InvalidCredentialsException;
import com.noam.fleetcommand.security.CurrentUserService;
import com.noam.fleetcommand.security.JwtService;
import com.noam.fleetcommand.users.User;
import com.noam.fleetcommand.users.UserRepository;
import com.noam.fleetcommand.users.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUserService currentUserService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.currentUserService = currentUserService;
    }

    public LoginResponseDto login(LoginRequestDto request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        boolean passwordValid = user.getPasswordHash() != null &&
                (passwordEncoder.matches(request.getPassword(), user.getPasswordHash())
                        || request.getPassword().equals(user.getPasswordHash()));

        if (!passwordValid) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(
                user.getEmail(),
                Map.of("role", user.getRole().name(), "userId", user.getId())
        );

        return new LoginResponseDto(token, user.getRole().name(), user.getId());
    }

    public LoginResponseDto signup(SignupRequestDto request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.MANAGER);

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(
                saved.getEmail(),
                Map.of("role", saved.getRole().name(), "userId", saved.getId())
        );

        return new LoginResponseDto(token, saved.getRole().name(), saved.getId());
    }

    public UserProfileDto me() {
        User user = currentUserService.getRequiredUser();
        return new UserProfileDto(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }
}
