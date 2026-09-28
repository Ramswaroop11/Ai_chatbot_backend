package com.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", unique = true, nullable = false)
    private String conversationId;

    @Column(nullable = false)
    private String title;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(
            mappedBy = "conversation",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("createdAt ASC")
    private List<Message> messages = new ArrayList<>();


    public Conversation() {
    }


    public Conversation(String conversationId) {

        this.conversationId = conversationId;

        this.title = "New Chat";

        this.createdAt = LocalDateTime.now();

        this.updatedAt = LocalDateTime.now();
    }


    public Long getId() {
        return id;
    }


    public String getConversationId() {
        return conversationId;
    }


    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }


    public String getTitle() {
        return title;
    }


    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = LocalDateTime.now();
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }


    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }


    public List<Message> getMessages() {
        return messages;
    }


    public void addMessage(Message message) {

        messages.add(message);

        message.setConversation(this);

        updatedAt = LocalDateTime.now();
    }
}