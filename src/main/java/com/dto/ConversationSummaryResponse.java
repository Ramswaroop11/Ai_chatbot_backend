package com.dto;


import java.time.LocalDateTime;

public class ConversationSummaryResponse {

    private String conversationId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String title;

    public ConversationSummaryResponse() {
    }

    public ConversationSummaryResponse(
            String conversationId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String title) {

        this.conversationId = conversationId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.title = title;
    }

    public String getConversationId() {
        return conversationId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getTitle() {
        return title;
    }
}
