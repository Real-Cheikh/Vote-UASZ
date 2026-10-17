package sn.uasz.voteuasz.reports;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OfficialReportRepository extends JpaRepository<OfficialReport, Long> {
    Optional<OfficialReport> findByNumeroRapport(String numeroRapport);
    Optional<OfficialReport> findByElectionId(Long electionId);
}
