package com.codeeditor.android.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.codeeditor.android.R;
import com.codeeditor.android.github.GitHubApiService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileTreeAdapter extends RecyclerView.Adapter<FileTreeAdapter.ViewHolder> {
    
    private List<FileTreeItem> items = new ArrayList<>();
    private OnItemClickListener listener;
    
    public static class FileTreeItem {
        public String name;
        public String path;
        public String sha;
        public boolean isDirectory;
        public int level;
        public boolean isExpanded;
        public List<FileTreeItem> children;
        
        public FileTreeItem(GitHubApiService.GitHubFile file, int level) {
            this.name = file.name;
            this.path = file.path;
            this.sha = file.sha;
            this.isDirectory = file.isDirectory();
            this.level = level;
            this.isExpanded = false;
            this.children = new ArrayList<>();
        }
    }
    
    public interface OnItemClickListener {
        void onFileClick(FileTreeItem item);
        void onFolderClick(FileTreeItem item, int position);
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    
    public void setItems(List<FileTreeItem> items) {
        this.items = items;
        sortItems();
        notifyDataSetChanged();
    }
    
    public void addChildrenAt(int position, List<FileTreeItem> children) {
        if (position >= 0 && position < items.size()) {
            FileTreeItem parent = items.get(position);
            parent.isExpanded = true;
            parent.children = children;
            
            sortChildren(children);
            items.addAll(position + 1, children);
            notifyItemChanged(position);
            notifyItemRangeInserted(position + 1, children.size());
        }
    }
    
    public void collapseAt(int position) {
        if (position >= 0 && position < items.size()) {
            FileTreeItem parent = items.get(position);
            parent.isExpanded = false;
            
            int count = countChildren(position);
            if (count > 0) {
                for (int i = 0; i < count; i++) {
                    items.remove(position + 1);
                }
                notifyItemChanged(position);
                notifyItemRangeRemoved(position + 1, count);
            }
        }
    }
    
    private int countChildren(int position) {
        FileTreeItem parent = items.get(position);
        int count = 0;
        for (int i = position + 1; i < items.size(); i++) {
            if (items.get(i).level > parent.level) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }
    
    private void sortItems() {
        Collections.sort(items, getComparator());
    }
    
    private void sortChildren(List<FileTreeItem> children) {
        Collections.sort(children, getComparator());
    }
    
    private Comparator<FileTreeItem> getComparator() {
        return (a, b) -> {
            if (a.isDirectory && !b.isDirectory) return -1;
            if (!a.isDirectory && b.isDirectory) return 1;
            return a.name.compareToIgnoreCase(b.name);
        };
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file_tree, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FileTreeItem item = items.get(position);
        holder.bind(item, listener, position);
    }
    
    @Override
    public int getItemCount() {
        return items.size();
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView iconView;
        private final TextView nameView;
        private final View indentView;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.fileIcon);
            nameView = itemView.findViewById(R.id.fileName);
            indentView = itemView.findViewById(R.id.indent);
        }
        
        public void bind(FileTreeItem item, OnItemClickListener listener, int position) {
            nameView.setText(item.name);
            
            ViewGroup.LayoutParams params = indentView.getLayoutParams();
            params.width = item.level * 32;
            indentView.setLayoutParams(params);
            
            if (item.isDirectory) {
                iconView.setImageResource(item.isExpanded ? R.drawable.ic_folder_open : R.drawable.ic_folder);
            } else {
                iconView.setImageResource(getFileIcon(item.name));
            }
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    if (item.isDirectory) {
                        listener.onFolderClick(item, position);
                    } else {
                        listener.onFileClick(item);
                    }
                }
            });
        }
        
        private int getFileIcon(String fileName) {
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".java")) return R.drawable.ic_file_java;
            if (lower.endsWith(".kt")) return R.drawable.ic_file_kotlin;
            if (lower.endsWith(".js") || lower.endsWith(".jsx")) return R.drawable.ic_file_js;
            if (lower.endsWith(".ts") || lower.endsWith(".tsx")) return R.drawable.ic_file_ts;
            if (lower.endsWith(".py")) return R.drawable.ic_file_python;
            if (lower.endsWith(".html") || lower.endsWith(".htm")) return R.drawable.ic_file_html;
            if (lower.endsWith(".css") || lower.endsWith(".scss")) return R.drawable.ic_file_css;
            if (lower.endsWith(".json")) return R.drawable.ic_file_json;
            if (lower.endsWith(".xml")) return R.drawable.ic_file_xml;
            if (lower.endsWith(".md")) return R.drawable.ic_file_md;
            return R.drawable.ic_file_default;
        }
    }
}
