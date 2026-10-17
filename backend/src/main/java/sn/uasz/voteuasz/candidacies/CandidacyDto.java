package sn.uasz.voteuasz.candidacies;

import sn.uasz.voteuasz.users.UserDto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class CandidacyDto {

    private Long id;
    private Long electionId;
    private String electionTitre;
    private UserDto candidat;
    private String professionFoi;
    private String biographie;
    private String photoUrl;
    private CandidacyStatus statut;
    private String motifDecision;
    private OffsetDateTime dateDecision;
    private OffsetDateTime dateSoumission;
    private List<CandidateDocumentDto> documents;

    public CandidacyDto() {
    }

    public static CandidacyDto fromEntity(Candidacy candidacy) {
        CandidacyDto dto = new CandidacyDto();
        dto.setId(candidacy.getId());
        if (candidacy.getElection() != null) {
            dto.setElectionId(candidacy.getElection().getId());
            dto.setElectionTitre(candidacy.getElection().getTitre());
        }
        if (candidacy.getCandidat() != null) {
            dto.setCandidat(UserDto.fromEntity(candidacy.getCandidat()));
        }
        dto.setProfessionFoi(candidacy.getProfessionFoi());
        dto.setBiographie(candidacy.getBiographie());
        dto.setPhotoUrl(candidacy.getPhotoUrl());
        dto.setStatut(candidacy.getStatut());
        dto.setMotifDecision(candidacy.getMotifDecision());
        dto.setDateDecision(candidacy.getDateDecision());
        dto.setDateSoumission(candidacy.getDateSoumission());

        if (candidacy.getDocuments() != null) {
            dto.setDocuments(candidacy.getDocuments().stream()
                    .map(CandidateDocumentDto::fromEntity)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getElectionId() {
        return electionId;
    }

    public void setElectionId(Long electionId) {
        this.electionId = electionId;
    }

    public String getElectionTitre() {
        return electionTitre;
    }

    public void setElectionTitre(String electionTitre) {
        this.electionTitre = electionTitre;
    }

    public UserDto getCandidat() {
        return candidat;
    }

    public void setCandidat(UserDto candidat) {
        this.candidat = candidat;
    }

    public String getProfessionFoi() {
        return professionFoi;
    }

    public void setProfessionFoi(String professionFoi) {
        this.professionFoi = professionFoi;
    }

    public String getBiographie() {
        return biographie;
    }

    public void setBiographie(String biographie) {
        this.biographie = biographie;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public CandidacyStatus getStatut() {
        return statut;
    }

    public void setStatut(CandidacyStatus statut) {
        this.statut = statut;
    }

    public String getMotifDecision() {
        return motifDecision;
    }

    public void setMotifDecision(String motifDecision) {
        this.motifDecision = motifDecision;
    }

    public OffsetDateTime getDateDecision() {
        return dateDecision;
    }

    public void setDateDecision(OffsetDateTime dateDecision) {
        this.dateDecision = dateDecision;
    }

    public OffsetDateTime getDateSoumission() {
        return dateSoumission;
    }

    public void setDateSoumission(OffsetDateTime dateSoumission) {
        this.dateSoumission = dateSoumission;
    }

    public List<CandidateDocumentDto> getDocuments() {
        return documents;
    }

    public void setDocuments(List<CandidateDocumentDto> documents) {
        this.documents = documents;
    }
}
