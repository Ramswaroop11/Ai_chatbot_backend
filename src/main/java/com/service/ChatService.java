package com.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dto.ConversationSummaryResponse;
import com.entity.Conversation;
import com.entity.Message;
import com.entity.Role;
import com.repository.ConversationRepository;
import com.dto.ConversationHistoryResponse;
import com.dto.MessageResponse;
import java.util.List;
import java.util.UUID;

@Service
public class ChatService {

    private final ChatClient chatClient;

    private final ConversationRepository conversationRepository;


    public ChatService(
            ChatClient.Builder chatClientBuilder,
            ConversationRepository conversationRepository) {

        this.chatClient = chatClientBuilder.build();
        this.conversationRepository = conversationRepository;
    }


    // =========================================================
    // CREATE NEW CHAT
    // =========================================================

    @Transactional
    public String createNewConversation() {

        String conversationId =
                UUID.randomUUID().toString();

        Conversation conversation =
                new Conversation(conversationId);

        conversationRepository.save(conversation);

        return conversationId;
    }

    @Transactional(readOnly = true)
    public ConversationHistoryResponse getConversationHistory(
            String conversationId) {

        Conversation conversation =
                conversationRepository
                        .findByConversationId(conversationId)
                        .orElse(null);

        if (conversation == null) {

            return new ConversationHistoryResponse(
                    conversationId,
                    List.of()
            );
        }

        List<MessageResponse> messages =
                conversation.getMessages()
                        .stream()
                        .map(message ->
                                new MessageResponse(
                                        message.getRole().name(),
                                        message.getContent()
                                )
                        )
                        .toList();

        return new ConversationHistoryResponse(
                conversationId,
                messages
        );
    }
    // =========================================================
    // GENERATE AI RESPONSE
    // =========================================================

    @Transactional
    public String generateResponse(
            String message,
            String conversationId) {

        // 1. Find conversation

        Conversation conversation =
                conversationRepository
                        .findByConversationId(conversationId)
                        .orElseGet(() -> {

                            Conversation newConversation =
                                    new Conversation(conversationId);

                            return conversationRepository
                                    .save(newConversation);
                        });


        // 2. Save user message

        Message userMessage =
                new Message(
                        Role.USER,
                        message
                );

        conversation.addMessage(userMessage);

        conversationRepository.save(conversation);


        // 3. Get conversation history

        List<Message> messages =
                conversation.getMessages();


        // 4. Build conversation context

        StringBuilder context =
                new StringBuilder();

        for (Message msg : messages) {

            context
                    .append(msg.getRole())
                    .append(": ")
                    .append(msg.getContent())
                    .append("\n");
        }


        // 5. Send history + current message to Gemini

        String prompt = """
                You are a helpful AI assistant.

                Here is the conversation history:

                %s

                Continue the conversation naturally.

                Current user message:
                %s
                """.formatted(
                        context,
                        message
                );


        String response =
                chatClient
                        .prompt()
                        .user(prompt)
                        .call()
                        .content();


        // 6. Save AI response

        Message assistantMessage =
                new Message(
                        Role.ASSISTANT,
                        response
                );

        conversation.addMessage(assistantMessage);

        conversationRepository.save(conversation);

        // 7. Return response

        return response;
    }
    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> getAllConversations() {

        return conversationRepository.findAll()
                .stream()
                .sorted((a, b) -> {

                    if (a.getUpdatedAt() == null) {
                        return 1;
                    }

                    if (b.getUpdatedAt() == null) {
                        return -1;
                    }

                    return b.getUpdatedAt()
                            .compareTo(a.getUpdatedAt());
                })
                .map(conversation -> {

                    String title = "New Chat";

                    if (!conversation.getMessages().isEmpty()) {

                        Message firstMessage =
                                conversation.getMessages().get(0);

                        title = firstMessage
                                .getContent();

                        // Keep sidebar title short
                        if (title.length() > 35) {
                            title = title.substring(0, 35) + "...";
                        }
                    }

                    return new ConversationSummaryResponse(
                            conversation.getConversationId(),
                            conversation.getCreatedAt(),
                            conversation.getUpdatedAt(),
                            title
                    );
                })
                .toList();
    }
    
}