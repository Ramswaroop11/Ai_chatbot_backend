package com.dto;

public class ChatHistoryResponse {

    private String conversationId;

    private String title;


    public ChatHistoryResponse() {
    }


    public ChatHistoryResponse(
            String conversationId,
            String title) {

        this.conversationId = conversationId;

        this.title = title;
    }


    public String getConversationId() {
        return conversationId;
    }


    public String getTitle() {
        return title;
    }
}
