package sn.uasz.voteuasz.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditEvent logEvent(String eventType, String details, Long electionId, String resultat) {
        try {
            String actor = getEffectiveActor();
            String uuid = UUID.randomUUID().toString();
            OffsetDateTime now = OffsetDateTime.now();

            // Calcul du chaînage cryptographique avec le hash précédent
            String previousHash = auditRepository.findLastEvent()
                    .map(AuditEvent::getEmpreinteChaine)
                    .orElse("0000000000000000000000000000000000000000000000000000000000000000");

            String contentToHash = previousHash + "|" + uuid + "|" + eventType + "|" + actor + "|" + now.toString();
            String chainHash = computeSha256(contentToHash);

            AuditEvent event = new AuditEvent();
            event.setEventUuid(uuid);
            event.setHorodatage(now);
            event.setEventType(eventType);
            event.setActeur(actor);
            event.setElectionId(electionId);
            event.setResultat(resultat);
            event.setDetails(details);
            event.setCorrelationId(UUID.randomUUID().toString().substring(0, 8));
            event.setEmpreinteChaine(chainHash);

            AuditEvent saved = auditRepository.save(event);
            log.info("Audit [{}] {} par {} - Statut: {}", eventType, details, actor, resultat);
            return saved;
        } catch (Exception e) {
            log.error("Échec lors de l'enregistrement de l'événement d'audit : {}", e.getMessage(), e);
            return null;
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> getEvents(Pageable pageable) {
        return auditRepository.findAllByOrderByHorodatageDesc(pageable);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> getEventsByElection(Long electionId) {
        return auditRepository.findByElectionIdOrderByHorodatageDesc(electionId);
    }

    private String getEffectiveActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            return auth.getName();
        }
        return "SYSTEM";
    }

    private String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 non disponible", e);
        }
    }
}
