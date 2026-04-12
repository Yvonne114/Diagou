package com.diagou.backend.controller;

import com.diagou.backend.model.UsersEntity;
import com.diagou.backend.model.enums.UserRole;
import com.diagou.backend.model.enums.UserStatus;
import com.diagou.backend.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UsersController {

    @Autowired
    private UsersRepository usersRepository;

    @GetMapping("/test")
    public UsersEntity createTestUser() {
        UsersEntity user = new UsersEntity();
        user.setEmail("test_" + System.currentTimeMillis() + "@diagou.com");
        user.setPasswordHash("encrypted_password_here");
        user.setFullName("Test User");
        user.setRole(UserRole.BUYER);
        user.setStatus(UserStatus.ACTIVE);
        
        // 儲存並回傳
        return usersRepository.save(user);
    }
}