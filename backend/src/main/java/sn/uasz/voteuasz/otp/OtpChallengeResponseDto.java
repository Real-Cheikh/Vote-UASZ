package sn.uasz.voteuasz.otp;

import java.time.OffsetDateTime;

public class OtpChallengeResponseDto {

    private String message;
    private OffsetDateTime dateExpiration;
    private int validiteSecondes;
    private String destinationMasquee; // Ex: m***@zig.univ.sn

    public OtpChallengeResponseDto() {
    }

    public OtpChallengeResponseDto(String message, OffsetDateTime dateExpiration, int validiteSecondes, String destinationMasquee) {
        this.message = message;
        this.dateExpiration = dateExpiration;
        this.validiteSecondes = validiteSecondes;
        this.destinationMasquee = destinationMasquee;
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
}
