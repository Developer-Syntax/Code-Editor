package com.codeeditor.android.autocomplete;

import com.codeeditor.android.syntax.SyntaxHighlighter;
import com.codeeditor.android.view.AutocompleteAdapter.Suggestion;
import com.codeeditor.android.view.AutocompleteAdapter.SuggestionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AutocompleteEngine {

    private static final Map<SyntaxHighlighter.Language, String[]> KEYWORDS = new HashMap<>();
    private static final Map<SyntaxHighlighter.Language, String[]> BUILTIN_FUNCTIONS = new HashMap<>();
    private static final Map<SyntaxHighlighter.Language, String[]> SNIPPETS = new HashMap<>();

    static {
        KEYWORDS.put(SyntaxHighlighter.Language.JAVA, new String[]{
            "abstract", "assert", "boolean", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "default", "do", "double",
            "else", "enum", "extends", "final", "finally", "float", "for",
            "if", "implements", "import", "instanceof", "int", "interface",
            "long", "native", "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch", "synchronized",
            "this", "throw", "throws", "transient", "try", "void", "volatile", "while",
            "true", "false", "null", "var", "record", "sealed", "permits"
        });

        KEYWORDS.put(SyntaxHighlighter.Language.KOTLIN, new String[]{
            "abstract", "actual", "annotation", "as", "break", "by", "catch",
            "class", "companion", "const", "constructor", "continue", "data",
            "do", "else", "enum", "expect", "external", "false", "final", "finally",
            "for", "fun", "get", "if", "import", "in", "infix", "init", "inline",
            "interface", "internal", "is", "lateinit", "null", "object", "open",
            "operator", "out", "override", "package", "private", "protected", "public",
            "return", "sealed", "set", "super", "suspend", "this", "throw", "true",
            "try", "typealias", "val", "var", "vararg", "when", "where", "while"
        });

        KEYWORDS.put(SyntaxHighlighter.Language.PYTHON, new String[]{
            "and", "as", "assert", "async", "await", "break", "class",
            "continue", "def", "del", "elif", "else", "except", "finally",
            "for", "from", "global", "if", "import", "in", "is", "lambda",
            "None", "nonlocal", "not", "or", "pass", "raise", "return",
            "try", "while", "with", "yield", "True", "False", "match", "case"
        });

        KEYWORDS.put(SyntaxHighlighter.Language.JAVASCRIPT, new String[]{
            "async", "await", "break", "case", "catch", "class", "const",
            "continue", "debugger", "default", "delete", "do", "else",
            "export", "extends", "finally", "for", "function", "if",
            "import", "in", "instanceof", "let", "new", "return", "static",
            "super", "switch", "this", "throw", "try", "typeof", "var",
            "void", "while", "with", "yield", "true", "false", "null", "undefined"
        });

        KEYWORDS.put(SyntaxHighlighter.Language.TYPESCRIPT, KEYWORDS.get(SyntaxHighlighter.Language.JAVASCRIPT));

        KEYWORDS.put(SyntaxHighlighter.Language.GO, new String[]{
            "break", "case", "chan", "const", "continue", "default", "defer",
            "else", "fallthrough", "for", "func", "go", "goto", "if", "import",
            "interface", "map", "package", "range", "return", "select", "struct",
            "switch", "type", "var", "true", "false", "nil"
        });

        KEYWORDS.put(SyntaxHighlighter.Language.RUST, new String[]{
            "as", "async", "await", "break", "const", "continue", "crate", "dyn",
            "else", "enum", "extern", "false", "fn", "for", "if", "impl", "in",
            "let", "loop", "match", "mod", "move", "mut", "pub", "ref", "return",
            "self", "Self", "static", "struct", "super", "trait", "true", "type",
            "unsafe", "use", "where", "while"
        });

        BUILTIN_FUNCTIONS.put(SyntaxHighlighter.Language.JAVA, new String[]{
            "println", "print", "printf", "toString", "equals", "hashCode",
            "compareTo", "length", "size", "get", "set", "add", "remove",
            "contains", "isEmpty", "clear", "substring", "charAt", "indexOf",
            "split", "trim", "toLowerCase", "toUpperCase", "valueOf", "parseInt",
            "parseDouble", "format"
        });

        BUILTIN_FUNCTIONS.put(SyntaxHighlighter.Language.KOTLIN, new String[]{
            "println", "print", "listOf", "mutableListOf", "mapOf", "mutableMapOf",
            "setOf", "mutableSetOf", "arrayOf", "arrayListOf", "hashMapOf",
            "let", "run", "apply", "also", "with", "takeIf", "takeUnless",
            "filter", "map", "forEach", "reduce", "fold", "any", "all", "none",
            "first", "last", "find", "sortedBy", "groupBy", "associate"
        });

        BUILTIN_FUNCTIONS.put(SyntaxHighlighter.Language.PYTHON, new String[]{
            "print", "input", "len", "range", "str", "int", "float", "bool",
            "list", "dict", "set", "tuple", "type", "isinstance", "hasattr",
            "getattr", "setattr", "open", "read", "write", "close", "append",
            "extend", "pop", "remove", "sort", "reverse", "join", "split",
            "strip", "replace", "format", "enumerate", "zip", "map", "filter",
            "reduce", "lambda", "sum", "max", "min", "abs", "round"
        });

        BUILTIN_FUNCTIONS.put(SyntaxHighlighter.Language.JAVASCRIPT, new String[]{
            "console.log", "console.error", "console.warn", "alert", "prompt",
            "confirm", "parseInt", "parseFloat", "isNaN", "isFinite", "eval",
            "setTimeout", "setInterval", "clearTimeout", "clearInterval",
            "fetch", "JSON.parse", "JSON.stringify", "Array.from", "Object.keys",
            "Object.values", "Object.entries", "map", "filter", "reduce", "forEach",
            "find", "findIndex", "some", "every", "includes", "indexOf", "slice",
            "splice", "push", "pop", "shift", "unshift", "sort", "reverse"
        });

        SNIPPETS.put(SyntaxHighlighter.Language.JAVA, new String[]{
            "public class ${1:ClassName} {\n    $0\n}|class|Create a new class",
            "public static void main(String[] args) {\n    $0\n}|main|Main method",
            "for (int ${1:i} = 0; ${1:i} < ${2:length}; ${1:i}++) {\n    $0\n}|for|For loop",
            "for (${1:Type} ${2:item} : ${3:collection}) {\n    $0\n}|foreach|Enhanced for loop",
            "if (${1:condition}) {\n    $0\n}|if|If statement",
            "if (${1:condition}) {\n    $0\n} else {\n    \n}|ifelse|If-else statement",
            "try {\n    $0\n} catch (${1:Exception} ${2:e}) {\n    ${2:e}.printStackTrace();\n}|try|Try-catch block",
            "while (${1:condition}) {\n    $0\n}|while|While loop",
            "switch (${1:variable}) {\n    case ${2:value}:\n        $0\n        break;\n    default:\n        break;\n}|switch|Switch statement",
            "System.out.println(${1:\"\"});|sout|Print line",
            "@Override\npublic ${1:void} ${2:methodName}() {\n    $0\n}|override|Override method"
        });

        SNIPPETS.put(SyntaxHighlighter.Language.KOTLIN, new String[]{
            "fun ${1:functionName}(${2:params}): ${3:Unit} {\n    $0\n}|fun|Function",
            "class ${1:ClassName} {\n    $0\n}|class|Class",
            "data class ${1:ClassName}(${2:val property: Type})|data|Data class",
            "for (${1:item} in ${2:collection}) {\n    $0\n}|for|For loop",
            "if (${1:condition}) {\n    $0\n}|if|If expression",
            "when (${1:variable}) {\n    ${2:value} -> $0\n    else -> {}\n}|when|When expression",
            "try {\n    $0\n} catch (e: ${1:Exception}) {\n    e.printStackTrace()\n}|try|Try-catch",
            "listOf(${1:items})|list|Create list",
            "mapOf(${1:\"key\"} to ${2:value})|map|Create map",
            "println(${1:\"\"})|print|Print line",
            "${1:collection}.forEach { ${2:item} ->\n    $0\n}|foreach|ForEach",
            "${1:collection}.filter { ${2:it} -> $0 }|filter|Filter",
            "${1:collection}.map { ${2:it} -> $0 }|map|Map transform"
        });

        SNIPPETS.put(SyntaxHighlighter.Language.PYTHON, new String[]{
            "def ${1:function_name}(${2:params}):\n    $0|def|Function definition",
            "class ${1:ClassName}:\n    def __init__(self${2:, params}):\n        $0|class|Class definition",
            "for ${1:item} in ${2:iterable}:\n    $0|for|For loop",
            "while ${1:condition}:\n    $0|while|While loop",
            "if ${1:condition}:\n    $0|if|If statement",
            "if ${1:condition}:\n    $0\nelse:\n    |ifelse|If-else statement",
            "try:\n    $0\nexcept ${1:Exception} as ${2:e}:\n    print(${2:e})|try|Try-except block",
            "with open(${1:\"filename\"}, ${2:\"r\"}) as ${3:f}:\n    $0|with|With statement",
            "[${1:expr} for ${2:item} in ${3:iterable}]|listcomp|List comprehension",
            "{${1:key}: ${2:value} for ${3:item} in ${4:iterable}}|dictcomp|Dict comprehension",
            "lambda ${1:x}: $0|lambda|Lambda function",
            "print(f\"${1:}\")$0|print|Print f-string",
            "if __name__ == \"__main__\":\n    $0|main|Main guard"
        });

        SNIPPETS.put(SyntaxHighlighter.Language.JAVASCRIPT, new String[]{
            "function ${1:functionName}(${2:params}) {\n    $0\n}|func|Function",
            "const ${1:name} = (${2:params}) => {\n    $0\n};|arrow|Arrow function",
            "const ${1:name} = (${2:params}) => $0;|arrowshort|Short arrow function",
            "class ${1:ClassName} {\n    constructor(${2:params}) {\n        $0\n    }\n}|class|Class",
            "for (let ${1:i} = 0; ${1:i} < ${2:length}; ${1:i}++) {\n    $0\n}|for|For loop",
            "for (const ${1:item} of ${2:array}) {\n    $0\n}|forof|For-of loop",
            "${1:array}.forEach((${2:item}) => {\n    $0\n});|foreach|ForEach",
            "${1:array}.map((${2:item}) => $0)|map|Map",
            "${1:array}.filter((${2:item}) => $0)|filter|Filter",
            "if (${1:condition}) {\n    $0\n}|if|If statement",
            "try {\n    $0\n} catch (${1:error}) {\n    console.error(${1:error});\n}|try|Try-catch",
            "async function ${1:functionName}(${2:params}) {\n    $0\n}|async|Async function",
            "await ${1:promise}|await|Await",
            "console.log(${1:\"\"});|log|Console log",
            "import { ${1:module} } from '${2:package}';|import|Import",
            "export default ${1:name};|export|Export default"
        });

        SNIPPETS.put(SyntaxHighlighter.Language.HTML, new String[]{
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n    <meta charset=\"UTF-8\">\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n    <title>${1:Document}</title>\n</head>\n<body>\n    $0\n</body>\n</html>|html|HTML5 template",
            "<div class=\"${1:class}\">$0</div>|div|Div with class",
            "<a href=\"${1:#}\">$0</a>|a|Anchor link",
            "<img src=\"${1:src}\" alt=\"${2:alt}\">|img|Image",
            "<ul>\n    <li>$0</li>\n</ul>|ul|Unordered list",
            "<form action=\"${1:#}\" method=\"${2:post}\">\n    $0\n</form>|form|Form",
            "<input type=\"${1:text}\" name=\"${2:name}\" id=\"${3:id}\">|input|Input",
            "<button type=\"${1:button}\">$0</button>|btn|Button",
            "<script src=\"${1:script.js}\"></script>|script|Script tag",
            "<link rel=\"stylesheet\" href=\"${1:style.css}\">|link|Link stylesheet"
        });

        SNIPPETS.put(SyntaxHighlighter.Language.CSS, new String[]{
            ".${1:class} {\n    $0\n}|class|Class selector",
            "#${1:id} {\n    $0\n}|id|ID selector",
            "display: flex;\njustify-content: ${1:center};\nalign-items: ${2:center};|flex|Flexbox",
            "display: grid;\ngrid-template-columns: ${1:repeat(3, 1fr)};\ngap: ${2:1rem};|grid|Grid",
            "@media (max-width: ${1:768px}) {\n    $0\n}|media|Media query",
            "transition: ${1:all} ${2:0.3s} ${3:ease};|trans|Transition",
            "transform: ${1:translateX(0)};|transform|Transform",
            "animation: ${1:name} ${2:1s} ${3:ease} ${4:infinite};|anim|Animation"
        });

        SNIPPETS.put(SyntaxHighlighter.Language.GO, new String[]{
            "func ${1:functionName}(${2:params}) ${3:returnType} {\n    $0\n}|func|Function",
            "func main() {\n    $0\n}|main|Main function",
            "type ${1:TypeName} struct {\n    $0\n}|struct|Struct",
            "type ${1:InterfaceName} interface {\n    $0\n}|interface|Interface",
            "for ${1:i} := 0; ${1:i} < ${2:n}; ${1:i}++ {\n    $0\n}|for|For loop",
            "for ${1:index}, ${2:value} := range ${3:slice} {\n    $0\n}|range|Range loop",
            "if ${1:condition} {\n    $0\n}|if|If statement",
            "if ${1:err} != nil {\n    $0\n}|iferr|Error check",
            "switch ${1:variable} {\ncase ${2:value}:\n    $0\ndefault:\n}|switch|Switch",
            "go func() {\n    $0\n}()|goroutine|Goroutine",
            "fmt.Println(${1:\"\"})|print|Print line"
        });

        SNIPPETS.put(SyntaxHighlighter.Language.RUST, new String[]{
            "fn ${1:function_name}(${2:params}) -> ${3:()} {\n    $0\n}|fn|Function",
            "fn main() {\n    $0\n}|main|Main function",
            "struct ${1:StructName} {\n    $0\n}|struct|Struct",
            "impl ${1:StructName} {\n    $0\n}|impl|Implementation",
            "enum ${1:EnumName} {\n    $0\n}|enum|Enum",
            "for ${1:item} in ${2:iterator} {\n    $0\n}|for|For loop",
            "loop {\n    $0\n}|loop|Infinite loop",
            "while ${1:condition} {\n    $0\n}|while|While loop",
            "if ${1:condition} {\n    $0\n}|if|If expression",
            "match ${1:value} {\n    ${2:pattern} => $0,\n    _ => {},\n}|match|Match expression",
            "let ${1:name}: ${2:Type} = $0;|let|Let binding",
            "let mut ${1:name} = $0;|letmut|Mutable let",
            "println!(\"${1:}\");|print|Print line"
        });
    }

    private List<String> localIdentifiers = new ArrayList<>();
    private SyntaxHighlighter.Language currentLanguage = SyntaxHighlighter.Language.PLAIN_TEXT;

    public void setLanguage(SyntaxHighlighter.Language language) {
        this.currentLanguage = language;
    }

    public void updateLocalIdentifiers(String code) {
        localIdentifiers.clear();
        if (code == null || code.isEmpty()) return;

        Pattern identifierPattern = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\b");
        Matcher matcher = identifierPattern.matcher(code);

        while (matcher.find()) {
            String identifier = matcher.group(1);
            if (!localIdentifiers.contains(identifier) && identifier.length() > 2) {
                localIdentifiers.add(identifier);
            }
        }
    }

    public List<Suggestion> getSuggestions(String prefix, int maxResults) {
        List<Suggestion> results = new ArrayList<>();
        if (prefix == null || prefix.isEmpty()) return results;

        String lowerPrefix = prefix.toLowerCase();

        String[] keywords = KEYWORDS.get(currentLanguage);
        if (keywords != null) {
            for (String keyword : keywords) {
                if (keyword.toLowerCase().startsWith(lowerPrefix)) {
                    results.add(new Suggestion(keyword, "keyword", SuggestionType.KEYWORD));
                }
            }
        }

        String[] functions = BUILTIN_FUNCTIONS.get(currentLanguage);
        if (functions != null) {
            for (String func : functions) {
                if (func.toLowerCase().startsWith(lowerPrefix)) {
                    results.add(new Suggestion(func, "built-in function", SuggestionType.FUNCTION));
                }
            }
        }

        for (String identifier : localIdentifiers) {
            if (identifier.toLowerCase().startsWith(lowerPrefix) && !identifier.equals(prefix)) {
                SuggestionType type = Character.isUpperCase(identifier.charAt(0)) 
                    ? SuggestionType.CLASS : SuggestionType.VARIABLE;
                results.add(new Suggestion(identifier, "local", type));
            }
        }

        results.sort((a, b) -> {
            if (a.type == SuggestionType.KEYWORD && b.type != SuggestionType.KEYWORD) return -1;
            if (b.type == SuggestionType.KEYWORD && a.type != SuggestionType.KEYWORD) return 1;
            return a.text.compareTo(b.text);
        });

        if (results.size() > maxResults) {
            results = results.subList(0, maxResults);
        }

        return results;
    }

    public List<Suggestion> getSnippets(String prefix) {
        List<Suggestion> results = new ArrayList<>();
        String[] snippets = SNIPPETS.get(currentLanguage);
        
        if (snippets == null) return results;

        String lowerPrefix = prefix == null ? "" : prefix.toLowerCase();

        for (String snippet : snippets) {
            String[] parts = snippet.split("\\|");
            if (parts.length >= 3) {
                String code = parts[0];
                String trigger = parts[1];
                String description = parts[2];

                if (lowerPrefix.isEmpty() || trigger.toLowerCase().startsWith(lowerPrefix)) {
                    String cleanCode = code.replace("${1:", "").replace("${2:", "")
                        .replace("${3:", "").replace("${4:", "").replace("}", "")
                        .replace("$0", "");
                    results.add(new Suggestion(trigger, description, SuggestionType.SNIPPET, cleanCode));
                }
            }
        }

        return results;
    }

    public static String[] getLanguageKeywords(SyntaxHighlighter.Language language) {
        return KEYWORDS.getOrDefault(language, new String[0]);
    }
}
