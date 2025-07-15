package olsh.backend.marimoservice.service;

import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olsh.backend.marimoservice.entity.Component;
import olsh.backend.marimoservice.exception.OperationFailedException;
import olsh.backend.marimoservice.exception.ResourceNotFoundException;
import olsh.backend.marimoservice.grpc.client.ArticlesServiceClient;
import olsh.backend.marimoservice.grpc.client.LabsServiceClient;
import olsh.backend.marimoservice.grpc.client.UsersServiceClient;
import olsh.backend.marimoservice.repository.ComponentRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComponentService {

    private final ComponentRepository componentRepository;
    private final AssetService assetService;
    private final UsersServiceClient usersServiceClient;
    private final ArticlesServiceClient articlesServiceClient;
    private final LabsServiceClient labsServiceClient;

    @Cacheable(value = "components", key = "#id")
    public Component getComponent(String id) {
        log.debug("Getting component: id={}", id);
        return componentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Component not found with id: " + id));
    }

    @Transactional(rollbackFor = Exception.class)
    public Component createComponent(String name, String contentType, 
                                   String contentId, String ownerId, String initialCode) {
        log.debug("Creating component: name={}, contentType={}, contentId={}", name, contentType, contentId);
        
        try {
            validateAssociatedEntities(ownerId, contentType, contentId);

            // Check if component with same name already exists for this content
            if (componentRepository.existsByNameAndContentTypeAndContentId(name, contentType, contentId)) {
                throw new IllegalArgumentException("Component with name '" + name + "' already exists for this content");
            }

            // Generate unique component ID and auto-generate notebook path
            String componentId = UUID.randomUUID().toString();
            String notebookPath = String.format("/notebooks/%s/notebook.py", componentId);

            Component component = Component.builder()
                    .id(componentId)
                    .name(name)
                    .contentType(contentType)
                    .contentId(contentId)
                    .ownerId(ownerId)
                    .notebookPath(notebookPath)
                    .build();

            Component savedComponent = componentRepository.save(component);
            log.info("Component created in DB: id={}, name={}", savedComponent.getId(), savedComponent.getName());

            // Upload the initial notebook file to MinIO
            if (initialCode != null && !initialCode.isEmpty()) {
                log.info("Uploading initial notebook to MinIO: componentId={}", savedComponent.getId());
                assetService.uploadInitialNotebook(savedComponent, initialCode);
            }

            return savedComponent;
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors
        } catch (Exception e) {
            log.error("Failed to create component with name '{}'", name, e);
            throw new OperationFailedException("Failed to create component.", e);
        }
    }

    private void validateAssociatedEntities(String ownerId, String contentType, String contentId) {
        // Validate Owner ID
        if (!usersServiceClient.userExists(Long.parseLong(ownerId))) {
            throw new IllegalArgumentException("Invalid ownerId: User does not exist.");
        }

        // Validate Content ID based on Content Type
        long longContentId;
        try {
            longContentId = Long.parseLong(contentId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid contentId: Must be a numeric ID.");
        }

        switch (contentType.toLowerCase()) {
            case "article":
                if (!articlesServiceClient.articleExists(longContentId)) {
                    throw new IllegalArgumentException("Invalid contentId: Article does not exist.");
                }
                break;
            case "lab":
                if (!labsServiceClient.labExists(longContentId)) {
                    throw new IllegalArgumentException("Invalid contentId: Lab does not exist.");
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid contentType: Must be 'article' or 'lab'.");
        }
    }

    @CacheEvict(value = "components", key = "#id")
    @Transactional(rollbackFor = Exception.class)
    public Component updateComponent(String id, String name) {
        log.debug("Updating component: id={}", id);
        
        Component component = getComponent(id);
        if (name != null) component.setName(name);
        
        Component updated = componentRepository.save(component);
        log.info("Component updated: id={}, name={}", updated.getId(), updated.getName());
        return updated;
    }

    @CacheEvict(value = "components", key = "#id")
    @Transactional(rollbackFor = Exception.class)
    public void deleteComponent(String id) {
        log.debug("Deleting component: id={}", id);

        // Ensure the component exists before attempting to delete
        if (!componentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Component not found, cannot delete: " + id);
        }

        try {
            // First, delete all associated assets from MinIO
            assetService.deleteAssetsByComponent(id);
            
            componentRepository.deleteById(id);
            
            log.info("Component hard-deleted: id={}", id);
        } catch (Exception e) {
            log.error("Failed to delete component with id '{}'", id, e);
            throw new OperationFailedException("Failed to delete component.", e);
        }
    }

    public List<Component> getComponentsByContent(String contentType, String contentId) {
        log.debug("Getting components by content: contentType={}, contentId={}", contentType, contentId);
        return componentRepository.findByContentTypeAndContentId(contentType, contentId);
    }

    public List<Component> getComponentsByOwner(String ownerId) {
        log.debug("Getting components by owner: ownerId={}", ownerId);
        return componentRepository.findByOwnerId(ownerId);
    }

    public Page<Component> getComponents(String contentType, String contentId, String ownerId, Pageable pageable) {
        if (ownerId != null) {
            return componentRepository.findByOwnerId(ownerId, pageable);
        }
        if (contentType != null && contentId != null) {
            return componentRepository.findByContentTypeAndContentId(contentType, contentId, pageable);
        }
        return componentRepository.findAll(pageable);
    }

    public Page<Component> searchComponents(String query, Pageable pageable) {
        return componentRepository.findByNameContainingIgnoreCase(query, pageable);
    }

    public long getComponentCount(String contentType, String contentId) {
        return componentRepository.countByContentTypeAndContentId(contentType, contentId);
    }

    public long getComponentCountByOwner(String ownerId) {
        return componentRepository.countByOwnerId(ownerId);
    }
} 