package com.codeeditor.android.utils;

import java.util.ArrayList;
import java.util.List;

public class UndoRedoManager {
    
    private static final int MAX_HISTORY_SIZE = 100;
    
    private final List<TextState> history = new ArrayList<>();
    private int currentIndex = -1;
    private boolean isPerformingUndoRedo = false;
    
    public static class TextState {
        public final String text;
        public final int cursorPosition;
        
        public TextState(String text, int cursorPosition) {
            this.text = text;
            this.cursorPosition = cursorPosition;
        }
    }
    
    public void saveState(String text, int cursorPosition) {
        if (isPerformingUndoRedo) {
            return;
        }
        
        if (currentIndex >= 0 && currentIndex < history.size()) {
            TextState lastState = history.get(currentIndex);
            if (lastState.text.equals(text)) {
                return;
            }
        }
        
        while (history.size() > currentIndex + 1) {
            history.remove(history.size() - 1);
        }
        
        history.add(new TextState(text, cursorPosition));
        currentIndex = history.size() - 1;
        
        while (history.size() > MAX_HISTORY_SIZE) {
            history.remove(0);
            currentIndex--;
        }
    }
    
    public TextState undo() {
        if (!canUndo()) {
            return null;
        }
        
        isPerformingUndoRedo = true;
        currentIndex--;
        TextState state = history.get(currentIndex);
        isPerformingUndoRedo = false;
        
        return state;
    }
    
    public TextState redo() {
        if (!canRedo()) {
            return null;
        }
        
        isPerformingUndoRedo = true;
        currentIndex++;
        TextState state = history.get(currentIndex);
        isPerformingUndoRedo = false;
        
        return state;
    }
    
    public boolean canUndo() {
        return currentIndex > 0;
    }
    
    public boolean canRedo() {
        return currentIndex < history.size() - 1;
    }
    
    public void clear() {
        history.clear();
        currentIndex = -1;
    }
}
