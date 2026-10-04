package com.guilhermedev.librarymedia.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
    private CurrentUser() {
    }

    public static Long id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                    "Authentication is required");
        }
        return userId;
    }
}
