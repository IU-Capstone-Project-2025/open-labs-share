package olsh.backend.marimomanagerservice.service;

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
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.marimomanagerservice.config.MinioConfig;
import olsh.backend.marimomanagerservice.entity.Component;
import olsh.backend.marimomanagerservice.entity.ComponentAsset;
import olsh.backend.marimomanagerservice.exception.OperationFailedException;
import olsh.backend.marimomanagerservice.exception.ResourceNotFoundException;
import olsh.backend.marimomanagerservice.repository.ComponentAssetRepository;
import olsh.backend.marimomanagerservice.repository.ComponentRepository;

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
            String fileName = "component.py";
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
        
        Component component = componentRepository.findById(componentId)
                .orElseThrow(() -> new ResourceNotFoundException("Component not found: " + componentId));

        try {
            String assetId = UUID.randomUUID().toString();
            String filePath = String.format("components/%s/assets/%s/%s", 
                    componentId, assetType.name().toLowerCase(), fileName);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(filePath)
                            .stream(new ByteArrayInputStream(fileData), fileData.length, -1)
                            .contentType(mimeType)
                            .build()
            );

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
            minioClient.removeObject(
                    io.minio.RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(asset.getFilePath().startsWith("/") ? asset.getFilePath().substring(1) : asset.getFilePath())
                            .build()
            );
            
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
        
        // Also clean up component files from various locations for backward compatibility
        try {
            // Try to delete from new location: components/{componentId}/component.py
            String newComponentPath = String.format("components/%s/component.py", componentId);
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(newComponentPath)
                            .build()
            );
            log.debug("Deleted component from new location: {}", newComponentPath);
        } catch (Exception e) {
            log.debug("Component not found in new location or failed to delete: {}", e.getMessage());
        }
        
        try {
            // Try to delete from old location: components/{componentId}/notebook.py (for backward compatibility)
            String oldNotebookPath = String.format("components/%s/notebook.py", componentId);
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(oldNotebookPath)
                            .build()
            );
            log.debug("Deleted notebook from old location: {}", oldNotebookPath);
        } catch (Exception e) {
            log.debug("Notebook not found in old location or failed to delete: {}", e.getMessage());
        }
        
        try {
            // Try to delete from legacy location: notebooks/{componentId}/notebook.py (for backward compatibility)
            String legacyNotebookPath = String.format("notebooks/%s/notebook.py", componentId);
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(legacyNotebookPath)
                            .build()
            );
            log.debug("Deleted notebook from legacy location: {}", legacyNotebookPath);
        } catch (Exception e) {
            log.debug("Notebook not found in legacy location or failed to delete: {}", e.getMessage());
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