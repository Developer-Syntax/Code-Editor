package com.codeeditor.android.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.codeeditor.android.R;
import com.codeeditor.android.utils.RecentFilesManager.RecentFile;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecentFilesAdapter extends RecyclerView.Adapter<RecentFilesAdapter.ViewHolder> {

    private List<RecentFile> recentFiles = new ArrayList<>();
    private OnRecentFileClickListener listener;

    public interface OnRecentFileClickListener {
        void onRecentFileClick(RecentFile file);
        void onRecentFileLongClick(RecentFile file, int position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_recent_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecentFile file = recentFiles.get(position);
        holder.bind(file);
    }

    @Override
    public int getItemCount() {
        return recentFiles.size();
    }

    public void setRecentFiles(List<RecentFile> files) {
        this.recentFiles = files;
        notifyDataSetChanged();
    }

    public void setOnRecentFileClickListener(OnRecentFileClickListener listener) {
        this.listener = listener;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivFileIcon;
        private TextView tvFileName;
        private TextView tvFilePath;
        private ImageView ivSource;

        ViewHolder(View itemView) {
            super(itemView);
            ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFilePath = itemView.findViewById(R.id.tvFilePath);
            ivSource = itemView.findViewById(R.id.ivSource);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onRecentFileClick(recentFiles.get(pos));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onRecentFileLongClick(recentFiles.get(pos), pos);
                    return true;
                }
                return false;
            });
        }

        void bind(RecentFile file) {
            tvFileName.setText(file.name);

            if (file.isGitHub) {
                String path = file.owner + "/" + file.repo;
                if (file.path != null && !file.path.isEmpty()) {
                    path += "/" + file.path;
                }
                tvFilePath.setText(path);
                ivSource.setImageResource(R.drawable.ic_github);
            } else {
                tvFilePath.setText(formatTimestamp(file.lastOpened));
                ivSource.setImageResource(R.drawable.ic_folder);
            }

            ivFileIcon.setImageResource(getFileIcon(file.name));
        }

        private int getFileIcon(String filename) {
            if (filename == null) return R.drawable.ic_file_default;
            
            String lower = filename.toLowerCase();
            if (lower.endsWith(".java")) return R.drawable.ic_file_java;
            if (lower.endsWith(".kt") || lower.endsWith(".kts")) return R.drawable.ic_file_kotlin;
            if (lower.endsWith(".py")) return R.drawable.ic_file_python;
            if (lower.endsWith(".js") || lower.endsWith(".jsx")) return R.drawable.ic_file_js;
            if (lower.endsWith(".ts") || lower.endsWith(".tsx")) return R.drawable.ic_file_ts;
            if (lower.endsWith(".html") || lower.endsWith(".htm")) return R.drawable.ic_file_html;
            if (lower.endsWith(".css") || lower.endsWith(".scss")) return R.drawable.ic_file_css;
            if (lower.endsWith(".json")) return R.drawable.ic_file_json;
            if (lower.endsWith(".xml")) return R.drawable.ic_file_xml;
            if (lower.endsWith(".md")) return R.drawable.ic_file_md;
            return R.drawable.ic_file_default;
        }

        private String formatTimestamp(long timestamp) {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;

            if (diff < 60000) {
                return "Just now";
            } else if (diff < 3600000) {
                return (diff / 60000) + " min ago";
            } else if (diff < 86400000) {
                return (diff / 3600000) + " hours ago";
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
                return sdf.format(new Date(timestamp));
            }
        }
    }
}
