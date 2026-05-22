package com.hospital.portal.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class ErrorResponseDTO {

    private int status;
    private String error;
    private String message;
    private String path;
    private LocalDateTime timestamp;
    private List<String> details;

    public ErrorResponseDTO() { this.timestamp = LocalDateTime.now(); }

    public ErrorResponseDTO(int status, String error, String message, String path) {
        this();
        this.status  = status;
        this.error   = error;
        this.message = message;
        this.path    = path;
    }

    public int getStatus()              { return status; }
    public String getError()            { return error; }
    public String getMessage()          { return message; }
    public String getPath()             { return path; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public List<String> getDetails()    { return details; }

    public void setStatus(int v)           { this.status = v; }
    public void setError(String v)         { this.error = v; }
    public void setMessage(String v)       { this.message = v; }
    public void setPath(String v)          { this.path = v; }
    public void setTimestamp(LocalDateTime v){ this.timestamp = v; }
    public void setDetails(List<String> v) { this.details = v; }
}
