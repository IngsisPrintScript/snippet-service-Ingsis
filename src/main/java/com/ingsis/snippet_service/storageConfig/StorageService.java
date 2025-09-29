package com.ingsis.snippet_service.storageConfig;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
public class StorageService {

    private final BlobServiceClient blobServiceClient;

    @Autowired
    public StorageService(BlobServiceClient blobServiceClient) {
        this.blobServiceClient = blobServiceClient;
    }

    public String upload(String containerName, String blobName, byte[] data) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        if (!containerClient.exists()) {
            containerClient.create();
        }

        BlobClient blobClient = containerClient.getBlobClient(blobName);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
            blobClient.upload(inputStream, data.length, true);
        } catch (IOException e) {
            throw new RuntimeException("Error uploading blob", e);
        }

        return blobClient.getBlobUrl();
    }
}