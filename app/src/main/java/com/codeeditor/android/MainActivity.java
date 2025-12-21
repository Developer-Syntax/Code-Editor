package com.codeeditor.android;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.codeeditor.android.adapter.FileTreeAdapter;
import com.codeeditor.android.adapter.RecentFilesAdapter;
import com.codeeditor.android.adapter.RepoAdapter;
import com.codeeditor.android.adapter.SnippetAdapter;
import com.codeeditor.android.adapter.TabAdapter;
import com.codeeditor.android.autocomplete.AutocompleteEngine;
import com.codeeditor.android.databinding.ActivityMainBinding;
import com.codeeditor.android.github.GitHubApiService;
import com.codeeditor.android.github.GitHubAuthManager;
import com.codeeditor.android.model.OpenFile;
import com.codeeditor.android.syntax.SyntaxHighlighter;
import com.codeeditor.android.theme.EditorTheme;
import com.codeeditor.android.theme.ThemeManager;
import com.codeeditor.android.utils.FileTemplates;
import com.codeeditor.android.utils.FileUtils;
import com.codeeditor.android.utils.FindReplaceHelper;
import com.codeeditor.android.utils.RecentFilesManager;
import com.codeeditor.android.utils.SnippetManager;
import com.codeeditor.android.view.AutocompleteAdapter;
import com.codeeditor.android.view.QuickActionsView;
import com.codeeditor.android.view.SymbolToolbarView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {
    
    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final int MANAGE_STORAGE_REQUEST_CODE = 101;
    
    private ActivityMainBinding binding;
    private GitHubAuthManager authManager;
    private GitHubApiService apiService;
    
    private TabAdapter tabAdapter;
    private FileTreeAdapter fileTreeAdapter;
    private RepoAdapter repoAdapter;
    
    private String currentOwner;
    private String currentRepo;
    private String currentBranch;
    
    private List<GitHubApiService.Repository> allRepos = new ArrayList<>();
    
    private FindReplaceHelper findReplaceHelper;
    private AlertDialog findReplaceDialog;
    private Handler autoSaveHandler;
    private Runnable autoSaveRunnable;
    private boolean autoSaveEnabled = true;
    private int autoSaveInterval = 30;
    
    private AutocompleteEngine autocompleteEngine;
    private AutocompleteAdapter autocompleteAdapter;
    private RecentFilesManager recentFilesManager;
    private RecentFilesAdapter recentFilesAdapter;
    private SnippetManager snippetManager;
    private SyntaxHighlighter.Language currentLanguage = SyntaxHighlighter.Language.PLAIN_TEXT;
    
    private final ActivityResultLauncher<Intent> openFileLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    openLocalFile(uri);
                }
            }
        }
    );
    
    private final ActivityResultLauncher<Intent> saveFileLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    saveLocalFile(uri);
                }
            }
        }
    );
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            
            authManager = new GitHubAuthManager(this);
            findReplaceHelper = new FindReplaceHelper();
            autoSaveHandler = new Handler(Looper.getMainLooper());
            
            loadPreferences();
            setupToolbar();
            setupAdapters();
            setupNavigation();
            setupCodeEditor();
            setupWelcomePanel();
            setupKeyboardShortcuts();
            setupAutoSave();
            applyThemeSettings();
            
            setupNewFeatures();
            
            updateGitHubUI();
            updateWelcomeVisibility();
            
            handleIntent(getIntent());
            
            checkStoragePermission();
        } catch (Exception e) {
            showErrorDialog("Initialization Error", "Failed to initialize the application", e);
        }
    }
    
    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                showStoragePermissionDialog();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                showStoragePermissionDialog();
            }
        }
    }
    
    private void showStoragePermissionDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.storage_permission_title)
            .setMessage(R.string.storage_permission_message)
            .setCancelable(false)
            .setPositiveButton(R.string.grant_permission, (dialog, which) -> {
                requestStoragePermission();
            })
            .setNegativeButton(R.string.exit_app, (dialog, which) -> {
                finish();
            })
            .show();
    }
    
    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, MANAGE_STORAGE_REQUEST_CODE);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, MANAGE_STORAGE_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                STORAGE_PERMISSION_CODE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_SHORT).show();
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    showStoragePermissionDialog();
                } else {
                    showSettingsDialog();
                }
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MANAGE_STORAGE_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_SHORT).show();
                } else {
                    showStoragePermissionDialog();
                }
            }
        }
    }
    
    private void showSettingsDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.permission_required)
            .setMessage(R.string.permission_settings_message)
            .setCancelable(false)
            .setPositiveButton(R.string.open_settings, (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            })
            .setNegativeButton(R.string.exit_app, (dialog, which) -> {
                finish();
            })
            .show();
    }
    
    private void showErrorDialog(String title, String message, Throwable error) {
        try {
            String errorDetails = message;
            if (error != null) {
                errorDetails += "\n\n" + error.getClass().getSimpleName();
                if (error.getMessage() != null) {
                    errorDetails += ": " + error.getMessage();
                }
            }
            
            new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(errorDetails)
                .setPositiveButton("OK", null)
                .setNeutralButton("Details", (dialog, which) -> {
                    if (error != null) {
                        showFullErrorDetails(error);
                    }
                })
                .show();
        } catch (Exception e) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }
    
    private void showFullErrorDetails(Throwable error) {
        try {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            error.printStackTrace(pw);
            String stackTrace = sw.toString();
            
            android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
            android.widget.TextView textView = new android.widget.TextView(this);
            textView.setText(stackTrace);
            textView.setPadding(32, 32, 32, 32);
            textView.setTextIsSelectable(true);
            textView.setTypeface(android.graphics.Typeface.MONOSPACE);
            textView.setTextSize(10);
            scrollView.addView(textView);
            
            new AlertDialog.Builder(this)
                .setTitle("Error Details")
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .setNeutralButton("Copy", (dialog, which) -> {
                    android.content.ClipboardManager clipboard = 
                            (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        android.content.ClipData clip = android.content.ClipData.newPlainText("Error", stackTrace);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }
    
    private void handleIntent(Intent intent) {
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();
            if (data != null && "codeeditor".equals(data.getScheme()) && "github-callback".equals(data.getHost())) {
                handleGitHubCallback(data);
            }
        }
    }
    
    private void handleGitHubCallback(Uri callbackUri) {
        authManager.handleCallback(callbackUri, new GitHubAuthManager.AuthCallback() {
            @Override
            public void onSuccess(String accessToken) {
                apiService = new GitHubApiService(accessToken);
                fetchUserInfo();
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                updateGitHubUI();
            }
        });
    }
    
    private void setupNewFeatures() {
        try {
            autocompleteEngine = new AutocompleteEngine();
            recentFilesManager = new RecentFilesManager(this);
            snippetManager = new SnippetManager(this);
            
            setupSymbolToolbar();
            setupAutocomplete();
            setupQuickActions();
            setupRecentFiles();
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error setting up new features", e);
            showErrorDialog("Setup Error", "Failed to initialize some features", e);
        }
    }
    
    private void setupSymbolToolbar() {
        binding.symbolToolbar.setOnSymbolClickListener(symbol -> {
            binding.codeEditor.insertText(symbol);
        });
    }
    
    private void setupAutocomplete() {
        autocompleteAdapter = new AutocompleteAdapter();
        binding.rvAutocomplete.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAutocomplete.setAdapter(autocompleteAdapter);
        
        autocompleteAdapter.setOnSuggestionClickListener(suggestion -> {
            binding.codeEditor.insertCompletion(suggestion.text);
            hideAutocomplete();
        });
        
        binding.codeEditor.setOnAutocompleteListener(new com.codeeditor.android.view.CodeEditorView.OnAutocompleteListener() {
            @Override
            public void onAutocompleteRequest(String prefix, int wordStart) {
                autocompleteEngine.setLanguage(currentLanguage);
                List<AutocompleteAdapter.Suggestion> suggestions = autocompleteEngine.getSuggestions(prefix, 10);
                if (!suggestions.isEmpty()) {
                    autocompleteAdapter.setSuggestions(suggestions);
                    showAutocomplete();
                } else {
                    hideAutocomplete();
                }
            }
            
            @Override
            public void onAutocompleteDismiss() {
                hideAutocomplete();
            }
        });
        
        binding.codeEditor.setOnFontSizeChangeListener(newSize -> {
            Toast.makeText(this, getString(R.string.font_size_changed, newSize), Toast.LENGTH_SHORT).show();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit().putString("font_size", String.valueOf(newSize)).apply();
        });
    }
    
    private void showAutocomplete() {
        binding.autocompletePanel.setVisibility(View.VISIBLE);
    }
    
    private void hideAutocomplete() {
        binding.autocompletePanel.setVisibility(View.GONE);
    }
    
    private void setupQuickActions() {
        binding.fabQuickActions.setOnQuickActionListener(action -> {
            switch (action) {
                case UNDO:
                    binding.codeEditor.undo();
                    break;
                case REDO:
                    binding.codeEditor.redo();
                    break;
                case COPY:
                    binding.codeEditor.copy();
                    break;
                case CUT:
                    binding.codeEditor.cut();
                    break;
                case PASTE:
                    binding.codeEditor.paste();
                    break;
                case SELECT_ALL:
                    binding.codeEditor.selectAll();
                    break;
                case FIND:
                    showFindReplaceDialog();
                    break;
                case REPLACE:
                    showFindReplaceDialog();
                    break;
                case GO_TO_LINE:
                    showGoToLineDialog();
                    break;
                case DUPLICATE_LINE:
                    binding.codeEditor.duplicateCurrentLine();
                    break;
                case TOGGLE_COMMENT:
                    binding.codeEditor.toggleCurrentLineComment();
                    break;
                case SNIPPETS:
                    showSnippetsDialog();
                    break;
                case SAVE:
                    saveCurrentFile();
                    break;
            }
        });
    }
    
    private void setupRecentFiles() {
        recentFilesAdapter = new RecentFilesAdapter();
        binding.rvRecentFiles.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRecentFiles.setAdapter(recentFilesAdapter);
        
        recentFilesAdapter.setOnRecentFileClickListener(new RecentFilesAdapter.OnRecentFileClickListener() {
            @Override
            public void onRecentFileClick(RecentFilesManager.RecentFile file) {
                if (file.isGitHub) {
                    if (authManager.isLoggedIn()) {
                        openGitHubRecentFile(file);
                    } else {
                        Toast.makeText(MainActivity.this, R.string.sign_in_github, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    try {
                        Uri uri = Uri.parse(file.path);
                        openLocalFile(uri);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, R.string.error_opening, Toast.LENGTH_SHORT).show();
                    }
                }
            }
            
            @Override
            public void onRecentFileLongClick(RecentFilesManager.RecentFile file, int position) {
                new AlertDialog.Builder(MainActivity.this)
                    .setMessage(R.string.delete_confirm)
                    .setPositiveButton(R.string.delete, (d, w) -> {
                        recentFilesManager.removeRecentFile(file);
                        updateRecentFilesUI();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            }
        });
        
        binding.btnClearRecent.setOnClickListener(v -> {
            recentFilesManager.clearRecentFiles();
            updateRecentFilesUI();
        });
        
        updateRecentFilesUI();
    }
    
    private void updateRecentFilesUI() {
        List<RecentFilesManager.RecentFile> recentFiles = recentFilesManager.getRecentFiles();
        boolean hasRecent = !recentFiles.isEmpty();
        
        binding.tvRecentFilesTitle.setVisibility(hasRecent ? View.VISIBLE : View.GONE);
        binding.rvRecentFiles.setVisibility(hasRecent ? View.VISIBLE : View.GONE);
        binding.btnClearRecent.setVisibility(hasRecent ? View.VISIBLE : View.GONE);
        
        if (hasRecent) {
            recentFilesAdapter.setRecentFiles(recentFiles);
        }
    }
    
    private void openGitHubRecentFile(RecentFilesManager.RecentFile file) {
        if (apiService == null) {
            String token = authManager.getAccessToken();
            apiService = new GitHubApiService(token);
        }
        
        apiService.getFileContent(file.owner, file.repo, file.path, file.branch,
            new GitHubApiService.ApiCallback<GitHubApiService.GitHubFile>() {
                @Override
                public void onSuccess(GitHubApiService.GitHubFile ghFile) {
                    OpenFile openFile = new OpenFile(ghFile.name, ghFile.path, ghFile.content, ghFile.sha);
                    openFile.setGitHubInfo(file.owner, file.repo, file.branch);
                    
                    tabAdapter.addTab(openFile);
                    displayFile(openFile);
                    updateWelcomeVisibility();
                }
                
                @Override
                public void onError(String error) {
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void showSnippetsDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_snippets, null);
        
        RecyclerView rvSnippets = dialogView.findViewById(R.id.rvSnippets);
        EditText etSearch = dialogView.findViewById(R.id.etSnippetSearch);
        
        SnippetAdapter snippetAdapter = new SnippetAdapter();
        rvSnippets.setLayoutManager(new LinearLayoutManager(this));
        rvSnippets.setAdapter(snippetAdapter);
        
        List<SnippetManager.Snippet> allSnippets = new ArrayList<>();
        allSnippets.addAll(SnippetManager.getBuiltInSnippets(currentLanguage));
        allSnippets.addAll(snippetManager.getCustomSnippets(currentLanguage));
        snippetAdapter.setSnippets(allSnippets);
        
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogView)
            .create();
        
        snippetAdapter.setOnSnippetClickListener(new SnippetAdapter.OnSnippetClickListener() {
            @Override
            public void onSnippetClick(SnippetManager.Snippet snippet) {
                binding.codeEditor.insertText(snippet.code);
                dialog.dismiss();
            }
            
            @Override
            public void onSnippetLongClick(SnippetManager.Snippet snippet, int position) {
                if (snippet.isCustom) {
                    new AlertDialog.Builder(MainActivity.this)
                        .setMessage(R.string.delete_confirm)
                        .setPositiveButton(R.string.delete, (d, w) -> {
                            snippetManager.deleteCustomSnippet(currentLanguage, snippet.trigger);
                            Toast.makeText(MainActivity.this, R.string.snippet_deleted, Toast.LENGTH_SHORT).show();
                            allSnippets.remove(position);
                            snippetAdapter.notifyItemRemoved(position);
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                }
            }
        });
        
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase();
                List<SnippetManager.Snippet> filtered = allSnippets.stream()
                    .filter(sn -> sn.name.toLowerCase().contains(query) || 
                                  sn.trigger.toLowerCase().contains(query))
                    .collect(Collectors.toList());
                snippetAdapter.setSnippets(filtered);
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnAddSnippet).setOnClickListener(v -> {
            dialog.dismiss();
            showAddSnippetDialog();
        });
        
        dialog.show();
    }
    
    private void showAddSnippetDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_snippet, null);
        
        EditText etName = dialogView.findViewById(R.id.etSnippetName);
        EditText etTrigger = dialogView.findViewById(R.id.etSnippetTrigger);
        EditText etCode = dialogView.findViewById(R.id.etSnippetCode);
        
        new AlertDialog.Builder(this)
            .setTitle(R.string.add_snippet)
            .setView(dialogView)
            .setPositiveButton(R.string.save, (d, w) -> {
                String name = etName.getText().toString().trim();
                String trigger = etTrigger.getText().toString().trim();
                String code = etCode.getText().toString();
                
                if (!name.isEmpty() && !trigger.isEmpty() && !code.isEmpty()) {
                    SnippetManager.Snippet snippet = new SnippetManager.Snippet(
                        name, trigger, code, "Custom snippet", currentLanguage, true
                    );
                    snippetManager.saveCustomSnippet(snippet);
                    Toast.makeText(this, R.string.snippet_saved, Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    private void loadPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        autoSaveEnabled = prefs.getBoolean("auto_save", true);
        autoSaveInterval = Integer.parseInt(prefs.getString("auto_save_interval", "30"));
    }
    
    private void applyThemeSettings() {
        ThemeManager themeManager = ThemeManager.getInstance(this);
        themeManager.loadThemeFromPreferences();
        EditorTheme theme = themeManager.getCurrentTheme();
        
        SyntaxHighlighter.setThemeColors(
            theme.keyword, theme.string, theme.number, theme.comment,
            theme.function, theme.className, theme.annotation,
            theme.tag, theme.attribute, theme.operator,
            theme.variable, theme.constant
        );
        
        binding.codeEditor.applyTheme(
            theme.background, theme.foreground,
            theme.lineNumberBackground, theme.lineNumberForeground,
            theme.currentLine, theme.selection, theme.cursor
        );
        
        binding.codeEditor.setFont(themeManager.getCurrentFont());
        
        binding.toolbar.setBackgroundColor(theme.toolbarBackground);
        binding.rvTabs.setBackgroundColor(theme.tabBackground);
        getWindow().setStatusBarColor(theme.statusBarBackground);
    }
    
    private void setupKeyboardShortcuts() {
    }
    
    private void setupAutoSave() {
        autoSaveRunnable = () -> {
            if (autoSaveEnabled) {
                autoSaveCurrentFile();
                autoSaveHandler.postDelayed(autoSaveRunnable, autoSaveInterval * 1000L);
            }
        };
        
        if (autoSaveEnabled) {
            autoSaveHandler.postDelayed(autoSaveRunnable, autoSaveInterval * 1000L);
        }
    }
    
    private void autoSaveCurrentFile() {
        OpenFile file = tabAdapter.getSelectedTab();
        if (file != null && file.isModified) {
            String currentText = binding.codeEditor.getText();
            file.content = currentText;
            
            if (file.localUri != null && !file.localUri.isEmpty()) {
                try {
                    Uri uri = Uri.parse(file.localUri);
                    FileUtils.writeToUri(this, uri, currentText);
                    file.markSaved(null);
                    tabAdapter.notifyItemChanged(tabAdapter.getSelectedPosition());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_files);
        }
        
        binding.toolbar.setNavigationOnClickListener(v -> {
            binding.drawerLayout.openDrawer(GravityCompat.START);
        });
    }
    
    private void setupAdapters() {
        tabAdapter = new TabAdapter();
        binding.rvTabs.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvTabs.setAdapter(tabAdapter);
        
        tabAdapter.setOnTabClickListener(new TabAdapter.OnTabClickListener() {
            @Override
            public void onTabClick(OpenFile file, int position) {
                switchToTab(position);
            }
            
            @Override
            public void onTabClose(OpenFile file, int position) {
                closeTab(position);
            }
        });
        
        fileTreeAdapter = new FileTreeAdapter();
        binding.rvFileTree.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFileTree.setAdapter(fileTreeAdapter);
        
        fileTreeAdapter.setOnItemClickListener(new FileTreeAdapter.OnItemClickListener() {
            @Override
            public void onFileClick(FileTreeAdapter.FileTreeItem item) {
                openGitHubFile(item);
            }
            
            @Override
            public void onFolderClick(FileTreeAdapter.FileTreeItem item, int position) {
                toggleFolder(item, position);
            }
        });
        
        repoAdapter = new RepoAdapter();
        binding.rvRepos.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRepos.setAdapter(repoAdapter);
        
        repoAdapter.setOnRepoClickListener(this::openRepository);
    }
    
    private void setupNavigation() {
        View headerView = binding.navigationView.getHeaderView(0);
        if (headerView != null) {
            headerView.setOnClickListener(v -> {
                if (!authManager.isLoggedIn()) {
                    startGitHubOAuth();
                }
            });
        }
        
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                showSidebarPanel(position);
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        
        binding.btnLogout.setOnClickListener(v -> confirmLogout());
        binding.btnGitHubLogin.setOnClickListener(v -> startGitHubOAuth());
        
        binding.etRepoSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRepos(s.toString());
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        View userHeader = findViewById(R.id.ivAvatarPlaceholder);
        if (userHeader != null) {
            userHeader.getRootView().findViewById(R.id.tvUsername).setOnClickListener(v -> {
                if (!authManager.isLoggedIn()) {
                    startGitHubOAuth();
                }
            });
        }
    }
    
    private void setupCodeEditor() {
        binding.codeEditor.setOnCursorChangeListener((line, column) -> {
            binding.tvPosition.setText(getString(R.string.line) + " " + line + ", " + 
                getString(R.string.column) + " " + column);
        });
        
        binding.codeEditor.setOnTextChangeListener(text -> {
            OpenFile currentFile = tabAdapter.getSelectedTab();
            if (currentFile != null) {
                currentFile.updateContent(text);
                tabAdapter.updateTabModified(tabAdapter.getSelectedPosition(), currentFile.isModified);
            }
        });
    }
    
    private void setupWelcomePanel() {
        binding.btnOpenFile.setOnClickListener(v -> openFileChooser());
        binding.btnConnectGitHub.setOnClickListener(v -> {
            if (authManager.isLoggedIn()) {
                binding.drawerLayout.openDrawer(GravityCompat.START);
                binding.tabLayout.selectTab(binding.tabLayout.getTabAt(1));
            } else {
                startGitHubOAuth();
            }
        });
    }
    
    private void showSidebarPanel(int position) {
        if (position == 0) {
            binding.explorerPanel.setVisibility(View.VISIBLE);
            binding.reposPanel.setVisibility(View.GONE);
        } else {
            binding.explorerPanel.setVisibility(View.GONE);
            binding.reposPanel.setVisibility(View.VISIBLE);
            
            if (authManager.isLoggedIn()) {
                binding.loginPrompt.setVisibility(View.GONE);
                binding.rvRepos.setVisibility(View.VISIBLE);
            } else {
                binding.loginPrompt.setVisibility(View.VISIBLE);
                binding.rvRepos.setVisibility(View.GONE);
            }
        }
    }
    
    private void updateGitHubUI() {
        boolean loggedIn = authManager.isLoggedIn();
        
        binding.btnLogout.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
        binding.ivAvatar.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
        binding.ivAvatarPlaceholder.setVisibility(loggedIn ? View.GONE : View.VISIBLE);
        
        if (loggedIn) {
            String token = authManager.getAccessToken();
            apiService = new GitHubApiService(token);
            
            String username = authManager.getUsername();
            if (username != null) {
                binding.tvUsername.setText(username);
                binding.tvLoginStatus.setText(R.string.signed_in_as);
            }
            
            binding.loginPrompt.setVisibility(View.GONE);
            binding.rvRepos.setVisibility(View.VISIBLE);
            loadRepositories();
            
            binding.btnConnectGitHub.setText(R.string.repos);
        } else {
            binding.tvUsername.setText(R.string.sign_in_github);
            binding.tvLoginStatus.setText(R.string.tap_to_sign_in);
            
            binding.loginPrompt.setVisibility(View.VISIBLE);
            binding.rvRepos.setVisibility(View.GONE);
            
            binding.btnConnectGitHub.setText(R.string.connect_github);
        }
    }
    
    private void updateWelcomeVisibility() {
        boolean hasOpenTabs = tabAdapter.getItemCount() > 0;
        binding.welcomePanel.setVisibility(hasOpenTabs ? View.GONE : View.VISIBLE);
        binding.codeEditor.setVisibility(hasOpenTabs ? View.VISIBLE : View.GONE);
        binding.rvTabs.setVisibility(hasOpenTabs ? View.VISIBLE : View.GONE);
        binding.symbolToolbar.setVisibility(hasOpenTabs ? View.VISIBLE : View.GONE);
        binding.fabQuickActions.setVisibility(hasOpenTabs ? View.VISIBLE : View.GONE);
        
        if (!hasOpenTabs) {
            hideAutocomplete();
            updateRecentFilesUI();
        }
    }
    
    private void startGitHubOAuth() {
        if (!authManager.isOAuthConfigured()) {
            showOAuthSetupDialog();
            return;
        }
        showDeviceFlowDialog();
    }
    
    private void showOAuthSetupDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_github_login, null);
        
        View codeContainer = dialogView.findViewById(R.id.codeContainer);
        View setupContainer = dialogView.findViewById(R.id.setupContainer);
        EditText etClientId = dialogView.findViewById(R.id.etClientId);
        EditText etClientSecret = dialogView.findViewById(R.id.etClientSecret);
        TextView tvOAuthHelp = dialogView.findViewById(R.id.tvOAuthHelp);
        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        TextView tvDescription = dialogView.findViewById(R.id.tvDescription);
        
        codeContainer.setVisibility(View.GONE);
        setupContainer.setVisibility(View.VISIBLE);
        tvTitle.setText(R.string.setup_github_oauth);
        tvDescription.setText(R.string.setup_oauth_desc);
        
        etClientSecret.setVisibility(View.GONE);
        
        String existingClientId = authManager.getClientId();
        if (existingClientId != null) {
            etClientId.setText(existingClientId);
        }
        
        tvOAuthHelp.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, 
                    Uri.parse("https://github.com/settings/developers"));
            startActivity(intent);
        });
        
        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.save_and_continue, (dialog, which) -> {
                    String clientId = etClientId.getText().toString().trim();
                    if (!clientId.isEmpty()) {
                        authManager.setClientId(clientId);
                        showDeviceFlowDialog();
                    } else {
                        Toast.makeText(this, R.string.client_id_required, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    private void showDeviceFlowDialog() {
        Intent loginIntent = authManager.getLoginIntent();
        if (loginIntent == null) {
            Toast.makeText(this, R.string.credentials_required, Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(loginIntent);
        Toast.makeText(this, R.string.redirecting_to_github, Toast.LENGTH_SHORT).show();
    }
    
    private void fetchUserInfo() {
        apiService.getCurrentUser(new GitHubApiService.ApiCallback<GitHubApiService.GitHubUser>() {
            @Override
            public void onSuccess(GitHubApiService.GitHubUser user) {
                authManager.setUsername(user.login);
                Toast.makeText(MainActivity.this, R.string.login_success, Toast.LENGTH_SHORT).show();
                updateGitHubUI();
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, R.string.login_failed, Toast.LENGTH_SHORT).show();
                updateGitHubUI();
            }
        });
    }
    
    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.logout_confirm)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    authManager.logout();
                    updateGitHubUI();
                    clearRepoData();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }
    
    private void clearRepoData() {
        currentOwner = null;
        currentRepo = null;
        currentBranch = null;
        allRepos.clear();
        repoAdapter.setRepos(new ArrayList<>());
        fileTreeAdapter.setItems(new ArrayList<>());
        binding.repoHeader.setVisibility(View.GONE);
    }
    
    private void loadRepositories() {
        apiService.getRepositories(new GitHubApiService.ApiCallback<List<GitHubApiService.Repository>>() {
            @Override
            public void onSuccess(List<GitHubApiService.Repository> repos) {
                allRepos = repos;
                repoAdapter.setRepos(repos);
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, R.string.error_github, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void filterRepos(String query) {
        if (query.isEmpty()) {
            repoAdapter.setRepos(allRepos);
        } else {
            String lowerQuery = query.toLowerCase();
            List<GitHubApiService.Repository> filtered = allRepos.stream()
                    .filter(r -> r.full_name.toLowerCase().contains(lowerQuery) ||
                            (r.description != null && r.description.toLowerCase().contains(lowerQuery)))
                    .collect(Collectors.toList());
            repoAdapter.setRepos(filtered);
        }
    }
    
    private void openRepository(GitHubApiService.Repository repo) {
        currentOwner = repo.owner.login;
        currentRepo = repo.name;
        currentBranch = repo.default_branch;
        
        binding.repoHeader.setVisibility(View.VISIBLE);
        binding.tvRepoName.setText(repo.full_name);
        binding.tvBranch.setText(currentBranch);
        
        binding.ivGitStatus.setVisibility(View.VISIBLE);
        binding.tvBranchStatus.setVisibility(View.VISIBLE);
        binding.tvBranchStatus.setText(currentBranch);
        
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0));
        loadRepositoryContents("");
        
        binding.drawerLayout.closeDrawer(GravityCompat.START);
    }
    
    private void loadRepositoryContents(String path) {
        apiService.getRepositoryContents(currentOwner, currentRepo, path, currentBranch,
                new GitHubApiService.ApiCallback<List<GitHubApiService.GitHubFile>>() {
                    @Override
                    public void onSuccess(List<GitHubApiService.GitHubFile> files) {
                        List<FileTreeAdapter.FileTreeItem> items = new ArrayList<>();
                        for (GitHubApiService.GitHubFile file : files) {
                            items.add(new FileTreeAdapter.FileTreeItem(file, 0));
                        }
                        fileTreeAdapter.setItems(items);
                    }
                    
                    @Override
                    public void onError(String error) {
                        Toast.makeText(MainActivity.this, R.string.error_github, Toast.LENGTH_LONG).show();
                    }
                });
    }
    
    private void toggleFolder(FileTreeAdapter.FileTreeItem item, int position) {
        if (item.isExpanded) {
            fileTreeAdapter.collapseAt(position);
        } else {
            apiService.getRepositoryContents(currentOwner, currentRepo, item.path, currentBranch,
                    new GitHubApiService.ApiCallback<List<GitHubApiService.GitHubFile>>() {
                        @Override
                        public void onSuccess(List<GitHubApiService.GitHubFile> files) {
                            List<FileTreeAdapter.FileTreeItem> children = new ArrayList<>();
                            for (GitHubApiService.GitHubFile file : files) {
                                children.add(new FileTreeAdapter.FileTreeItem(file, item.level + 1));
                            }
                            fileTreeAdapter.addChildrenAt(position, children);
                        }
                        
                        @Override
                        public void onError(String error) {
                            Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
    
    private void openGitHubFile(FileTreeAdapter.FileTreeItem item) {
        apiService.getFileContent(currentOwner, currentRepo, item.path, currentBranch,
                new GitHubApiService.ApiCallback<GitHubApiService.GitHubFile>() {
                    @Override
                    public void onSuccess(GitHubApiService.GitHubFile file) {
                        OpenFile openFile = new OpenFile(file.name, file.path, file.content, file.sha);
                        openFile.setGitHubInfo(currentOwner, currentRepo, currentBranch);
                        
                        tabAdapter.addTab(openFile);
                        displayFile(openFile);
                        updateWelcomeVisibility();
                        
                        binding.drawerLayout.closeDrawer(GravityCompat.START);
                    }
                    
                    @Override
                    public void onError(String error) {
                        Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void displayFile(OpenFile file) {
        SyntaxHighlighter.Language language = SyntaxHighlighter.detectLanguage(file.name);
        currentLanguage = language;
        binding.codeEditor.setLanguage(language);
        binding.codeEditor.setText(file.content);
        binding.tvLanguage.setText(SyntaxHighlighter.getLanguageDisplayName(language));
        binding.symbolToolbar.setLanguage(language);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(file.name);
        }
        
        if (file.isGitHubFile) {
            RecentFilesManager.RecentFile recentFile = new RecentFilesManager.RecentFile(file.name, file.path, true);
            recentFile.setGitHubInfo(file.owner, file.repo, file.branch);
            recentFilesManager.addRecentFile(recentFile);
        } else if (file.localUri != null) {
            RecentFilesManager.RecentFile recentFile = new RecentFilesManager.RecentFile(file.name, file.localUri, false);
            recentFile.setLocalUri(file.localUri);
            recentFilesManager.addRecentFile(recentFile);
        }
    }
    
    private void switchToTab(int position) {
        OpenFile currentFile = tabAdapter.getSelectedTab();
        if (currentFile != null) {
            currentFile.content = binding.codeEditor.getText();
        }
        
        tabAdapter.setSelectedPosition(position);
        OpenFile file = tabAdapter.getSelectedTab();
        if (file != null) {
            displayFile(file);
        }
    }
    
    private void closeTab(int position) {
        if (position < 0 || position >= tabAdapter.getTabs().size()) return;
        
        if (position == tabAdapter.getSelectedPosition()) {
            OpenFile currentFile = tabAdapter.getSelectedTab();
            if (currentFile != null) {
                currentFile.content = binding.codeEditor.getText();
            }
        }
        
        OpenFile file = tabAdapter.getTabs().get(position);
        
        if (file.isModified) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.unsaved_changes)
                    .setPositiveButton(R.string.save, (dialog, which) -> {
                        if (file.isGitHubFile) {
                            saveToGitHub(file, () -> removeTab(position));
                        } else if (file.localUri != null && !file.localUri.isEmpty()) {
                            try {
                                Uri uri = Uri.parse(file.localUri);
                                FileUtils.writeToUri(this, uri, file.content);
                                file.markSaved(null);
                                removeTab(position);
                            } catch (IOException e) {
                                Toast.makeText(this, R.string.error_saving, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            saveAs();
                        }
                    })
                    .setNegativeButton(R.string.discard, (dialog, which) -> removeTab(position))
                    .setNeutralButton(R.string.cancel, null)
                    .show();
        } else {
            removeTab(position);
        }
    }
    
    private void removeTab(int position) {
        tabAdapter.removeTab(position);
        
        if (tabAdapter.getItemCount() == 0) {
            binding.codeEditor.setText("");
            binding.tvLanguage.setText("Plain Text");
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.app_name);
            }
        } else {
            int newSelectedPos = tabAdapter.getSelectedPosition();
            if (newSelectedPos < 0 || newSelectedPos >= tabAdapter.getItemCount()) {
                newSelectedPos = Math.max(0, tabAdapter.getItemCount() - 1);
            }
            tabAdapter.setSelectedPosition(newSelectedPos);
            OpenFile file = tabAdapter.getSelectedTab();
            if (file != null) {
                displayFile(file);
            }
        }
        
        updateWelcomeVisibility();
    }
    
    public void saveCurrentFile() {
        OpenFile file = tabAdapter.getSelectedTab();
        if (file == null) return;
        
        file.content = binding.codeEditor.getText();
        
        if (file.isGitHubFile) {
            showCommitDialog(file);
        } else if (file.localUri != null && !file.localUri.isEmpty()) {
            try {
                Uri uri = Uri.parse(file.localUri);
                FileUtils.writeToUri(this, uri, file.content);
                file.markSaved(null);
                tabAdapter.notifyItemChanged(tabAdapter.getSelectedPosition());
                Toast.makeText(this, R.string.file_saved, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(this, R.string.error_saving, Toast.LENGTH_SHORT).show();
            }
        } else {
            saveAs();
        }
    }
    
    private void showCommitDialog(OpenFile file) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_commit, null);
        TextView tvFileName = dialogView.findViewById(R.id.tvFileName);
        EditText etMessage = dialogView.findViewById(R.id.etCommitMessage);
        
        tvFileName.setText(file.path);
        etMessage.setText("Update " + file.name);
        
        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String message = etMessage.getText().toString().trim();
                    if (message.isEmpty()) {
                        message = "Update " + file.name;
                    }
                    saveToGitHub(file, message, null);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    private void saveToGitHub(OpenFile file, Runnable onComplete) {
        saveToGitHub(file, "Update " + file.name, onComplete);
    }
    
    private void saveToGitHub(OpenFile file, String message, Runnable onComplete) {
        Toast.makeText(this, R.string.saving_to_github, Toast.LENGTH_SHORT).show();
        
        apiService.updateFile(file.owner, file.repo, file.path, file.content, message, file.sha, file.branch,
                new GitHubApiService.ApiCallback<GitHubApiService.CommitResult>() {
                    @Override
                    public void onSuccess(GitHubApiService.CommitResult result) {
                        file.markSaved(result.content.sha);
                        tabAdapter.updateTabModified(tabAdapter.getSelectedPosition(), false);
                        tabAdapter.updateTabSha(tabAdapter.getSelectedPosition(), result.content.sha);
                        Toast.makeText(MainActivity.this, R.string.saved_to_github, Toast.LENGTH_SHORT).show();
                        
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    }
                    
                    @Override
                    public void onError(String error) {
                        Toast.makeText(MainActivity.this, R.string.error_github, Toast.LENGTH_LONG).show();
                    }
                });
    }
    
    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        openFileLauncher.launch(intent);
    }
    
    private void openLocalFile(Uri uri) {
        try {
            getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        
        try {
            String content = FileUtils.readFromUri(this, uri);
            String filename = FileUtils.getFileName(this, uri);
            
            OpenFile file = new OpenFile(filename, uri.toString(), content, null);
            file.localUri = uri.toString();
            tabAdapter.addTab(file);
            displayFile(file);
            updateWelcomeVisibility();
            
            Toast.makeText(this, R.string.file_opened, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, R.string.error_opening, Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveLocalFile(Uri uri) {
        try {
            String currentText = binding.codeEditor.getText();
            FileUtils.writeToUri(this, uri, currentText);
            
            OpenFile file = tabAdapter.getSelectedTab();
            if (file != null) {
                String uriString = uri.toString();
                file.localUri = uriString;
                file.path = uriString;
                file.name = FileUtils.getFileName(this, uri);
                file.content = currentText;
                file.markSaved(null);
                tabAdapter.notifyItemChanged(tabAdapter.getSelectedPosition());
            }
            
            Toast.makeText(this, R.string.file_saved, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, R.string.error_saving, Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveAs() {
        OpenFile file = tabAdapter.getSelectedTab();
        String filename = file != null ? file.name : "untitled.txt";
        
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, filename);
        saveFileLauncher.launch(intent);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_save) {
            saveCurrentFile();
            return true;
        } else if (id == R.id.action_new) {
            showNewFileDialog();
            return true;
        } else if (id == R.id.action_open) {
            openFileChooser();
            return true;
        } else if (id == R.id.action_save_as) {
            saveAs();
            return true;
        } else if (id == R.id.action_find) {
            showFindDialog();
            return true;
        } else if (id == R.id.action_find_replace) {
            showFindReplaceDialog();
            return true;
        } else if (id == R.id.action_go_to_line) {
            showGoToLineDialog();
            return true;
        } else if (id == R.id.action_undo) {
            binding.codeEditor.undo();
            return true;
        } else if (id == R.id.action_redo) {
            binding.codeEditor.redo();
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_new_android_project) {
            showNewAndroidProjectDialog();
            return true;
        } else if (id == R.id.action_build_apk) {
            openBuildActivity();
            return true;
        } else if (id == R.id.action_terminal) {
            openTerminal();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void showNewFileDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_file, null);
        EditText etFileName = dialogView.findViewById(R.id.etFileName);
        ChipGroup chipGroup = dialogView.findViewById(R.id.chipGroupTemplates);
        
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        
        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        
        dialogView.findViewById(R.id.btnCreate).setOnClickListener(v -> {
            String fileName = etFileName.getText().toString().trim();
            if (fileName.isEmpty()) {
                Toast.makeText(this, R.string.file_name_required, Toast.LENGTH_SHORT).show();
                return;
            }
            
            String template = "";
            int checkedId = chipGroup.getCheckedChipId();
            if (checkedId == R.id.chipJava) {
                template = FileTemplates.getTemplate("java");
                if (!fileName.endsWith(".java")) fileName += ".java";
            } else if (checkedId == R.id.chipKotlin) {
                template = FileTemplates.getTemplate("kotlin");
                if (!fileName.endsWith(".kt")) fileName += ".kt";
            } else if (checkedId == R.id.chipPython) {
                template = FileTemplates.getTemplate("python");
                if (!fileName.endsWith(".py")) fileName += ".py";
            } else if (checkedId == R.id.chipJavaScript) {
                template = FileTemplates.getTemplate("javascript");
                if (!fileName.endsWith(".js")) fileName += ".js";
            } else if (checkedId == R.id.chipHTML) {
                template = FileTemplates.getTemplate("html");
                if (!fileName.endsWith(".html")) fileName += ".html";
            } else if (checkedId == R.id.chipCSS) {
                template = FileTemplates.getTemplate("css");
                if (!fileName.endsWith(".css")) fileName += ".css";
            } else if (checkedId == R.id.chipMarkdown) {
                template = FileTemplates.getTemplate("markdown");
                if (!fileName.endsWith(".md")) fileName += ".md";
            }
            
            OpenFile newFile = new OpenFile(fileName, "", template, null);
            tabAdapter.addTab(newFile);
            displayFile(newFile);
            updateWelcomeVisibility();
            
            Toast.makeText(this, R.string.file_created, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void showFindDialog() {
        showFindReplaceDialog();
    }
    
    private void showFindReplaceDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_find_replace, null);
        EditText etFind = dialogView.findViewById(R.id.etFind);
        EditText etReplace = dialogView.findViewById(R.id.etReplace);
        CheckBox cbCaseSensitive = dialogView.findViewById(R.id.cbCaseSensitive);
        CheckBox cbRegex = dialogView.findViewById(R.id.cbRegex);
        CheckBox cbWholeWord = dialogView.findViewById(R.id.cbWholeWord);
        TextView tvMatchCount = dialogView.findViewById(R.id.tvMatchCount);
        
        findReplaceHelper.setText(binding.codeEditor.getText());
        
        findReplaceDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        
        Runnable performFind = () -> {
            String query = etFind.getText().toString();
            if (query.isEmpty()) {
                tvMatchCount.setVisibility(View.GONE);
                binding.codeEditor.clearHighlightedMatch();
                return;
            }
            
            findReplaceHelper.setText(binding.codeEditor.getText());
            FindReplaceHelper.FindOptions options = new FindReplaceHelper.FindOptions(
                cbCaseSensitive.isChecked(),
                cbRegex.isChecked(),
                cbWholeWord.isChecked()
            );
            
            findReplaceHelper.find(query, options);
            int count = findReplaceHelper.getMatchCount();
            
            if (count > 0) {
                tvMatchCount.setText(getString(R.string.matches_found, count));
                tvMatchCount.setVisibility(View.VISIBLE);
                
                FindReplaceHelper.Match match = findReplaceHelper.getCurrentMatch();
                if (match != null) {
                    binding.codeEditor.highlightMatch(match.start, match.end);
                }
            } else {
                tvMatchCount.setText(R.string.no_matches);
                tvMatchCount.setVisibility(View.VISIBLE);
                binding.codeEditor.clearHighlightedMatch();
            }
        };
        
        etFind.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                performFind.run();
            }
        });
        
        dialogView.findViewById(R.id.btnFindNext).setOnClickListener(v -> {
            FindReplaceHelper.Match match = findReplaceHelper.findNext();
            if (match != null) {
                binding.codeEditor.highlightMatch(match.start, match.end);
            }
        });
        
        dialogView.findViewById(R.id.btnFindPrev).setOnClickListener(v -> {
            FindReplaceHelper.Match match = findReplaceHelper.findPrevious();
            if (match != null) {
                binding.codeEditor.highlightMatch(match.start, match.end);
            }
        });
        
        dialogView.findViewById(R.id.btnReplace).setOnClickListener(v -> {
            FindReplaceHelper.Match match = findReplaceHelper.getCurrentMatch();
            if (match != null) {
                String replacement = etReplace.getText().toString();
                binding.codeEditor.replaceText(match.start, match.end, replacement);
                findReplaceHelper.setText(binding.codeEditor.getText());
                
                FindReplaceHelper.FindOptions options = new FindReplaceHelper.FindOptions(
                    cbCaseSensitive.isChecked(),
                    cbRegex.isChecked(),
                    cbWholeWord.isChecked()
                );
                findReplaceHelper.find(etFind.getText().toString(), options);
                
                FindReplaceHelper.Match nextMatch = findReplaceHelper.getCurrentMatch();
                if (nextMatch != null) {
                    binding.codeEditor.highlightMatch(nextMatch.start, nextMatch.end);
                }
                
                tvMatchCount.setText(getString(R.string.matches_found, findReplaceHelper.getMatchCount()));
            }
        });
        
        dialogView.findViewById(R.id.btnReplaceAll).setOnClickListener(v -> {
            String query = etFind.getText().toString();
            String replacement = etReplace.getText().toString();
            
            if (!query.isEmpty()) {
                FindReplaceHelper.FindOptions options = new FindReplaceHelper.FindOptions(
                    cbCaseSensitive.isChecked(),
                    cbRegex.isChecked(),
                    cbWholeWord.isChecked()
                );
                
                findReplaceHelper.setText(binding.codeEditor.getText());
                findReplaceHelper.find(query, options);
                int count = findReplaceHelper.getMatchCount();
                
                if (count > 0) {
                    String newText = findReplaceHelper.replaceAll(query, replacement, options);
                    binding.codeEditor.setText(newText);
                    Toast.makeText(this, getString(R.string.replaced_count, count), Toast.LENGTH_SHORT).show();
                }
                
                tvMatchCount.setText(R.string.no_matches);
                binding.codeEditor.clearHighlightedMatch();
            }
        });
        
        findReplaceDialog.setOnDismissListener(d -> {
            binding.codeEditor.clearHighlightedMatch();
        });
        
        findReplaceDialog.show();
    }
    
    private void showGoToLineDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_go_to_line, null);
        EditText etLineNumber = dialogView.findViewById(R.id.etLineNumber);
        TextView tvLineRange = dialogView.findViewById(R.id.tvLineRange);
        
        int totalLines = binding.codeEditor.getLineCount();
        tvLineRange.setText(getString(R.string.line_range, totalLines));
        
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        
        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        
        dialogView.findViewById(R.id.btnGo).setOnClickListener(v -> {
            String input = etLineNumber.getText().toString().trim();
            if (input.isEmpty()) {
                return;
            }
            
            try {
                int lineNumber = Integer.parseInt(input);
                if (lineNumber >= 1 && lineNumber <= totalLines) {
                    binding.codeEditor.goToLine(lineNumber);
                    dialog.dismiss();
                } else {
                    Toast.makeText(this, R.string.invalid_line, Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, R.string.invalid_line, Toast.LENGTH_SHORT).show();
            }
        });
        
        dialog.show();
    }
    
    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        
        boolean hasUnsaved = false;
        for (OpenFile file : tabAdapter.getTabs()) {
            if (file.isModified) {
                hasUnsaved = true;
                break;
            }
        }
        
        if (hasUnsaved) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.unsaved_changes)
                    .setPositiveButton(R.string.discard, (dialog, which) -> finish())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadPreferences();
        applyThemeSettings();
        
        if (autoSaveEnabled) {
            autoSaveHandler.postDelayed(autoSaveRunnable, autoSaveInterval * 1000L);
        }
    }
    
    private void showNewAndroidProjectDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_project, null);
        EditText projectNameInput = dialogView.findViewById(R.id.projectNameInput);
        EditText packageNameInput = dialogView.findViewById(R.id.packageNameInput);
        
        new AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Create", (dialog, which) -> {
                String projectName = projectNameInput.getText().toString().trim();
                String packageName = packageNameInput.getText().toString().trim();
                
                if (projectName.isEmpty()) {
                    Toast.makeText(this, "Project name is required", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (packageName.isEmpty()) {
                    Toast.makeText(this, "Package name is required", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                createAndroidProject(projectName, packageName);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void createAndroidProject(String projectName, String packageName) {
        com.codeeditor.android.build.BuildService buildService = 
            new com.codeeditor.android.build.BuildService(this);
        
        buildService.createNewProject(projectName, packageName, 
            new com.codeeditor.android.build.BuildService.ProjectCreationCallback() {
                @Override
                public void onSuccess(java.io.File projectDir) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, 
                            "Project created: " + projectName, Toast.LENGTH_SHORT).show();
                        
                        Intent intent = new Intent(MainActivity.this, BuildActivity.class);
                        intent.putExtra("project_path", projectDir.getAbsolutePath());
                        startActivity(intent);
                    });
                }
                
                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, 
                            "Error: " + message, Toast.LENGTH_LONG).show();
                    });
                }
            });
    }
    
    private void openBuildActivity() {
        com.codeeditor.android.build.BuildWorkspace workspace = 
            new com.codeeditor.android.build.BuildWorkspace(this);
        
        java.util.List<java.io.File> projects = workspace.listProjects();
        
        if (projects.isEmpty()) {
            new AlertDialog.Builder(this)
                .setTitle("No Projects")
                .setMessage("No Android projects found. Would you like to create a new project?")
                .setPositiveButton("Create New", (dialog, which) -> showNewAndroidProjectDialog())
                .setNegativeButton("Cancel", null)
                .show();
            return;
        }
        
        String[] projectNames = new String[projects.size()];
        for (int i = 0; i < projects.size(); i++) {
            projectNames[i] = projects.get(i).getName();
        }
        
        new AlertDialog.Builder(this)
            .setTitle("Select Project")
            .setItems(projectNames, (dialog, which) -> {
                java.io.File selectedProject = projects.get(which);
                Intent intent = new Intent(this, BuildActivity.class);
                intent.putExtra("project_path", selectedProject.getAbsolutePath());
                startActivity(intent);
            })
            .setNeutralButton("New Project", (dialog, which) -> showNewAndroidProjectDialog())
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void openTerminal() {
        Intent intent = new Intent(this, TerminalActivity.class);
        OpenFile openFile = tabAdapter.getSelectedTab();
        if (openFile != null && openFile.localUri != null) {
            try {
                java.io.File file = new java.io.File(Uri.parse(openFile.localUri).getPath());
                if (file.getParentFile() != null) {
                    intent.putExtra("working_directory", file.getParentFile().getAbsolutePath());
                }
            } catch (Exception e) {
            }
        }
        startActivity(intent);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        autoSaveCurrentFile();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (authManager != null) {
            authManager.dispose();
        }
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.isCtrlPressed()) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_S:
                    saveCurrentFile();
                    return true;
                case KeyEvent.KEYCODE_O:
                    openFileChooser();
                    return true;
                case KeyEvent.KEYCODE_N:
                    showNewFileDialog();
                    return true;
                case KeyEvent.KEYCODE_F:
                    showFindDialog();
                    return true;
                case KeyEvent.KEYCODE_H:
                    showFindReplaceDialog();
                    return true;
                case KeyEvent.KEYCODE_G:
                    showGoToLineDialog();
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
