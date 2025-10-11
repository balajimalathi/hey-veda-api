package com.skndan.veda.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Service for extracting text content from various document formats
 */
@ApplicationScoped
public class DocumentTextExtractor {

    /**
     * Extract text from a file based on its type
     * 
     * @param file     The file to extract text from
     * @param fileType The file extension/type (pdf, docx, txt, etc.)
     * @return Extracted text content
     * @throws IOException if extraction fails
     */
    public String extractText(File file, String fileType) throws IOException {
        return switch (fileType.toLowerCase()) {
            case "pdf" -> extractFromPdf(file);
            case "docx", "doc" -> extractFromDocx(file);
            case "txt" -> extractFromText(file);
            default -> throw new UnsupportedOperationException(
                    "Unsupported file type: " + fileType + ". Supported types: pdf, docx, txt");
        };
    }

    /**
     * Extract text from PDF file
     */
    private String extractFromPdf(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * Extract text from DOCX file
     */
    private String extractFromDocx(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    /**
     * Extract text from plain text file
     */
    private String extractFromText(File file) throws IOException {
        return Files.readString(file.toPath());
    }

    /**
     * Check if file type is supported for text extraction
     */
    public boolean isSupported(String fileType) {
        return fileType != null && 
               (fileType.equalsIgnoreCase("pdf") || 
                fileType.equalsIgnoreCase("docx") || 
                fileType.equalsIgnoreCase("doc") || 
                fileType.equalsIgnoreCase("txt"));
    }
}