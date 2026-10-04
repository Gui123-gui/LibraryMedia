package com.guilhermedev.librarymedia.auth;

import com.guilhermedev.librarymedia.common.ApiException;
import com.guilhermedev.librarymedia.common.JwtService;
import com.guilhermedev.librarymedia.list.MediaList;
import com.guilhermedev.librarymedia.list.MediaListRepository;
import com.guilhermedev.librarymedia.user.UserAccount;
import com.guilhermedev.librarymedia.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Account registration and login")
public class AuthController {
    private final UserRepository userRepository;
    private final MediaListRepository listRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, MediaListRepository listRepository,
                          PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.listRepository = listRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Password must be no longer than 72 UTF-8 bytes");
        }
        if (!request.password().equals(request.passwordConfirmation())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Password confirmation does not match");
        }
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "Email is already registered");
        }

        UserAccount user;
        try {
            user = userRepository.save(new UserAccount(request.name().trim(), normalizedEmail,
                    passwordEncoder.encode(request.password())));
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED",
                    "Email is already registered");
        }
        listRepository.save(new MediaList(user, "Favorites", true));
        return new RegisterResponse(user.getId(), user.getName(), user.getEmail());
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Password must be no longer than 72 UTF-8 bytes");
        }
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        UserAccount user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> invalidCredentials());
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return new LoginResponse(jwtService.createToken(user.getId()), "Bearer", jwtService.expiresInSeconds());
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is incorrect");
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(min = 8, max = 72) String passwordConfirmation) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank @Size(max = 72) String password) {
    }

    public record RegisterResponse(Long id, String name, String email) { }
    public record LoginResponse(String accessToken, String tokenType, long expiresIn) { }
}
