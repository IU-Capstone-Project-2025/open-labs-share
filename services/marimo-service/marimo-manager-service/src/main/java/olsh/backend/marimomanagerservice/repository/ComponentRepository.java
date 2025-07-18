package olsh.backend.marimomanagerservice.repository;

import olsh.backend.marimomanagerservice.entity.Component;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComponentRepository extends JpaRepository<Component, String> {

    Optional<Component> findById(String id);

    List<Component> findByContentTypeAndContentId(String contentType, String contentId);

    List<Component> findByOwnerId(String ownerId);

    @Query("SELECT c FROM Component c WHERE " +
           "(:contentType IS NULL OR c.contentType = :contentType) AND " +
           "(:contentId IS NULL OR c.contentId = :contentId) AND " +
           "(:ownerId IS NULL OR c.ownerId = :ownerId)")
    Page<Component> findByFilters(@Param("contentType") String contentType,
                                  @Param("contentId") String contentId,
                                  @Param("ownerId") String ownerId,
                                  Pageable pageable);

    long countByContentTypeAndContentId(String contentType, String contentId);

    long countByOwnerId(String ownerId);

    boolean existsByNameAndContentTypeAndContentId(
            String name, String contentType, String contentId);

    Page<Component> findByOwnerId(String ownerId, Pageable pageable);
    Page<Component> findByContentTypeAndContentId(String contentType, String contentId, Pageable pageable);
    Page<Component> findByNameContainingIgnoreCase(String name, Pageable pageable);
} 