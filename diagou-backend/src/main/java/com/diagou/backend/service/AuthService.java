package com.diagou.backend.service;

import com.diagou.backend.dto.RegisterRequest;
import com.diagou.backend.exception.BusinessException;
import com.diagou.backend.exception.UnauthorizedException;
import com.diagou.backend.model.UsersEntity;
import com.diagou.backend.model.enums.UserRole;
import com.diagou.backend.model.enums.UserStatus;
import com.diagou.backend.repository.UsersRepository;
import com.diagou.backend.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final VerificationService verificationService;

    public AuthService(UsersRepository usersRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       VerificationService verificationService) {
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.verificationService = verificationService;
    }

    public UsersEntity register(RegisterRequest request) {
        // 驗證 email 驗證碼
        verificationService.verify("email", request.getEmail(), request.getEmailCode());

        // 驗證 phone 驗證碼（有填 phone 才驗）
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            verificationService.verify("phone", request.getPhone(), request.getPhoneCode());
        }

        if (usersRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("DUPLICATE_EMAIL", "該 Email 已被註冊");
        }

        UsersEntity user = new UsersEntity();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setDisplayName(request.getDisplayName() != null ? request.getDisplayName() : request.getFullName());
        user.setRole(UserRole.BUYER);
        user.setStatus(UserStatus.ACTIVE);

        return usersRepository.save(user);
    }

    public UsersEntity login(String email, String password) {
        UsersEntity user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Email 或密碼錯誤"));

        if (user.getStatus() == UserStatus.DELETED) {
            throw new UnauthorizedException("此帳號已被停用");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("Email 或密碼錯誤");
        }

        user.setLastLoginAt(OffsetDateTime.now());
        return usersRepository.save(user);
    }

    public String generateAccessToken(UsersEntity user) {
        return jwtTokenProvider.generateAccessToken(user.getId(), user.getRole().name());
    }

    public String generateRefreshToken(UsersEntity user) {
        return jwtTokenProvider.generateRefreshToken(user.getId(), user.getRole().name());
    }

    public String refreshAccessToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("Refresh Token 無效或已過期");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        String role = jwtTokenProvider.getRoleFromToken(refreshToken);
        return jwtTokenProvider.generateAccessToken(userId, role);
    }

    public UsersEntity getUserById(UUID userId) {
        return usersRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("使用者不存在"));
    }

    public UsersEntity updateProfile(UUID userId, com.diagou.backend.dto.UpdateProfileRequest request) {
        UsersEntity user = getUserById(userId);

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            verificationService.verify("phone", request.getPhone(), request.getPhoneCode());
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank() && !request.getEmail().equals(user.getEmail())) {
            verificationService.verify("email", request.getEmail(), request.getEmailCode());
            if (usersRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException("DUPLICATE_EMAIL", "該 Email 已被使用");
            }
            user.setEmail(request.getEmail());
        }

        return usersRepository.save(user);
    }
}
