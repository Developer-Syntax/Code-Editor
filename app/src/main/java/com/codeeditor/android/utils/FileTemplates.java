package com.codeeditor.android.utils;

import java.util.HashMap;
import java.util.Map;

public class FileTemplates {
    
    private static final Map<String, String> TEMPLATES = new HashMap<>();
    
    static {
        TEMPLATES.put("java", 
            "public class Main {\n" +
            "    public static void main(String[] args) {\n" +
            "        System.out.println(\"Hello, World!\");\n" +
            "    }\n" +
            "}\n"
        );
        
        TEMPLATES.put("kotlin",
            "fun main() {\n" +
            "    println(\"Hello, World!\")\n" +
            "}\n"
        );
        
        TEMPLATES.put("python",
            "#!/usr/bin/env python3\n" +
            "\n" +
            "def main():\n" +
            "    print(\"Hello, World!\")\n" +
            "\n" +
            "if __name__ == \"__main__\":\n" +
            "    main()\n"
        );
        
        TEMPLATES.put("javascript",
            "// JavaScript\n" +
            "\n" +
            "function main() {\n" +
            "    console.log('Hello, World!');\n" +
            "}\n" +
            "\n" +
            "main();\n"
        );
        
        TEMPLATES.put("typescript",
            "// TypeScript\n" +
            "\n" +
            "function main(): void {\n" +
            "    console.log('Hello, World!');\n" +
            "}\n" +
            "\n" +
            "main();\n"
        );
        
        TEMPLATES.put("html",
            "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <title>Document</title>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <h1>Hello, World!</h1>\n" +
            "</body>\n" +
            "</html>\n"
        );
        
        TEMPLATES.put("css",
            "/* CSS Styles */\n" +
            "\n" +
            "* {\n" +
            "    margin: 0;\n" +
            "    padding: 0;\n" +
            "    box-sizing: border-box;\n" +
            "}\n" +
            "\n" +
            "body {\n" +
            "    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;\n" +
            "    line-height: 1.6;\n" +
            "}\n"
        );
        
        TEMPLATES.put("markdown",
            "# Title\n" +
            "\n" +
            "## Introduction\n" +
            "\n" +
            "Write your content here.\n" +
            "\n" +
            "## Features\n" +
            "\n" +
            "- Feature 1\n" +
            "- Feature 2\n" +
            "- Feature 3\n" +
            "\n" +
            "## Conclusion\n" +
            "\n" +
            "Thank you for reading!\n"
        );
        
        TEMPLATES.put("c",
            "#include <stdio.h>\n" +
            "\n" +
            "int main() {\n" +
            "    printf(\"Hello, World!\\n\");\n" +
            "    return 0;\n" +
            "}\n"
        );
        
        TEMPLATES.put("cpp",
            "#include <iostream>\n" +
            "\n" +
            "int main() {\n" +
            "    std::cout << \"Hello, World!\" << std::endl;\n" +
            "    return 0;\n" +
            "}\n"
        );
        
        TEMPLATES.put("go",
            "package main\n" +
            "\n" +
            "import \"fmt\"\n" +
            "\n" +
            "func main() {\n" +
            "    fmt.Println(\"Hello, World!\")\n" +
            "}\n"
        );
        
        TEMPLATES.put("rust",
            "fn main() {\n" +
            "    println!(\"Hello, World!\");\n" +
            "}\n"
        );
        
        TEMPLATES.put("swift",
            "import Foundation\n" +
            "\n" +
            "print(\"Hello, World!\")\n"
        );
        
        TEMPLATES.put("dart",
            "void main() {\n" +
            "  print('Hello, World!');\n" +
            "}\n"
        );
        
        TEMPLATES.put("ruby",
            "#!/usr/bin/env ruby\n" +
            "\n" +
            "def main\n" +
            "  puts 'Hello, World!'\n" +
            "end\n" +
            "\n" +
            "main\n"
        );
        
        TEMPLATES.put("php",
            "<?php\n" +
            "\n" +
            "function main() {\n" +
            "    echo \"Hello, World!\\n\";\n" +
            "}\n" +
            "\n" +
            "main();\n"
        );
        
        TEMPLATES.put("sql",
            "-- SQL Query\n" +
            "\n" +
            "SELECT * FROM table_name\n" +
            "WHERE condition\n" +
            "ORDER BY column_name;\n"
        );
        
        TEMPLATES.put("shell",
            "#!/bin/bash\n" +
            "\n" +
            "echo \"Hello, World!\"\n"
        );
        
        TEMPLATES.put("yaml",
            "# Configuration\n" +
            "\n" +
            "name: example\n" +
            "version: 1.0.0\n" +
            "\n" +
            "settings:\n" +
            "  enabled: true\n" +
            "  count: 10\n"
        );
        
        TEMPLATES.put("json",
            "{\n" +
            "    \"name\": \"example\",\n" +
            "    \"version\": \"1.0.0\",\n" +
            "    \"description\": \"A sample JSON file\"\n" +
            "}\n"
        );
        
        TEMPLATES.put("xml",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<root>\n" +
            "    <item>\n" +
            "        <name>Example</name>\n" +
            "        <value>123</value>\n" +
            "    </item>\n" +
            "</root>\n"
        );
    }
    
    public static String getTemplate(String type) {
        return TEMPLATES.getOrDefault(type.toLowerCase(), "");
    }
    
    public static String getTemplateForExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return "";
        }
        
        extension = extension.toLowerCase();
        
        switch (extension) {
            case "java":
                return TEMPLATES.get("java");
            case "kt":
            case "kts":
                return TEMPLATES.get("kotlin");
            case "py":
            case "pyw":
                return TEMPLATES.get("python");
            case "js":
            case "mjs":
                return TEMPLATES.get("javascript");
            case "ts":
                return TEMPLATES.get("typescript");
            case "html":
            case "htm":
                return TEMPLATES.get("html");
            case "css":
            case "scss":
            case "sass":
            case "less":
                return TEMPLATES.get("css");
            case "md":
            case "markdown":
                return TEMPLATES.get("markdown");
            case "c":
            case "h":
                return TEMPLATES.get("c");
            case "cpp":
            case "cc":
            case "cxx":
            case "hpp":
                return TEMPLATES.get("cpp");
            case "go":
                return TEMPLATES.get("go");
            case "rs":
                return TEMPLATES.get("rust");
            case "swift":
                return TEMPLATES.get("swift");
            case "dart":
                return TEMPLATES.get("dart");
            case "rb":
                return TEMPLATES.get("ruby");
            case "php":
                return TEMPLATES.get("php");
            case "sql":
                return TEMPLATES.get("sql");
            case "sh":
            case "bash":
            case "zsh":
                return TEMPLATES.get("shell");
            case "yaml":
            case "yml":
                return TEMPLATES.get("yaml");
            case "json":
                return TEMPLATES.get("json");
            case "xml":
            case "svg":
                return TEMPLATES.get("xml");
            default:
                return "";
        }
    }
    
    public static String[] getAvailableTemplates() {
        return TEMPLATES.keySet().toArray(new String[0]);
    }
}
