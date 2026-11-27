package sn.uasz.voteuasz.reports;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OfficialReportRepository extends JpaRepository<OfficialReport, Long> {
    Optional<OfficialReport> findByNumeroRapport(String numeroRapport);

    @Query("SELECT r FROM OfficialReport r WHERE r.election.id = :electionId")
    Optional<OfficialReport> findByElectionId(@Param("electionId") Long electionId);
}
