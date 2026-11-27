package sn.uasz.voteuasz.candidacies;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidacyRepository extends JpaRepository<Candidacy, Long> {

    @Query("SELECT DISTINCT c FROM Candidacy c JOIN FETCH c.candidat u LEFT JOIN FETCH c.election e LEFT JOIN FETCH e.college WHERE c.election.id = :electionId ORDER BY c.id ASC")
    List<Candidacy> findByElectionId(@Param("electionId") Long electionId);

    @Query("SELECT DISTINCT c FROM Candidacy c JOIN FETCH c.candidat u LEFT JOIN FETCH c.election e LEFT JOIN FETCH e.college WHERE c.election.id = :electionId AND c.statut = :statut ORDER BY c.id ASC")
    List<Candidacy> findByElectionIdAndStatut(@Param("electionId") Long electionId, @Param("statut") CandidacyStatus statut);

    @Query("SELECT DISTINCT c FROM Candidacy c JOIN FETCH c.candidat u LEFT JOIN FETCH c.election e LEFT JOIN FETCH e.college WHERE u.id = :candidatId ORDER BY c.id DESC")
    List<Candidacy> findByCandidatId(@Param("candidatId") Long candidatId);

    @Query("SELECT DISTINCT c FROM Candidacy c JOIN FETCH c.candidat u LEFT JOIN FETCH c.election e LEFT JOIN FETCH e.college WHERE c.election.id = :electionId AND u.id = :candidatId")
    Optional<Candidacy> findByElectionIdAndCandidatId(@Param("electionId") Long electionId, @Param("candidatId") Long candidatId);

    @Query("SELECT c FROM Candidacy c JOIN FETCH c.candidat u LEFT JOIN FETCH c.election e LEFT JOIN FETCH e.college WHERE c.id = :id")
    Optional<Candidacy> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT DISTINCT c FROM Candidacy c JOIN FETCH c.candidat u LEFT JOIN FETCH c.election e LEFT JOIN FETCH e.college ORDER BY c.id DESC")
    List<Candidacy> findAllWithDetails();

    boolean existsByElectionIdAndCandidatId(Long electionId, Long candidatId);
}
