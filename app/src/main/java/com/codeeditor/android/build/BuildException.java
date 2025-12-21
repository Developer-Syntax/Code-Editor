package com.codeeditor.android.build;

public class BuildException extends Exception {
    
    private String phase;
    private String details;
    
    public BuildException(String message) {
        super(message);
    }
    
    public BuildException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public BuildException(String phase, String message) {
        super(message);
        this.phase = phase;
    }
    
    public BuildException(String phase, String message, Throwable cause) {
        super(message, cause);
        this.phase = phase;
    }
    
    public BuildException(String phase, String message, String details) {
        super(message);
        this.phase = phase;
        this.details = details;
    }
    
    public String getPhase() {
        return phase;
    }
    
    public String getDetails() {
        return details;
    }
    
    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        if (phase != null) {
            sb.append("[").append(phase).append("] ");
        }
        sb.append(super.getMessage());
        if (details != null) {
            sb.append("\n").append(details);
        }
        return sb.toString();
    }
}
