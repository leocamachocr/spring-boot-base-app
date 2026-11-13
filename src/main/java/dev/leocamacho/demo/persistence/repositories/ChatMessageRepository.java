package dev.leocamacho.demo.persistence.repositories;

import dev.leocamacho.demo.persistence.model.ChatMessageEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, UUID> {

    List<ChatMessageEntity> findByConversationIdOrderByCreatedAtDescIdDesc(UUID conversationId, Limit limit);
}
