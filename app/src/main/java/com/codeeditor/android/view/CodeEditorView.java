package com.codeeditor.android.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.codeeditor.android.R;
import com.codeeditor.android.syntax.SyntaxHighlighter;
import com.codeeditor.android.utils.AutoIndentHelper;
import com.codeeditor.android.utils.BracketMatcher;
import com.codeeditor.android.utils.UndoRedoManager;

public class CodeEditorView extends LinearLayout {
    
    private TextView lineNumberView;
    private EditText codeEditText;
    private ScrollView verticalScrollView;
    private HorizontalScrollView horizontalScrollView;
    private ScrollView lineNumberScrollView;
    
    private SyntaxHighlighter.Language currentLanguage = SyntaxHighlighter.Language.PLAIN_TEXT;
    private UndoRedoManager undoRedoManager;
    private AutoIndentHelper autoIndentHelper;
    private OnCursorChangeListener cursorChangeListener;
    private OnTextChangeListener textChangeListener;
    
    private boolean isUpdatingText = false;
    private Runnable highlightRunnable;
    private static final long HIGHLIGHT_DELAY = 300;
    
    private boolean autoIndentEnabled = true;
    private boolean autoBracketEnabled = true;
    private boolean lineNumbersEnabled = true;
    private boolean wordWrapEnabled = true;
    private boolean highlightCurrentLineEnabled = true;
    private int fontSize = 14;
    private int tabSize = 4;
    
    private int currentLineHighlightColor;
    private int matchingBracketColor;
    
    private int highlightedMatchStart = -1;
    private int highlightedMatchEnd = -1;
    
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;
    private static final int MIN_FONT_SIZE = 8;
    private static final int MAX_FONT_SIZE = 32;
    private OnFontSizeChangeListener fontSizeChangeListener;
    
    private OnAutocompleteListener autocompleteListener;
    private String currentWord = "";
    private int wordStart = 0;
    
    public interface OnCursorChangeListener {
        void onCursorChanged(int line, int column);
    }
    
    public interface OnTextChangeListener {
        void onTextChanged(String text);
    }
    
    public interface OnFontSizeChangeListener {
        void onFontSizeChanged(int newSize);
    }
    
    public interface OnAutocompleteListener {
        void onAutocompleteRequest(String prefix, int wordStart);
        void onAutocompleteDismiss();
    }
    
    public CodeEditorView(Context context) {
        super(context);
        init(context);
    }
    
    public CodeEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public CodeEditorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        setOrientation(HORIZONTAL);
        setBackgroundColor(ContextCompat.getColor(context, R.color.editor_background));
        
        undoRedoManager = new UndoRedoManager();
        autoIndentHelper = new AutoIndentHelper(tabSize, true);
        
        currentLineHighlightColor = ContextCompat.getColor(context, R.color.current_line_highlight);
        matchingBracketColor = ContextCompat.getColor(context, R.color.matching_bracket);
        
        loadPreferences(context);
        
        lineNumberView = new TextView(context);
        lineNumberView.setBackgroundColor(ContextCompat.getColor(context, R.color.line_number_background));
        lineNumberView.setTextColor(ContextCompat.getColor(context, R.color.line_number_text));
        lineNumberView.setTypeface(Typeface.MONOSPACE);
        lineNumberView.setTextSize(fontSize);
        lineNumberView.setGravity(Gravity.END);
        lineNumberView.setPadding(12, 12, 12, 12);
        lineNumberView.setText("1");
        lineNumberView.setMinWidth(48);
        
