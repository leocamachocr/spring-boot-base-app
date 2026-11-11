package dev.leocamacho.demo.persistence.model;

/**
 * Kind of content stored in a chat message. Text messages store plain text; tool call and tool result messages
 * store a JSON array in the same content column.
 */
public enum ChatMessageKind {
    TEXT,
    TOOL_CALL,
    TOOL_RESULT
}
