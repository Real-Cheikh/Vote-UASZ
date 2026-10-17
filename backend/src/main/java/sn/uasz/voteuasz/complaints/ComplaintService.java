package sn.uasz.voteuasz.complaints;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.uasz.voteuasz.audit.AuditService;
import sn.uasz.voteuasz.common.BusinessException;
import sn.uasz.voteuasz.common.ResourceNotFoundException;
import sn.uasz.voteuasz.elections.Election;
import sn.uasz.voteuasz.elections.ElectionRepository;
import sn.uasz.voteuasz.security.UserPrincipal;
import sn.uasz.voteuasz.users.User;
import sn.uasz.voteuasz.users.UserRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ElectionRepository electionRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ComplaintService(ComplaintRepository complaintRepository,
                            ElectionRepository electionRepository,
                            UserRepository userRepository,
                            AuditService auditService) {
        this.complaintRepository = complaintRepository;
        this.electionRepository = electionRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ComplaintDto> findByElection(Long electionId) {
        return complaintRepository.findByElectionId(electionId).stream()
                .map(ComplaintDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ComplaintDto> findMyComplaints() {
        Long userId = getCurrentUserId();
        return complaintRepository.findByUserId(userId).stream()
                .map(ComplaintDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ComplaintDto submitComplaint(ComplaintSubmitDto dto) {
        Long userId = getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        Election election = electionRepository.findById(dto.getElectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Scrutin introuvable : " + dto.getElectionId()));

        Complaint complaint = new Complaint();
        complaint.setElection(election);
        complaint.setUser(user);
        complaint.setObjet(dto.getObjet());
        complaint.setDescription(dto.getDescription());
        complaint.setPiecesJointesUrls(dto.getPiecesJointesUrls());
        complaint.setStatut(ComplaintStatus.SUBMITTED);
        complaint.setDateDepot(OffsetDateTime.now());

        Complaint saved = complaintRepository.save(complaint);
        auditService.logEvent("COMPLAINT_SUBMITTED", "Réclamation déposée : " + dto.getObjet(), election.getId(), "SUCCESS");

        return ComplaintDto.fromEntity(saved);
    }

    @Transactional
    public ComplaintDto resolveComplaint(Long id, ComplaintResolutionDto resolutionDto) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réclamation introuvable : " + id));

        complaint.setStatut(resolutionDto.getStatut());
        complaint.setReponseCommission(resolutionDto.getReponseCommission());
        complaint.setDateTraitement(OffsetDateTime.now());

        Complaint updated = complaintRepository.save(complaint);
        auditService.logEvent(
                "COMPLAINT_RESOLVED",
                String.format("Décision sur contestation ID %d : %s", id, resolutionDto.getStatut()),
                complaint.getElection().getId(),
                "SUCCESS"
        );

        return ComplaintDto.fromEntity(updated);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        throw new BusinessException("Session non authentifiée");
    }
}
