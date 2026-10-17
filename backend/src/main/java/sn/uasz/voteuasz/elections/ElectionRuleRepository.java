package sn.uasz.voteuasz.elections;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ElectionRuleRepository extends JpaRepository<ElectionRule, Long> {
    Optional<ElectionRule> findByElectionId(Long electionId);
}
