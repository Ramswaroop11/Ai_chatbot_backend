package com.dto;

import com.entity.Conversation;
import com.entity.Message;

import java.util.List;
import java.util.stream.Collectors;

public class ConversationResponse {

    private String conversationId;

    private String title;

    private List<MessageResponse> messages;


    public ConversationResponse() {
    }


    public ConversationResponse(
            Conversation conversation) {

        this.conversationId =
                conversation.getConversationId();

        this.title =
                conversation.getTitle();

        this.messages =
                conversation.getMessages()
                        .stream()
                        .map(MessageResponse::new)
                        .collect(Collectors.toList());
    }


    public String getConversationId() {

        return conversationId;
    }


    public String getTitle() {

        return title;
    }


    public List<MessageResponse> getMessages() {

        return messages;
    }


    public static class MessageResponse {

        private String role;

        private String content;


        public MessageResponse(
                Message message) {

            this.role =
                    message.getRole().name();

            this.content =
                    message.getContent();
        }


        public String getRole() {

            return role;
        }


        public String getContent() {

            return content;
        }
    }
}