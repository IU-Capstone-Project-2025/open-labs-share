package olsh.backend.marimomanagerservice.repository;

import olsh.backend.marimomanagerservice.entity.ComponentAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComponentAssetRepository extends JpaRepository<ComponentAsset, String> {

    List<ComponentAsset> findByComponentId(String componentId);

    List<ComponentAsset> findByComponentIdAndAssetType(String componentId, ComponentAsset.AssetType assetType);

    Optional<ComponentAsset> findByComponentIdAndFilePath(String componentId, String filePath);

    boolean existsByComponentIdAndFilePath(String componentId, String filePath);

    long countByComponentId(String componentId);

    void deleteByComponentId(String componentId);
} 