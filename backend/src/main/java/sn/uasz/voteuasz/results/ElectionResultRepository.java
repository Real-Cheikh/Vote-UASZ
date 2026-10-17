package sn.uasz.voteuasz.results;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ElectionResultRepository extends JpaRepository<ElectionResult, Long> {
    Optional<ElectionResult> findByElectionId(Long electionId);
    boolean existsByElectionId(Long electionId);
}
