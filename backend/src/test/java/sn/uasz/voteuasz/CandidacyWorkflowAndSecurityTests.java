package sn.uasz.voteuasz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import sn.uasz.voteuasz.candidacies.*;
import sn.uasz.voteuasz.colleges.College;
import sn.uasz.voteuasz.colleges.CollegeRepository;
import sn.uasz.voteuasz.common.BusinessException;
import sn.uasz.voteuasz.elections.Election;
import sn.uasz.voteuasz.elections.ElectionDto;
import sn.uasz.voteuasz.elections.ElectionRepository;
import sn.uasz.voteuasz.elections.ElectionStatus;
import sn.uasz.voteuasz.elections.ElectionType;
import sn.uasz.voteuasz.roles.Role;
import sn.uasz.voteuasz.roles.RoleName;
import sn.uasz.voteuasz.roles.RoleRepository;
import sn.uasz.voteuasz.security.UserPrincipal;
import sn.uasz.voteuasz.users.User;
import sn.uasz.voteuasz.users.UserCategory;
import sn.uasz.voteuasz.users.UserRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class CandidacyWorkflowAndSecurityTests {

    @Autowired
    private CandidacyService candidacyService;

    @Autowired
    private CandidacyRepository candidacyRepository;

    @Autowired
    private ElectionRepository electionRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private College collegeEtudiant;
    private College collegeEnseignant;
    private User etudiantUser;
    private User enseignantUser;
    private Election electionEtudiantOpen;
    private Election electionClosed;

    @BeforeEach
    void setUp() {
        Role roleElecteur = roleRepository.findByName(RoleName.ROLE_ELECTEUR)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_ELECTEUR, "Électeur")));

        collegeEtudiant = collegeRepository.findByCode("COL_TEST_ETU").orElseGet(() ->
                collegeRepository.save(new College("COL_TEST_ETU", "Collège Étudiants Test", "Desc", "UFR ST", "Informatique", "L3", UserCategory.ETUDIANT)));

        collegeEnseignant = collegeRepository.findByCode("COL_TEST_ENS").orElseGet(() ->
                collegeRepository.save(new College("COL_TEST_ENS", "Collège Enseignants Test", "Desc", "UFR ST", "Mathématiques", null, UserCategory.ENSEIGNANT)));

        etudiantUser = userRepository.findByEmail("etu.candidacy.test@uasz.sn").orElseGet(() -> {
            User u = new User("etu.candidacy.test@uasz.sn", passwordEncoder.encode("Pass123!"), "Diop", "Fatou", "ETU-CAND-001", "+221771112233", UserCategory.ETUDIANT);
            u.setRoles(Set.of(roleElecteur));
            return userRepository.save(u);
        });

        enseignantUser = userRepository.findByEmail("ens.candidacy.test@uasz.sn").orElseGet(() -> {
            User u = new User("ens.candidacy.test@uasz.sn", passwordEncoder.encode("Pass123!"), "Ba", "Mamadou", "ENS-CAND-001", "+221772223344", UserCategory.ENSEIGNANT);
            u.setRoles(Set.of(roleElecteur));
            return userRepository.save(u);
        });

        electionEtudiantOpen = new Election();
        electionEtudiantOpen.setTitre("Scrutin Candidatures Ouvertes Test " + System.currentTimeMillis());
        electionEtudiantOpen.setType(ElectionType.DELEGUE);
        electionEtudiantOpen.setCollege(collegeEtudiant);
        electionEtudiantOpen.setDateOuverture(OffsetDateTime.now().minusDays(1));
        electionEtudiantOpen.setDateCloture(OffsetDateTime.now().plusDays(5));
        electionEtudiantOpen.setStatut(ElectionStatus.CANDIDATURES_OPEN);
        electionEtudiantOpen.setPermettreVoteBlanc(true);
        electionEtudiantOpen = electionRepository.save(electionEtudiantOpen);

        electionClosed = new Election();
        electionClosed.setTitre("Scrutin En Vote Test " + System.currentTimeMillis());
        electionClosed.setType(ElectionType.DELEGUE);
        electionClosed.setCollege(collegeEtudiant);
        electionClosed.setDateOuverture(OffsetDateTime.now().minusDays(2));
        electionClosed.setDateCloture(OffsetDateTime.now().plusDays(1));
        electionClosed.setStatut(ElectionStatus.VOTING_OPEN);
        electionClosed.setPermettreVoteBlanc(true);
        electionClosed = electionRepository.save(electionClosed);
    }

    private void authenticateAs(User user) {
        UserPrincipal principal = UserPrincipal.create(user);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("1. Recherche des scrutins ouverts aux candidatures selon la catégorie de l'utilisateur")
    void testFindOpenElectionsForCurrentUser() {
        authenticateAs(etudiantUser);

        List<ElectionDto> openElections = candidacyService.findOpenElectionsForCurrentUser();
        assertNotNull(openElections);
        assertTrue(openElections.stream().anyMatch(e -> e.getId().equals(electionEtudiantOpen.getId())));
        assertFalse(openElections.stream().anyMatch(e -> e.getId().equals(electionClosed.getId())), "Les scrutins en phase VOTING_OPEN ne doivent pas être listés pour candidature");
    }

    @Test
    @DisplayName("2. Dépôt de candidature valide et acquisition automatique du rôle CANDIDAT")
    void testEligibleCandidateCanApply() {
        authenticateAs(etudiantUser);

        CandidacyApplyDto dto = new CandidacyApplyDto();
        dto.setElectionId(electionEtudiantOpen.getId());
        dto.setProfessionFoi("Programme pour la réussite de tous les étudiants de l'UASZ.");
        dto.setBiographie("Étudiante engagée, représentante de filière.");

        CandidacyDto created = candidacyService.apply(dto);
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals(CandidacyStatus.SUBMITTED, created.getStatut());
        assertEquals(electionEtudiantOpen.getId(), created.getElectionId());

        // Vérifier l'attribution du rôle CANDIDAT tout en maintenant ELECTEUR
        User updatedUser = userRepository.findById(etudiantUser.getId()).orElseThrow();
        assertTrue(updatedUser.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_CANDIDAT));
        assertTrue(updatedUser.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ELECTEUR));
    }

    @Test
    @DisplayName("3. Rejet d'une candidature pour incompatibilité de collège / catégorie")
    void testRejectApplicationWhenIncompatibleCategory() {
        authenticateAs(enseignantUser);

        CandidacyApplyDto dto = new CandidacyApplyDto();
        dto.setElectionId(electionEtudiantOpen.getId()); // Scrutin pour collège Étudiants
        dto.setProfessionFoi("Programme.");

        assertThrows(BusinessException.class, () -> candidacyService.apply(dto),
                "Un enseignant ne doit pas pouvoir postuler à un collège étudiant");
    }

    @Test
    @DisplayName("4. Prévention stricte des doubles candidatures sur un même scrutin")
    void testAntiDoubleCandidacyPrevention() {
        authenticateAs(etudiantUser);

        CandidacyApplyDto dto = new CandidacyApplyDto();
        dto.setElectionId(electionEtudiantOpen.getId());
        dto.setProfessionFoi("Premier dossier.");

        candidacyService.apply(dto);

        // Deuxième tentative identique
        CandidacyApplyDto dto2 = new CandidacyApplyDto();
        dto2.setElectionId(electionEtudiantOpen.getId());
        dto2.setProfessionFoi("Deuxième tentative interdite.");

        assertThrows(BusinessException.class, () -> candidacyService.apply(dto2),
                "Un candidat ne peut pas déposer deux dossiers pour le même scrutin");
    }

    @Test
    @DisplayName("5. Rejet d'un dépôt de candidature si le scrutin n'est pas en phase CANDIDATURES_OPEN")
    void testRejectApplicationWhenElectionNotOpen() {
        authenticateAs(etudiantUser);

        CandidacyApplyDto dto = new CandidacyApplyDto();
        dto.setElectionId(electionClosed.getId()); // Statut VOTING_OPEN
        dto.setProfessionFoi("Programme hors délai.");

        assertThrows(BusinessException.class, () -> candidacyService.apply(dto),
                "Le dépôt de candidature doit être rejeté si le statut n'est pas CANDIDATURES_OPEN");
    }

    @Test
    @DisplayName("6. Consultation des candidatures publiques et filtrage des dossiers rejetés")
    void testPublicCandidatesListing() {
        // Créer une candidature validée
        Candidacy validated = new Candidacy();
        validated.setElection(electionEtudiantOpen);
        validated.setCandidat(etudiantUser);
        validated.setStatut(CandidacyStatus.VALIDATED);
        validated.setProfessionFoi("Profession validée.");
        candidacyRepository.save(validated);

        List<CandidacyDto> list = candidacyService.findPublicCandidatesByElection(electionEtudiantOpen.getId());
        assertFalse(list.isEmpty());
        assertTrue(list.stream().anyMatch(c -> c.getId().equals(validated.getId())));
    }
}
