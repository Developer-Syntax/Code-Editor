package com.codeeditor.android.build;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BuildResult {
    
    public enum Status {
        SUCCESS,
        FAILED,
        CANCELLED
    }
    
    private Status status;
    private File outputApk;
    private long buildTimeMs;
    private List<String> errors;
    private List<String> warnings;
    private String failureMessage;
    
    public BuildResult() {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }
    
    public static BuildResult success(File apk, long buildTimeMs) {
        BuildResult result = new BuildResult();
        result.status = Status.SUCCESS;
        result.outputApk = apk;
        result.buildTimeMs = buildTimeMs;
        return result;
    }
    
    public static BuildResult failed(String message) {
        BuildResult result = new BuildResult();
        result.status = Status.FAILED;
        result.failureMessage = message;
        return result;
    }
    
    public static BuildResult failed(String message, List<String> errors) {
        BuildResult result = failed(message);
        result.errors.addAll(errors);
        return result;
    }
    
    public static BuildResult cancelled() {
        BuildResult result = new BuildResult();
        result.status = Status.CANCELLED;
        return result;
    }
    
    public void addError(String error) {
        errors.add(error);
    }
    
    public void addWarning(String warning) {
        warnings.add(warning);
    }
    
    public Status getStatus() { return status; }
    public File getOutputApk() { return outputApk; }
    public long getBuildTimeMs() { return buildTimeMs; }
    public List<String> getErrors() { return new ArrayList<>(errors); }
    public List<String> getWarnings() { return new ArrayList<>(warnings); }
    public String getFailureMessage() { return failureMessage; }
    
    public boolean isSuccess() { return status == Status.SUCCESS; }
    public boolean isFailed() { return status == Status.FAILED; }
    public boolean isCancelled() { return status == Status.CANCELLED; }
    
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Build ").append(status.name());
        
        if (isSuccess()) {
            sb.append(" in ").append(buildTimeMs / 1000.0).append("s");
            if (outputApk != null) {
                sb.append("\nOutput: ").append(outputApk.getName());
                sb.append(" (").append(outputApk.length() / 1024).append(" KB)");
            }
        } else if (isFailed()) {
            sb.append(": ").append(failureMessage);
            if (!errors.isEmpty()) {
                sb.append("\n\nErrors:\n");
                for (String error : errors) {
                    sb.append("  - ").append(error).append("\n");
                }
            }
        }
        
        if (!warnings.isEmpty()) {
            sb.append("\n\nWarnings:\n");
            for (String warning : warnings) {
                sb.append("  - ").append(warning).append("\n");
            }
        }
        
        return sb.toString();
    }
}
