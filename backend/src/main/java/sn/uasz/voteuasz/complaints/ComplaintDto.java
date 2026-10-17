package sn.uasz.voteuasz.complaints;

import sn.uasz.voteuasz.users.UserDto;

import java.time.OffsetDateTime;

public class ComplaintDto {

    private Long id;
    private Long electionId;
    private String electionTitre;
    private UserDto user;
    private String objet;
    private String description;
    private ComplaintStatus statut;
    private String reponseCommission;
    private String piecesJointesUrls;
    private OffsetDateTime dateDepot;
    private OffsetDateTime dateTraitement;

    public ComplaintDto() {
    }

    public static ComplaintDto fromEntity(Complaint c) {
        ComplaintDto dto = new ComplaintDto();
        dto.setId(c.getId());
        if (c.getElection() != null) {
            dto.setElectionId(c.getElection().getId());
            dto.setElectionTitre(c.getElection().getTitre());
        }
        if (c.getUser() != null) {
            dto.setUser(UserDto.fromEntity(c.getUser()));
        }
        dto.setObjet(c.getObjet());
        dto.setDescription(c.getDescription());
        dto.setStatut(c.getStatut());
        dto.setReponseCommission(c.getReponseCommission());
        dto.setPiecesJointesUrls(c.getPiecesJointesUrls());
        dto.setDateDepot(c.getDateDepot());
        dto.setDateTraitement(c.getDateTraitement());
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

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }

    public String getObjet() {
        return objet;
    }

    public void setObjet(String objet) {
        this.objet = objet;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ComplaintStatus getStatut() {
        return statut;
    }

    public void setStatut(ComplaintStatus statut) {
        this.statut = statut;
    }

    public String getReponseCommission() {
        return reponseCommission;
    }

    public void setReponseCommission(String reponseCommission) {
        this.reponseCommission = reponseCommission;
    }

    public String getPiecesJointesUrls() {
        return piecesJointesUrls;
    }

    public void setPiecesJointesUrls(String piecesJointesUrls) {
        this.piecesJointesUrls = piecesJointesUrls;
    }

    public OffsetDateTime getDateDepot() {
        return dateDepot;
    }

    public void setDateDepot(OffsetDateTime dateDepot) {
        this.dateDepot = dateDepot;
    }

    public OffsetDateTime getDateTraitement() {
        return dateTraitement;
    }

    public void setDateTraitement(OffsetDateTime dateTraitement) {
        this.dateTraitement = dateTraitement;
    }
}
