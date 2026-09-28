package dev.leocamacho.demo.persistence.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import dev.leocamacho.demo.persistence.model.UserEntity;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String username);
}
