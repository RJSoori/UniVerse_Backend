package com.example.backend_service.skills;

import com.example.backend_service.common.exception.BadRequestException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Pulls raw text out of an uploaded PDF resume for skill extraction. Java's equivalent of
 * the pdf-parse/PyPDF2 step in a Node/Python pipeline — Apache PDFBox does the same job here.
 *
 * Deliberately doesn't attempt OCR: a scanned/image-only PDF has no extractable text layer
 * and comes back blank, which the caller treats as a normal "couldn't read this file" error
 * rather than a crash.
 */
final class PdfTextExtractor {

    private PdfTextExtractor() {
    }

    static String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please choose a CV file to upload.");
        }
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            if (document.isEncrypted()) {
                throw new BadRequestException("That PDF is password-protected. Please upload an unprotected copy.");
            }
            return new PDFTextStripper().getText(document);
        } catch (IOException e) {
            throw new BadRequestException("Unable to read that file. Make sure it's a valid, non-corrupted PDF.");
        }
    }
}
