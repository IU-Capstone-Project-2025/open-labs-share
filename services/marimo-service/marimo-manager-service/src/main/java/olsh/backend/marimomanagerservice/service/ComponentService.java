package olsh.backend.marimomanagerservice.service;

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
import olsh.backend.marimomanagerservice.entity.Component;
import olsh.backend.marimomanagerservice.exception.OperationFailedException;
import olsh.backend.marimomanagerservice.exception.ResourceNotFoundException;
import olsh.backend.marimomanagerservice.grpc.client.ArticlesServiceClient;
import olsh.backend.marimomanagerservice.grpc.client.LabsServiceClient;
import olsh.backend.marimomanagerservice.grpc.client.UsersServiceClient;
import olsh.backend.marimomanagerservice.repository.ComponentRepository;

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

            if (componentRepository.existsByNameAndContentTypeAndContentId(name, contentType, contentId)) {
                throw new IllegalArgumentException("Component with name '" + name + "' already exists for this content");
            }

            String componentId = UUID.randomUUID().toString();
            String notebookPath = String.format("/components/%s/component.py", componentId);

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

            if (initialCode != null && !initialCode.isEmpty()) {
                log.info("Uploading initial notebook to MinIO: componentId={}", savedComponent.getId());
                assetService.uploadInitialNotebook(savedComponent, initialCode);
            }

            return savedComponent;
        } catch (IllegalArgumentException e) {
            throw e; 
        } catch (Exception e) {
            log.error("Failed to create component with name '{}'", name, e);
            throw new OperationFailedException("Failed to create component.", e);
        }
    }

    private void validateAssociatedEntities(String ownerId, String contentType, String contentId) {
        if (!usersServiceClient.userExists(Long.parseLong(ownerId))) {
            throw new IllegalArgumentException("Invalid ownerId: User does not exist.");
        }

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

        if (!componentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Component not found, cannot delete: " + id);
        }

        try {
            assetService.deleteAssetsByComponent(id);
            
            componentRepository.deleteById(id);
            
            log.info("Component hard-deleted: id={}", id);
        } catch (Exception e) {
            log.error("Failed to delete component with id '{}'", id, e);
            throw new OperationFailedException("Failed to delete component.", e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteComponentsForContent(String contentType, String contentId) {
        log.info("Deleting all components for contentType={}, contentId={}", contentType, contentId);
        
        List<Component> components = getComponentsByContent(contentType, contentId);
        log.info("Found {} components to delete for {}:{}", components.size(), contentType, contentId);
        
        for (Component component : components) {
            try {
                // Delete associated assets and MinIO objects
                assetService.deleteAssetsByComponent(component.getId());
                
                // Delete the component from database
                componentRepository.deleteById(component.getId());
                
                log.debug("Deleted component: id={}, name={}", component.getId(), component.getName());
            } catch (Exception e) {
                log.error("Failed to delete component with id '{}': {}", component.getId(), e.getMessage(), e);
                // Continue with other components instead of failing completely
            }
        }
        
        log.info("Completed deletion of {} components for {}:{}", components.size(), contentType, contentId);
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