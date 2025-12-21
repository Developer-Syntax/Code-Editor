package com.codeeditor.android.build;

public interface BuildTask {
    
    String getName();
    
    boolean execute(BuildContext context) throws BuildException;
    
    void cancel();
    
    default int getProgress() {
        return 0;
    }
    
    default String getProgressMessage() {
        return getName();
    }
}
