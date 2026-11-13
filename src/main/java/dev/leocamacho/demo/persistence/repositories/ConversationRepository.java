package dev.leocamacho.demo.persistence.repositories;

import dev.leocamacho.demo.persistence.model.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID> {

    Optional<ConversationEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
}
