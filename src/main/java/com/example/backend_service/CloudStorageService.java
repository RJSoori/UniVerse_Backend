package com.example.backend_service;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;

@Service
public class CloudStorageService {

    private final BlobContainerClient containerClient;

    public CloudStorageService(
            @Value("${azure.storage.connection-string:}") String connectionString,
            @Value("${azure.storage.container:recruiter-documents}") String containerName) {

        if (!StringUtils.hasText(connectionString)) {
            this.containerClient = null;
            return;
        }

        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();

        this.containerClient = serviceClient.getBlobContainerClient(containerName);
        if (!this.containerClient.exists()) {
            this.containerClient.create();
        }
    }

    public String uploadDocument(MultipartFile file, Long recruiterId, String descriptor) {
        try {
            String originalFileName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "document";
            String blobName = String.format("recruiter-documents/%d/%s-%s-%s",
                    recruiterId,
                    descriptor,
                    UUID.randomUUID().toString(),
                    originalFileName.replaceAll("\\s+", "_"));

            if (containerClient == null) {
                throw new IllegalStateException("Azure Blob storage is not configured. Set AZURE_STORAGE_CONNECTION_STRING or azure.storage.connection-string.");
            }

            BlobClient blobClient = containerClient.getBlobClient(blobName);
            BlobHttpHeaders headers = new BlobHttpHeaders();
            if (StringUtils.hasText(file.getContentType())) {
                headers.setContentType(file.getContentType());
            }

            blobClient.upload(file.getInputStream(), file.getSize(), true);
            blobClient.setHttpHeaders(headers);

            return blobClient.getBlobUrl();
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload document to Azure Blob Storage", e);
        }
    }
}