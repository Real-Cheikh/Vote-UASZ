package sn.uasz.voteuasz.voting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.uasz.voteuasz.audit.AuditService;
import sn.uasz.voteuasz.candidacies.Candidacy;
import sn.uasz.voteuasz.candidacies.CandidacyRepository;
import sn.uasz.voteuasz.candidacies.CandidacyStatus;
import sn.uasz.voteuasz.common.ResourceNotFoundException;
import sn.uasz.voteuasz.common.VoteException;
import sn.uasz.voteuasz.elections.Election;
import sn.uasz.voteuasz.elections.ElectionRepository;
import sn.uasz.voteuasz.elections.ElectionStatus;
import sn.uasz.voteuasz.eligibility.EligibilityService;
import sn.uasz.voteuasz.otp.OtpService;
import sn.uasz.voteuasz.security.UserPrincipal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class VotingService {

    private static final Logger log = LoggerFactory.getLogger(VotingService.class);

    private final VoteCredentialRepository credentialRepository;
    private final BallotRepository ballotRepository;
    private final ElectionRepository electionRepository;
    private final CandidacyRepository candidacyRepository;
    private final EligibilityService eligibilityService;
    private final OtpService otpService;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    public VotingService(VoteCredentialRepository credentialRepository,
                         BallotRepository ballotRepository,
                         ElectionRepository electionRepository,
                         CandidacyRepository candidacyRepository,
                         EligibilityService eligibilityService,
                         OtpService otpService,
                         AuditService auditService) {
        this.credentialRepository = credentialRepository;
        this.ballotRepository = ballotRepository;
        this.electionRepository = electionRepository;
        this.candidacyRepository = candidacyRepository;
        this.eligibilityService = eligibilityService;
        this.otpService = otpService;
        this.auditService = auditService;
    }

    /**
     * Étape 1 : Vérification d'éligibilité, validation de l'OTP et délivrance d'un jeton d'autorisation de vote anonyme.
     * Cette méthode marque de façon atomique le droit de vote comme consommé, mais n'a AUCUNE connaissance du choix futur.
     */
    @Transactional
    public VoteAuthorizationResponseDto requestVoteAuthorization(VoteAuthorizationRequestDto requestDto) {
        Long userId = getCurrentUserId();
        Long electionId = requestDto.getElectionId();

        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ResourceNotFoundException("Scrutin introuvable : " + electionId));

        OffsetDateTime now = OffsetDateTime.now();
        if (election.getStatut() != ElectionStatus.VOTING_OPEN) {
            throw new VoteException("Le scrutin n'est pas ouvert au vote");
        }
        if (now.isBefore(election.getDateOuverture()) || now.isAfter(election.getDateCloture())) {
            throw new VoteException("Le scrutin n'est pas dans sa plage horaire officielle de vote");
        }

        // Vérification du code OTP
        otpService.verifyOtp(userId, electionId, requestDto.getOtpCode());

        // Consommation atomique immédiate du droit de vote dans le registre d'éligibilité
        boolean consumed = eligibilityService.markVoteConsumed(electionId, userId);
        if (!consumed) {
            auditService.logEvent("DOUBLE_VOTE_ATTEMPT_BLOCKED", "Tentative de double vote rejetée au niveau du registre", electionId, "DENIED");
            throw new VoteException("Votre droit de vote pour ce scrutin a déjà été consommé. Le double vote est strictement impossible.");
        }

        // Génération d'un jeton de vote anonyme cryptographique
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String clearVoteToken = HexFormat.of().formatHex(tokenBytes);
        String tokenHash = computeSha256(clearVoteToken);

        // Validité du jeton d'autorisation : 15 minutes
        OffsetDateTime expiration = now.plusMinutes(15);
        VoteCredential credential = new VoteCredential(tokenHash, election, expiration);
        credentialRepository.save(credential);

        auditService.logEvent("VOTE_TOKEN_ISSUED", "Jeton de vote anonyme émis avec succès", electionId, "SUCCESS");

        return new VoteAuthorizationResponseDto(
                clearVoteToken,
                election.getId(),
                election.getTitre(),
                expiration,
                "Autorisation de vote accordée. Vous disposez de 15 minutes pour glisser votre bulletin dans l'urne."
        );
    }

    /**
     * Étape 2 : Dépôt du bulletin dans l'urne électronique.
     * Reçoit le jeton anonyme et le choix. Le système ne dispose d'aucun lien vers l'identité de l'électeur.
     */
    @Transactional
    public VoteReceiptDto castBallot(CastBallotDto ballotDto) {
        OffsetDateTime now = OffsetDateTime.now();
        String tokenHash = computeSha256(ballotDto.getVoteToken());

        // Invalidation atomique du jeton d'autorisation : empêche la réutilisation ou les accès concurrents
        int rowsUpdated = credentialRepository.consumeTokenAtomically(tokenHash, now);
        if (rowsUpdated == 0) {
            auditService.logEvent("BALLOT_REJECTED", "Jeton d'autorisation invalide, expiré ou déjà consommé", ballotDto.getElectionId(), "DENIED");
            throw new VoteException("Jeton d'autorisation de vote invalide, expiré ou déjà utilisé. Votre bulletin a été rejeté.");
        }

        Election election = electionRepository.findById(ballotDto.getElectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Scrutin introuvable : " + ballotDto.getElectionId()));

        if (election.getStatut() != ElectionStatus.VOTING_OPEN) {
            throw new VoteException("Impossible de déposer un bulletin : le scrutin n'est pas ouvert");
        }

        Candidacy candidacy = null;
        if (ballotDto.isEstBlanc()) {
            if (!election.isPermettreVoteBlanc()) {
                throw new VoteException("Le vote blanc n'est pas autorisé pour ce scrutin");
            }
        } else {
            if (ballotDto.getCandidacyId() == null) {
                throw new VoteException("Veuillez sélectionner un candidat ou opter pour le vote blanc");
            }
            candidacy = candidacyRepository.findById(ballotDto.getCandidacyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Candidat introuvable"));

            if (!candidacy.getElection().getId().equals(election.getId())) {
                throw new VoteException("Ce candidat ne concourt pas dans ce scrutin");
            }
            if (candidacy.getStatut() != CandidacyStatus.VALIDATED) {
                throw new VoteException("La candidature sélectionnée n'a pas été officiellement validée");
            }
        }

        // Génération du reçu de vote cryptographique unique (non corrélé au choix)
        String receiptSeed = UUID.randomUUID() + "|" + election.getId() + "|" + now.toInstant().toEpochMilli();
        String receiptHash = computeSha256(receiptSeed);

        // Insertion du bulletin anonyme dans l'urne
        Ballot ballot = new Ballot(election, candidacy, ballotDto.isEstBlanc(), receiptHash);
        ballotRepository.save(ballot);

        auditService.logEvent("BALLOT_CAST_SUCCESS", "Bulletin anonyme déposé dans l'urne avec succès", election.getId(), "SUCCESS");
        log.info("Bulletin enregistré avec succès dans l'urne électorale [Scrutin ID: {}]", election.getId());

        return new VoteReceiptDto(
                receiptHash,
                election.getId(),
                election.getTitre(),
                now,
                "Votre vote a été validé et scellé dans l'urne électronique avec succès."
        );
    }

    /**
     * Vérification publique du reçu de vote : atteste de la présence du bulletin dans l'urne sans révéler le choix.
     */
    @Transactional(readOnly = true)
    public ReceiptVerificationDto verifyReceipt(String receiptHash) {
        if (receiptHash == null || !receiptHash.trim().matches("^[a-fA-F0-9]{64}$")) {
            return new ReceiptVerificationDto(
                    false,
                    receiptHash,
                    null,
                    null,
                    null,
                    "Format d'empreinte invalide. Une empreinte SHA-256 valide doit comporter exactement 64 caractères hexadécimaux."
            );
        }

        String cleanHash = receiptHash.trim().toLowerCase();
        return ballotRepository.findByRecuVoteHash(cleanHash)
                .map(b -> new ReceiptVerificationDto(
                        true,
                        b.getRecuVoteHash(),
                        b.getElection().getId(),
                        b.getElection().getTitre(),
                        b.getDateEnregistrement(),
                        "Bulletin officiellement certifié présent dans l'urne électronique."
                ))
                .orElseGet(() -> new ReceiptVerificationDto(
                        false,
                        cleanHash,
                        null,
                        null,
                        null,
                        "Aucun bulletin correspondant à cette empreinte de reçu n'a été trouvé dans l'urne."
                ));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        throw new VoteException("Utilisateur non authentifié");
    }

    private String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 indisponible", e);
        }
    }
}
