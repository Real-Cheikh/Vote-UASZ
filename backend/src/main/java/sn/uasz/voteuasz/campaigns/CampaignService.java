package sn.uasz.voteuasz.campaigns;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.uasz.voteuasz.candidacies.Candidacy;
import sn.uasz.voteuasz.candidacies.CandidacyRepository;
import sn.uasz.voteuasz.candidacies.CandidacyStatus;
import sn.uasz.voteuasz.common.BusinessException;
import sn.uasz.voteuasz.common.ResourceNotFoundException;
import sn.uasz.voteuasz.security.UserPrincipal;

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

        // Protection IDOR et intégrité : vérification que l'utilisateur connecté est le candidat titulaire ou la commission
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            boolean isOwner = candidacy.getCandidat().getId().equals(principal.getId());
            boolean isCommission = principal.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_COMMISSION_ELECTORALE") 
                                || a.getAuthority().equals("ROLE_SUPER_ADMIN_ELECTORAL"));
            if (!isOwner && !isCommission) {
                throw new BusinessException("Accès refusé : vous ne pouvez publier de contenu de campagne que pour votre propre candidature.");
            }
        }

        // Le candidat ne peut publier de campagne que si sa candidature a été officiellement validée
        if (candidacy.getStatut() != CandidacyStatus.VALIDATED) {
            throw new BusinessException("Impossible de publier un contenu de campagne : la candidature n'a pas encore été officiellement validée par la Commission.");
        }

        CampaignContent content = new CampaignContent();
        content.setCandidacy(candidacy);
        content.setTitre(titre);
        content.setTexte(texte);
        content.setMediaUrl(mediaUrl);
        content.setTypeMedia(typeMedia != null ? typeMedia : "TEXT");
        content.setStatutModeration("APPROVED");
        content.setDatePublication(OffsetDateTime.now());

        return campaignRepository.save(content);
    }
}
