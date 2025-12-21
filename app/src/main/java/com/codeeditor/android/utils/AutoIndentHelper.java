package com.codeeditor.android.utils;

import com.codeeditor.android.syntax.SyntaxHighlighter;

public class AutoIndentHelper {
    
    private int tabSize = 4;
    private boolean useSpaces = true;
    
    public AutoIndentHelper() {}
    
    public AutoIndentHelper(int tabSize, boolean useSpaces) {
        this.tabSize = tabSize;
        this.useSpaces = useSpaces;
    }
    
    public void setTabSize(int tabSize) {
        this.tabSize = tabSize;
    }
    
    public void setUseSpaces(boolean useSpaces) {
        this.useSpaces = useSpaces;
    }
    
    public String getIndent() {
        if (useSpaces) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < tabSize; i++) {
                sb.append(' ');
            }
            return sb.toString();
        }
        return "\t";
    }
    
    public String getIndentForLine(String line) {
        StringBuilder indent = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == ' ' || c == '\t') {
                indent.append(c);
            } else {
                break;
            }
        }
        return indent.toString();
    }
    
    public int getIndentLevel(String line) {
        int spaces = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') {
                spaces++;
            } else if (c == '\t') {
                spaces += tabSize;
            } else {
                break;
            }
        }
        return spaces / tabSize;
    }
    
    public String calculateNewLineIndent(String text, int cursorPosition, SyntaxHighlighter.Language language) {
        if (text == null || cursorPosition <= 0) {
            return "";
        }
        
        int lineStart = text.lastIndexOf('\n', cursorPosition - 1) + 1;
        String currentLine = text.substring(lineStart, cursorPosition);
        
        String baseIndent = getIndentForLine(currentLine);
        String trimmedLine = currentLine.trim();
        
        boolean shouldIncrease = shouldIncreaseIndent(trimmedLine, language);
        
        if (shouldIncrease) {
            return baseIndent + getIndent();
        }
        
        return baseIndent;
    }
    
    private boolean shouldIncreaseIndent(String line, SyntaxHighlighter.Language language) {
        if (line.isEmpty()) {
            return false;
        }
        
        char lastChar = line.charAt(line.length() - 1);
        
        if (lastChar == '{' || lastChar == '[' || lastChar == '(') {
            return true;
        }
        
        switch (language) {
            case PYTHON:
                return line.endsWith(":");
                
            case RUBY:
                return line.endsWith("do") || 
                       line.startsWith("def ") || 
                       line.startsWith("class ") ||
                       line.startsWith("module ") ||
                       line.startsWith("if ") ||
                       line.startsWith("elsif ") ||
                       line.startsWith("else") ||
                       line.startsWith("unless ") ||
                       line.startsWith("while ") ||
                       line.startsWith("until ") ||
                       line.startsWith("for ") ||
                       line.startsWith("begin") ||
                       line.startsWith("case ");
                       
            case LUA:
                return line.startsWith("function ") ||
                       line.startsWith("if ") ||
                       line.startsWith("elseif ") ||
                       line.startsWith("else") ||
                       line.startsWith("for ") ||
                       line.startsWith("while ") ||
                       line.startsWith("repeat") ||
                       line.endsWith(" do") ||
                       line.endsWith(" then");
                       
            case SHELL:
                return line.endsWith(" then") ||
                       line.endsWith(" do") ||
                       line.startsWith("else") ||
                       line.startsWith("elif ");
                       
            default:
                return false;
        }
    }
    
    public String calculateClosingBracketIndent(String text, int cursorPosition) {
        if (text == null || cursorPosition <= 0) {
            return "";
        }
        
        int lineStart = text.lastIndexOf('\n', cursorPosition - 1) + 1;
        String currentLine = text.substring(lineStart, cursorPosition);
        String trimmed = currentLine.trim();
        
        if (trimmed.equals("}") || trimmed.equals("]") || trimmed.equals(")")) {
            String indent = getIndentForLine(currentLine);
            if (indent.length() >= tabSize) {
                if (useSpaces) {
                    return indent.substring(0, indent.length() - tabSize);
                } else if (indent.length() > 0) {
                    return indent.substring(0, indent.length() - 1);
                }
            }
        }
        
        return null;
    }
    
    public String formatOnPaste(String pastedText, String existingIndent) {
        if (pastedText == null || pastedText.isEmpty()) {
            return pastedText;
        }
        
        String[] lines = pastedText.split("\n", -1);
        if (lines.length <= 1) {
            return pastedText;
        }
        
        StringBuilder result = new StringBuilder(lines[0]);
        
        for (int i = 1; i < lines.length; i++) {
            result.append("\n");
            String line = lines[i];
            String trimmedLine = line.replaceFirst("^\\s+", "");
            if (!trimmedLine.isEmpty()) {
                result.append(existingIndent).append(trimmedLine);
            }
        }
        
        return result.toString();
    }
    
    public String increaseIndent(String selectedText) {
        if (selectedText == null || selectedText.isEmpty()) {
            return getIndent();
        }
        
        String[] lines = selectedText.split("\n", -1);
        StringBuilder result = new StringBuilder();
        String indent = getIndent();
        
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                result.append("\n");
            }
            result.append(indent).append(lines[i]);
        }
        
        return result.toString();
    }
    
    public String decreaseIndent(String selectedText) {
        if (selectedText == null || selectedText.isEmpty()) {
            return selectedText;
        }
        
        String[] lines = selectedText.split("\n", -1);
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                result.append("\n");
            }
            
            String line = lines[i];
            if (useSpaces) {
                if (line.startsWith(getIndent())) {
                    line = line.substring(tabSize);
                } else {
                    int spaces = 0;
                    while (spaces < line.length() && spaces < tabSize && line.charAt(spaces) == ' ') {
                        spaces++;
                    }
                    line = line.substring(spaces);
                }
            } else {
                if (line.startsWith("\t")) {
                    line = line.substring(1);
                }
            }
            result.append(line);
        }
        
        return result.toString();
    }
}
