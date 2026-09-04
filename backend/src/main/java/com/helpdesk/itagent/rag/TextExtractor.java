package com.helpdesk.itagent.rag;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@SuppressWarnings("null")
public class TextExtractor {

    public String extract(Path file, String originalName) {
        String name = originalName.toLowerCase(Locale.ROOT);
        try {
            if (name.endsWith(".pdf")) {
                return extractPdf(file);
            }
            if (name.endsWith(".docx")) {
                return extractDocx(file);
            }
            if (name.endsWith(".txt") || name.endsWith(".md")) {
                return Files.readString(file, StandardCharsets.UTF_8);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported file type. Upload PDF, DOCX, TXT, or MD.");
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read document: " + ex.getMessage());
        }
    }

    private String extractPdf(Path file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractDocx(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file); XWPFDocument doc = new XWPFDocument(in)) {
            return doc.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));
        }
    }
}
