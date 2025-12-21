package com.codeeditor.android.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.codeeditor.android.R;
import com.google.android.material.button.MaterialButton;

import com.codeeditor.android.syntax.SyntaxHighlighter;

public class SymbolToolbarView extends HorizontalScrollView {

    private LinearLayout container;
    private OnSymbolClickListener listener;
    private SyntaxHighlighter.Language currentLanguage = SyntaxHighlighter.Language.PLAIN_TEXT;

    private static final String[] COMMON_SYMBOLS = {"Tab", "{", "}", "(", ")", "[", "]", "<", ">", ";", ":", "'", "\"", "=", "+", "-", "*", "/", "\\", "|", "&", "!", "?", "@", "#", "$", "%", "^", "_", "~", "`"};
    
    private static final String[] OPERATORS = {"->", "=>", "::", "++", "--", "==", "!=", "<=", ">=", "&&", "||", "<<", ">>", "??", "?.", ".."};
    
    private static final String[] JAVA_KOTLIN_SYMBOLS = {"Tab", "{", "}", "(", ")", "[", "]", "<", ">", ";", ":", "@", ".", "=", "->", "==", "!=", "&&", "||"};
    private static final String[] PYTHON_SYMBOLS = {"Tab", "(", ")", "[", "]", "{", "}", ":", "=", "->", "==", "!=", "#", "_", "\"", "'", "*", "**"};
    private static final String[] JS_TS_SYMBOLS = {"Tab", "{", "}", "(", ")", "[", "]", ";", ":", "=>", "===", "!==", "&&", "||", "?.", "??", "`", "$"};
    private static final String[] HTML_SYMBOLS = {"Tab", "<", ">", "/", "=", "\"", "'", "{", "}", "!", "-", "."};
    private static final String[] CSS_SYMBOLS = {"Tab", "{", "}", "(", ")", "[", "]", ";", ":", "#", ".", ",", "!", "@", "%", "px", "em", "rem"};

    public interface OnSymbolClickListener {
        void onSymbolClick(String symbol);
    }

    public SymbolToolbarView(Context context) {
        super(context);
        init(context);
    }

    public SymbolToolbarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SymbolToolbarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setHorizontalScrollBarEnabled(false);
        setBackgroundColor(ContextCompat.getColor(context, R.color.symbol_toolbar_background));
        setPadding(8, 4, 8, 4);

        container = new LinearLayout(context);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setLayoutParams(new LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        addSymbols(context);
        addView(container);
    }

    private void addSymbols(Context context) {
        for (String symbol : COMMON_SYMBOLS) {
            addSymbolButton(context, symbol);
        }
        
        View divider = new View(context);
        divider.setBackgroundColor(ContextCompat.getColor(context, R.color.divider));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(1, 
            LinearLayout.LayoutParams.MATCH_PARENT);
        dividerParams.setMargins(8, 8, 8, 8);
        container.addView(divider, dividerParams);
        
        for (String symbol : OPERATORS) {
            addSymbolButton(context, symbol);
        }
    }

    private void addSymbolButton(Context context, String symbol) {
        MaterialButton button = new MaterialButton(context, null, 
            com.google.android.material.R.attr.materialButtonOutlinedStyle);
        
        button.setText(symbol);
        button.setTextSize(12);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(16, 8, 16, 8);
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setTextColor(ContextCompat.getColor(context, R.color.symbol_text));
        button.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.symbol_button_background));
        button.setStrokeWidth(0);
        button.setCornerRadius(8);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(4, 0, 4, 0);

        button.setOnClickListener(v -> {
            if (listener != null) {
                String insertSymbol = symbol.equals("Tab") ? "\t" : symbol;
                listener.onSymbolClick(insertSymbol);
            }
        });

        container.addView(button, params);
    }

    public void setOnSymbolClickListener(OnSymbolClickListener listener) {
        this.listener = listener;
    }

    public void setLanguage(SyntaxHighlighter.Language language) {
        if (language == currentLanguage) return;
        currentLanguage = language;
        
        container.removeAllViews();
        Context context = getContext();
        
        String[] symbols = getSymbolsForLanguage(language);
        for (String symbol : symbols) {
            addSymbolButton(context, symbol);
        }
        
        View divider = new View(context);
        divider.setBackgroundColor(ContextCompat.getColor(context, R.color.divider));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(1, 
            LinearLayout.LayoutParams.MATCH_PARENT);
        dividerParams.setMargins(8, 8, 8, 8);
        container.addView(divider, dividerParams);
        
        for (String op : OPERATORS) {
            addSymbolButton(context, op);
        }
    }
    
    private String[] getSymbolsForLanguage(SyntaxHighlighter.Language language) {
        switch (language) {
            case JAVA:
            case KOTLIN:
                return JAVA_KOTLIN_SYMBOLS;
            case PYTHON:
                return PYTHON_SYMBOLS;
            case JAVASCRIPT:
            case TYPESCRIPT:
                return JS_TS_SYMBOLS;
            case HTML:
            case XML:
                return HTML_SYMBOLS;
            case CSS:
            case SCSS:
                return CSS_SYMBOLS;
            default:
                return COMMON_SYMBOLS;
        }
    }
}
