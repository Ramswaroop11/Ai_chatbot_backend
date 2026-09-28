package com.controller;

import com.dto.ChatRequest;
import com.dto.ChatResponse;
import com.dto.ChatHistoryResponse;
import com.dto.ConversationResponse;
import com.entity.Conversation;
import com.repository.ConversationRepository;
import com.service.ChatService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(
	    origins = {
	        "http://localhost:5173",
	        "https://ramswaroop11-ai-chatbot.netlify.app"
	    }
	)
public class ChatController {

    private final ChatService chatService;

    private final ConversationRepository conversationRepository;


    public ChatController(
            ChatService chatService,
            ConversationRepository conversationRepository) {

        this.chatService = chatService;

        this.conversationRepository =
                conversationRepository;
    }

    @GetMapping("/test")
    String test() {
    	return "Backend is working";
    }
    // =========================================================
    // NEW CHAT
    // =========================================================

    @PostMapping("/new")
    public ResponseEntity<?> createNewChat() {

        String conversationId =
                UUID.randomUUID().toString();

        Conversation conversation =
                new Conversation(conversationId);

        conversationRepository.save(
                conversation
        );


        return ResponseEntity.ok(
                Map.of(
                        "conversationId",
                        conversationId,

                        "title",
                        conversation.getTitle()
                )
        );
    }


    // =========================================================
    // NORMAL CHAT
    // =========================================================

    @PostMapping
    public ChatResponse chat(
            @RequestBody ChatRequest request) {

        String response =
                chatService.generateResponse(
                        request.getMessage(),
                        request.getConversationId()
                );


        return new ChatResponse(response);
    }


    // =========================================================
    // CHAT HISTORY SIDEBAR
    // =========================================================

    @GetMapping("/history")
    public List<ChatHistoryResponse> getChatHistory() {

        return conversationRepository
                .findAll()
                .stream()

                .sorted(
                        (a, b) ->
                                b.getUpdatedAt()
                                        .compareTo(
                                                a.getUpdatedAt()
                                        )
                )

                .map(
                        conversation ->
                                new ChatHistoryResponse(
                                        conversation
                                                .getConversationId(),

                                        conversation
                                                .getTitle()
                                )
                )

                .collect(Collectors.toList());
    }


    // =========================================================
    // LOAD ONE CONVERSATION
    // =========================================================

    @GetMapping("/{conversationId}")
    public ResponseEntity<?> getConversation(
            @PathVariable String conversationId) {

        return conversationRepository
                .findByConversationId(
                        conversationId
                )

                .map(
                        conversation ->
                                ResponseEntity.ok(
                                        new ConversationResponse(
                                                conversation
                                        )
                                )
                )

                .orElseGet(
                        () ->
                                ResponseEntity
                                        .notFound()
                                        .build()
                );
    }


    // =========================================================
    // RENAME CHAT
    // =========================================================

    @PutMapping("/{conversationId}/rename")
    public ResponseEntity<?> renameChat(
            @PathVariable String conversationId,

            @RequestBody Map<String, String> request) {

        String title =
                request.get("title");


        if (title == null ||
                title.trim().isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Chat title cannot be empty."
                    );
        }


        return conversationRepository
                .findByConversationId(
                        conversationId
                )

                .map(
                        conversation -> {

                            conversation.setTitle(
                                    title.trim()
                            );

                            conversationRepository.save(
                                    conversation
                            );


                            return ResponseEntity.ok(
                                    Map.of(
                                            "message",
                                            "Chat renamed successfully",

                                            "title",
                                            conversation.getTitle()
                                    )
                            );
                        }
                )

                .orElseGet(
                        () ->
                                ResponseEntity
                                        .notFound()
                                        .build()
                );
    }


    // =========================================================
    // DELETE CHAT
    // =========================================================

    @DeleteMapping("/{conversationId}")
    public ResponseEntity<?> deleteChat(
            @PathVariable String conversationId) {

        return conversationRepository
                .findByConversationId(
                        conversationId
                )

                .map(
                        conversation -> {

                            conversationRepository.delete(
                                    conversation
                            );


                            return ResponseEntity.ok(
                                    Map.of(
                                            "message",
                                            "Chat deleted successfully"
                                    )
                            );
                        }
                )

                .orElseGet(
                        () ->
                                ResponseEntity
                                        .notFound()
                                        .build()
                );
    }
}