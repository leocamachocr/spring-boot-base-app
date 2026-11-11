CREATE TABLE chat_conversations (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_activity_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_chat_conversations_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

CREATE INDEX idx_chat_conversations_owner ON chat_conversations (owner_id);

CREATE TABLE chat_messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    role VARCHAR(16) NOT NULL,
    kind VARCHAR(32) NOT NULL,
    content CLOB NOT NULL,
    created_at TIMESTAMP(9) NOT NULL,
    CONSTRAINT fk_chat_messages_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversations (id)
);

CREATE INDEX idx_chat_messages_conversation_created ON chat_messages (conversation_id, created_at);
