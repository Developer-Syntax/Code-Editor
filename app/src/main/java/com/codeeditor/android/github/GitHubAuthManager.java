package com.codeeditor.android.github;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.codeeditor.android.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GitHubAuthManager {
    private static final String TAG = "GitHubAuthManager";
    
    private static final String GITHUB_AUTH_URL = "https://github.com/login/oauth/authorize";
    private static final String GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token";
    
    private static final String REDIRECT_URI = "codeeditor://github-callback";
    private static final String SCOPE = "user repo";
    
    private static final String PREFS_NAME = "github_auth";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_AVATAR_URL = "avatar_url";
    private static final String KEY_STATE = "oauth_state";
    private static final String KEY_CLIENT_ID = "client_id";
    private static final String KEY_CLIENT_SECRET = "client_secret";
    
    private final Context context;
    private SharedPreferences securePrefs;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final ExecutorService executor;
    private final Handler mainHandler;
    
    public interface AuthCallback {
        void onSuccess(String accessToken);
        void onError(String error);
    }
    
    public GitHubAuthManager(Context context) {
        this.context = context.getApplicationContext();
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        initSecurePrefs();
    }
    
    private void initSecurePrefs() {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            
            securePrefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Failed to create encrypted prefs", e);
            securePrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
    }
    
    private String getEffectiveClientId() {
        String savedClientId = securePrefs.getString(KEY_CLIENT_ID, null);
        if (savedClientId != null && !savedClientId.isEmpty()) {
            return savedClientId;
        }
        String buildConfigId = BuildConfig.GITHUB_CLIENT_ID;
        if (buildConfigId != null && !buildConfigId.isEmpty()) {
            return buildConfigId;
        }
        return null;
    }
    
    public Intent getLoginIntent() {
        String clientId = getEffectiveClientId();
        if (clientId == null || clientId.isEmpty()) {
            return null;
        }
        
        String state = UUID.randomUUID().toString();
        securePrefs.edit().putString(KEY_STATE, state).apply();
        
        String authUrl = GITHUB_AUTH_URL + 
                "?client_id=" + clientId +
                "&redirect_uri=" + Uri.encode(REDIRECT_URI) +
                "&scope=" + Uri.encode(SCOPE) +
                "&state=" + state;
        
        return new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
    }
    
    public void handleCallback(Uri callbackUri, AuthCallback callback) {
        if (callbackUri == null) {
            callback.onError("Invalid callback");
            return;
        }
        
        String code = callbackUri.getQueryParameter("code");
        String state = callbackUri.getQueryParameter("state");
        String error = callbackUri.getQueryParameter("error");
        
        if (error != null) {
            String errorDesc = callbackUri.getQueryParameter("error_description");
            callback.onError(errorDesc != null ? errorDesc : error);
            return;
        }
        
        if (code == null || code.isEmpty()) {
            callback.onError("No authorization code received");
            return;
        }
        
        String savedState = securePrefs.getString(KEY_STATE, null);
        if (savedState == null || !savedState.equals(state)) {
            callback.onError("Invalid state parameter");
            return;
        }
        
        securePrefs.edit().remove(KEY_STATE).apply();
        
        exchangeCodeForToken(code, callback);
    }
    
    private void exchangeCodeForToken(String code, AuthCallback callback) {
        String clientId = getEffectiveClientId();
        
        if (clientId == null) {
            callback.onError("OAuth not configured");
            return;
        }
        
        executor.execute(() -> {
            try {
                FormBody.Builder bodyBuilder = new FormBody.Builder()
                        .add("client_id", clientId)
                        .add("code", code)
                        .add("redirect_uri", REDIRECT_URI);
                
                String clientSecret = getClientSecret();
                if (clientSecret != null && !clientSecret.isEmpty()) {
                    bodyBuilder.add("client_secret", clientSecret);
                }
                
                RequestBody body = bodyBuilder.build();
                
                Request request = new Request.Builder()
                        .url(GITHUB_TOKEN_URL)
                        .header("Accept", "application/json")
                        .post(body)
                        .build();
                
                Response response = httpClient.newCall(request).execute();
                
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                    
                    if (json.has("error")) {
                        String error = json.get("error").getAsString();
                        String errorDesc = json.has("error_description") ? 
                                json.get("error_description").getAsString() : error;
                        mainHandler.post(() -> callback.onError(errorDesc));
                        return;
                    }
                    
                    if (json.has("access_token")) {
                        String accessToken = json.get("access_token").getAsString();
                        setAccessToken(accessToken);
                        mainHandler.post(() -> callback.onSuccess(accessToken));
                    } else {
                        mainHandler.post(() -> callback.onError("No access token in response"));
                    }
                } else {
                    mainHandler.post(() -> callback.onError("Failed to exchange code: " + response.code()));
                }
            } catch (IOException e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }
        });
    }
    
    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }
    
    public boolean isOAuthConfigured() {
        String clientId = getEffectiveClientId();
        return clientId != null && !clientId.isEmpty();
    }
    
    @Nullable
    public String getAccessToken() {
        return securePrefs.getString(KEY_ACCESS_TOKEN, null);
    }
    
    public void setAccessToken(String token) {
        securePrefs.edit().putString(KEY_ACCESS_TOKEN, token).apply();
    }
    
    public void setUsername(String username) {
        securePrefs.edit().putString(KEY_USERNAME, username).apply();
    }
    
    @Nullable
    public String getUsername() {
        return securePrefs.getString(KEY_USERNAME, null);
    }
    
    public void setAvatarUrl(String avatarUrl) {
        securePrefs.edit().putString(KEY_AVATAR_URL, avatarUrl).apply();
    }
    
    @Nullable
    public String getAvatarUrl() {
        return securePrefs.getString(KEY_AVATAR_URL, null);
    }
    
    public void setClientId(String clientId) {
        securePrefs.edit().putString(KEY_CLIENT_ID, clientId).apply();
    }
    
    @Nullable
    public String getClientId() {
        return securePrefs.getString(KEY_CLIENT_ID, null);
    }
    
    public void setClientSecret(String clientSecret) {
        securePrefs.edit().putString(KEY_CLIENT_SECRET, clientSecret).apply();
    }
    
    @Nullable
    public String getClientSecret() {
        String savedSecret = securePrefs.getString(KEY_CLIENT_SECRET, null);
        if (savedSecret != null && !savedSecret.isEmpty()) {
            return savedSecret;
        }
        String buildConfigSecret = BuildConfig.GITHUB_CLIENT_SECRET;
        if (buildConfigSecret != null && !buildConfigSecret.isEmpty()) {
            return buildConfigSecret;
        }
        return null;
    }
    
    public void logout() {
        securePrefs.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_USERNAME)
                .remove(KEY_AVATAR_URL)
                .apply();
    }
    
    public void dispose() {
        executor.shutdown();
    }
}
