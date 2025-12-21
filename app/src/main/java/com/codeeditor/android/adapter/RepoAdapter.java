package com.codeeditor.android.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.codeeditor.android.R;
import com.codeeditor.android.github.GitHubApiService;

import java.util.ArrayList;
import java.util.List;

public class RepoAdapter extends RecyclerView.Adapter<RepoAdapter.ViewHolder> {
    
    private List<GitHubApiService.Repository> repos = new ArrayList<>();
    private OnRepoClickListener listener;
    
    public interface OnRepoClickListener {
        void onRepoClick(GitHubApiService.Repository repo);
    }
    
    public void setOnRepoClickListener(OnRepoClickListener listener) {
        this.listener = listener;
    }
    
    public void setRepos(List<GitHubApiService.Repository> repos) {
        this.repos = repos;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_repo, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(repos.get(position), listener);
    }
    
    @Override
    public int getItemCount() {
        return repos.size();
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameView;
        private final TextView descView;
        private final TextView visibilityView;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.repoName);
            descView = itemView.findViewById(R.id.repoDesc);
            visibilityView = itemView.findViewById(R.id.repoVisibility);
        }
        
        public void bind(GitHubApiService.Repository repo, OnRepoClickListener listener) {
            nameView.setText(repo.full_name);
            descView.setText(repo.description != null ? repo.description : "No description");
            visibilityView.setText(repo.isPrivate ? "Private" : "Public");
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRepoClick(repo);
                }
            });
        }
    }
}
