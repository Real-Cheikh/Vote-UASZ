package sn.uasz.voteuasz.results;

public class CandidateResultDto {

    private Long candidacyId;
    private Long candidateUserId;
    private String candidateNom;
    private String candidatePrenom;
    private String photoUrl;
    private long voixObtenues;
    private double pourcentage;

    public CandidateResultDto() {
    }

    public CandidateResultDto(Long candidacyId, Long candidateUserId, String candidateNom, String candidatePrenom,
                              String photoUrl, long voixObtenues, double pourcentage) {
        this.candidacyId = candidacyId;
        this.candidateUserId = candidateUserId;
        this.candidateNom = candidateNom;
        this.candidatePrenom = candidatePrenom;
        this.photoUrl = photoUrl;
        this.voixObtenues = voixObtenues;
        this.pourcentage = pourcentage;
    }

    public Long getCandidacyId() {
        return candidacyId;
    }

    public void setCandidacyId(Long candidacyId) {
        this.candidacyId = candidacyId;
    }

    public Long getCandidateUserId() {
        return candidateUserId;
    }

    public void setCandidateUserId(Long candidateUserId) {
        this.candidateUserId = candidateUserId;
    }

    public String getCandidateNom() {
        return candidateNom;
    }

    public void setCandidateNom(String candidateNom) {
        this.candidateNom = candidateNom;
    }

    public String getCandidatePrenom() {
        return candidatePrenom;
    }

    public void setCandidatePrenom(String candidatePrenom) {
        this.candidatePrenom = candidatePrenom;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public long getVoixObtenues() {
        return voixObtenues;
    }

    public void setVoixObtenues(long voixObtenues) {
        this.voixObtenues = voixObtenues;
    }

    public double getPourcentage() {
        return pourcentage;
    }

    public void setPourcentage(double pourcentage) {
        this.pourcentage = pourcentage;
    }
}
