package olsh.backend.marimomanagerservice.repository;

import olsh.backend.marimomanagerservice.entity.ExecutionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExecutionRecordRepository extends JpaRepository<ExecutionRecord, String> {
    Page<ExecutionRecord> findBySessionIdOrderByCreatedAtDesc(String sessionId, Pageable pageable);
    long countBySessionId(String sessionId);
} 