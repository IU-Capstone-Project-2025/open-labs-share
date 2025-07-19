package olsh.backend.marimomanagerservice.repository;

import olsh.backend.marimomanagerservice.entity.WidgetState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WidgetStateRepository extends JpaRepository<WidgetState, String> {

    /**
     * Find all active widget states for a given session
     */
    @Query("SELECT ws FROM WidgetState ws WHERE ws.sessionId = :sessionId AND ws.active = true")
    List<WidgetState> findActiveBySessionId(@Param("sessionId") String sessionId);

    /**
     * Find a specific widget state by session and widget ID
     */
    @Query("SELECT ws FROM WidgetState ws WHERE ws.sessionId = :sessionId AND ws.widgetId = :widgetId AND ws.active = true")
    Optional<WidgetState> findBySessionIdAndWidgetId(@Param("sessionId") String sessionId, @Param("widgetId") String widgetId);

    /**
     * Find all widget states of a specific type for a session
     */
    @Query("SELECT ws FROM WidgetState ws WHERE ws.sessionId = :sessionId AND ws.widgetType = :widgetType AND ws.active = true")
    List<WidgetState> findBySessionIdAndWidgetType(@Param("sessionId") String sessionId, @Param("widgetType") String widgetType);

    /**
     * Deactivate all widget states for a session (soft delete)
     */
    @Query("UPDATE WidgetState ws SET ws.active = false WHERE ws.sessionId = :sessionId")
    int deactivateBySessionId(@Param("sessionId") String sessionId);

    /**
     * Find the latest version of a widget state
     */
    @Query("SELECT ws FROM WidgetState ws WHERE ws.sessionId = :sessionId AND ws.widgetId = :widgetId ORDER BY ws.version DESC LIMIT 1")
    Optional<WidgetState> findLatestBySessionIdAndWidgetId(@Param("sessionId") String sessionId, @Param("widgetId") String widgetId);

    /**
     * Count active widgets for a session
     */
    @Query("SELECT COUNT(ws) FROM WidgetState ws WHERE ws.sessionId = :sessionId AND ws.active = true")
    long countActiveBySessionId(@Param("sessionId") String sessionId);
}
