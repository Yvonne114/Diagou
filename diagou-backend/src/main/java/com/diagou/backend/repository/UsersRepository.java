package com.diagou.backend.repository;

import com.diagou.backend.model.UsersEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<UsersEntity, UUID> {
    
    boolean existsByEmail(String email);
}