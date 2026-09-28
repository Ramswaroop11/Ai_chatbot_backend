package com.controller;

import com.dto.ChatRequest;
import com.service.PdfService;
import com.service.RagChatService;
import com.service.TextChunkService;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rag")
@CrossOrigin(
        origins = {
                "http://localhost:5173",
                "https://ramswaroop11-ai-chatbot.netlify.app"
        }
)
public class RagController {

    private final PdfService pdfService;
    private final TextChunkService textChunkService;
    private final VectorStore vectorStore;
    private final RagChatService ragChatService;

    public RagController(
            PdfService pdfService,
            TextChunkService textChunkService,
            VectorStore vectorStore,
            RagChatService ragChatService) {

        this.pdfService = pdfService;
        this.textChunkService = textChunkService;
        this.vectorStore = vectorStore;
        this.ragChatService = ragChatService;
    }

    // =========================================================
    // PDF UPLOAD
    // =========================================================

    @PostMapping("/upload")
    public ResponseEntity<String> uploadDocument(
            @RequestParam("file") MultipartFile file) {

        // =====================================================
        // 1. VALIDATE FILE
        // =====================================================

        if (file == null || file.isEmpty()) {

            return ResponseEntity.badRequest()
                    .body("Please select a PDF file.");
        }

        // =====================================================
        // 2. VALIDATE PDF
        // =====================================================

        if (!"application/pdf".equals(
                file.getContentType())) {

            return ResponseEntity.badRequest()
                    .body("Only PDF files are allowed.");
        }

        try {

            // =================================================
            // 3. GENERATE UNIQUE DOCUMENT ID
            // =================================================

            String documentId =
                    UUID.randomUUID().toString();

            String fileName =
                    file.getOriginalFilename();

            if (fileName == null ||
                    fileName.trim().isEmpty()) {

                fileName = "uploaded-document.pdf";
            }

            System.out.println(
                    "===================================="
            );

            System.out.println(
                    "NEW PDF UPLOAD"
            );

            System.out.println(
                    "File: " + fileName
            );

            System.out.println(
                    "Document ID: " + documentId
            );

            // =================================================
            // 4. EXTRACT PDF TEXT
            // =================================================

            String text =
                    pdfService.extractText(file);

            System.out.println(
                    "Extracted characters: "
                            + text.length()
            );

            // =================================================
            // 5. SPLIT INTO CHUNKS
            // =================================================

            List<Document> chunks =
                    textChunkService.splitText(
                            text,
                            documentId,
                            fileName
                    );

            System.out.println(
                    "Total chunks: "
                            + chunks.size()
            );

            // =================================================
            // 6. STORE IN PINECONE
            // =================================================

            vectorStore.add(chunks);

            System.out.println(
                    "Successfully stored "
                            + chunks.size()
                            + " chunks in Pinecone."
            );

            // =================================================
            // 7. ENABLE RAG FOR THIS PDF
            // =================================================

            ragChatService.enableRag(
                    documentId
            );

            System.out.println(
                    "RAG enabled for document: "
                            + documentId
            );

            System.out.println(
                    "===================================="
            );

            // =================================================
            // 8. RESPONSE
            // =================================================

            return ResponseEntity.ok(
                    "PDF processed successfully. "
                            + "Extracted characters: "
                            + text.length()
                            + ", Total chunks: "
                            + chunks.size()
                            + ", Stored in Pinecone successfully."
            );

        } catch (IOException e) {

            e.printStackTrace();

            return ResponseEntity
                    .internalServerError()
                    .body(
                            "Failed to read PDF."
                    );

        } catch (Exception e) {

            System.out.println(
                    "========== PINECONE ERROR =========="
            );

            e.printStackTrace();

            System.out.println(
                    "Message: "
                            + e.getMessage()
            );

            if (e.getCause() != null) {

                System.out.println(
                        "Cause: "
                                + e.getCause().getMessage()
                );

                e.getCause().printStackTrace();
            }

            System.out.println(
                    "===================================="
            );

            return ResponseEntity
                    .internalServerError()
                    .body(
                            "PDF processed, but failed to store "
                                    + "embeddings in Pinecone."
                    );
        }
    }

    // =========================================================
    // CHAT
    // =========================================================

    @PostMapping("/chat")
    public ResponseEntity<String> chat(
            @RequestBody ChatRequest request) {

        // =====================================================
        // 1. VALIDATE MESSAGE
        // =====================================================

        if (request == null ||
                request.getMessage() == null ||
                request.getMessage()
                        .trim()
                        .isEmpty()) {

            return ResponseEntity.badRequest()
                    .body(
                            "Message cannot be empty."
                    );
        }

        // =====================================================
        // 2. VALIDATE CONVERSATION ID
        // =====================================================

        if (request.getConversationId() == null ||
                request.getConversationId()
                        .trim()
                        .isEmpty()) {

            return ResponseEntity.badRequest()
                    .body(
                            "Conversation ID cannot be empty."
                    );
        }

        try {

            // =================================================
            // 3. GENERAL OR RAG QUESTION
            // =================================================

            String answer =
                    ragChatService.askQuestion(
                            request.getMessage(),
                            request.getConversationId()
                    );

            return ResponseEntity.ok(answer);

        } catch (Exception e) {

            System.out.println(
                    "========== CHAT ERROR =========="
            );

            e.printStackTrace();

            System.out.println(
                    "Message: "
                            + e.getMessage()
            );

            System.out.println(
                    "================================"
            );

            return ResponseEntity
                    .internalServerError()
                    .body(
                            "Failed to process your message."
                    );
        }
    }
}