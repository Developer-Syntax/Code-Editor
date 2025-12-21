package com.codeeditor.android.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.codeeditor.android.syntax.SyntaxHighlighter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SnippetManager {

    private static final String PREFS_NAME = "snippets_prefs";
    private static final String KEY_CUSTOM_SNIPPETS = "custom_snippets";

    public static class Snippet {
        public String name;
        public String trigger;
        public String code;
        public String description;
        public SyntaxHighlighter.Language language;
        public boolean isCustom;

        public Snippet(String name, String trigger, String code, String description, 
                       SyntaxHighlighter.Language language, boolean isCustom) {
            this.name = name;
            this.trigger = trigger;
            this.code = code;
            this.description = description;
            this.language = language;
            this.isCustom = isCustom;
        }
    }

    private SharedPreferences prefs;
    private Gson gson;
    private Map<SyntaxHighlighter.Language, List<Snippet>> customSnippets;

    public SnippetManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        loadCustomSnippets();
    }

    private void loadCustomSnippets() {
        String json = prefs.getString(KEY_CUSTOM_SNIPPETS, "{}");
        Type type = new TypeToken<HashMap<String, ArrayList<Snippet>>>(){}.getType();
        Map<String, ArrayList<Snippet>> raw = gson.fromJson(json, type);
        
        customSnippets = new HashMap<>();
        if (raw != null) {
            for (Map.Entry<String, ArrayList<Snippet>> entry : raw.entrySet()) {
                try {
                    SyntaxHighlighter.Language lang = SyntaxHighlighter.Language.valueOf(entry.getKey());
                    customSnippets.put(lang, entry.getValue());
                } catch (IllegalArgumentException e) {
                }
            }
        }
    }

    public void saveCustomSnippet(Snippet snippet) {
        List<Snippet> langSnippets = customSnippets.getOrDefault(snippet.language, new ArrayList<>());
        
        langSnippets.removeIf(s -> s.trigger.equals(snippet.trigger));
        langSnippets.add(snippet);
        customSnippets.put(snippet.language, langSnippets);
        
        saveAllSnippets();
    }

    public void deleteCustomSnippet(SyntaxHighlighter.Language language, String trigger) {
        List<Snippet> langSnippets = customSnippets.get(language);
        if (langSnippets != null) {
            langSnippets.removeIf(s -> s.trigger.equals(trigger));
            saveAllSnippets();
        }
    }

    public List<Snippet> getCustomSnippets(SyntaxHighlighter.Language language) {
        return customSnippets.getOrDefault(language, new ArrayList<>());
    }

    private void saveAllSnippets() {
        Map<String, List<Snippet>> raw = new HashMap<>();
        for (Map.Entry<SyntaxHighlighter.Language, List<Snippet>> entry : customSnippets.entrySet()) {
            raw.put(entry.getKey().name(), entry.getValue());
        }
        String json = gson.toJson(raw);
        prefs.edit().putString(KEY_CUSTOM_SNIPPETS, json).apply();
    }

    public static List<Snippet> getBuiltInSnippets(SyntaxHighlighter.Language language) {
        List<Snippet> snippets = new ArrayList<>();

        switch (language) {
            case JAVA:
                snippets.add(new Snippet("Main Method", "main", 
                    "public static void main(String[] args) {\n    \n}", 
                    "Java main method", language, false));
                snippets.add(new Snippet("For Loop", "for",
                    "for (int i = 0; i < length; i++) {\n    \n}",
                    "For loop", language, false));
                snippets.add(new Snippet("For Each", "foreach",
                    "for (Type item : collection) {\n    \n}",
                    "Enhanced for loop", language, false));
                snippets.add(new Snippet("Try Catch", "try",
                    "try {\n    \n} catch (Exception e) {\n    e.printStackTrace();\n}",
                    "Try-catch block", language, false));
                snippets.add(new Snippet("If Statement", "if",
                    "if (condition) {\n    \n}",
                    "If statement", language, false));
                snippets.add(new Snippet("Print Line", "sout",
                    "System.out.println();",
                    "Print to console", language, false));
                break;

            case KOTLIN:
                snippets.add(new Snippet("Function", "fun",
                    "fun functionName(): Unit {\n    \n}",
                    "Kotlin function", language, false));
                snippets.add(new Snippet("Data Class", "data",
                    "data class ClassName(val property: Type)",
                    "Data class", language, false));
                snippets.add(new Snippet("When Expression", "when",
                    "when (value) {\n    condition -> result\n    else -> default\n}",
                    "When expression", language, false));
                snippets.add(new Snippet("Coroutine", "launch",
                    "lifecycleScope.launch {\n    \n}",
                    "Launch coroutine", language, false));
                break;

            case PYTHON:
                snippets.add(new Snippet("Function", "def",
                    "def function_name():\n    pass",
                    "Function definition", language, false));
                snippets.add(new Snippet("Class", "class",
                    "class ClassName:\n    def __init__(self):\n        pass",
                    "Class definition", language, false));
                snippets.add(new Snippet("Main Guard", "main",
                    "if __name__ == \"__main__\":\n    ",
                    "Main guard", language, false));
                snippets.add(new Snippet("List Comprehension", "listcomp",
                    "[item for item in iterable]",
                    "List comprehension", language, false));
                break;

            case JAVASCRIPT:
            case TYPESCRIPT:
                snippets.add(new Snippet("Arrow Function", "arrow",
                    "const functionName = () => {\n    \n};",
                    "Arrow function", language, false));
                snippets.add(new Snippet("Async Function", "async",
                    "async function functionName() {\n    \n}",
                    "Async function", language, false));
                snippets.add(new Snippet("Console Log", "log",
                    "console.log();",
                    "Console log", language, false));
                snippets.add(new Snippet("Import", "import",
                    "import { module } from 'package';",
                    "ES6 import", language, false));
                snippets.add(new Snippet("React Component", "rfc",
                    "function ComponentName() {\n    return (\n        <div>\n            \n        </div>\n    );\n}\n\nexport default ComponentName;",
                    "React functional component", language, false));
                break;

            case HTML:
                snippets.add(new Snippet("HTML5 Template", "html",
                    "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n    <meta charset=\"UTF-8\">\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n    <title>Document</title>\n</head>\n<body>\n    \n</body>\n</html>",
                    "HTML5 boilerplate", language, false));
                snippets.add(new Snippet("Div with Class", "div",
                    "<div class=\"\"></div>",
                    "Div element", language, false));
                break;

            case CSS:
                snippets.add(new Snippet("Flexbox Center", "flex",
                    "display: flex;\njustify-content: center;\nalign-items: center;",
                    "Flexbox centering", language, false));
                snippets.add(new Snippet("Grid Layout", "grid",
                    "display: grid;\ngrid-template-columns: repeat(3, 1fr);\ngap: 1rem;",
                    "CSS Grid", language, false));
                snippets.add(new Snippet("Media Query", "media",
                    "@media (max-width: 768px) {\n    \n}",
                    "Responsive media query", language, false));
                break;

            default:
                break;
        }

        return snippets;
    }
}
