package sn.uasz.voteuasz.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByElectionIdOrderByHorodatageDesc(Long electionId);

    Page<AuditEvent> findAllByOrderByHorodatageDesc(Pageable pageable);

    @Query("SELECT a FROM AuditEvent a ORDER BY a.id DESC LIMIT 1")
    Optional<AuditEvent> findLastEvent();
}
