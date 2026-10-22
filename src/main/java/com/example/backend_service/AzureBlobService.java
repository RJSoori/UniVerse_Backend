package com.example.backend_service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.example.backend_service.common.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@Service
public class AzureBlobService {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10MB

    // Extension -> expected magic bytes (file signature) for the allowed upload types.
    private static final Map<String, byte[]> ALLOWED_SIGNATURES = Map.of(
            "pdf", new byte[]{0x25, 0x50, 0x44, 0x46}, // %PDF
            "png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
            "jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
            // webp checked separately below (RIFF....WEBP)
    );

    private final BlobServiceClient blobServiceClient;
    private final String containerName;

    public AzureBlobService(@Value("${AZURE_STORAGE_CONNECTION_STRING}") String connectionString,
                            @Value("${AZURE_STORAGE_CONTAINER}") String containerName) {
        this.blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
        this.containerName = containerName;
    }

    public String uploadFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("File exceeds the maximum allowed size of 10MB");
        }

        String extension = extractExtension(file.getOriginalFilename());
        byte[] header = readHeader(file, 12);
        if (!isAllowedFile(extension, header)) {
            throw new BadRequestException("Unsupported file type. Allowed types: PDF, PNG, JPEG, WEBP");
        }

        String fileName = UUID.randomUUID() + "." + extension;
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(fileName);

        blobClient.upload(file.getInputStream(), file.getSize(), true);

        return blobClient.getBlobUrl();
    }

    private boolean isAllowedFile(String extension, byte[] header) {
        if ("webp".equals(extension)) {
            return header.length >= 12
                    && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
        }
        byte[] signature = ALLOWED_SIGNATURES.get(extension);
        if (signature == null || header.length < signature.length) {
            return false;
        }
        return Arrays.equals(Arrays.copyOf(header, signature.length), signature);
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        // Strip any path components a malicious client might smuggle in.
        String sanitized = originalFilename.replace("\\", "/");
        sanitized = sanitized.substring(sanitized.lastIndexOf('/') + 1);
        int dotIndex = sanitized.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == sanitized.length() - 1) {
            return "";
        }
        return sanitized.substring(dotIndex + 1).toLowerCase();
    }

    private byte[] readHeader(MultipartFile file, int maxBytes) throws IOException {
        byte[] buffer = new byte[maxBytes];
        int read;
        try (var input = file.getInputStream()) {
            read = input.readNBytes(buffer, 0, maxBytes);
        }
        return read == maxBytes ? buffer : Arrays.copyOf(buffer, read);
    }
}
