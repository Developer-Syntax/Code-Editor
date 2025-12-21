package com.codeeditor.android.github;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GitHubApiService {
    private static final String BASE_URL = "https://api.github.com";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    private final OkHttpClient client;
    private final Gson gson;
    private final String accessToken;
    private final ExecutorService executor;
    private final Handler mainHandler;
    
    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }
    
    public static class Repository {
        public long id;
        public String name;
        public String full_name;
        public String description;
        public boolean isPrivate;
        public String default_branch;
        public String html_url;
        public Owner owner;
        
        public static class Owner {
            public String login;
            public String avatar_url;
        }
    }
    
    public static class GitHubFile {
        public String name;
        public String path;
        public String sha;
        public long size;
        public String type;
        public String content;
        public String download_url;
        public String encoding;
        
        public boolean isDirectory() {
            return "dir".equals(type);
        }
    }
    
    public static class GitHubUser {
        public long id;
        public String login;
        public String name;
        public String avatar_url;
        public String email;
    }
    
    public static class CommitResult {
        public Commit commit;
        public Content content;
        
        public static class Commit {
            public String sha;
            public String message;
        }
        
        public static class Content {
            public String name;
            public String path;
            public String sha;
        }
    }
    
    public GitHubApiService(String accessToken) {
        this.accessToken = accessToken;
        this.client = new OkHttpClient.Builder().build();
        this.gson = new Gson();
        this.executor = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    private Request.Builder createRequest(String endpoint) {
        return new Request.Builder()
                .url(BASE_URL + endpoint)
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28");
    }
    
    public void getCurrentUser(ApiCallback<GitHubUser> callback) {
        executor.execute(() -> {
            try {
                Request request = createRequest("/user").get().build();
                Response response = client.newCall(request).execute();
                
                if (response.isSuccessful() && response.body() != null) {
                    GitHubUser user = gson.fromJson(response.body().string(), GitHubUser.class);
                    mainHandler.post(() -> callback.onSuccess(user));
                } else {
                    mainHandler.post(() -> callback.onError("Failed to get user: " + response.code()));
                }
            } catch (IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
    
    public void getRepositories(ApiCallback<List<Repository>> callback) {
        executor.execute(() -> {
            try {
                Request request = createRequest("/user/repos?sort=updated&per_page=100").get().build();
                Response response = client.newCall(request).execute();
                
                if (response.isSuccessful() && response.body() != null) {
                    Type listType = new TypeToken<List<Repository>>(){}.getType();
                    List<Repository> repos = gson.fromJson(response.body().string(), listType);
                    mainHandler.post(() -> callback.onSuccess(repos));
                } else {
                    mainHandler.post(() -> callback.onError("Failed to get repos: " + response.code()));
                }
            } catch (IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
    
    public void getRepositoryContents(String owner, String repo, String path, String branch, ApiCallback<List<GitHubFile>> callback) {
        executor.execute(() -> {
            try {
                String endpoint = "/repos/" + owner + "/" + repo + "/contents/" + path;
                if (branch != null && !branch.isEmpty()) {
                    endpoint += "?ref=" + branch;
                }
                
                Request request = createRequest(endpoint).get().build();
                Response response = client.newCall(request).execute();
                
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    List<GitHubFile> files;
                    
                    if (body.trim().startsWith("[")) {
                        Type listType = new TypeToken<List<GitHubFile>>(){}.getType();
                        files = gson.fromJson(body, listType);
                    } else {
                        files = new ArrayList<>();
                        GitHubFile file = gson.fromJson(body, GitHubFile.class);
                        files.add(file);
                    }
                    
                    mainHandler.post(() -> callback.onSuccess(files));
                } else {
                    mainHandler.post(() -> callback.onError("Failed to get contents: " + response.code()));
                }
            } catch (IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
    
    public void getFileContent(String owner, String repo, String path, String branch, ApiCallback<GitHubFile> callback) {
        executor.execute(() -> {
            try {
                String endpoint = "/repos/" + owner + "/" + repo + "/contents/" + path;
                if (branch != null && !branch.isEmpty()) {
                    endpoint += "?ref=" + branch;
                }
                
                Request request = createRequest(endpoint).get().build();
                Response response = client.newCall(request).execute();
                
                if (response.isSuccessful() && response.body() != null) {
                    GitHubFile file = gson.fromJson(response.body().string(), GitHubFile.class);
                    
                    if (file.content != null && "base64".equals(file.encoding)) {
                        String cleanContent = file.content.replaceAll("\\s", "");
                        byte[] decoded = Base64.decode(cleanContent, Base64.DEFAULT);
                        file.content = new String(decoded);
                    }
                    
                    mainHandler.post(() -> callback.onSuccess(file));
                } else {
                    mainHandler.post(() -> callback.onError("Failed to get file: " + response.code()));
                }
            } catch (IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
    
    public void updateFile(String owner, String repo, String path, String content, String message, String sha, String branch, ApiCallback<CommitResult> callback) {
        executor.execute(() -> {
            try {
                String endpoint = "/repos/" + owner + "/" + repo + "/contents/" + path;
                
                String encodedContent = Base64.encodeToString(content.getBytes(), Base64.NO_WRAP);
                
                JsonObject json = new JsonObject();
                json.addProperty("message", message);
                json.addProperty("content", encodedContent);
                if (sha != null) {
                    json.addProperty("sha", sha);
                }
                if (branch != null && !branch.isEmpty()) {
                    json.addProperty("branch", branch);
                }
                
                RequestBody body = RequestBody.create(gson.toJson(json), JSON);
                Request request = createRequest(endpoint).put(body).build();
                Response response = client.newCall(request).execute();
                
                if (response.isSuccessful() && response.body() != null) {
                    CommitResult result = gson.fromJson(response.body().string(), CommitResult.class);
                    mainHandler.post(() -> callback.onSuccess(result));
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    mainHandler.post(() -> callback.onError("Failed to update file: " + response.code() + " " + errorBody));
                }
            } catch (IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
    
    public void createFile(String owner, String repo, String path, String content, String message, String branch, ApiCallback<CommitResult> callback) {
        updateFile(owner, repo, path, content, message, null, branch, callback);
    }
    
    public void deleteFile(String owner, String repo, String path, String message, String sha, String branch, ApiCallback<CommitResult> callback) {
        executor.execute(() -> {
            try {
                String endpoint = "/repos/" + owner + "/" + repo + "/contents/" + path;
                
                JsonObject json = new JsonObject();
                json.addProperty("message", message);
                json.addProperty("sha", sha);
                if (branch != null && !branch.isEmpty()) {
                    json.addProperty("branch", branch);
                }
                
                RequestBody body = RequestBody.create(gson.toJson(json), JSON);
                Request request = createRequest(endpoint).delete(body).build();
                Response response = client.newCall(request).execute();
                
                if (response.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(null));
                } else {
                    mainHandler.post(() -> callback.onError("Failed to delete file: " + response.code()));
                }
            } catch (IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
    
    public void getBranches(String owner, String repo, ApiCallback<List<String>> callback) {
        executor.execute(() -> {
            try {
                Request request = createRequest("/repos/" + owner + "/" + repo + "/branches").get().build();
                Response response = client.newCall(request).execute();
                
                if (response.isSuccessful() && response.body() != null) {
                    JsonArray array = gson.fromJson(response.body().string(), JsonArray.class);
                    List<String> branches = new ArrayList<>();
                    for (int i = 0; i < array.size(); i++) {
                        branches.add(array.get(i).getAsJsonObject().get("name").getAsString());
                    }
                    mainHandler.post(() -> callback.onSuccess(branches));
                } else {
                    mainHandler.post(() -> callback.onError("Failed to get branches: " + response.code()));
                }
            } catch (IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
}
