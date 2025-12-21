package com.codeeditor.android.theme;

public class EditorTheme {
    
    private final String name;
    
    public final int background;
    public final int foreground;
    public final int lineNumberBackground;
    public final int lineNumberForeground;
    public final int currentLine;
    public final int selection;
    public final int cursor;
    
    public final int keyword;
    public final int string;
    public final int number;
    public final int comment;
    public final int function;
    public final int className;
    public final int variable;
    public final int operator;
    public final int annotation;
    public final int tag;
    public final int attribute;
    public final int constant;
    
    public final int toolbarBackground;
    public final int statusBarBackground;
    public final int tabBackground;
    public final int tabActiveBackground;
    public final int sidebarBackground;
    
    private EditorTheme(Builder builder) {
        this.name = builder.name;
        this.background = builder.background;
        this.foreground = builder.foreground;
        this.lineNumberBackground = builder.lineNumberBackground;
        this.lineNumberForeground = builder.lineNumberForeground;
        this.currentLine = builder.currentLine;
        this.selection = builder.selection;
        this.cursor = builder.cursor;
        this.keyword = builder.keyword;
        this.string = builder.string;
        this.number = builder.number;
        this.comment = builder.comment;
        this.function = builder.function;
        this.className = builder.className;
        this.variable = builder.variable;
        this.operator = builder.operator;
        this.annotation = builder.annotation;
        this.tag = builder.tag;
        this.attribute = builder.attribute;
        this.constant = builder.constant;
        this.toolbarBackground = builder.toolbarBackground;
        this.statusBarBackground = builder.statusBarBackground;
        this.tabBackground = builder.tabBackground;
        this.tabActiveBackground = builder.tabActiveBackground;
        this.sidebarBackground = builder.sidebarBackground;
    }
    
    public String getName() {
        return name;
    }
    
    public static class Builder {
        private String name;
        private int background;
        private int foreground;
        private int lineNumberBackground;
        private int lineNumberForeground;
        private int currentLine;
        private int selection;
        private int cursor;
        private int keyword;
        private int string;
        private int number;
        private int comment;
        private int function;
        private int className;
        private int variable;
        private int operator;
        private int annotation;
        private int tag;
        private int attribute;
        private int constant;
        private int toolbarBackground;
        private int statusBarBackground;
        private int tabBackground;
        private int tabActiveBackground;
        private int sidebarBackground;
        
        public Builder(String name) {
            this.name = name;
        }
        
        public Builder setBackground(int color) { this.background = color; return this; }
        public Builder setForeground(int color) { this.foreground = color; return this; }
        public Builder setLineNumberBackground(int color) { this.lineNumberBackground = color; return this; }
        public Builder setLineNumberForeground(int color) { this.lineNumberForeground = color; return this; }
        public Builder setCurrentLine(int color) { this.currentLine = color; return this; }
        public Builder setSelection(int color) { this.selection = color; return this; }
        public Builder setCursor(int color) { this.cursor = color; return this; }
        public Builder setKeyword(int color) { this.keyword = color; return this; }
        public Builder setString(int color) { this.string = color; return this; }
        public Builder setNumber(int color) { this.number = color; return this; }
        public Builder setComment(int color) { this.comment = color; return this; }
        public Builder setFunction(int color) { this.function = color; return this; }
        public Builder setClassName(int color) { this.className = color; return this; }
        public Builder setVariable(int color) { this.variable = color; return this; }
        public Builder setOperator(int color) { this.operator = color; return this; }
        public Builder setAnnotation(int color) { this.annotation = color; return this; }
        public Builder setTag(int color) { this.tag = color; return this; }
        public Builder setAttribute(int color) { this.attribute = color; return this; }
        public Builder setConstant(int color) { this.constant = color; return this; }
        public Builder setToolbarBackground(int color) { this.toolbarBackground = color; return this; }
        public Builder setStatusBarBackground(int color) { this.statusBarBackground = color; return this; }
        public Builder setTabBackground(int color) { this.tabBackground = color; return this; }
        public Builder setTabActiveBackground(int color) { this.tabActiveBackground = color; return this; }
        public Builder setSidebarBackground(int color) { this.sidebarBackground = color; return this; }
        
        public EditorTheme build() {
            return new EditorTheme(this);
        }
    }
}
