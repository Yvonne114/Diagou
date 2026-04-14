package com.diagou.backend.controller;

import com.diagou.backend.dto.LoginRequest;
import com.diagou.backend.dto.RegisterRequest;
import com.diagou.backend.dto.UpdateProfileRequest;
import com.diagou.backend.dto.UserResponse;
import com.diagou.backend.exception.UnauthorizedException;
import com.diagou.backend.model.UsersEntity;
import com.diagou.backend.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final boolean cookieSecure;

    public AuthController(AuthService authService,
                          @Value("${app.cookie.secure:false}") boolean cookieSecure) {
        this.authService = authService;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest request) {
        UsersEntity user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        UsersEntity user = authService.login(request.getEmail(), request.getPassword());

        String accessToken = authService.generateAccessToken(user);
        String refreshToken = authService.generateRefreshToken(user);

        addTokenCookie(response, "access_token", accessToken, 30 * 60);
        addTokenCookie(response, "refresh_token", refreshToken, 7 * 24 * 60 * 60);

        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        clearTokenCookie(response, "access_token");
        clearTokenCookie(response, "refresh_token");
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request,
                                        HttpServletResponse response) {
        String refreshToken = extractCookie(request, "refresh_token");
        if (refreshToken == null) {
            throw new UnauthorizedException("Refresh Token 不存在");
        }

        String newAccessToken = authService.refreshAccessToken(refreshToken);
        addTokenCookie(response, "access_token", newAccessToken, 30 * 60);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        if (authentication == null) {
            throw new UnauthorizedException("未認證");
        }
        UUID userId = (UUID) authentication.getPrincipal();
        UsersEntity user = authService.getUserById(userId);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(Authentication authentication,
                                                       @RequestBody UpdateProfileRequest request) {
        if (authentication == null) {
            throw new UnauthorizedException("未認證");
        }
        UUID userId = (UUID) authentication.getPrincipal();
        UsersEntity user = authService.updateProfile(userId, request);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    private void addTokenCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private void clearTokenCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private String extractCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
