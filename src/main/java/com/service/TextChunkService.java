package com.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TextChunkService {

    public List<Document> splitText(
            String text,
            String documentId,
            String fileName) {

        Document document = new Document(text);

        // Metadata identifying which PDF these chunks belong to
        document.getMetadata().put("documentId", documentId);
        document.getMetadata().put("fileName", fileName);

        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(800)
                .withMinChunkSizeChars(350)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(10000)
                .withKeepSeparator(true)
                .build();

        List<Document> chunks =
                splitter.apply(List.of(document));

        // Make sure EVERY chunk contains the PDF metadata
        for (Document chunk : chunks) {

            chunk.getMetadata().put(
                    "documentId",
                    documentId
            );

            chunk.getMetadata().put(
                    "fileName",
                    fileName
            );
        }

        return chunks;
    }
}