        codeEditText = new EditText(context);
        codeEditText.setBackgroundColor(ContextCompat.getColor(context, R.color.editor_background));
        codeEditText.setTextColor(ContextCompat.getColor(context, R.color.editor_text));
        codeEditText.setTypeface(Typeface.MONOSPACE);
        codeEditText.setTextSize(fontSize);
        codeEditText.setGravity(Gravity.START | Gravity.TOP);
        codeEditText.setPadding(12, 12, 12, 12);
        codeEditText.setInputType(InputType.TYPE_CLASS_TEXT 
            | InputType.TYPE_TEXT_FLAG_MULTI_LINE 
            | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        codeEditText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        codeEditText.setHorizontallyScrolling(!wordWrapEnabled);
        codeEditText.setHorizontalScrollBarEnabled(!wordWrapEnabled);
        codeEditText.setVerticalScrollBarEnabled(false);
        codeEditText.setMinHeight(300);
        
        if (autoBracketEnabled) {
            codeEditText.setFilters(new InputFilter[]{new BracketAutoCloseFilter()});
        }
        
        horizontalScrollView = new HorizontalScrollView(context);
        horizontalScrollView.setFillViewport(true);
        horizontalScrollView.setHorizontalScrollBarEnabled(!wordWrapEnabled);
        horizontalScrollView.addView(codeEditText, new LayoutParams(
            wordWrapEnabled ? LayoutParams.MATCH_PARENT : LayoutParams.WRAP_CONTENT, 
            LayoutParams.WRAP_CONTENT));
        
        verticalScrollView = new ScrollView(context) {
            @Override
            protected void onScrollChanged(int l, int t, int oldl, int oldt) {
                super.onScrollChanged(l, t, oldl, oldt);
                if (lineNumberScrollView != null) {
                    lineNumberScrollView.scrollTo(0, t);
                }
            }
        };
        verticalScrollView.setFillViewport(true);
        verticalScrollView.setVerticalScrollBarEnabled(true);
        verticalScrollView.addView(horizontalScrollView, new LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        
        lineNumberScrollView = new ScrollView(context) {
            @Override
            protected void onScrollChanged(int l, int t, int oldl, int oldt) {
                super.onScrollChanged(l, t, oldl, oldt);
                verticalScrollView.scrollTo(0, t);
            }
        };
        lineNumberScrollView.setVerticalScrollBarEnabled(false);
        lineNumberScrollView.setFillViewport(true);
        lineNumberScrollView.addView(lineNumberView, new LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        
        if (lineNumbersEnabled) {
            addView(lineNumberScrollView, new LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        }
        addView(verticalScrollView, new LayoutParams(
            0, LayoutParams.MATCH_PARENT, 1));
        
        setupTextWatcher();
        setupCursorListener();
        setupKeyboardShortcuts();
        setupPinchZoom(context);
        
        undoRedoManager.saveState("", 0);
    }
    
    private void setupPinchZoom(Context context) {
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 2.0f));
                
                int newSize = (int) (14 * scaleFactor);
                newSize = Math.max(MIN_FONT_SIZE, Math.min(newSize, MAX_FONT_SIZE));
                
                if (newSize != fontSize) {
                    fontSize = newSize;
                    codeEditText.setTextSize(fontSize);
                    lineNumberView.setTextSize(fontSize);
                    
                    if (fontSizeChangeListener != null) {
                        fontSizeChangeListener.onFontSizeChanged(fontSize);
                    }
                }
                return true;
            }
        });
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getPointerCount() > 1) {
            return true;
        }
        return super.onInterceptTouchEvent(ev);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() > 1) {
            scaleGestureDetector.onTouchEvent(event);
            return true;
        }
        return super.onTouchEvent(event);
    }
    
    private void loadPreferences(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        fontSize = Integer.parseInt(prefs.getString("font_size", "14"));
        tabSize = Integer.parseInt(prefs.getString("tab_size", "4"));
        autoIndentEnabled = prefs.getBoolean("auto_indent", true);
        autoBracketEnabled = prefs.getBoolean("auto_bracket", true);
        lineNumbersEnabled = prefs.getBoolean("line_numbers", true);
        wordWrapEnabled = prefs.getBoolean("word_wrap", true);
        highlightCurrentLineEnabled = prefs.getBoolean("highlight_current_line", true);
        
        autoIndentHelper.setTabSize(tabSize);
    }
    
    private void setupTextWatcher() {
        codeEditText.addTextChangedListener(new TextWatcher() {
            private int beforeLength = 0;
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                beforeLength = s.length();
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdatingText) {
                    return;
                }
                
                updateLineNumbers();
                scheduleHighlighting();
                
                String text = s.toString();
                int cursorPos = codeEditText.getSelectionStart();
                
                if (autoIndentEnabled && text.length() > beforeLength) {
                    int insertPos = cursorPos - 1;
                    if (insertPos >= 0 && insertPos < text.length() && text.charAt(insertPos) == '\n') {
                        handleNewLine(s, insertPos);
                    }
                }
                
                undoRedoManager.saveState(text, cursorPos);
                
                if (textChangeListener != null) {
                    textChangeListener.onTextChanged(text);
                }
                
                updateCursorPosition();
                checkForAutocomplete(text, cursorPos);
            }
        });
    }
    
    private void checkForAutocomplete(String text, int cursorPos) {
        if (autocompleteListener == null || cursorPos <= 0) {
            return;
        }
        
        int start = cursorPos - 1;
        while (start >= 0 && isIdentifierChar(text.charAt(start))) {
            start--;
        }
        start++;
        
        if (cursorPos - start >= 2) {
            currentWord = text.substring(start, cursorPos);
            wordStart = start;
            autocompleteListener.onAutocompleteRequest(currentWord, wordStart);
        } else {
            currentWord = "";
            autocompleteListener.onAutocompleteDismiss();
        }
    }
    
    private boolean isIdentifierChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }
    
    public void insertCompletion(String completion) {
        if (wordStart >= 0) {
            int cursorPos = codeEditText.getSelectionStart();
            isUpdatingText = true;
            codeEditText.getText().replace(wordStart, cursorPos, completion);
            isUpdatingText = false;
            updateLineNumbers();
            scheduleHighlighting();
        }
    }
    
    private void handleNewLine(Editable s, int insertPos) {
        String text = s.toString();
        String indent = autoIndentHelper.calculateNewLineIndent(text, insertPos, currentLanguage);
        
        if (!indent.isEmpty()) {
            isUpdatingText = true;
            int cursorPos = insertPos + 1;
            s.insert(cursorPos, indent);
            codeEditText.setSelection(cursorPos + indent.length());
            isUpdatingText = false;
        }
    }
    
    private void setupCursorListener() {
        codeEditText.setOnClickListener(v -> {
            updateCursorPosition();
            highlightMatchingBracket();
        });
        
        codeEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                updateCursorPosition();
            }
        });
        
        codeEditText.setAccessibilityDelegate(new AccessibilityDelegate() {
            @Override
            public void sendAccessibilityEvent(android.view.View host, int eventType) {
                super.sendAccessibilityEvent(host, eventType);
                if (eventType == android.view.accessibility.AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
                    updateCursorPosition();
                    highlightMatchingBracket();
                }
            }
        });
    }
    
    private void setupKeyboardShortcuts() {
        codeEditText.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                boolean ctrl = event.isCtrlPressed();
                
                if (ctrl) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_Z:
                            if (event.isShiftPressed()) {
                                redo();
                            } else {
                                undo();
                            }
                            return true;
                        case KeyEvent.KEYCODE_Y:
                            redo();
                            return true;
                        case KeyEvent.KEYCODE_D:
                            duplicateLine();
                            return true;
                        case KeyEvent.KEYCODE_SLASH:
                            toggleComment();
                            return true;
                    }
                }
                
                if (keyCode == KeyEvent.KEYCODE_TAB) {
                    handleTabKey(event.isShiftPressed());
                    return true;
                }
            }
            return false;
        });
    }
    
    private void handleTabKey(boolean shiftPressed) {
        int start = codeEditText.getSelectionStart();
        int end = codeEditText.getSelectionEnd();
        
        String text = codeEditText.getText().toString();
        
        if (start == end) {
            if (shiftPressed) {
                int lineStart = text.lastIndexOf('\n', start - 1) + 1;
                String beforeCursor = text.substring(lineStart, start);
                String indent = autoIndentHelper.getIndent();
                if (beforeCursor.startsWith(indent)) {
                    isUpdatingText = true;
                    codeEditText.getText().delete(lineStart, lineStart + indent.length());
                    codeEditText.setSelection(start - indent.length());
                    isUpdatingText = false;
                }
            } else {
                isUpdatingText = true;
                codeEditText.getText().insert(start, autoIndentHelper.getIndent());
                isUpdatingText = false;
            }
        } else {
            String selected = text.substring(start, end);
            String modified;
            if (shiftPressed) {
                modified = autoIndentHelper.decreaseIndent(selected);
            } else {
                modified = autoIndentHelper.increaseIndent(selected);
            }
            
            isUpdatingText = true;
            codeEditText.getText().replace(start, end, modified);
            codeEditText.setSelection(start, start + modified.length());
            isUpdatingText = false;
        }
    }
    
    private void duplicateLine() {
        String text = codeEditText.getText().toString();
        int cursorPos = codeEditText.getSelectionStart();
        
        int lineStart = text.lastIndexOf('\n', cursorPos - 1) + 1;
        int lineEnd = text.indexOf('\n', cursorPos);
        if (lineEnd == -1) lineEnd = text.length();
        
        String line = text.substring(lineStart, lineEnd);
        
        isUpdatingText = true;
        codeEditText.getText().insert(lineEnd, "\n" + line);
        codeEditText.setSelection(cursorPos + line.length() + 1);
        isUpdatingText = false;
    }
    
    private void toggleComment() {
        String text = codeEditText.getText().toString();
        int cursorPos = codeEditText.getSelectionStart();
        
        int lineStart = text.lastIndexOf('\n', cursorPos - 1) + 1;
        int lineEnd = text.indexOf('\n', cursorPos);
        if (lineEnd == -1) lineEnd = text.length();
        
        String line = text.substring(lineStart, lineEnd);
        String trimmedLine = line.trim();
        
        String commentPrefix = getCommentPrefix();
        
        isUpdatingText = true;
        if (trimmedLine.startsWith(commentPrefix)) {
            int commentStart = line.indexOf(commentPrefix);
            String uncommented = line.substring(0, commentStart) + 
                                line.substring(commentStart + commentPrefix.length());
            if (uncommented.startsWith(" ")) {
                uncommented = uncommented.substring(1);
            }
            codeEditText.getText().replace(lineStart, lineEnd, uncommented);
        } else {
            String commented = autoIndentHelper.getIndentForLine(line) + commentPrefix + " " + trimmedLine;
            codeEditText.getText().replace(lineStart, lineEnd, commented);
        }
        isUpdatingText = false;
    }
    
    private String getCommentPrefix() {
        switch (currentLanguage) {
            case PYTHON:
            case RUBY:
            case SHELL:
            case PERL:
            case R:
            case YAML:
            case TOML:
                return "#";
            case LUA:
            case SQL:
                return "--";
            case HTML:
            case XML:
                return "<!--";
            default:
                return "//";
        }
    }
    
    private void highlightMatchingBracket() {
        Editable editable = codeEditText.getText();
        if (editable == null) return;
        
        BackgroundColorSpan[] spans = editable.getSpans(0, editable.length(), BackgroundColorSpan.class);
        for (BackgroundColorSpan span : spans) {
            if (span.getBackgroundColor() == matchingBracketColor) {
                editable.removeSpan(span);
            }
        }
        
        int cursorPos = codeEditText.getSelectionStart();
        if (cursorPos <= 0 || cursorPos > editable.length()) return;
        
        String text = editable.toString();
        char charBefore = text.charAt(cursorPos - 1);
        
        if (BracketMatcher.isOpenBracket(charBefore) || BracketMatcher.isCloseBracket(charBefore)) {
            int matchPos = BracketMatcher.findMatchingBracket(text, cursorPos - 1);
            if (matchPos >= 0) {
                editable.setSpan(
                    new BackgroundColorSpan(matchingBracketColor),
                    cursorPos - 1, cursorPos,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                editable.setSpan(
                    new BackgroundColorSpan(matchingBracketColor),
                    matchPos, matchPos + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }
    }
    
    private void updateCursorPosition() {
        if (cursorChangeListener != null) {
            int pos = codeEditText.getSelectionStart();
            String text = codeEditText.getText().toString();
            
            int line = 1;
            int column = 1;
            
            for (int i = 0; i < pos && i < text.length(); i++) {
                if (text.charAt(i) == '\n') {
                    line++;
                    column = 1;
                } else {
                    column++;
                }
            }
            
            cursorChangeListener.onCursorChanged(line, column);
        }
    }
    
    private void updateLineNumbers() {
        String text = codeEditText.getText().toString();
        int lineCount = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lineCount++;
            }
        }
        
        StringBuilder lineNumbers = new StringBuilder();
        for (int i = 1; i <= lineCount; i++) {
            lineNumbers.append(i);
            if (i < lineCount) {
                lineNumbers.append("\n");
            }
        }
        
        lineNumberView.setText(lineNumbers.toString());
    }
    
    private void scheduleHighlighting() {
        if (highlightRunnable != null) {
            removeCallbacks(highlightRunnable);
        }
        
        highlightRunnable = () -> {
            if (!isUpdatingText && currentLanguage != SyntaxHighlighter.Language.PLAIN_TEXT) {
                applyHighlighting();
            }
        };
        
        postDelayed(highlightRunnable, HIGHLIGHT_DELAY);
    }
    
    private void applyHighlighting() {
        Editable editable = codeEditText.getText();
        if (editable == null || editable.length() == 0) {
            return;
        }
        
        int cursorPos = codeEditText.getSelectionStart();
        
        ForegroundColorSpan[] existingSpans = editable.getSpans(0, editable.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : existingSpans) {
            editable.removeSpan(span);
        }
        
        SyntaxHighlighter.applySpans(editable, currentLanguage);
        
        if (cursorPos >= 0 && cursorPos <= editable.length()) {
            codeEditText.setSelection(cursorPos);
        }
    }
    
    public void setText(String text) {
        isUpdatingText = true;
        codeEditText.setText(text != null ? text : "");
        isUpdatingText = false;
        
        undoRedoManager.clear();
        undoRedoManager.saveState(text != null ? text : "", 0);
        
        updateLineNumbers();
        if (currentLanguage != SyntaxHighlighter.Language.PLAIN_TEXT) {
            post(this::applyHighlighting);
        }
    }
    
    public String getText() {
        return codeEditText.getText().toString();
    }
    
    public void setLanguage(SyntaxHighlighter.Language language) {
        this.currentLanguage = language;
        if (language != SyntaxHighlighter.Language.PLAIN_TEXT) {
            post(this::applyHighlighting);
        }
    }
    
    public SyntaxHighlighter.Language getLanguage() {
        return currentLanguage;
    }
    
    public void undo() {
        UndoRedoManager.TextState state = undoRedoManager.undo();
        
        if (state != null) {
            isUpdatingText = true;
            codeEditText.setText(state.text);
            int pos = Math.min(state.cursorPosition, state.text.length());
            if (pos >= 0) {
                codeEditText.setSelection(pos);
            }
            isUpdatingText = false;
            
            updateLineNumbers();
            if (currentLanguage != SyntaxHighlighter.Language.PLAIN_TEXT) {
                post(this::applyHighlighting);
            }
        }
    }
    
    public void redo() {
        UndoRedoManager.TextState state = undoRedoManager.redo();
        
        if (state != null) {
            isUpdatingText = true;
            codeEditText.setText(state.text);
            int pos = Math.min(state.cursorPosition, state.text.length());
            if (pos >= 0) {
                codeEditText.setSelection(pos);
            }
            isUpdatingText = false;
            
            updateLineNumbers();
            if (currentLanguage != SyntaxHighlighter.Language.PLAIN_TEXT) {
                post(this::applyHighlighting);
            }
        }
    }
    
    public boolean canUndo() {
        return undoRedoManager.canUndo();
    }
    
    public boolean canRedo() {
        return undoRedoManager.canRedo();
    }
    
    public void setOnCursorChangeListener(OnCursorChangeListener listener) {
        this.cursorChangeListener = listener;
    }
    
    public void setOnTextChangeListener(OnTextChangeListener listener) {
        this.textChangeListener = listener;
    }
    
    public void clearHistory() {
        undoRedoManager.clear();
    }
    
    public void requestEditorFocus() {
        codeEditText.requestFocus();
    }
    
    public void goToLine(int lineNumber) {
        String text = codeEditText.getText().toString();
        int currentLine = 1;
        int position = 0;
        
        for (int i = 0; i < text.length(); i++) {
            if (currentLine == lineNumber) {
                position = i;
                break;
            }
            if (text.charAt(i) == '\n') {
                currentLine++;
            }
        }
        
        if (currentLine == lineNumber || (lineNumber == 1 && currentLine == 1)) {
            codeEditText.setSelection(position);
            codeEditText.requestFocus();
        }
    }
    
    public int getLineCount() {
        String text = codeEditText.getText().toString();
        int count = 1;
        for (char c : text.toCharArray()) {
            if (c == '\n') count++;
        }
        return count;
    }
    
    public int getSelectionStart() {
        return codeEditText.getSelectionStart();
    }
    
    public int getSelectionEnd() {
        return codeEditText.getSelectionEnd();
    }
    
    public void setSelection(int start, int end) {
        if (start >= 0 && end <= codeEditText.getText().length()) {
            codeEditText.setSelection(start, end);
        }
    }
    
    public void setSelection(int position) {
        if (position >= 0 && position <= codeEditText.getText().length()) {
            codeEditText.setSelection(position);
        }
    }
    
    public void replaceText(int start, int end, String replacement) {
        isUpdatingText = true;
        codeEditText.getText().replace(start, end, replacement);
        isUpdatingText = false;
        
        updateLineNumbers();
        scheduleHighlighting();
    }
    
    public void highlightMatch(int start, int end) {
        clearHighlightedMatch();
        
        if (start >= 0 && end <= codeEditText.getText().length()) {
            Editable editable = codeEditText.getText();
            editable.setSpan(
                new BackgroundColorSpan(ContextCompat.getColor(getContext(), R.color.search_highlight)),
                start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            highlightedMatchStart = start;
            highlightedMatchEnd = end;
            
            codeEditText.setSelection(start, end);
        }
    }
    
    public void clearHighlightedMatch() {
        if (highlightedMatchStart >= 0) {
            Editable editable = codeEditText.getText();
            BackgroundColorSpan[] spans = editable.getSpans(
                highlightedMatchStart, highlightedMatchEnd, BackgroundColorSpan.class);
            for (BackgroundColorSpan span : spans) {
                editable.removeSpan(span);
            }
            highlightedMatchStart = -1;
            highlightedMatchEnd = -1;
        }
    }
    
    public void setFontSize(int size) {
        this.fontSize = size;
        codeEditText.setTextSize(size);
        lineNumberView.setTextSize(size);
    }
    
    public void setWordWrap(boolean enabled) {
        this.wordWrapEnabled = enabled;
        codeEditText.setHorizontallyScrolling(!enabled);
        codeEditText.setHorizontalScrollBarEnabled(!enabled);
        horizontalScrollView.setHorizontalScrollBarEnabled(!enabled);
    }
    
    public void setLineNumbersVisible(boolean visible) {
        this.lineNumbersEnabled = visible;
        lineNumberScrollView.setVisibility(visible ? VISIBLE : GONE);
    }
    
    public void setAutoIndent(boolean enabled) {
        this.autoIndentEnabled = enabled;
    }
    
    public void setAutoBracket(boolean enabled) {
        this.autoBracketEnabled = enabled;
        if (enabled) {
            codeEditText.setFilters(new InputFilter[]{new BracketAutoCloseFilter()});
        } else {
            codeEditText.setFilters(new InputFilter[0]);
        }
    }
    
    public void insertText(String text) {
        int start = codeEditText.getSelectionStart();
        int end = codeEditText.getSelectionEnd();
        
        isUpdatingText = true;
        codeEditText.getText().replace(start, end, text);
        isUpdatingText = false;
        
        updateLineNumbers();
        scheduleHighlighting();
    }
    
    private class BracketAutoCloseFilter implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, 
                                   Spanned dest, int dstart, int dend) {
            if (!autoBracketEnabled || source.length() != 1) {
                return null;
            }
            
            char c = source.charAt(0);
            String destStr = dest.toString();
            
            if (BracketMatcher.shouldAutoClose(destStr, dstart, c)) {
                String pair = BracketMatcher.getAutoClosePair(c);
                if (pair != null) {
                    post(() -> {
                        int pos = codeEditText.getSelectionStart();
                        if (pos > 0) {
                            codeEditText.setSelection(pos - 1);
                        }
                    });
                    return pair;
                }
            }
            
            return null;
        }
    }
    
    public void applyTheme(int backgroundColor, int foregroundColor, 
                           int lineNumberBg, int lineNumberFg,
                           int currentLineColor, int selectionColor, int cursorColor) {
        setBackgroundColor(backgroundColor);
        codeEditText.setBackgroundColor(backgroundColor);
        codeEditText.setTextColor(foregroundColor);
        codeEditText.setHighlightColor(selectionColor);
        
        lineNumberView.setBackgroundColor(lineNumberBg);
        lineNumberView.setTextColor(lineNumberFg);
        
        currentLineHighlightColor = currentLineColor;
        
        scheduleHighlighting();
    }
    
    public void setFont(Typeface typeface) {
        if (typeface != null) {
            codeEditText.setTypeface(typeface);
            lineNumberView.setTypeface(typeface);
        }
    }
    
    public EditText getCodeEditText() {
        return codeEditText;
    }
    
    public TextView getLineNumberView() {
        return lineNumberView;
    }
    
    public void setOnFontSizeChangeListener(OnFontSizeChangeListener listener) {
        this.fontSizeChangeListener = listener;
    }
    
    public void setOnAutocompleteListener(OnAutocompleteListener listener) {
        this.autocompleteListener = listener;
    }
    
    public int getFontSize() {
        return fontSize;
    }
    
    public void duplicateCurrentLine() {
        String text = codeEditText.getText().toString();
        int cursorPos = codeEditText.getSelectionStart();
        
        int lineStart = text.lastIndexOf('\n', cursorPos - 1) + 1;
        int lineEnd = text.indexOf('\n', cursorPos);
        if (lineEnd == -1) lineEnd = text.length();
        
        String line = text.substring(lineStart, lineEnd);
        
        isUpdatingText = true;
        codeEditText.getText().insert(lineEnd, "\n" + line);
        codeEditText.setSelection(cursorPos + line.length() + 1);
        isUpdatingText = false;
        
        updateLineNumbers();
        scheduleHighlighting();
    }
    
    public void toggleCurrentLineComment() {
        String text = codeEditText.getText().toString();
        int cursorPos = codeEditText.getSelectionStart();
        
        int lineStart = text.lastIndexOf('\n', cursorPos - 1) + 1;
        int lineEnd = text.indexOf('\n', cursorPos);
        if (lineEnd == -1) lineEnd = text.length();
        
        String line = text.substring(lineStart, lineEnd);
        String trimmedLine = line.trim();
        
        String commentPrefix = getCommentPrefix();
        
        isUpdatingText = true;
        if (trimmedLine.startsWith(commentPrefix)) {
            int commentStart = line.indexOf(commentPrefix);
            String uncommented = line.substring(0, commentStart) + 
                                line.substring(commentStart + commentPrefix.length());
            if (uncommented.length() > commentStart && uncommented.charAt(commentStart) == ' ') {
                uncommented = uncommented.substring(0, commentStart) + uncommented.substring(commentStart + 1);
            }
            codeEditText.getText().replace(lineStart, lineEnd, uncommented);
        } else {
            String commented = autoIndentHelper.getIndentForLine(line) + commentPrefix + " " + trimmedLine;
            codeEditText.getText().replace(lineStart, lineEnd, commented);
        }
        isUpdatingText = false;
        
        updateLineNumbers();
        scheduleHighlighting();
    }
    
    public void selectAll() {
        codeEditText.selectAll();
    }
    
    public void copy() {
        int start = codeEditText.getSelectionStart();
        int end = codeEditText.getSelectionEnd();
        if (start != end) {
            android.content.ClipboardManager clipboard = 
                (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            String selectedText = codeEditText.getText().toString().substring(start, end);
            android.content.ClipData clip = android.content.ClipData.newPlainText("code", selectedText);
            clipboard.setPrimaryClip(clip);
        }
    }
    
    public void cut() {
        copy();
        int start = codeEditText.getSelectionStart();
        int end = codeEditText.getSelectionEnd();
        if (start != end) {
            isUpdatingText = true;
            codeEditText.getText().delete(start, end);
            isUpdatingText = false;
            updateLineNumbers();
            scheduleHighlighting();
        }
    }
    
    public void paste() {
        android.content.ClipboardManager clipboard = 
            (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null) {
            CharSequence pasteText = clipboard.getPrimaryClip().getItemAt(0).getText();
            if (pasteText != null) {
                insertText(pasteText.toString());
            }
        }
    }
}
