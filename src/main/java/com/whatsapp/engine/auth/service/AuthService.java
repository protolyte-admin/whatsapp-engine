package com.whatsapp.engine.auth.service;

import com.whatsapp.engine.auth.Role;
import com.whatsapp.engine.auth.User;
import com.whatsapp.engine.auth.dto.AuthResponse;
import com.whatsapp.engine.auth.dto.LoginRequest;
import com.whatsapp.engine.auth.dto.RegisterRequest;
import com.whatsapp.engine.auth.repository.UserRepository;
import com.whatsapp.engine.common.exception.ApplicationException;
import com.whatsapp.engine.organization.Organization;
import com.whatsapp.engine.organization.repository.OrganizationRepository;
import com.whatsapp.engine.security.JwtService;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthService(
            OrganizationRepository organizationRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserRepository userRepository
    ) {
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        String slug = request.organizationSlug().trim().toLowerCase(Locale.ROOT);

        if (organizationRepository.existsBySlugIgnoreCase(slug)) {
            throw new ApplicationException(HttpStatus.CONFLICT, "ORGANIZATION_EXISTS", "Organization slug already exists");
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApplicationException(HttpStatus.CONFLICT, "USER_EXISTS", "Email already exists");
        }

        Organization organization = new Organization();
        organization.setName(request.organizationName().trim());
        organization.setSlug(slug);
        organization.setActive(true);
        Organization savedOrganization = organizationRepository.save(organization);

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.ORG_ADMIN);
        user.setActive(true);
        user.setOrganization(savedOrganization);
        User savedUser = userRepository.save(user);

        return buildAuthResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> invalidCredentialsException());

        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw invalidCredentialsException();
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        Organization organization = user.getOrganization();
        String token = jwtService.generateToken(user);

        return AuthResponse.bearer(
                token,
                jwtService.expirationMillis(),
                new AuthResponse.UserSummary(user.getId(), user.getFullName(), user.getEmail(), user.getRole()),
                new AuthResponse.OrganizationSummary(
                        organization.getId(),
                        organization.getName(),
                        organization.getSlug()
                )
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private ApplicationException invalidCredentialsException() {
        return new ApplicationException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
    }
}
