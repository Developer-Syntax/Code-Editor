package com.codeeditor.android.model;

public class OpenFile {
    public String name;
    public String path;
    public String content;
    public String originalContent;
    public String sha;
    public boolean isModified;
    public boolean isGitHubFile;
    public String owner;
    public String repo;
    public String branch;
    public String localUri;
    
    public OpenFile(String name, String path, String content, String sha) {
        this.name = name;
        this.path = path;
        this.content = content;
        this.originalContent = content;
        this.sha = sha;
        this.isModified = false;
        this.isGitHubFile = false;
        this.localUri = null;
    }
    
    public OpenFile(String name, String path, String content, String sha, String localUri) {
        this(name, path, content, sha);
        this.localUri = localUri;
    }
    
    public void setGitHubInfo(String owner, String repo, String branch) {
        this.owner = owner;
        this.repo = repo;
        this.branch = branch;
        this.isGitHubFile = true;
    }
    
    public void updateContent(String content) {
        this.content = content;
        this.isModified = !content.equals(originalContent);
    }
    
    public void markSaved(String newSha) {
        this.originalContent = this.content;
        this.sha = newSha;
        this.isModified = false;
    }
}
