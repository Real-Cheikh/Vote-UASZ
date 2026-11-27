package sn.uasz.voteuasz.otp;

import java.time.OffsetDateTime;

public class OtpChallengeResponseDto {

    private String message;
    private OffsetDateTime dateExpiration;
    private int validiteSecondes;
    private String destinationMasquee; // Ex: m***@zig.univ.sn
    private String mode;               // "SIMULATION" ou "EMAIL"
    private String simulationCode;     // Code clair disponible exclusivement en mode démo / simulation

    public OtpChallengeResponseDto() {
    }

    public OtpChallengeResponseDto(String message, OffsetDateTime dateExpiration, int validiteSecondes, String destinationMasquee) {
        this(message, dateExpiration, validiteSecondes, destinationMasquee, "EMAIL", null);
    }

    public OtpChallengeResponseDto(String message, OffsetDateTime dateExpiration, int validiteSecondes,
                                   String destinationMasquee, String mode, String simulationCode) {
        this.message = message;
        this.dateExpiration = dateExpiration;
        this.validiteSecondes = validiteSecondes;
        this.destinationMasquee = destinationMasquee;
        this.mode = mode;
        this.simulationCode = simulationCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OffsetDateTime getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(OffsetDateTime dateExpiration) {
        this.dateExpiration = dateExpiration;
    }

    public int getValiditeSecondes() {
        return validiteSecondes;
    }

    public void setValiditeSecondes(int validiteSecondes) {
        this.validiteSecondes = validiteSecondes;
    }

    public String getDestinationMasquee() {
        return destinationMasquee;
    }

    public void setDestinationMasquee(String destinationMasquee) {
        this.destinationMasquee = destinationMasquee;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getSimulationCode() {
        return simulationCode;
    }

    public void setSimulationCode(String simulationCode) {
        this.simulationCode = simulationCode;
    }
}
