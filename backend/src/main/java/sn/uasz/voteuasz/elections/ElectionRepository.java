package sn.uasz.voteuasz.elections;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ElectionRepository extends JpaRepository<Election, Long> {
    List<Election> findByStatut(ElectionStatus statut);

    List<Election> findByCollegeId(Long collegeId);

    @Query("SELECT e FROM Election e WHERE e.statut IN :statuts ORDER BY e.dateOuverture DESC")
    List<Election> findByStatutIn(@Param("statuts") List<ElectionStatus> statuts);

    @Query("SELECT e FROM Election e WHERE e.statut = 'VOTING_OPEN' ORDER BY e.dateCloture ASC")
    List<Election> findActiveElections();
}
