package com.codeeditor.android.syntax;

import android.graphics.Color;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxHighlighter {
    
    public enum Language {
        PLAIN_TEXT,
        JAVA,
        KOTLIN,
        PYTHON,
        JAVASCRIPT,
        TYPESCRIPT,
        HTML,
        CSS,
        SCSS,
        JSON,
        XML,
        C,
        CPP,
        CSHARP,
        GO,
        RUST,
        PHP,
        RUBY,
        SWIFT,
        DART,
        SQL,
        SHELL,
        MARKDOWN,
        YAML,
        TOML,
        LUA,
        PERL,
        R,
        SCALA
    }
    
    private static int COLOR_KEYWORD = Color.parseColor("#569CD6");
    private static int COLOR_STRING = Color.parseColor("#CE9178");
    private static int COLOR_NUMBER = Color.parseColor("#B5CEA8");
    private static int COLOR_COMMENT = Color.parseColor("#6A9955");
    private static int COLOR_FUNCTION = Color.parseColor("#DCDCAA");
    private static int COLOR_CLASS = Color.parseColor("#4EC9B0");
    private static int COLOR_ANNOTATION = Color.parseColor("#DCDCAA");
    private static int COLOR_TAG = Color.parseColor("#569CD6");
    private static int COLOR_ATTRIBUTE = Color.parseColor("#9CDCFE");
    private static int COLOR_OPERATOR = Color.parseColor("#D4D4D4");
    private static int COLOR_VARIABLE = Color.parseColor("#9CDCFE");
    private static int COLOR_CONSTANT = Color.parseColor("#4FC1FF");
    private static int COLOR_TYPE = Color.parseColor("#4EC9B0");
    private static int COLOR_HEADING = Color.parseColor("#569CD6");
    private static int COLOR_LINK = Color.parseColor("#3794FF");
    private static int COLOR_BOLD = Color.parseColor("#D7BA7D");
    
    public static void setThemeColors(int keyword, int string, int number, int comment,
                                       int function, int className, int annotation,
                                       int tag, int attribute, int operator,
                                       int variable, int constant) {
        COLOR_KEYWORD = keyword;
        COLOR_STRING = string;
        COLOR_NUMBER = number;
        COLOR_COMMENT = comment;
        COLOR_FUNCTION = function;
        COLOR_CLASS = className;
        COLOR_ANNOTATION = annotation;
        COLOR_TAG = tag;
        COLOR_ATTRIBUTE = attribute;
        COLOR_OPERATOR = operator;
        COLOR_VARIABLE = variable;
        COLOR_CONSTANT = constant;
        COLOR_TYPE = className;
        COLOR_HEADING = keyword;
    }
    
    private static final Map<String, Language> EXTENSION_MAP = new HashMap<>();
    
    static {
        EXTENSION_MAP.put("java", Language.JAVA);
        EXTENSION_MAP.put("kt", Language.KOTLIN);
        EXTENSION_MAP.put("kts", Language.KOTLIN);
        EXTENSION_MAP.put("py", Language.PYTHON);
        EXTENSION_MAP.put("pyw", Language.PYTHON);
        EXTENSION_MAP.put("js", Language.JAVASCRIPT);
        EXTENSION_MAP.put("jsx", Language.JAVASCRIPT);
        EXTENSION_MAP.put("mjs", Language.JAVASCRIPT);
        EXTENSION_MAP.put("ts", Language.TYPESCRIPT);
        EXTENSION_MAP.put("tsx", Language.TYPESCRIPT);
        EXTENSION_MAP.put("html", Language.HTML);
        EXTENSION_MAP.put("htm", Language.HTML);
        EXTENSION_MAP.put("xhtml", Language.HTML);
        EXTENSION_MAP.put("css", Language.CSS);
        EXTENSION_MAP.put("scss", Language.CSS);
        EXTENSION_MAP.put("sass", Language.CSS);
        EXTENSION_MAP.put("less", Language.CSS);
        EXTENSION_MAP.put("json", Language.JSON);
        EXTENSION_MAP.put("xml", Language.XML);
        EXTENSION_MAP.put("svg", Language.XML);
        EXTENSION_MAP.put("plist", Language.XML);
        EXTENSION_MAP.put("c", Language.C);
        EXTENSION_MAP.put("h", Language.C);
        EXTENSION_MAP.put("cpp", Language.CPP);
        EXTENSION_MAP.put("cc", Language.CPP);
        EXTENSION_MAP.put("cxx", Language.CPP);
        EXTENSION_MAP.put("hpp", Language.CPP);
        EXTENSION_MAP.put("hh", Language.CPP);
        EXTENSION_MAP.put("cs", Language.CSHARP);
        EXTENSION_MAP.put("go", Language.GO);
        EXTENSION_MAP.put("rs", Language.RUST);
        EXTENSION_MAP.put("php", Language.PHP);
        EXTENSION_MAP.put("phtml", Language.PHP);
        EXTENSION_MAP.put("rb", Language.RUBY);
        EXTENSION_MAP.put("erb", Language.RUBY);
        EXTENSION_MAP.put("swift", Language.SWIFT);
        EXTENSION_MAP.put("dart", Language.DART);
        EXTENSION_MAP.put("sql", Language.SQL);
        EXTENSION_MAP.put("sh", Language.SHELL);
        EXTENSION_MAP.put("bash", Language.SHELL);
        EXTENSION_MAP.put("zsh", Language.SHELL);
        EXTENSION_MAP.put("fish", Language.SHELL);
        EXTENSION_MAP.put("ps1", Language.SHELL);
        EXTENSION_MAP.put("md", Language.MARKDOWN);
        EXTENSION_MAP.put("markdown", Language.MARKDOWN);
        EXTENSION_MAP.put("yaml", Language.YAML);
        EXTENSION_MAP.put("yml", Language.YAML);
        EXTENSION_MAP.put("toml", Language.TOML);
        EXTENSION_MAP.put("lua", Language.LUA);
        EXTENSION_MAP.put("pl", Language.PERL);
        EXTENSION_MAP.put("pm", Language.PERL);
        EXTENSION_MAP.put("r", Language.R);
        EXTENSION_MAP.put("R", Language.R);
        EXTENSION_MAP.put("scala", Language.SCALA);
        EXTENSION_MAP.put("sc", Language.SCALA);
        EXTENSION_MAP.put("gradle", Language.KOTLIN);
        EXTENSION_MAP.put("groovy", Language.KOTLIN);
        EXTENSION_MAP.put("vue", Language.HTML);
        EXTENSION_MAP.put("svelte", Language.HTML);
    }
    
    public static Language detectLanguage(String filename) {
        if (filename == null || !filename.contains(".")) {
            if (filename != null) {
                String lower = filename.toLowerCase();
                if (lower.equals("makefile") || lower.equals("dockerfile") || lower.equals("jenkinsfile")) {
                    return Language.SHELL;
                }
                if (lower.equals("gemfile") || lower.equals("rakefile")) {
                    return Language.RUBY;
                }
            }
            return Language.PLAIN_TEXT;
        }
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return EXTENSION_MAP.getOrDefault(extension, Language.PLAIN_TEXT);
    }
    
    public static SpannableStringBuilder highlight(String text, Language language) {
        SpannableStringBuilder spannable = new SpannableStringBuilder(text);
        applySpansToSpannable(spannable, language);
        return spannable;
    }
    
    public static void applySpans(Editable editable, Language language) {
        applySpansToSpannable(editable, language);
    }
    
    private static void applySpansToSpannable(Spannable spannable, Language language) {
        switch (language) {
            case JAVA:
                highlightJava(spannable);
                break;
            case KOTLIN:
                highlightKotlin(spannable);
                break;
            case PYTHON:
                highlightPython(spannable);
                break;
            case JAVASCRIPT:
            case TYPESCRIPT:
                highlightJavaScript(spannable);
                break;
            case HTML:
            case XML:
                highlightHTML(spannable);
                break;
            case CSS:
                highlightCSS(spannable);
                break;
            case JSON:
                highlightJSON(spannable);
                break;
            case C:
            case CPP:
                highlightCpp(spannable);
                break;
            case CSHARP:
                highlightCSharp(spannable);
                break;
            case GO:
                highlightGo(spannable);
                break;
            case RUST:
                highlightRust(spannable);
                break;
            case PHP:
                highlightPHP(spannable);
                break;
            case RUBY:
                highlightRuby(spannable);
                break;
            case SWIFT:
                highlightSwift(spannable);
                break;
            case DART:
                highlightDart(spannable);
                break;
            case SQL:
                highlightSQL(spannable);
                break;
            case SHELL:
                highlightShell(spannable);
                break;
            case MARKDOWN:
                highlightMarkdown(spannable);
                break;
            case YAML:
                highlightYAML(spannable);
                break;
            case TOML:
                highlightTOML(spannable);
                break;
            case LUA:
                highlightLua(spannable);
                break;
            case PERL:
                highlightPerl(spannable);
                break;
            case R:
                highlightR(spannable);
                break;
            case SCALA:
                highlightScala(spannable);
                break;
            default:
                break;
        }
    }
    
    private static void highlightJava(Spannable spannable) {
        String[] keywords = {
            "abstract", "assert", "boolean", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "default", "do", "double",
            "else", "enum", "extends", "final", "finally", "float", "for",
            "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private",
            "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while", "true", "false", "null",
            "var", "yield", "record", "sealed", "permits", "non-sealed"
        };
        
        highlightKeywords(spannable, keywords, COLOR_KEYWORD);
        highlightPattern(spannable, "//.*", COLOR_COMMENT);
        highlightPattern(spannable, "/\\*[\\s\\S]*?\\*/", COLOR_COMMENT);
        highlightPattern(spannable, "\"(?:[^\"\\\\]|\\\\.)*\"", COLOR_STRING);
        highlightPattern(spannable, "'(?:[^'\\\\]|\\\\.)*'", COLOR_STRING);
        highlightPattern(spannable, "\\b\\d+(\\.\\d+)?[fFdDlL]?\\b", COLOR_NUMBER);
        highlightPattern(spannable, "0x[0-9a-fA-F]+[lL]?\\b", COLOR_NUMBER);
        highlightPattern(spannable, "@\\w+", COLOR_ANNOTATION);
        highlightPattern(spannable, "\\b[A-Z][a-zA-Z0-9_]*\\b", COLOR_CLASS);
    }
    
    private static void highlightKotlin(Spannable spannable) {
        String[] keywords = {
            "abstract", "actual", "annotation", "as", "break", "by", "catch",
            "class", "companion", "const", "constructor", "continue", "crossinline",
            "data", "delegate", "do", "dynamic", "else", "enum", "expect", "external",
            "false", "final", "finally", "for", "fun", "get", "if", "import",
            "in", "infix", "init", "inline", "inner", "interface", "internal",
            "is", "it", "lateinit", "noinline", "null", "object", "open", "operator",
            "out", "override", "package", "private", "protected", "public", "reified",
            "return", "sealed", "set", "super", "suspend", "tailrec", "this",
            "throw", "true", "try", "typealias", "typeof", "val", "var", "vararg",
            "when", "where", "while"
        };
        
        highlightKeywords(spannable, keywords, COLOR_KEYWORD);
        highlightPattern(spannable, "//.*", COLOR_COMMENT);
        highlightPattern(spannable, "/\\*[\\s\\S]*?\\*/", COLOR_COMMENT);
        highlightPattern(spannable, "\"\"\"[\\s\\S]*?\"\"\"", COLOR_STRING);
        highlightPattern(spannable, "\"(?:[^\"\\\\]|\\\\.)*\"", COLOR_STRING);
        highlightPattern(spannable, "'(?:[^'\\\\]|\\\\.)*'", COLOR_STRING);
        highlightPattern(spannable, "\\b\\d+(\\.\\d+)?[fFdDlL]?\\b", COLOR_NUMBER);
        highlightPattern(spannable, "@\\w+", COLOR_ANNOTATION);
        highlightPattern(spannable, "\\b[A-Z][a-zA-Z0-9_]*\\b", COLOR_CLASS);
    }
    
    private static void highlightPython(Spannable spannable) {
        String[] keywords = {
            "and", "as", "assert", "async", "await", "break", "class",
            "continue", "def", "del", "elif", "else", "except", "finally",
            "for", "from", "global", "if", "import", "in", "is", "lambda",
            "None", "nonlocal", "not", "or", "pass", "raise", "return",
            "try", "while", "with", "yield", "True", "False", "match", "case",
            "self", "cls", "super"
        };
        
        highlightKeywords(spannable, keywords, COLOR_KEYWORD);
        highlightPattern(spannable, "#.*", COLOR_COMMENT);
        highlightPattern(spannable, "\"\"\"[\\s\\S]*?\"\"\"", COLOR_STRING);
        highlightPattern(spannable, "'''[\\s\\S]*?'''", COLOR_STRING);
        highlightPattern(spannable, "f\"(?:[^\"\\\\]|\\\\.)*\"", COLOR_STRING);
        highlightPattern(spannable, "r\"(?:[^\"\\\\]|\\\\.)*\"", COLOR_STRING);
        highlightPattern(spannable, "\"(?:[^\"\\\\]|\\\\.)*\"", COLOR_STRING);
        highlightPattern(spannable, "'(?:[^'\\\\]|\\\\.)*'", COLOR_STRING);
        highlightPattern(spannable, "\\b\\d+(\\.\\d+)?[jJ]?\\b", COLOR_NUMBER);
        highlightPattern(spannable, "@\\w+", COLOR_ANNOTATION);
        highlightPattern(spannable, "\\b[A-Z][a-zA-Z0-9_]*\\b", COLOR_CLASS);
    }
    
    private static void highlightJavaScript(Spannable spannable) {
        String[] keywords = {
            "async", "await", "break", "case", "catch", "class", "const",
            "continue", "debugger", "default", "delete", "do", "else",
            "export", "extends", "finally", "for", "function", "if",
            "import", "in", "instanceof", "let", "new", "return", "static",
            "super", "switch", "this", "throw", "try", "typeof", "var",
            "void", "while", "with", "yield", "true", "false", "null", "undefined",
            "of", "as", "from", "get", "set", "implements", "interface",
            "package", "private", "protected", "public", "type", "enum",
            "readonly", "abstract", "declare", "namespace", "module"
        };
        
        highlightKeywords(spannable, keywords, COLOR_KEYWORD);
        highlightPattern(spannable, "//.*", COLOR_COMMENT);
        highlightPattern(spannable, "/\\*[\\s\\S]*?\\*/", COLOR_COMMENT);
        highlightPattern(spannable, "\"(?:[^\"\\\\]|\\\\.)*\"", COLOR_STRING);
        highlightPattern(spannable, "'(?:[^'\\\\]|\\\\.)*'", COLOR_STRING);
        highlightPattern(spannable, "`(?:[^`\\\\]|\\\\.)*`", COLOR_STRING);
        highlightPattern(spannable, "\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?\\b", COLOR_NUMBER);
        highlightPattern(spannable, "=>", COLOR_OPERATOR);
    }
    
    private static void highlightHTML(Spannable spannable) {
        highlightPattern(spannable, "<!--[\\s\\S]*?-->", COLOR_COMMENT);
        highlightPattern(spannable, "</?\\w+", COLOR_TAG);
        highlightPattern(spannable, "/?>", COLOR_TAG);
        highlightPattern(spannable, "\\s\\w+(?==)", COLOR_ATTRIBUTE);
        highlightPattern(spannable, "\"[^\"]*\"", COLOR_STRING);
        highlightPattern(spannable, "'[^']*'", COLOR_STRING);
        highlightPattern(spannable, "&\\w+;", COLOR_CONSTANT);
    }
    
    private static void highlightCSS(Spannable spannable) {
        highlightPattern(spannable, "/\\*[\\s\\S]*?\\*/", COLOR_COMMENT);
        highlightPattern(spannable, "[.#]?[\\w-]+(?=\\s*\\{)", COLOR_CLASS);
        highlightPattern(spannable, "[\\w-]+(?=\\s*:)", COLOR_ATTRIBUTE);
        highlightPattern(spannable, "\"[^\"]*\"", COLOR_STRING);
        highlightPattern(spannable, "'[^']*'", COLOR_STRING);
        highlightPattern(spannable, "#[0-9a-fA-F]{3,8}\\b", COLOR_NUMBER);
        highlightPattern(spannable, "\\b\\d+(\\.\\d+)?(px|em|rem|%|vh|vw|vmin|vmax|ch|ex|cm|mm|in|pt|pc)?\\b", COLOR_NUMBER);
        highlightPattern(spannable, "@\\w+", COLOR_KEYWORD);
        highlightPattern(spannable, "!important", COLOR_KEYWORD);
    }
    
    private static void highlightJSON(Spannable spannable) {
        highlightPattern(spannable, "\"[^\"]*\"(?=\\s*:)", COLOR_ATTRIBUTE);
        highlightPattern(spannable, "\"[^\"]*\"", COLOR_STRING);
        highlightPattern(spannable, "\\b(true|false|null)\\b", COLOR_KEYWORD);
        highlightPattern(spannable, "-?\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?\\b", COLOR_NUMBER);
    }
    
    private static void highlightCpp(Spannable spannable) {
        String[] keywords = {
            "alignas", "alignof", "and", "and_eq", "asm", "auto", "bitand",
            "bitor", "bool", "break", "case", "catch", "char", "char8_t",
            "char16_t", "char32_t", "class", "compl", "concept", "const",
            "consteval", "constexpr", "constinit", "const_cast", "continue",
            "co_await", "co_return", "co_yield", "decltype", "default", "delete",
            "do", "double", "dynamic_cast", "else", "enum", "explicit", "export",
            "extern", "false", "float", "for", "friend", "goto", "if", "inline",
            "int", "long", "mutable", "namespace", "new", "noexcept", "not",
            "not_eq", "nullptr", "operator", "or", "or_eq", "private", "protected",
            "public", "register", "reinterpret_cast", "requires", "return",
            "short", "signed", "sizeof", "static", "static_assert", "static_cast",
            "struct", "switch", "template", "this", "thread_local", "throw",
            "true", "try", "typedef", "typeid", "typename", "union", "unsigned",
            "using", "virtual", "void", "volatile", "wchar_t", "while", "xor", "xor_eq",
            "#include", "#define", "#ifdef", "#ifndef", "#endif", "#pragma", "#if", "#else", "#elif"
        };
        
        highlightKeywords(spannable, keywords, COLOR_KEYWORD);
        highlightPattern(spannable, "//.*", COLOR_COMMENT);
        highlightPattern(spannable, "/\\*[\\s\\S]*?\\*/", COLOR_COMMENT);
        highlightPattern(spannable, "\"(?:[^\"\\\\]|\\\\.)*\"", COLOR_STRING);
        highlightPattern(spannable, "'(?:[^'\\\\]|\\\\.)*'", COLOR_STRING);
        highlightPattern(spannable, "<[^>]+>(?=\\s*$)", COLOR_STRING);
        highlightPattern(spannable, "\\b\\d+(\\.\\d+)?[fFuUlL]*\\b", COLOR_NUMBER);
        highlightPattern(spannable, "0x[0-9a-fA-F]+[uUlL]*\\b", COLOR_NUMBER);
        highlightPattern(spannable, "\\b[A-Z][A-Z0-9_]*\\b", COLOR_CONSTANT);
        highlightPattern(spannable, "\\b[A-Z][a-zA-Z0-9_]*\\b", COLOR_CLASS);
    }
    
    private static void highlightCSharp(Spannable spannable) {
        String[] keywords = {
            "abstract", "as", "base", "bool", "break", "byte", "case", "catch",
            "char", "checked", "class", "const", "continue", "decimal", "default",
            "delegate", "do", "double", "else", "enum", "event", "explicit",
            "extern", "false", "finally", "fixed", "float", "for", "foreach",
            "goto", "if", "implicit", "in", "int", "interface", "internal", "is",
            "lock", "long", "namespace", "new", "null", "object", "operator",
            "out", "override", "params", "private", "protected", "public",
            "readonly", "ref", "return", "sbyte", "sealed", "short", "sizeof",
            "stackalloc", "static", "string", "struct", "switch", "this", "throw",
            "true", "try", "typeof", "uint", "ulong", "unchecked", "unsafe",
            "ushort", "using", "var", "virtual", "void", "volatile", "while",
            "async", "await", "dynamic", "nameof", "record", "init", "required"
        };
        
        highlightKeywords(spannable, keywords, COLOR_KEYWORD);
        highlightPattern(spannable, "//.*", COLOR_COMMENT);
        highlightPattern(spannable, "/\\*[\\s\\S]*?\\*/", COLOR_COMMENT);
        highlightPattern(spannable, "@\"(?:[^\"]|\"\")*\"", COLOR_STRING);
        highlightPattern(spannable, "\\$\"(?:[^\"\\\\]|\\\\.)*\"", COLOR_STRING);
        highlightPattern(spannable, "\"(?:[^\"\\\\]|\\\\.)*\"", COLOR_STRING);
        highlightPattern(spannable, "'(?:[^'\\\\]|\\\\.)*'", COLOR_STRING);
        highlightPattern(spannable, "\\b\\d+(\\.\\d+)?[fFdDmMuUlL]*\\b", COLOR_NUMBER);
        highlightPattern(spannable, "\\[\\w+\\]", COLOR_ANNOTATION);
        highlightPattern(spannable, "\\b[A-Z][a-zA-Z0-9_]*\\b", COLOR_CLASS);
    }
    
    private static void highlightGo(Spannable spannable) {
        String[] keywords = {
            "break", "case", "chan", "const", "continue", "default", "defer",
            "else", "fallthrough", "for", "func", "go", "goto", "if", "import",
            "interface", "map", "package", "range", "return", "select", "struct",
            "switch", "type", "var", "true", "false", "nil", "iota",
            "bool", "byte", "complex64", "complex128", "error", "float32", "float64",
            "int", "int8", "int16", "int32", "int64", "rune", "string",
            "uint", "uint8", "uint16", "uint32", "uint64", "uintptr", "any"
        };
        
        highlightKeywords(spannable, keywords, COLOR_KEYWORD);
        highlightPattern(spannable, "//.*", COLOR_COMMENT);
        highlightPattern(spannable, "/\\*[\\s\\S]*?\\*/", COLOR_COMMENT);
        highlightPattern(spannable, "`[^`]*`", COLOR_STRING);
        highlightPattern(spannable, "\"(?:[^\"\\\\]|\\\\.)*\"", COLOR_STRING);
        highlightPattern(spannable, "'(?:[^'\\\\]|\\\\.)*'", COLOR_STRING);
        highlightPattern(spannable, "\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?i?\\b", COLOR_NUMBER);
        highlightPattern(spannable, "0x[0-9a-fA-F]+\\b", COLOR_NUMBER);
        highlightPattern(spannable, "\\b[A-Z][a-zA-Z0-9_]*\\b", COLOR_CLASS);
    }
    
    private static void highlightRust(Spannable spannable) {
        String[] keywords = {
            "as", "async", "await", "break", "const", "continue", "crate", "dyn",
            "else", "enum", "extern", "false", "fn", "for", "if", "impl", "in",
            "let", "loop", "match", "mod", "move", "mut", "pub", "ref", "return",
            "self", "Self", "static", "struct", "super", "trait", "true", "type",
            "unsafe", "use", "where", "while", "abstract", "become", "box", "do",
            "final", "macro", "override", "priv", "try", "typeof", "unsized",
            "virtual", "yield",
            "bool", "char", "str", "u8", "u16", "u32", "u64", "u128", "usize",
            "i8", "i16", "i32", "i64", "i128", "isize", "f32", "f64",
            "Option", "Result", "Some", "None", "Ok", "Err", "Vec", "String", "Box"
        };
        
        highlightKeywords(spannable, keywords, COLOR_KEYWORD);
        highlightPattern(spannable, "//.*", COLOR_COMMENT);
        highlightPattern(spannable, "/\\*[\\s\\S]*?\\*/", COLOR_COMMENT);
        highlightPattern(spannable, "r#*\"[\\s\\S]*?\"#*", COLOR_STRING);
        highlightPattern(spannable, "\"(?:[^\"\\\\]|\\\\.)*\"", COLOR_STRING);
        highlightPattern(spannable, "'[^']*'", COLOR_STRING);
        highlightPattern(spannable, "\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?(_?\\w+)?\\b", COLOR_NUMBER);
        highlightPattern(spannable, "0x[0-9a-fA-F_]+\\b", COLOR_NUMBER);
        highlightPattern(spannable, "#\\[\\w+", COLOR_ANNOTATION);
        highlightPattern(spannable, "#!\\[\\w+", COLOR_ANNOTATION);
        highlightPattern(spannable, "\\b[A-Z][a-zA-Z0-9_]*\\b", COLOR_CLASS);
        highlightPattern(spannable, "'\\w+", COLOR_CONSTANT);
    }
    
    private static void highlightPHP(Spannable spannable) {
        String[] keywords = {
            "abstract", "and", "array", "as", "break", "callable", "case", "catch",
            "class", "clone", "const", "continue", "declare", "default", "die", "do",
            "echo", "else", "elseif", "empty", "enddeclare", "endfor", "endforeach",
            "endif", "endswitch", "endwhile", "eval", "exit", "extends", "final",
            "finally", "fn", "for", "foreach", "function", "global", "goto", "if",
            "implements", "include", "include_once", "instanceof", "insteadof",
            "interface", "isset", "list", "match", "namespace", "new", "or", "print",
            "private", "protected", "public", "readonly", "require", "require_once",
            "return", "static", "switch", "throw", "trait", "try", "unset", "use",
            "var", "while", "xor", "yield", "true", "false", "null",
            "int", "float", "bool", "string", "void", "mixed", "never", "object"
        };
        
        highlightKeywords(spannable, keywords, COLOR_KEYWORD);
        highlightPattern(spannable, "//.*", COLOR_COMMENT);
        highlightPattern(spannable, "#.*", COLOR_COMMENT);
        highlightPattern(spannable, "/\\*[\\s\\S]*?\\*/", COLOR_COMMENT);
        highlightPattern(spannable, "\"(?:[^\"\\\\]|\\\\.)*\"", COLOR_STRING);
        highlightPattern(spannable, "'(?:[^'\\\\]|\\\\.)*'", COLOR_STRING);
        highlightPattern(spannable, "<<<['\"]?\\w+['\"]?[\\s\\S]*?^\\w+;", COLOR_STRING);
        highlightPattern(spannable, "\\$\\w+", COLOR_VARIABLE);
        highlightPattern(spannable, "\\b\\d+(\\.\\d+)?\\b", COLOR_NUMBER);
        highlightPattern(spannable, "\\b[A-Z][a-zA-Z0-9_]*\\b", COLOR_CLASS);
    }
    
    private static void highlightRuby(Spannable spannable) {
        String[] keywords = {
            "alias", "and", "begin", "break", "case", "class", "def", "defined?",
            "do", "else", "elsif", "end", "ensure", "false", "for", "if", "in",
            "module", "next", "nil", "not", "or", "redo", "rescue", "retry",
            "return", "self", "super", "then", "true", "undef", "unless", "until",
            "when", "while", "yield", "require", "require_relative", "include",
            "extend", "attr_reader", "attr_writer", "attr_accessor", "private",
            "protected", "public", "raise", "lambda", "proc"
        };
        
        highlightKeywords(spannable, keywords, COLOR_KEYWORD);
        highlightPattern(spannable, "#.*", COLOR_COMMENT);
        highlightPattern(spannable, "=begin[\\s\\S]*?=end", COLOR_COMMENT);
        highlightPattern(spannable, "\"(?:[^\"\\\\]|\\\\.)*\"", COLOR_STRING);
        highlightPattern(spannable, "'(?:[^'\\\\]|\\\\.)*'", COLOR_STRING);
        highlightPattern(spannable, "%[qQwWiIxsr]?[\\{\\[\\(<][\\s\\S]*?[\\}\\]\\)>]", COLOR_STRING);
        highlightPattern(spannable, ":[a-zA-Z_]\\w*", COLOR_CONSTANT);
        highlightPattern(spannable, "@{1,2}\\w+", COLOR_VARIABLE);
        highlightPattern(spannable, "\\$\\w+", COLOR_VARIABLE);
        highlightPattern(spannable, "\\b\\d+(\\.\\d+)?\\b", COLOR_NUMBER);
        highlightPattern(spannable, "\\b[A-Z][a-zA-Z0-9_]*\\b", COLOR_CLASS);
    }
    
    private static void highlightSwift(Spannable spannable) {
        String[] keywords = {
            "actor", "any", "as", "associatedtype", "async", "await", "break",
            "case", "catch", "class", "continue", "default", "defer", "deinit",
            "do", "else", "enum", "extension", "fallthrough", "false", "fileprivate",
            "for", "func", "guard", "if", "import", "in", "indirect", "infix",
            "init", "inout", "internal", "is", "isolated", "lazy", "let", "mutating",
            "nil", "nonisolated", "nonmutating", "open", "operator", "optional",
            "override", "postfix", "precedencegroup", "prefix", "private", "protocol",
            "public", "repeat", "required", "rethrows", "return", "self", "Self",
            "set", "some", "static", "struct", "subscript", "super", "switch",
            "throw", "throws", "true", "try", "typealias", "unowned", "var",
            "weak", "where", "while",
            "Int", "Double", "Float", "Bool", "String", "Array", "Dictionary",
            "Set", "Optional", "Any", "AnyObject", "Void"
        };
        
        highlightKeywords(spannable, keywords, COLOR_KEYWORD);
        highlightPattern(spannable, "//.*", COLOR_COMMENT);
        highlightPattern(spannable, "/\\*[\\s\\S]*?\\*/", COLOR_COMMENT);
        highlightPattern(spannable, "\"\"\"[\\s\\S]*?\"\"\"", COLOR_STRING);
        highlightPattern(spannable, "\"(?:[^\"\\\\]|\\\\.)*\"", COLOR_STRING);
        highlightPattern(spannable, "\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?\\b", COLOR_NUMBER);
        highlightPattern(spannable, "0x[0-9a-fA-F]+\\b", COLOR_NUMBER);
        highlightPattern(spannable, "@\\w+", COLOR_ANNOTATION);
        highlightPattern(spannable, "#\\w+", COLOR_ANNOTATION);
        highlightPattern(spannable, "\\b[A-Z][a-zA-Z0-9_]*\\b", COLOR_CLASS);
    }
    
    private static void highlightDart(Spannable spannable) {
        String[] keywords = {
            "abstract", "as", "assert", "async", "await", "base", "break", "case",
            "catch", "class", "const", "continue", "covariant", "default", "deferred",
            "do", "dynamic", "else", "enum", "export", "extends", "extension",
            "external", "factory", "false", "final", "finally", "for", "Function",
            "get", "hide", "if", "implements", "import", "in", "interface", "is",
            "late", "library", "mixin", "new", "null", "on", "operator", "part",
            "required", "rethrow", "return", "sealed", "set", "show", "static",
            "super", "switch", "sync", "this", "throw", "true", "try", "typedef",
            "var", "void", "when", "while", "with", "yield",
            "int", "double", "num", "bool", "String", "List", "Map", "Set",
            "Future", "Stream", "Iterable", "Object", "Never", "dynamic"
        };
        
        highlightKeywords(spannable, keywords, COLOR_KEYWORD);
        highlightPattern(spannable, "//.*", COLOR_COMMENT);
        highlightPattern(spannable, "/\\*[\\s\\S]*?\\*/", COLOR_COMMENT);
        highlightPattern(spannable, "///.*", COLOR_COMMENT);
        highlightPattern(spannable, "r\"[^\"]*\"", COLOR_STRING);
        highlightPattern(spannable, "r'[^']*'", COLOR_STRING);
        highlightPattern(spannable, "\"\"\"[\\s\\S]*?\"\"\"", COLOR_STRING);
        highlightPattern(spannable, "'''[\\s\\S]*?'''", COLOR_STRING);
        highlightPattern(spannable, "\"(?:[^\"\\\\]|\\\\.)*\"", COLOR_STRING);
        highlightPattern(spannable, "'(?:[^'\\\\]|\\\\.)*'", COLOR_STRING);
        highlightPattern(spannable, "\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?\\b", COLOR_NUMBER);
        highlightPattern(spannable, "@\\w+", COLOR_ANNOTATION);
        highlightPattern(spannable, "\\b[A-Z][a-zA-Z0-9_]*\\b", COLOR_CLASS);
    }
    
    private static void highlightSQL(Spannable spannable) {
        String[] keywords = {
            "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET",
            "DELETE", "CREATE", "DROP", "ALTER", "TABLE", "DATABASE", "INDEX",
            "VIEW", "TRIGGER", "PROCEDURE", "FUNCTION", "JOIN", "INNER", "LEFT",
            "RIGHT", "OUTER", "FULL", "CROSS", "ON", "AND", "OR", "NOT", "IN",
            "BETWEEN", "LIKE", "IS", "NULL", "AS", "ORDER", "BY", "ASC", "DESC",
            "GROUP", "HAVING", "LIMIT", "OFFSET", "UNION", "ALL", "DISTINCT",
            "EXISTS", "CASE", "WHEN", "THEN", "ELSE", "END", "IF", "BEGIN",
            "COMMIT", "ROLLBACK", "TRANSACTION", "PRIMARY", "KEY", "FOREIGN",
            "REFERENCES", "UNIQUE", "CHECK", "DEFAULT", "CONSTRAINT", "CASCADE",
            "RESTRICT", "NO", "ACTION", "GRANT", "REVOKE", "TO", "WITH",
            "INTEGER", "VARCHAR", "TEXT", "BOOLEAN", "DATE", "TIMESTAMP", "FLOAT",
            "DECIMAL", "BLOB", "SERIAL", "BIGINT", "SMALLINT", "CHAR", "DOUBLE",
            "select", "from", "where", "insert", "into", "values", "update", "set",
            "delete", "create", "drop", "alter", "table", "database", "index",
            "view", "trigger", "procedure", "function", "join", "inner", "left",
            "right", "outer", "full", "cross", "on", "and", "or", "not", "in",
            "between", "like", "is", "null", "as", "order", "by", "asc", "desc",
            "group", "having", "limit", "offset", "union", "all", "distinct",
            "exists", "case", "when", "then", "else", "end", "if", "begin",
            "commit", "rollback", "transaction", "primary", "key", "foreign",
            "references", "unique", "check", "default", "constraint", "cascade"
        };
        
        highlightKeywords(spannable, keywords, COLOR_KEYWORD);
        highlightPattern(spannable, "--.*", COLOR_COMMENT);
        highlightPattern(spannable, "/\\*[\\s\\S]*?\\*/", COLOR_COMMENT);
        highlightPattern(spannable, "'(?:[^'\\\\]|\\\\.)*'", COLOR_STRING);
        highlightPattern(spannable, "\"(?:[^\"\\\\]|\\\\.)*\"", COLOR_STRING);
        highlightPattern(spannable, "\\b\\d+(\\.\\d+)?\\b", COLOR_NUMBER);
    }
    
    private static void highlightShell(Spannable spannable) {
        String[] keywords = {
            "if", "then", "else", "elif", "fi", "for", "while", "do", "done",
            "case", "esac", "in", "function", "select", "until", "return",
            "break", "continue", "local", "declare", "typeset", "readonly",
            "export", "unset", "shift", "eval", "exec", "exit", "trap", "source",
            "alias", "unalias", "set", "true", "false", "test", "echo", "printf",
            "read", "cd", "pwd", "pushd", "popd", "dirs", "let", "expr"
        };
        
        highlightKeywords(spannable, keywords, COLOR_KEYWORD);
        highlightPattern(spannable, "#.*", COLOR_COMMENT);
        highlightPattern(spannable, "\"(?:[^\"\\\\]|\\\\.)*\"", COLOR_STRING);
        highlightPattern(spannable, "'[^']*'", COLOR_STRING);
        highlightPattern(spannable, "\\$\\{?\\w+\\}?", COLOR_VARIABLE);
        highlightPattern(spannable, "\\$\\([^)]+\\)", COLOR_VARIABLE);
        highlightPattern(spannable, "\\b\\d+\\b", COLOR_NUMBER);
    }
    
    private static void highlightMarkdown(Spannable spannable) {
        highlightPattern(spannable, "^#{1,6}\\s.*$", COLOR_HEADING);
        highlightPattern(spannable, "\\*\\*[^*]+\\*\\*", COLOR_BOLD);
        highlightPattern(spannable, "__[^_]+__", COLOR_BOLD);
        highlightPattern(spannable, "\\*[^*]+\\*", COLOR_STRING);
        highlightPattern(spannable, "_[^_]+_", COLOR_STRING);
        highlightPattern(spannable, "`[^`]+`", COLOR_CONSTANT);
        highlightPattern(spannable, "```[\\s\\S]*?```", COLOR_CONSTANT);
        highlightPattern(spannable, "\\[[^\\]]+\\]\\([^)]+\\)", COLOR_LINK);
        highlightPattern(spannable, "^>\\s.*$", COLOR_COMMENT);
        highlightPattern(spannable, "^[-*+]\\s", COLOR_KEYWORD);
        highlightPattern(spannable, "^\\d+\\.\\s", COLOR_KEYWORD);
    }
    
    private static void highlightYAML(Spannable spannable) {
        highlightPattern(spannable, "#.*", COLOR_COMMENT);
        highlightPattern(spannable, "^\\s*[\\w-]+(?=:)", COLOR_ATTRIBUTE);
        highlightPattern(spannable, ":\\s*[|>]", COLOR_OPERATOR);
        highlightPattern(spannable, "\"(?:[^\"\\\\]|\\\\.)*\"", COLOR_STRING);
        highlightPattern(spannable, "'(?:[^'\\\\]|\\\\.)*'", COLOR_STRING);
        highlightPattern(spannable, "\\b(true|false|yes|no|on|off|null|~)\\b", COLOR_KEYWORD);
        highlightPattern(spannable, "\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?\\b", COLOR_NUMBER);
        highlightPattern(spannable, "^---$", COLOR_OPERATOR);
        highlightPattern(spannable, "^\\.\\.\\.$", COLOR_OPERATOR);
        highlightPattern(spannable, "&\\w+", COLOR_ANNOTATION);
        highlightPattern(spannable, "\\*\\w+", COLOR_ANNOTATION);
    }
    
    private static void highlightTOML(Spannable spannable) {
        highlightPattern(spannable, "#.*", COLOR_COMMENT);
        highlightPattern(spannable, "^\\s*\\[\\[?[^\\]]+\\]\\]?", COLOR_CLASS);
        highlightPattern(spannable, "^\\s*[\\w.-]+(?=\\s*=)", COLOR_ATTRIBUTE);
        highlightPattern(spannable, "\"\"\"[\\s\\S]*?\"\"\"", COLOR_STRING);
        highlightPattern(spannable, "'''[\\s\\S]*?'''", COLOR_STRING);
        highlightPattern(spannable, "\"(?:[^\"\\\\]|\\\\.)*\"", COLOR_STRING);
        highlightPattern(spannable, "'[^']*'", COLOR_STRING);
        highlightPattern(spannable, "\\b(true|false)\\b", COLOR_KEYWORD);
        highlightPattern(spannable, "\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?\\b", COLOR_NUMBER);
        highlightPattern(spannable, "\\d{4}-\\d{2}-\\d{2}(T\\d{2}:\\d{2}:\\d{2})?", COLOR_CONSTANT);
    }
    
    private static void highlightLua(Spannable spannable) {
        String[] keywords = {
            "and", "break", "do", "else", "elseif", "end", "false", "for",
            "function", "goto", "if", "in", "local", "nil", "not", "or",
            "repeat", "return", "then", "true", "until", "while"
        };
        
        highlightKeywords(spannable, keywords, COLOR_KEYWORD);
        highlightPattern(spannable, "--\\[\\[[\\s\\S]*?\\]\\]", COLOR_COMMENT);
        highlightPattern(spannable, "--.*", COLOR_COMMENT);
        highlightPattern(spannable, "\\[\\[[\\s\\S]*?\\]\\]", COLOR_STRING);
        highlightPattern(spannable, "\"(?:[^\"\\\\]|\\\\.)*\"", COLOR_STRING);
        highlightPattern(spannable, "'(?:[^'\\\\]|\\\\.)*'", COLOR_STRING);
        highlightPattern(spannable, "\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?\\b", COLOR_NUMBER);
        highlightPattern(spannable, "0x[0-9a-fA-F]+\\b", COLOR_NUMBER);
    }
    
    private static void highlightPerl(Spannable spannable) {
        String[] keywords = {
            "if", "elsif", "else", "unless", "while", "until", "for", "foreach",
            "do", "sub", "my", "our", "local", "use", "no", "require", "package",
            "return", "last", "next", "redo", "goto", "die", "warn", "print",
            "say", "open", "close", "read", "write", "chomp", "chop", "split",
            "join", "push", "pop", "shift", "unshift", "grep", "map", "sort",
            "keys", "values", "each", "exists", "delete", "defined", "undef",
            "bless", "ref", "tie", "untie", "BEGIN", "END", "CHECK", "INIT"
        };
        
        highlightKeywords(spannable, keywords, COLOR_KEYWORD);
        highlightPattern(spannable, "#.*", COLOR_COMMENT);
        highlightPattern(spannable, "=\\w+[\\s\\S]*?=cut", COLOR_COMMENT);
        highlightPattern(spannable, "\"(?:[^\"\\\\]|\\\\.)*\"", COLOR_STRING);
        highlightPattern(spannable, "'(?:[^'\\\\]|\\\\.)*'", COLOR_STRING);
        highlightPattern(spannable, "\\$\\w+", COLOR_VARIABLE);
        highlightPattern(spannable, "@\\w+", COLOR_VARIABLE);
        highlightPattern(spannable, "%\\w+", COLOR_VARIABLE);
        highlightPattern(spannable, "\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?\\b", COLOR_NUMBER);
    }
    
    private static void highlightR(Spannable spannable) {
        String[] keywords = {
            "if", "else", "repeat", "while", "function", "for", "in", "next",
            "break", "TRUE", "FALSE", "NULL", "Inf", "NaN", "NA", "NA_integer_",
            "NA_real_", "NA_complex_", "NA_character_", "library", "require",
            "source", "return", "invisible", "stop", "warning", "message"
        };
        
        highlightKeywords(spannable, keywords, COLOR_KEYWORD);
        highlightPattern(spannable, "#.*", COLOR_COMMENT);
        highlightPattern(spannable, "\"(?:[^\"\\\\]|\\\\.)*\"", COLOR_STRING);
        highlightPattern(spannable, "'(?:[^'\\\\]|\\\\.)*'", COLOR_STRING);
        highlightPattern(spannable, "\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?[iL]?\\b", COLOR_NUMBER);
        highlightPattern(spannable, "<-|->|<<-|->>", COLOR_OPERATOR);
    }
    
    private static void highlightScala(Spannable spannable) {
        String[] keywords = {
            "abstract", "case", "catch", "class", "def", "do", "else", "extends",
            "false", "final", "finally", "for", "forSome", "if", "implicit",
            "import", "lazy", "match", "new", "null", "object", "override",
            "package", "private", "protected", "return", "sealed", "super",
            "this", "throw", "trait", "true", "try", "type", "val", "var",
            "while", "with", "yield", "given", "using", "enum", "export",
            "then", "derives", "end", "extension", "infix", "inline", "opaque",
            "open", "transparent"
        };
        
        highlightKeywords(spannable, keywords, COLOR_KEYWORD);
        highlightPattern(spannable, "//.*", COLOR_COMMENT);
        highlightPattern(spannable, "/\\*[\\s\\S]*?\\*/", COLOR_COMMENT);
        highlightPattern(spannable, "\"\"\"[\\s\\S]*?\"\"\"", COLOR_STRING);
        highlightPattern(spannable, "s\"(?:[^\"\\\\]|\\\\.)*\"", COLOR_STRING);
        highlightPattern(spannable, "f\"(?:[^\"\\\\]|\\\\.)*\"", COLOR_STRING);
        highlightPattern(spannable, "\"(?:[^\"\\\\]|\\\\.)*\"", COLOR_STRING);
        highlightPattern(spannable, "'(?:[^'\\\\]|\\\\.)*'", COLOR_STRING);
        highlightPattern(spannable, "\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?[fFdDlL]?\\b", COLOR_NUMBER);
        highlightPattern(spannable, "@\\w+", COLOR_ANNOTATION);
        highlightPattern(spannable, "\\b[A-Z][a-zA-Z0-9_]*\\b", COLOR_CLASS);
    }
    
    private static void highlightKeywords(Spannable spannable, String[] keywords, int color) {
        String text = spannable.toString();
        for (String keyword : keywords) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(keyword) + "\\b");
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                spannable.setSpan(
                    new ForegroundColorSpan(color),
                    matcher.start(),
                    matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }
    }
    
    private static void highlightPattern(Spannable spannable, String regex, int color) {
        try {
            Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(spannable.toString());
            while (matcher.find()) {
                spannable.setSpan(
                    new ForegroundColorSpan(color),
                    matcher.start(),
                    matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static String getLanguageDisplayName(Language language) {
        switch (language) {
            case JAVA: return "Java";
            case KOTLIN: return "Kotlin";
            case PYTHON: return "Python";
            case JAVASCRIPT: return "JavaScript";
            case TYPESCRIPT: return "TypeScript";
            case HTML: return "HTML";
            case CSS: return "CSS";
            case JSON: return "JSON";
            case XML: return "XML";
            case C: return "C";
            case CPP: return "C++";
            case CSHARP: return "C#";
            case GO: return "Go";
            case RUST: return "Rust";
            case PHP: return "PHP";
            case RUBY: return "Ruby";
            case SWIFT: return "Swift";
            case DART: return "Dart";
            case SQL: return "SQL";
            case SHELL: return "Shell";
            case MARKDOWN: return "Markdown";
            case YAML: return "YAML";
            case TOML: return "TOML";
            case LUA: return "Lua";
            case PERL: return "Perl";
            case R: return "R";
            case SCALA: return "Scala";
            default: return "Plain Text";
        }
    }
    
    public static Language[] getAllLanguages() {
        return Language.values();
    }
    
    public static String[] getSupportedExtensions() {
        return EXTENSION_MAP.keySet().toArray(new String[0]);
    }
}
