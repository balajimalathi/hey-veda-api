package com.skndan.veda.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Service for extracting text content from various document formats
 */
@ApplicationScoped
public class DocumentTextExtractor {

    @Inject
    MinioService minioService;

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

    public List<PageContent> extractPerPage(File file, String fileName) throws IOException {
        List<PageContent> pages = new ArrayList<>();

        try (PDDocument document = PDDocument.load(file)) {
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                PDPage page = document.getPage(i);

                // 1️⃣ Extract text for this page
                org.apache.pdfbox.text.PDFTextStripper pageStripper = new org.apache.pdfbox.text.PDFTextStripper();
                pageStripper.setStartPage(i + 1);
                pageStripper.setEndPage(i + 1);
                String text = pageStripper.getText(document).trim();

                // 2️⃣ Extract images for this page
                PDResources resources = page.getResources();
                List<String> imageUrls = new ArrayList<>();
                for (COSName name : resources.getXObjectNames()) {
                    var xobject = resources.getXObject(name);
                    if (xobject instanceof PDImageXObject image) {
                        BufferedImage buffered = image.getImage();

                        // upload to S3
                        String imageFileName = "page_" + (i + 1) + "_" + name.getName() + ".png";
                        File temp = File.createTempFile("pdfimg_", ".png");
                        ImageIO.write(buffered, "png", temp);

                        String url = minioService.uploadBookFile("books", fileName + "/" + imageFileName,
                                temp.getAbsoluteFile().toPath());
                        imageUrls.add(url);
                        temp.delete();
                    }
                }

                System.out.println("Page number: " + i + " Imageurls: " + imageUrls);
                // 3️⃣ Build page content object
                pages.add(new PageContent(i + 1, text, imageUrls));
            }
        }
        return pages;
    }

    public record PageContent(int pageNumber, String text, List<String> imageUrls) {
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