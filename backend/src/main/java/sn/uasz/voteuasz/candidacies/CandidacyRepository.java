package sn.uasz.voteuasz.candidacies;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidacyRepository extends JpaRepository<Candidacy, Long> {
    List<Candidacy> findByElectionId(Long electionId);

    List<Candidacy> findByElectionIdAndStatut(Long electionId, CandidacyStatus statut);

    List<Candidacy> findByCandidatId(Long candidatId);

    Optional<Candidacy> findByElectionIdAndCandidatId(Long electionId, Long candidatId);

    boolean existsByElectionIdAndCandidatId(Long electionId, Long candidatId);
}
