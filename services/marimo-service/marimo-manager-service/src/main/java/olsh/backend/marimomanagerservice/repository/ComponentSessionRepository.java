package olsh.backend.marimomanagerservice.repository;

import olsh.backend.marimomanagerservice.entity.ComponentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComponentSessionRepository extends JpaRepository<ComponentSession, String> {

    Page<ComponentSession> findByUserIdAndStatus(String userId, ComponentSession.SessionStatus status, Pageable pageable);

    List<ComponentSession> findByComponentId(String componentId);

} 