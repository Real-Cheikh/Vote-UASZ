package sn.uasz.voteuasz.campaigns;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignRepository extends JpaRepository<CampaignContent, Long> {
    List<CampaignContent> findByCandidacyId(Long candidacyId);
}
