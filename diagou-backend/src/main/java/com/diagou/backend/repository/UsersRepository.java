package com.diagou.backend.repository;

import com.diagou.backend.model.UsersEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsersRepository extends JpaRepository<UsersEntity, UUID> {

    boolean existsByEmail(String email);

    Optional<UsersEntity> findByEmail(String email);
}