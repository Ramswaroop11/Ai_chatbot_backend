package com.dto;


import java.util.List;

public class ConversationHistoryResponse {

    private String conversationId;
    private List<MessageResponse> messages;

    public ConversationHistoryResponse() {
    }

    public ConversationHistoryResponse(
            String conversationId,
            List<MessageResponse> messages) {

        this.conversationId = conversationId;
        this.messages = messages;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public List<MessageResponse> getMessages() {
        return messages;
    }

    public void setMessages(List<MessageResponse> messages) {
        this.messages = messages;
    }
}