package dev.leocamacho.demo.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
public class ChatMessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationEntity conversation;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRole role;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatMessageKind kind;
    @Lob
    @Column(nullable = false)
    private String content;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public ChatMessageEntity() {
    }

    public ChatMessageEntity(ConversationEntity conversation, ChatRole role, ChatMessageKind kind, String content,
                             Instant createdAt) {
        this.conversation = conversation;
        this.role = role;
        this.kind = kind;
        this.content = content;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public ConversationEntity getConversation() {
        return conversation;
    }

    public void setConversation(ConversationEntity conversation) {
        this.conversation = conversation;
    }

    public ChatRole getRole() {
        return role;
    }

    public ChatMessageKind getKind() {
        return kind;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
