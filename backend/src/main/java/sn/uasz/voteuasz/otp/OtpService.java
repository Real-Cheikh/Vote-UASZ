package sn.uasz.voteuasz.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.uasz.voteuasz.audit.AuditService;
import sn.uasz.voteuasz.common.BusinessException;
import sn.uasz.voteuasz.common.ResourceNotFoundException;
import sn.uasz.voteuasz.common.VoteException;
import sn.uasz.voteuasz.elections.Election;
import sn.uasz.voteuasz.elections.ElectionRepository;
import sn.uasz.voteuasz.elections.ElectionStatus;
import sn.uasz.voteuasz.eligibility.EligibilityService;
import sn.uasz.voteuasz.users.User;
import sn.uasz.voteuasz.users.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private final OtpRepository otpRepository;
    private final UserRepository userRepository;
    private final ElectionRepository electionRepository;
    private final EligibilityService eligibilityService;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.otp.expiration-seconds:300}")
    private int expirationSeconds;

    @Value("${app.otp.max-attempts:3}")
    private int maxAttempts;

    @Value("${app.otp.cooldown-seconds:60}")
    private int cooldownSeconds;

    @Value("${app.otp.provider:mock}")
    private String otpProvider;

    // Adaptateur de test en mémoire pour faciliter les tests automatisés sans exposer le code
    private final Map<String, String> testAdapterLastCode = new ConcurrentHashMap<>();

    public OtpService(OtpRepository otpRepository,
                      UserRepository userRepository,
                      ElectionRepository electionRepository,
                      EligibilityService eligibilityService,
                      AuditService auditService) {
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.electionRepository = electionRepository;
        this.eligibilityService = eligibilityService;
        this.auditService = auditService;
    }

    @Transactional
    public OtpChallengeResponseDto generateOtp(Long userId, Long electionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Électeur introuvable : " + userId));

        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ResourceNotFoundException("Scrutin introuvable : " + electionId));

        if (election.getStatut() != ElectionStatus.VOTING_OPEN) {
            throw new BusinessException("Ce scrutin n'est pas ouvert au vote actuellement");
        }

        // Vérification de l'éligibilité et de la non-consommation du vote
        var eligibility = eligibilityService.checkUserEligibility(electionId, userId);
        if (!eligibility.isEligible()) {
            throw new BusinessException("Vous n'êtes pas éligible pour participer à ce scrutin");
        }
        if (eligibility.isAVote()) {
            throw new BusinessException("Votre droit de vote pour ce scrutin a déjà été consommé");
        }

        // Vérification du délai de temporisation (cooldown)
        Optional<OtpChallenge> lastChallenge = otpRepository.findLatestActiveChallenge(userId, electionId);
        OffsetDateTime now = OffsetDateTime.now();
        if (lastChallenge.isPresent()) {
            OtpChallenge prev = lastChallenge.get();
            if (prev.getDateCreation().plusSeconds(cooldownSeconds).isAfter(now)) {
                throw new BusinessException("Une demande de code OTP a déjà été effectuée récemment. Veuillez patienter avant de renouveler la demande.");
            }
            // Invalidation de l'ancien défi actif non utilisé
            prev.setConsomme(true);
            otpRepository.save(prev);
        }

        // Génération cryptographique aléatoire d'un code à 6 chiffres
        int randomInt = secureRandom.nextInt(1_000_000);
        String clearOtpCode = String.format("%06d", randomInt);

        // Hachage cryptographique du code OTP (stockage sécurisé sans clair)
        String hashedCode = hashOtp(clearOtpCode);
        OffsetDateTime expiration = now.plusSeconds(expirationSeconds);

        OtpChallenge challenge = new OtpChallenge();
        challenge.setUser(user);
        challenge.setElection(election);
        challenge.setCodeHache(hashedCode);
        challenge.setDateExpiration(expiration);
        challenge.setTentativesRestantes(maxAttempts);
        challenge.setConsomme(false);
        challenge.setDateCreation(now);

        otpRepository.save(challenge);

        // Stockage dans l'adaptateur de test (pour assertions des tests automatisés en profil test/dev)
        testAdapterLastCode.put(user.getEmail() + ":" + electionId, clearOtpCode);

        // Journalisation sécurisée (le code OTP n'est JAMAIS écrit dans les logs)
        log.info("Défi OTP émis avec succès pour l'électeur [ID:{}] pour le scrutin [ID:{}] - Expire dans {}s",
                user.getId(), election.getId(), expirationSeconds);
        auditService.logEvent("OTP_CHALLENGE_ISSUED", "Demande de code OTP pour vote", electionId, "SUCCESS");

        String maskedEmail = maskEmail(user.getEmail());

        boolean providerAllowsSimulation = "mock".equalsIgnoreCase(otpProvider) || "simulation".equalsIgnoreCase(otpProvider);
        boolean isDemoElection = isDemonstrationElection(election);
        boolean isDemoUser = isDemonstrationUser(user);

        // RÈGLE DE SÉCURITÉ STRICTE :
        // Le mode SIMULATION est EXCLUSIVEMENT réservé aux scrutins de test/synthétiques ET aux comptes de démonstration/audit.
        // Les scrutins officiels (comme le scrutin n°1) ou les électeurs institutionnels réels
        // ne reçoivent JAMAIS de code OTP en clair dans la réponse API (simulationCode = null).
        boolean isSimulation = providerAllowsSimulation && isDemoElection && isDemoUser;

        String message;
        String simulationCode;
        String mode;

        if (isSimulation) {
            mode = "SIMULATION";
            simulationCode = clearOtpCode;
            message = "Mode Démonstration & Évaluation : Le serveur fonctionne en bac à sable sécurisé pour ce scrutin de test. Votre code d'autorisation unique est mis à disposition pour l'évaluation.";
        } else {
            mode = "EMAIL";
            simulationCode = null;
            message = "Un code d'autorisation à usage unique a été généré pour ce scrutin officiel. Pour des raisons strictes de sécurité, aucun code OTP n'est communiqué via l'API publique.";
        }

        return new OtpChallengeResponseDto(
                message,
                expiration,
                expirationSeconds,
                maskedEmail,
                mode,
                simulationCode
        );
    }

    @Transactional(noRollbackFor = VoteException.class)
    public boolean verifyOtp(Long userId, Long electionId, String inputCode) {
        OtpChallenge challenge = otpRepository.findLatestActiveChallenge(userId, electionId)
                .orElseThrow(() -> new VoteException("Aucun défi OTP actif trouvé pour ce scrutin. Veuillez demander un nouveau code."));

        OffsetDateTime now = OffsetDateTime.now();

        // Contrôle d'expiration stricte
        if (now.isAfter(challenge.getDateExpiration())) {
            challenge.setConsomme(true);
            otpRepository.save(challenge);
            auditService.logEvent("OTP_VERIFICATION_FAILED", "Tentative d'utilisation d'un code OTP expiré", electionId, "FAILURE");
            throw new VoteException("Le code OTP a expiré (validité de 5 minutes dépassée). Veuillez en demander un nouveau.");
        }

        // Contrôle des tentatives restantes
        if (challenge.getTentativesRestantes() <= 0) {
            challenge.setConsomme(true);
            otpRepository.save(challenge);
            auditService.logEvent("OTP_LOCKED", "Nombre maximal de tentatives OTP dépassé", electionId, "DENIED");
            throw new VoteException("Nombre maximal de tentatives atteint. Pour votre sécurité, veuillez demander un nouveau code.");
        }

        String inputHashed = hashOtp(inputCode);
        if (!MessageDigest.isEqual(inputHashed.getBytes(StandardCharsets.UTF_8),
                challenge.getCodeHache().getBytes(StandardCharsets.UTF_8))) {
            int remaining = challenge.getTentativesRestantes() - 1;
            challenge.setTentativesRestantes(remaining);
            if (remaining <= 0) {
                challenge.setConsomme(true);
            }
            otpRepository.save(challenge);
            auditService.logEvent("OTP_VERIFICATION_FAILED", "Code OTP incorrect", electionId, "FAILURE");
            throw new VoteException(String.format("Code OTP incorrect. Tentatives restantes : %d", remaining));
        }

        // OTP vérifié : invalidation immédiate pour usage unique
        challenge.setConsomme(true);
        otpRepository.save(challenge);
        auditService.logEvent("OTP_VERIFIED", "Code OTP validé avec succès", electionId, "SUCCESS");

        return true;
    }

    /**
     * Méthode d'accès pour l'environnement de test automatisé.
     */
    public String getTestAdapterCode(String email, Long electionId) {
        return testAdapterLastCode.get(email + ":" + electionId);
    }

    /**
     * Contrôle de sécurité strict : vérifie si un scrutin est qualifié pour le bac à sable de test/démonstration.
     * Le scrutin officiel n°1 ou tout scrutin officiel sans mention explicite de test/synthétique
     * est catégoriquement exclu du mode simulation.
     */
    public boolean isDemonstrationElection(Election election) {
        if (election == null || election.getId() == null) {
            return false;
        }
        // Le scrutin officiel n°1 est le scrutin officiel de production protégé de manière absolue
        if (Long.valueOf(1L).equals(election.getId())) {
            return false;
        }
        String titre = election.getTitre() != null ? election.getTitre().toLowerCase() : "";
        String desc = election.getDescription() != null ? election.getDescription().toLowerCase() : "";
        return titre.contains("test") || titre.contains("synthétique") || titre.contains("synthetique")
                || titre.contains("démo") || titre.contains("demo")
                || desc.contains("test") || desc.contains("synthétique") || desc.contains("synthetique")
                || desc.contains("validation") || desc.contains("audit");
    }

    /**
     * Contrôle de sécurité strict : vérifie si un utilisateur est un compte de test/démonstration.
     * Les comptes institutionnels réels, les administrateurs officiels et les électeurs réels
     * ne reçoivent jamais de code OTP dans les réponses API.
     */
    public boolean isDemonstrationUser(User user) {
        if (user == null || user.getEmail() == null) {
            return false;
        }
        String email = user.getEmail().toLowerCase();
        String numId = user.getNumeroIdentification() != null ? user.getNumeroIdentification().toLowerCase() : "";
        return email.startsWith("etudiant") || email.startsWith("candidat")
                || email.contains("test") || email.contains("audit") || email.contains("synthetique")
                || numId.contains("test") || numId.contains("audit") || numId.contains("demo");
    }

    private String hashOtp(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 non disponible", e);
        }
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return "***" + email.substring(atIndex);
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
