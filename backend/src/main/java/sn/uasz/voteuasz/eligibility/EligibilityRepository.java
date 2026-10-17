package sn.uasz.voteuasz.eligibility;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EligibilityRepository extends JpaRepository<EligibilityRecord, Long> {

    Optional<EligibilityRecord> findByElectionIdAndUserId(Long electionId, Long userId);

    List<EligibilityRecord> findByElectionId(Long electionId);

    List<EligibilityRecord> findByUserId(Long userId);

    long countByElectionIdAndEligibleTrue(Long electionId);

    long countByElectionIdAndAVoteTrue(Long electionId);

    @Modifying
    @Query("UPDATE EligibilityRecord e SET e.aVote = true, e.dateVote = :now WHERE e.election.id = :electionId AND e.user.id = :userId AND e.aVote = false")
    int markVotedAtomically(@Param("electionId") Long electionId, @Param("userId") Long userId, @Param("now") OffsetDateTime now);
}
