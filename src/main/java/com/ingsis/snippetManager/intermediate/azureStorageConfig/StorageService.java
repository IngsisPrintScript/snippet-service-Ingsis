package com.ingsis.snippetManager.intermediate.azureStorageConfig;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public byte[] downloadFile(String containerName, String blobName) {
        try {
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient(blobName);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            blobClient.download(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error downloading blob", e);
        }
    }

    public byte[] download(String blobUrl) {
        BlobClient blobClient = new BlobClientBuilder()
                //Cg¿hange by connectionString("DefaultEndpointsProtocol=https;AccountName=tuCuenta;AccountKey=tuClave;EndpointSuffix=core.windows.net")
                .connectionString("UseDevelopmentStorage=true")
                .endpoint(blobUrl)
                .buildClient();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blobClient.download(outputStream);
        return outputStream.toByteArray();
    }
}
