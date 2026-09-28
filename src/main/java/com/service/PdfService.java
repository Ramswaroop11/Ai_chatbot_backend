package com.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class PdfService {

    public static final int MAX_PAGES = 20;

    public static final long MAX_FILE_SIZE =
            10 * 1024 * 1024; // 10 MB


    public String extractText(MultipartFile file)
            throws IOException {

        // =====================================================
        // FILE SIZE CHECK
        // =====================================================

        if (file.getSize() > MAX_FILE_SIZE) {

            throw new IllegalArgumentException(
                    "PDF is too large. Maximum allowed size is 10 MB."
            );
        }


        try (PDDocument document =
                     Loader.loadPDF(file.getBytes())) {


            // =================================================
            // PAGE COUNT CHECK
            // =================================================

            int pageCount =
                    document.getNumberOfPages();

            if (pageCount > MAX_PAGES) {

                throw new IllegalArgumentException(
                        "PDF has "
                                + pageCount
                                + " pages. "
                                + "Maximum allowed is "
                                + MAX_PAGES
                                + " pages."
                );
            }


            // =================================================
            // EXTRACT TEXT
            // =================================================

            PDFTextStripper pdfTextStripper =
                    new PDFTextStripper();

            String text =
                    pdfTextStripper.getText(document);


            // =================================================
            // CHECK EXTRACTED TEXT
            // =================================================

            if (text == null ||
                    text.trim().length() < 50) {

                throw new IllegalArgumentException(
                        "This PDF does not contain enough "
                                + "readable text. Please upload "
                                + "a text-based PDF."
                );
            }


            return text;
        }
    }
}