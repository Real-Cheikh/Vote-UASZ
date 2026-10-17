package sn.uasz.voteuasz.voting;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BallotRepository extends JpaRepository<Ballot, Long> {

    List<Ballot> findByElectionId(Long electionId);

    long countByElectionId(Long electionId);

    long countByElectionIdAndEstBlancTrue(Long electionId);

    long countByElectionIdAndCandidacyId(Long electionId, Long candidacyId);

    Optional<Ballot> findByRecuVoteHash(String recuVoteHash);

    boolean existsByRecuVoteHash(String recuVoteHash);

    @Query("SELECT b.candidacy.id, COUNT(b) FROM Ballot b WHERE b.election.id = :electionId AND b.estBlanc = false GROUP BY b.candidacy.id")
    List<Object[]> countVotesGroupedByCandidacy(@Param("electionId") Long electionId);
}
