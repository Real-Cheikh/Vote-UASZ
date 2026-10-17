package sn.uasz.voteuasz.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private boolean success = false;
    private int status;
    private String error;
    private String message;
    private Map<String, String> validationErrors;
    private LocalDateTime timestamp;

    public ErrorResponse(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(int status, String error, String message, Map<String, String> validationErrors) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.validationErrors = validationErrors;
        this.timestamp = LocalDateTime.now();
    }

    public boolean isSuccess() {
        return success;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
