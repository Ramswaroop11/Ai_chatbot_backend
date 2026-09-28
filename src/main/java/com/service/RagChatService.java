package com.service;

import com.entity.Conversation;
import com.entity.Message;
import com.entity.Role;
import com.repository.ConversationRepository;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RagChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ConversationRepository conversationRepository;

    /*
     * Stores the document currently being used for RAG.
     *
     * Example:
     * PDF A -> documentId = abc123
     * PDF B -> documentId = xyz789
     *
     * After PDF B is uploaded, only xyz789 is searched.
     */
    private String currentDocumentId;

    public RagChatService(
            ChatClient.Builder chatClientBuilder,
            VectorStore vectorStore,
            ConversationRepository conversationRepository) {

        this.chatClient = chatClientBuilder.build();

        this.vectorStore = vectorStore;

        this.conversationRepository =
                conversationRepository;
    }

    // =========================================================
    // ENABLE RAG AFTER SUCCESSFUL PDF UPLOAD
    // =========================================================

    public void enableRag(String documentId) {

        this.currentDocumentId = documentId;

        System.out.println(
                "RAG ENABLED FOR DOCUMENT: "
                        + documentId
        );
    }

    // =========================================================
    // CHECK WHETHER RAG IS ENABLED
    // =========================================================

    public boolean isRagEnabled() {

        return currentDocumentId != null
                && !currentDocumentId.trim().isEmpty();
    }

    // =========================================================
    // MAIN CHAT METHOD
    // =========================================================

    @Transactional
    public String askQuestion(
            String message,
            String conversationId) {

        // =====================================================
        // 1. FIND / CREATE CONVERSATION
        // =====================================================

        Conversation conversation =
                conversationRepository
                        .findByConversationId(conversationId)
                        .orElseGet(() ->
                                conversationRepository.save(
                                        new Conversation(
                                                conversationId
                                        )
                                )
                        );

        // =====================================================
        // 2. SAVE USER MESSAGE
        // =====================================================

        Message userMessage =
                new Message(
                        Role.USER,
                        message
                );

        conversation.addMessage(userMessage);

        conversationRepository.save(conversation);

        // =====================================================
        // 3. DETERMINE QUESTION TYPE
        // =====================================================

        boolean documentQuestion =
                isDocumentRelated(message);

        String response;

        // =====================================================
        // 4. NORMAL QUESTION → GEMINI DIRECTLY
        // =====================================================

        if (!documentQuestion) {

            System.out.println(
                    "QUESTION TYPE: GENERAL"
            );

            response =
                    chatClient
                            .prompt()
                            .user(message)
                            .call()
                            .content();

        }

        // =====================================================
        // 5. PDF QUESTION → PINECONE → GEMINI
        // =====================================================

        else {

            System.out.println(
                    "QUESTION TYPE: DOCUMENT"
            );

            response =
                    answerFromDocument(message);
        }

        // =====================================================
        // 6. SAVE AI RESPONSE
        // =====================================================

        Message assistantMessage =
                new Message(
                        Role.ASSISTANT,
                        response
                );

        conversation.addMessage(assistantMessage);

        conversationRepository.save(conversation);

        return response;
    }

    // =========================================================
    // DETERMINE WHETHER QUESTION IS DOCUMENT RELATED
    // =========================================================

    private boolean isDocumentRelated(String message) {

        /*
         * If no PDF has been uploaded,
         * there is no reason to search the document.
         */
        if (!isRagEnabled()) {

            return false;
        }

        String result =
                chatClient
                        .prompt()
                        .user("""
                                Determine whether the user's question
                                requires information from the uploaded
                                PDF document.

                                Return ONLY one word:

                                DOCUMENT

                                or

                                GENERAL

                                DOCUMENT means the user is asking about
                                the uploaded PDF, its contents, sections,
                                facts, rules, policies, people, dates,
                                topics, or anything that should be
                                answered using the PDF.

                                GENERAL means the user is asking a normal
                                question that does not require information
                                from the uploaded PDF.

                                User question:
                                %s
                                """.formatted(message))
                        .call()
                        .content();

        return result != null
                && result.trim()
                        .toUpperCase()
                        .contains("DOCUMENT");
    }

    // =========================================================
    // ANSWER USING CURRENT PDF
    // =========================================================

    private String answerFromDocument(String message) {

        // =====================================================
        // 1. CHECK CURRENT DOCUMENT
        // =====================================================

        if (!isRagEnabled()) {

            return chatClient
                    .prompt()
                    .user(message)
                    .call()
                    .content();
        }

        System.out.println(
                "Searching Pinecone for document: "
                        + currentDocumentId
        );

        // =====================================================
        // 2. SEARCH ONLY CURRENT PDF
        // =====================================================

        List<Document> documents =
                vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(message)
                                .topK(5)
                                .filterExpression(
                                        "documentId == '"
                                                + currentDocumentId
                                                + "'"
                                )
                                .build()
                );

        // =====================================================
        // 3. NO RELEVANT CONTENT
        // =====================================================

        if (documents == null ||
                documents.isEmpty()) {

            return "I could not find this information in the uploaded document.";
        }

        System.out.println(
                "Retrieved chunks: "
                        + documents.size()
        );

        // =====================================================
        // 4. COMBINE RETRIEVED CHUNKS
        // =====================================================

        StringBuilder context =
                new StringBuilder();

        for (Document document : documents) {

            context
                    .append(document.getText())
                    .append("\n\n---\n\n");
        }

        // =====================================================
        // 5. RAG PROMPT
        // =====================================================

        String prompt = """
                You are a document-based AI assistant.

                Answer the user's question ONLY using the
                document context provided below.

                Do not use outside knowledge.

                If the answer is not available in the
                document context, say exactly:

                "I could not find this information in the uploaded document."

                Do not make up information.

                DOCUMENT CONTEXT:
                %s

                USER QUESTION:
                %s
                """.formatted(
                        context,
                        message
                );

        // =====================================================
        // 6. GEMINI
        // =====================================================

        return chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();
    }
}