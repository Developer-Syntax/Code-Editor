package com.codeeditor.android.theme;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;

import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;

import com.codeeditor.android.R;

public class ThemeManager {
    
    private static ThemeManager instance;
    private EditorTheme currentTheme;
    private Typeface currentFont;
    private Context context;
    
    public static ThemeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ThemeManager(context.getApplicationContext());
        }
        return instance;
    }
    
    private ThemeManager(Context context) {
        this.context = context;
        loadThemeFromPreferences();
    }
    
    public void loadThemeFromPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String themeKey = prefs.getString("theme", "dark");
        String fontKey = prefs.getString("font_family", "monospace");
        
        currentTheme = getThemeByKey(themeKey);
        currentFont = getFontByKey(fontKey);
    }
    
    public EditorTheme getCurrentTheme() {
        return currentTheme;
    }
    
    public Typeface getCurrentFont() {
        return currentFont;
    }
    
    public void setTheme(String themeKey) {
        currentTheme = getThemeByKey(themeKey);
    }
    
    public void setFont(String fontKey) {
        currentFont = getFontByKey(fontKey);
    }
    
    private EditorTheme getThemeByKey(String key) {
        switch (key) {
            case "light":
                return createLightTheme();
            case "solarized_dark":
                return createSolarizedDarkTheme();
            case "solarized_light":
                return createSolarizedLightTheme();
            case "monokai":
                return createMonokaiTheme();
            case "github_dark":
                return createGitHubDarkTheme();
            case "dracula":
                return createDraculaTheme();
            case "one_dark":
                return createOneDarkTheme();
            case "dark":
            default:
                return createDarkTheme();
        }
    }
    
    private Typeface getFontByKey(String key) {
        try {
            switch (key) {
                case "jetbrains_mono":
                    return ResourcesCompat.getFont(context, R.font.jetbrains_mono);
                case "fira_code":
                    return ResourcesCompat.getFont(context, R.font.fira_code);
                case "source_code_pro":
                    return ResourcesCompat.getFont(context, R.font.source_code_pro);
                case "roboto_mono":
                    return ResourcesCompat.getFont(context, R.font.roboto_mono);
                case "monospace":
                default:
                    return Typeface.MONOSPACE;
            }
        } catch (Exception e) {
            return Typeface.MONOSPACE;
        }
    }
    
    private EditorTheme createDarkTheme() {
        return new EditorTheme.Builder("VS Code Dark")
                .setBackground(Color.parseColor("#1E1E1E"))
                .setForeground(Color.parseColor("#D4D4D4"))
                .setLineNumberBackground(Color.parseColor("#1E1E1E"))
                .setLineNumberForeground(Color.parseColor("#858585"))
                .setCurrentLine(Color.parseColor("#2A2D2E"))
                .setSelection(Color.parseColor("#264F78"))
                .setCursor(Color.parseColor("#AEAFAD"))
                .setKeyword(Color.parseColor("#569CD6"))
                .setString(Color.parseColor("#CE9178"))
                .setNumber(Color.parseColor("#B5CEA8"))
                .setComment(Color.parseColor("#6A9955"))
                .setFunction(Color.parseColor("#DCDCAA"))
                .setClassName(Color.parseColor("#4EC9B0"))
                .setVariable(Color.parseColor("#9CDCFE"))
                .setOperator(Color.parseColor("#D4D4D4"))
                .setAnnotation(Color.parseColor("#DCDCAA"))
                .setTag(Color.parseColor("#569CD6"))
                .setAttribute(Color.parseColor("#9CDCFE"))
                .setConstant(Color.parseColor("#4FC1FF"))
                .setToolbarBackground(Color.parseColor("#323233"))
                .setStatusBarBackground(Color.parseColor("#007ACC"))
                .setTabBackground(Color.parseColor("#252526"))
                .setTabActiveBackground(Color.parseColor("#1E1E1E"))
                .setSidebarBackground(Color.parseColor("#252526"))
                .build();
    }
    
    private EditorTheme createLightTheme() {
        return new EditorTheme.Builder("Light")
                .setBackground(Color.parseColor("#FFFFFF"))
                .setForeground(Color.parseColor("#000000"))
                .setLineNumberBackground(Color.parseColor("#F3F3F3"))
                .setLineNumberForeground(Color.parseColor("#237893"))
                .setCurrentLine(Color.parseColor("#F3F3F3"))
                .setSelection(Color.parseColor("#ADD6FF"))
                .setCursor(Color.parseColor("#000000"))
                .setKeyword(Color.parseColor("#0000FF"))
                .setString(Color.parseColor("#A31515"))
                .setNumber(Color.parseColor("#098658"))
                .setComment(Color.parseColor("#008000"))
                .setFunction(Color.parseColor("#795E26"))
                .setClassName(Color.parseColor("#267F99"))
                .setVariable(Color.parseColor("#001080"))
                .setOperator(Color.parseColor("#000000"))
                .setAnnotation(Color.parseColor("#795E26"))
                .setTag(Color.parseColor("#800000"))
                .setAttribute(Color.parseColor("#FF0000"))
                .setConstant(Color.parseColor("#0070C1"))
                .setToolbarBackground(Color.parseColor("#DDDDDD"))
                .setStatusBarBackground(Color.parseColor("#007ACC"))
                .setTabBackground(Color.parseColor("#ECECEC"))
                .setTabActiveBackground(Color.parseColor("#FFFFFF"))
                .setSidebarBackground(Color.parseColor("#F3F3F3"))
                .build();
    }
    
    private EditorTheme createMonokaiTheme() {
        return new EditorTheme.Builder("Monokai")
                .setBackground(Color.parseColor("#272822"))
                .setForeground(Color.parseColor("#F8F8F2"))
                .setLineNumberBackground(Color.parseColor("#272822"))
                .setLineNumberForeground(Color.parseColor("#90908A"))
                .setCurrentLine(Color.parseColor("#3E3D32"))
                .setSelection(Color.parseColor("#49483E"))
                .setCursor(Color.parseColor("#F8F8F0"))
                .setKeyword(Color.parseColor("#F92672"))
                .setString(Color.parseColor("#E6DB74"))
                .setNumber(Color.parseColor("#AE81FF"))
                .setComment(Color.parseColor("#75715E"))
                .setFunction(Color.parseColor("#A6E22E"))
                .setClassName(Color.parseColor("#66D9EF"))
                .setVariable(Color.parseColor("#F8F8F2"))
                .setOperator(Color.parseColor("#F92672"))
                .setAnnotation(Color.parseColor("#A6E22E"))
                .setTag(Color.parseColor("#F92672"))
                .setAttribute(Color.parseColor("#A6E22E"))
                .setConstant(Color.parseColor("#AE81FF"))
                .setToolbarBackground(Color.parseColor("#1E1F1C"))
                .setStatusBarBackground(Color.parseColor("#75715E"))
                .setTabBackground(Color.parseColor("#1E1F1C"))
                .setTabActiveBackground(Color.parseColor("#272822"))
                .setSidebarBackground(Color.parseColor("#1E1F1C"))
                .build();
    }
    
    private EditorTheme createDraculaTheme() {
        return new EditorTheme.Builder("Dracula")
                .setBackground(Color.parseColor("#282A36"))
                .setForeground(Color.parseColor("#F8F8F2"))
                .setLineNumberBackground(Color.parseColor("#282A36"))
                .setLineNumberForeground(Color.parseColor("#6272A4"))
                .setCurrentLine(Color.parseColor("#44475A"))
                .setSelection(Color.parseColor("#44475A"))
                .setCursor(Color.parseColor("#F8F8F2"))
                .setKeyword(Color.parseColor("#FF79C6"))
                .setString(Color.parseColor("#F1FA8C"))
                .setNumber(Color.parseColor("#BD93F9"))
                .setComment(Color.parseColor("#6272A4"))
                .setFunction(Color.parseColor("#50FA7B"))
                .setClassName(Color.parseColor("#8BE9FD"))
                .setVariable(Color.parseColor("#F8F8F2"))
                .setOperator(Color.parseColor("#FF79C6"))
                .setAnnotation(Color.parseColor("#50FA7B"))
                .setTag(Color.parseColor("#FF79C6"))
                .setAttribute(Color.parseColor("#50FA7B"))
                .setConstant(Color.parseColor("#BD93F9"))
                .setToolbarBackground(Color.parseColor("#21222C"))
                .setStatusBarBackground(Color.parseColor("#BD93F9"))
                .setTabBackground(Color.parseColor("#21222C"))
                .setTabActiveBackground(Color.parseColor("#282A36"))
                .setSidebarBackground(Color.parseColor("#21222C"))
                .build();
    }
    
    private EditorTheme createOneDarkTheme() {
        return new EditorTheme.Builder("One Dark")
                .setBackground(Color.parseColor("#282C34"))
                .setForeground(Color.parseColor("#ABB2BF"))
                .setLineNumberBackground(Color.parseColor("#282C34"))
                .setLineNumberForeground(Color.parseColor("#4B5263"))
                .setCurrentLine(Color.parseColor("#2C323C"))
                .setSelection(Color.parseColor("#3E4451"))
                .setCursor(Color.parseColor("#528BFF"))
                .setKeyword(Color.parseColor("#C678DD"))
                .setString(Color.parseColor("#98C379"))
                .setNumber(Color.parseColor("#D19A66"))
                .setComment(Color.parseColor("#5C6370"))
                .setFunction(Color.parseColor("#61AFEF"))
                .setClassName(Color.parseColor("#E5C07B"))
                .setVariable(Color.parseColor("#E06C75"))
                .setOperator(Color.parseColor("#56B6C2"))
                .setAnnotation(Color.parseColor("#61AFEF"))
                .setTag(Color.parseColor("#E06C75"))
                .setAttribute(Color.parseColor("#D19A66"))
                .setConstant(Color.parseColor("#D19A66"))
                .setToolbarBackground(Color.parseColor("#21252B"))
                .setStatusBarBackground(Color.parseColor("#21252B"))
                .setTabBackground(Color.parseColor("#21252B"))
                .setTabActiveBackground(Color.parseColor("#282C34"))
                .setSidebarBackground(Color.parseColor("#21252B"))
                .build();
    }
    
    private EditorTheme createGitHubDarkTheme() {
        return new EditorTheme.Builder("GitHub Dark")
                .setBackground(Color.parseColor("#0D1117"))
                .setForeground(Color.parseColor("#C9D1D9"))
                .setLineNumberBackground(Color.parseColor("#0D1117"))
                .setLineNumberForeground(Color.parseColor("#484F58"))
                .setCurrentLine(Color.parseColor("#161B22"))
                .setSelection(Color.parseColor("#264F78"))
                .setCursor(Color.parseColor("#C9D1D9"))
                .setKeyword(Color.parseColor("#FF7B72"))
                .setString(Color.parseColor("#A5D6FF"))
                .setNumber(Color.parseColor("#79C0FF"))
                .setComment(Color.parseColor("#8B949E"))
                .setFunction(Color.parseColor("#D2A8FF"))
                .setClassName(Color.parseColor("#FFA657"))
                .setVariable(Color.parseColor("#FFA657"))
                .setOperator(Color.parseColor("#FF7B72"))
                .setAnnotation(Color.parseColor("#D2A8FF"))
                .setTag(Color.parseColor("#7EE787"))
                .setAttribute(Color.parseColor("#79C0FF"))
                .setConstant(Color.parseColor("#79C0FF"))
                .setToolbarBackground(Color.parseColor("#161B22"))
                .setStatusBarBackground(Color.parseColor("#238636"))
                .setTabBackground(Color.parseColor("#010409"))
                .setTabActiveBackground(Color.parseColor("#0D1117"))
                .setSidebarBackground(Color.parseColor("#010409"))
                .build();
    }
    
    private EditorTheme createSolarizedDarkTheme() {
        return new EditorTheme.Builder("Solarized Dark")
                .setBackground(Color.parseColor("#002B36"))
                .setForeground(Color.parseColor("#839496"))
                .setLineNumberBackground(Color.parseColor("#002B36"))
                .setLineNumberForeground(Color.parseColor("#586E75"))
                .setCurrentLine(Color.parseColor("#073642"))
                .setSelection(Color.parseColor("#073642"))
                .setCursor(Color.parseColor("#839496"))
                .setKeyword(Color.parseColor("#859900"))
                .setString(Color.parseColor("#2AA198"))
                .setNumber(Color.parseColor("#D33682"))
                .setComment(Color.parseColor("#586E75"))
                .setFunction(Color.parseColor("#268BD2"))
                .setClassName(Color.parseColor("#B58900"))
                .setVariable(Color.parseColor("#CB4B16"))
                .setOperator(Color.parseColor("#839496"))
                .setAnnotation(Color.parseColor("#268BD2"))
                .setTag(Color.parseColor("#268BD2"))
                .setAttribute(Color.parseColor("#93A1A1"))
                .setConstant(Color.parseColor("#6C71C4"))
                .setToolbarBackground(Color.parseColor("#073642"))
                .setStatusBarBackground(Color.parseColor("#586E75"))
                .setTabBackground(Color.parseColor("#073642"))
                .setTabActiveBackground(Color.parseColor("#002B36"))
                .setSidebarBackground(Color.parseColor("#073642"))
                .build();
    }
    
    private EditorTheme createSolarizedLightTheme() {
        return new EditorTheme.Builder("Solarized Light")
                .setBackground(Color.parseColor("#FDF6E3"))
                .setForeground(Color.parseColor("#657B83"))
                .setLineNumberBackground(Color.parseColor("#FDF6E3"))
                .setLineNumberForeground(Color.parseColor("#93A1A1"))
                .setCurrentLine(Color.parseColor("#EEE8D5"))
                .setSelection(Color.parseColor("#EEE8D5"))
                .setCursor(Color.parseColor("#657B83"))
                .setKeyword(Color.parseColor("#859900"))
                .setString(Color.parseColor("#2AA198"))
                .setNumber(Color.parseColor("#D33682"))
                .setComment(Color.parseColor("#93A1A1"))
                .setFunction(Color.parseColor("#268BD2"))
                .setClassName(Color.parseColor("#B58900"))
                .setVariable(Color.parseColor("#CB4B16"))
                .setOperator(Color.parseColor("#657B83"))
                .setAnnotation(Color.parseColor("#268BD2"))
                .setTag(Color.parseColor("#268BD2"))
                .setAttribute(Color.parseColor("#586E75"))
                .setConstant(Color.parseColor("#6C71C4"))
                .setToolbarBackground(Color.parseColor("#EEE8D5"))
                .setStatusBarBackground(Color.parseColor("#93A1A1"))
                .setTabBackground(Color.parseColor("#EEE8D5"))
                .setTabActiveBackground(Color.parseColor("#FDF6E3"))
                .setSidebarBackground(Color.parseColor("#EEE8D5"))
                .build();
    }
}
