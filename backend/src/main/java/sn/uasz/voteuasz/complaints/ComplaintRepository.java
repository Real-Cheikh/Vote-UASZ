package sn.uasz.voteuasz.complaints;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByElectionId(Long electionId);
    List<Complaint> findByUserId(Long userId);
}
