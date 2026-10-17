package sn.uasz.voteuasz.eligibility;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.uasz.voteuasz.colleges.College;
import sn.uasz.voteuasz.common.BusinessException;
import sn.uasz.voteuasz.common.ResourceNotFoundException;
import sn.uasz.voteuasz.elections.Election;
import sn.uasz.voteuasz.elections.ElectionRepository;
import sn.uasz.voteuasz.elections.ElectionRule;
import sn.uasz.voteuasz.users.User;
import sn.uasz.voteuasz.users.UserRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class EligibilityService {

    private final EligibilityRepository eligibilityRepository;
    private final ElectionRepository electionRepository;
    private final UserRepository userRepository;

    public EligibilityService(EligibilityRepository eligibilityRepository,
                              ElectionRepository electionRepository,
                              UserRepository userRepository) {
        this.eligibilityRepository = eligibilityRepository;
        this.electionRepository = electionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public EligibilityDto checkUserEligibility(Long electionId, Long userId) {
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ResourceNotFoundException("Scrutin introuvable : " + electionId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Électeur introuvable : " + userId));

        Optional<EligibilityRecord> existingRecord = eligibilityRepository.findByElectionIdAndUserId(electionId, userId);

        if (existingRecord.isPresent()) {
            return EligibilityDto.fromEntity(existingRecord.get());
        }

        // Évaluation dynamique de l'éligibilité
        boolean isEligible = evaluateEligibility(election, user);
        String motif = isEligible ? null : "Vous ne répondez pas aux critères du collège électoral ou des règles du scrutin";

        EligibilityRecord newRecord = new EligibilityRecord(election, user, isEligible);
        EligibilityRecord saved = eligibilityRepository.save(newRecord);

        EligibilityDto dto = EligibilityDto.fromEntity(saved);
        dto.setMotifIneligibilite(motif);
        return dto;
    }

    @Transactional(readOnly = true)
    public List<EligibilityDto> findEligibleElectionsForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Électeur introuvable"));

        List<Election> activeElections = electionRepository.findActiveElections();
        List<EligibilityDto> result = new ArrayList<>();

        for (Election election : activeElections) {
            Optional<EligibilityRecord> recordOpt = eligibilityRepository.findByElectionIdAndUserId(election.getId(), user.getId());
            if (recordOpt.isPresent()) {
                if (recordOpt.get().isEligible()) {
                    result.add(EligibilityDto.fromEntity(recordOpt.get()));
                }
            } else if (evaluateEligibility(election, user)) {
                // Création à la volée de l'enregistrement d'éligibilité
                EligibilityRecord record = eligibilityRepository.save(new EligibilityRecord(election, user, true));
                result.add(EligibilityDto.fromEntity(record));
            }
        }
        return result;
    }

    @Transactional
    public boolean markVoteConsumed(Long electionId, Long userId) {
        int updated = eligibilityRepository.markVotedAtomically(electionId, userId, OffsetDateTime.now());
        return updated > 0;
    }

    private boolean evaluateEligibility(Election election, User user) {
        if (!user.isActif() || user.isBloque()) {
            return false;
        }

        College college = election.getCollege();
        if (college != null && college.getCategorie() != null) {
            if (college.getCategorie() != user.getCategorie()) {
                return false;
            }
        }

        ElectionRule rules = election.getRules();
        if (rules != null && rules.getCategorieRequise() != null) {
            if (!rules.getCategorieRequise().equalsIgnoreCase(user.getCategorie().name())) {
                return false;
            }
        }

        return true;
    }
}
