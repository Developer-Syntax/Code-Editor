package com.codeeditor.android.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class BracketMatcher {
    
    private static final Map<Character, Character> BRACKETS = new HashMap<>();
    private static final Map<Character, Character> QUOTES = new HashMap<>();
    
    static {
        BRACKETS.put('(', ')');
        BRACKETS.put('[', ']');
        BRACKETS.put('{', '}');
        BRACKETS.put('<', '>');
        
        QUOTES.put('"', '"');
        QUOTES.put('\'', '\'');
        QUOTES.put('`', '`');
    }
    
    public static boolean isOpenBracket(char c) {
        return BRACKETS.containsKey(c);
    }
    
    public static boolean isCloseBracket(char c) {
        return BRACKETS.containsValue(c);
    }
    
    public static boolean isQuote(char c) {
        return QUOTES.containsKey(c);
    }
    
    public static char getClosingBracket(char openBracket) {
        return BRACKETS.getOrDefault(openBracket, '\0');
    }
    
    public static char getOpeningBracket(char closeBracket) {
        for (Map.Entry<Character, Character> entry : BRACKETS.entrySet()) {
            if (entry.getValue() == closeBracket) {
                return entry.getKey();
            }
        }
        return '\0';
    }
    
    public static int findMatchingBracket(String text, int position) {
        if (text == null || position < 0 || position >= text.length()) {
            return -1;
        }
        
        char bracket = text.charAt(position);
        
        if (isOpenBracket(bracket)) {
            return findClosingBracket(text, position, bracket, BRACKETS.get(bracket));
        } else if (isCloseBracket(bracket)) {
            char openBracket = getOpeningBracket(bracket);
            return findOpeningBracket(text, position, openBracket, bracket);
        }
        
        return -1;
    }
    
    private static int findClosingBracket(String text, int start, char open, char close) {
        Stack<Integer> stack = new Stack<>();
        boolean inString = false;
        char stringChar = '\0';
        
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (!inString && isQuote(c)) {
                inString = true;
                stringChar = c;
            } else if (inString && c == stringChar && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = false;
            }
            
            if (!inString) {
                if (c == open) {
                    stack.push(i);
                } else if (c == close) {
                    if (!stack.isEmpty()) {
                        stack.pop();
                        if (stack.isEmpty()) {
                            return i;
                        }
                    }
                }
            }
        }
        
        return -1;
    }
    
    private static int findOpeningBracket(String text, int start, char open, char close) {
        Stack<Integer> stack = new Stack<>();
        boolean inString = false;
        char stringChar = '\0';
        
        for (int i = start; i >= 0; i--) {
            char c = text.charAt(i);
            
            if (!inString && isQuote(c)) {
                boolean escaped = i > 0 && text.charAt(i - 1) == '\\';
                if (!escaped) {
                    inString = true;
                    stringChar = c;
                }
            } else if (inString && c == stringChar) {
                boolean escaped = i > 0 && text.charAt(i - 1) == '\\';
                if (!escaped) {
                    inString = false;
                }
            }
            
            if (!inString) {
                if (c == close) {
                    stack.push(i);
                } else if (c == open) {
                    if (!stack.isEmpty()) {
                        stack.pop();
                        if (stack.isEmpty()) {
                            return i;
                        }
                    }
                }
            }
        }
        
        return -1;
    }
    
    public static String getAutoClosePair(char c) {
        if (BRACKETS.containsKey(c)) {
            return String.valueOf(c) + BRACKETS.get(c);
        }
        if (QUOTES.containsKey(c)) {
            return String.valueOf(c) + QUOTES.get(c);
        }
        return null;
    }
    
    public static boolean shouldAutoClose(String text, int position, char c) {
        if (!isOpenBracket(c) && !isQuote(c)) {
            return false;
        }
        
        if (position < text.length()) {
            char nextChar = text.charAt(position);
            if (!Character.isWhitespace(nextChar) && 
                nextChar != ')' && nextChar != ']' && nextChar != '}' && 
                nextChar != '>' && nextChar != ';' && nextChar != ',') {
                return false;
            }
        }
        
        if (isQuote(c)) {
            int quoteCount = 0;
            for (int i = 0; i < position; i++) {
                if (text.charAt(i) == c) {
                    if (i == 0 || text.charAt(i - 1) != '\\') {
                        quoteCount++;
                    }
                }
            }
            if (quoteCount % 2 != 0) {
                return false;
            }
        }
        
        return true;
    }
    
    public static boolean isValidBrackets(String text) {
        Stack<Character> stack = new Stack<>();
        boolean inString = false;
        char stringChar = '\0';
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (!inString && isQuote(c)) {
                inString = true;
                stringChar = c;
            } else if (inString && c == stringChar && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = false;
            }
            
            if (!inString) {
                if (isOpenBracket(c)) {
                    stack.push(c);
                } else if (isCloseBracket(c)) {
                    if (stack.isEmpty()) {
                        return false;
                    }
                    char open = getOpeningBracket(c);
                    if (stack.peek() != open) {
                        return false;
                    }
                    stack.pop();
                }
            }
        }
        
        return stack.isEmpty();
    }
}
