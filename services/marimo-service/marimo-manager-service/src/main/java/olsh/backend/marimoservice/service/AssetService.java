package olsh.backend.marimoservice.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.marimoservice.config.MinioConfig;
import olsh.backend.marimoservice.entity.Component;
import olsh.backend.marimoservice.entity.ComponentAsset;
import olsh.backend.marimoservice.exception.OperationFailedException;
import olsh.backend.marimoservice.exception.ResourceNotFoundException;
import olsh.backend.marimoservice.repository.ComponentAssetRepository;
import olsh.backend.marimoservice.repository.ComponentRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AssetService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final ComponentRepository componentRepository;
    private final ComponentAssetRepository assetRepository;

    public void uploadInitialNotebook(Component component, String code) {
        log.debug("Uploading initial notebook for componentId: {}", component.getId());
        try {
            byte[] fileData = code.getBytes(StandardCharsets.UTF_8);
            String fileName = "notebook.py";
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(component.getNotebookPath().substring(1)) // Remove leading slash for MinIO
                            .stream(new ByteArrayInputStream(fileData), fileData.length, -1)
                            .contentType("text/x-python")
                            .build()
            );

            ComponentAsset asset = ComponentAsset.builder()
                    .id(UUID.randomUUID().toString())
                    .component(component)
                    .assetType(ComponentAsset.AssetType.NOTEBOOK)
                    .fileName(fileName)
                    .filePath(component.getNotebookPath().substring(1)) // Store path without leading slash
                    .mimeType("text/x-python")
                    .fileSize((long) fileData.length)
                    .build();

            assetRepository.save(asset);
            log.info("Initial notebook uploaded for componentId: {}, path: {}", component.getId(), component.getNotebookPath());
        } catch (Exception e) {
            log.error("Failed to upload initial notebook for componentId: {}, error: {}",
                    component.getId(), e.getMessage(), e);
            throw new OperationFailedException("Failed to upload initial notebook", e);
        }
    }

    public String uploadAsset(String componentId, ComponentAsset.AssetType assetType, 
                            String fileName, byte[] fileData, String mimeType, Map<String, String> metadata) {
        log.debug("Uploading asset: componentId={}, assetType={}, fileName={}", 
                componentId, assetType, fileName);
        
        // Validate component exists
        Component component = componentRepository.findById(componentId)
                .orElseThrow(() -> new ResourceNotFoundException("Component not found: " + componentId));

        try {
            // Generate unique asset ID and file path
            String assetId = UUID.randomUUID().toString();
            String filePath = String.format("marimo/components/%s/assets/%s/%s", 
                    componentId, assetType.name().toLowerCase(), fileName);

            // Upload to MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(filePath)
                            .stream(new ByteArrayInputStream(fileData), fileData.length, -1)
                            .contentType(mimeType)
                            .build()
            );

            // Save asset metadata
            ComponentAsset asset = ComponentAsset.builder()
                    .id(assetId)
                    .component(component)
                    .assetType(assetType)
                    .fileName(fileName)
                    .filePath(filePath)
                    .mimeType(mimeType)
                    .fileSize((long) fileData.length)
                    .metadata(metadata != null ? new HashMap<>(metadata) : null)
                    .build();

            assetRepository.save(asset);
            
            log.info("Asset uploaded: assetId={}, componentId={}, filePath={}", 
                    assetId, componentId, filePath);
            
            return assetId;
            
        } catch (Exception e) {
            log.error("Failed to upload asset: componentId={}, fileName={}, error={}", 
                    componentId, fileName, e.getMessage(), e);
            throw new OperationFailedException("Failed to upload asset", e);
        }
    }

    public byte[] getAssetData(String assetId) {
        log.debug("Getting asset data: assetId={}", assetId);
        
        ComponentAsset asset = getAsset(assetId);

        try {
            InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(asset.getFilePath().startsWith("/") ? asset.getFilePath().substring(1) : asset.getFilePath())
                            .build()
            );
            
            return inputStream.readAllBytes();
            
        } catch (Exception e) {
            log.error("Failed to get asset data: assetId={}, error={}", assetId, e.getMessage(), e);
            throw new OperationFailedException("Failed to get asset data", e);
        }
    }

    public ComponentAsset getAsset(String assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + assetId));
    }

    public List<ComponentAsset> getAssetsByComponent(String componentId) {
        return assetRepository.findByComponentId(componentId);
    }

    public List<ComponentAsset> getAssetsByComponentAndType(String componentId, ComponentAsset.AssetType assetType) {
        return assetRepository.findByComponentIdAndAssetType(componentId, assetType);
    }

    public boolean assetExists(String componentId, String filePath) {
        return assetRepository.existsByComponentIdAndFilePath(componentId, filePath);
    }

    public void deleteAsset(String assetId) {
        log.debug("Deleting asset: assetId={}", assetId);
        
        ComponentAsset asset = getAsset(assetId);
        
        try {
            // Delete from MinIO
            minioClient.removeObject(
                    io.minio.RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(asset.getFilePath().startsWith("/") ? asset.getFilePath().substring(1) : asset.getFilePath())
                            .build()
            );
            
            // Delete from database
            assetRepository.deleteById(assetId);
            
            log.info("Asset deleted: assetId={}, filePath={}", assetId, asset.getFilePath());
            
        } catch (Exception e) {
            log.error("Failed to delete asset: assetId={}, error={}", assetId, e.getMessage(), e);
            throw new OperationFailedException("Failed to delete asset", e);
        }
    }

    public void deleteAssetsByComponent(String componentId) {
        log.debug("Deleting all assets for component: componentId={}", componentId);
        
        List<ComponentAsset> assets = getAssetsByComponent(componentId);
        
        for (ComponentAsset asset : assets) {
            try {
                deleteAsset(asset.getId());
            } catch (Exception e) {
                log.warn("Failed to delete asset during component cleanup: assetId={}, error={}", 
                        asset.getId(), e.getMessage());
            }
        }
        
        log.info("Deleted {} assets for component: componentId={}", assets.size(), componentId);
    }

    public boolean verifyAssetExists(String assetId) {
        try {
            ComponentAsset asset = getAsset(assetId);
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(asset.getFilePath())
                            .build()
            );
            return true;
        } catch (Exception e) {
            log.debug("Asset verification failed: assetId={}, error={}", assetId, e.getMessage());
            return false;
        }
    }
} 