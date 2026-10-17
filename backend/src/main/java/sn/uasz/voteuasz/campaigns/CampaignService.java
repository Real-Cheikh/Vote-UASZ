package sn.uasz.voteuasz.campaigns;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.uasz.voteuasz.candidacies.Candidacy;
import sn.uasz.voteuasz.candidacies.CandidacyRepository;
import sn.uasz.voteuasz.common.ResourceNotFoundException;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CandidacyRepository candidacyRepository;

    public CampaignService(CampaignRepository campaignRepository, CandidacyRepository candidacyRepository) {
        this.campaignRepository = campaignRepository;
        this.candidacyRepository = candidacyRepository;
    }

    @Transactional(readOnly = true)
    public List<CampaignContent> findByCandidacy(Long candidacyId) {
        return campaignRepository.findByCandidacyId(candidacyId);
    }

    @Transactional
    public CampaignContent publishContent(Long candidacyId, String titre, String texte, String mediaUrl, String typeMedia) {
        Candidacy candidacy = candidacyRepository.findById(candidacyId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable : " + candidacyId));

        CampaignContent content = new CampaignContent();
        content.setCandidacy(candidacy);
        content.setTitre(titre);
        content.setTexte(texte);
        content.setMediaUrl(mediaUrl);
        content.setTypeMedia(typeMedia);
        content.setStatutModeration("APPROVED");
        content.setDatePublication(OffsetDateTime.now());

        return campaignRepository.save(content);
    }
}
