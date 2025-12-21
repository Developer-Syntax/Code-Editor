package com.codeeditor.android.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.codeeditor.android.R;
import com.codeeditor.android.utils.SnippetManager.Snippet;

import java.util.ArrayList;
import java.util.List;

public class SnippetAdapter extends RecyclerView.Adapter<SnippetAdapter.ViewHolder> {

    private List<Snippet> snippets = new ArrayList<>();
    private OnSnippetClickListener listener;

    public interface OnSnippetClickListener {
        void onSnippetClick(Snippet snippet);
        void onSnippetLongClick(Snippet snippet, int position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_snippet, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Snippet snippet = snippets.get(position);
        holder.bind(snippet);
    }

    @Override
    public int getItemCount() {
        return snippets.size();
    }

    public void setSnippets(List<Snippet> snippets) {
        this.snippets = snippets;
        notifyDataSetChanged();
    }

    public void setOnSnippetClickListener(OnSnippetClickListener listener) {
        this.listener = listener;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName;
        private TextView tvTrigger;
        private TextView tvDescription;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvSnippetName);
            tvTrigger = itemView.findViewById(R.id.tvSnippetTrigger);
            tvDescription = itemView.findViewById(R.id.tvSnippetDescription);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSnippetClick(snippets.get(pos));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSnippetLongClick(snippets.get(pos), pos);
                    return true;
                }
                return false;
            });
        }

        void bind(Snippet snippet) {
            tvName.setText(snippet.name);
            tvTrigger.setText(snippet.trigger);
            tvDescription.setText(snippet.description);
        }
    }
}
