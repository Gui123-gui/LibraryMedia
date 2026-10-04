package com.guilhermedev.librarymedia.user;

import com.guilhermedev.librarymedia.common.CurrentUser;
import com.guilhermedev.librarymedia.common.ApiException;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Current authenticated user")
public class UserController {
    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public UserResponse me() {
        UserAccount user = userRepository.findById(CurrentUser.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "User not found"));
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }

    public record UserResponse(Long id, String name, String email) {
    }
}
