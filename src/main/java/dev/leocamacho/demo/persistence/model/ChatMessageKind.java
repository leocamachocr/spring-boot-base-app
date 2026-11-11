package dev.leocamacho.demo.persistence.model;

/**
 * Kind of content stored in a chat message. Later stages add tool call and tool result kinds, stored as JSON in the
 * same content column.
 */
public enum ChatMessageKind {
    TEXT
}
