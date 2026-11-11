package dev.leocamacho.demo.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_conversations")
public class ConversationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;

    public ConversationEntity() {
    }

    public ConversationEntity(UUID ownerId, Instant createdAt) {
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.lastActivityAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(Instant lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }
}
