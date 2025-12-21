package com.codeeditor.android.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class FindReplaceHelper {
    
    private String text;
    private List<Match> matches;
    private int currentMatchIndex;
    
    public static class Match {
        public final int start;
        public final int end;
        public final String text;
        
        public Match(int start, int end, String text) {
            this.start = start;
            this.end = end;
            this.text = text;
        }
    }
    
    public static class FindOptions {
        public boolean caseSensitive = false;
        public boolean useRegex = false;
        public boolean wholeWord = false;
        
        public FindOptions() {}
        
        public FindOptions(boolean caseSensitive, boolean useRegex, boolean wholeWord) {
            this.caseSensitive = caseSensitive;
            this.useRegex = useRegex;
            this.wholeWord = wholeWord;
        }
    }
    
    public FindReplaceHelper() {
        this.matches = new ArrayList<>();
        this.currentMatchIndex = -1;
    }
    
    public void setText(String text) {
        this.text = text;
        this.matches.clear();
        this.currentMatchIndex = -1;
    }
    
    public List<Match> find(String query, FindOptions options) {
        matches.clear();
        currentMatchIndex = -1;
        
        if (text == null || text.isEmpty() || query == null || query.isEmpty()) {
            return matches;
        }
        
        try {
            String pattern = query;
            
            if (!options.useRegex) {
                pattern = Pattern.quote(query);
            }
            
            if (options.wholeWord) {
                pattern = "\\b" + pattern + "\\b";
            }
            
            int flags = Pattern.MULTILINE;
            if (!options.caseSensitive) {
                flags |= Pattern.CASE_INSENSITIVE;
            }
            
            Pattern regex = Pattern.compile(pattern, flags);
            Matcher matcher = regex.matcher(text);
            
            while (matcher.find()) {
                matches.add(new Match(matcher.start(), matcher.end(), matcher.group()));
            }
            
            if (!matches.isEmpty()) {
                currentMatchIndex = 0;
            }
            
        } catch (PatternSyntaxException e) {
            e.printStackTrace();
        }
        
        return matches;
    }
    
    public Match findNext() {
        if (matches.isEmpty()) {
            return null;
        }
        
        currentMatchIndex++;
        if (currentMatchIndex >= matches.size()) {
            currentMatchIndex = 0;
        }
        
        return matches.get(currentMatchIndex);
    }
    
    public Match findPrevious() {
        if (matches.isEmpty()) {
            return null;
        }
        
        currentMatchIndex--;
        if (currentMatchIndex < 0) {
            currentMatchIndex = matches.size() - 1;
        }
        
        return matches.get(currentMatchIndex);
    }
    
    public Match getCurrentMatch() {
        if (matches.isEmpty() || currentMatchIndex < 0 || currentMatchIndex >= matches.size()) {
            return null;
        }
        return matches.get(currentMatchIndex);
    }
    
    public int getCurrentMatchIndex() {
        return currentMatchIndex;
    }
    
    public int getMatchCount() {
        return matches.size();
    }
    
    public String replace(String replacement) {
        if (text == null || matches.isEmpty() || currentMatchIndex < 0) {
            return text;
        }
        
        Match match = matches.get(currentMatchIndex);
        StringBuilder sb = new StringBuilder(text);
        sb.replace(match.start, match.end, replacement);
        
        int diff = replacement.length() - (match.end - match.start);
        for (int i = currentMatchIndex + 1; i < matches.size(); i++) {
            Match m = matches.get(i);
            matches.set(i, new Match(m.start + diff, m.end + diff, m.text));
        }
        
        matches.remove(currentMatchIndex);
        
        if (!matches.isEmpty()) {
            if (currentMatchIndex >= matches.size()) {
                currentMatchIndex = 0;
            }
        } else {
            currentMatchIndex = -1;
        }
        
        text = sb.toString();
        return text;
    }
    
    public String replaceAll(String query, String replacement, FindOptions options) {
        if (text == null || query == null || query.isEmpty()) {
            return text;
        }
        
        try {
            String pattern = query;
            
            if (!options.useRegex) {
                pattern = Pattern.quote(query);
            }
            
            if (options.wholeWord) {
                pattern = "\\b" + pattern + "\\b";
            }
            
            int flags = Pattern.MULTILINE;
            if (!options.caseSensitive) {
                flags |= Pattern.CASE_INSENSITIVE;
            }
            
            Pattern regex = Pattern.compile(pattern, flags);
            text = regex.matcher(text).replaceAll(
                options.useRegex ? replacement : Matcher.quoteReplacement(replacement)
            );
            
            matches.clear();
            currentMatchIndex = -1;
            
        } catch (PatternSyntaxException e) {
            e.printStackTrace();
        }
        
        return text;
    }
    
    public Match findNearestMatch(int cursorPosition) {
        if (matches.isEmpty()) {
            return null;
        }
        
        int nearestIndex = 0;
        int minDistance = Integer.MAX_VALUE;
        
        for (int i = 0; i < matches.size(); i++) {
            Match match = matches.get(i);
            int distance = Math.abs(match.start - cursorPosition);
            if (distance < minDistance) {
                minDistance = distance;
                nearestIndex = i;
            }
        }
        
        currentMatchIndex = nearestIndex;
        return matches.get(nearestIndex);
    }
    
    public void setCurrentMatchByPosition(int position) {
        for (int i = 0; i < matches.size(); i++) {
            Match match = matches.get(i);
            if (position >= match.start && position <= match.end) {
                currentMatchIndex = i;
                return;
            }
        }
    }
}
