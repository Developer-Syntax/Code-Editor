package com.codeeditor.android.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.codeeditor.android.R;

import java.util.ArrayList;
import java.util.List;

public class AutocompleteAdapter extends RecyclerView.Adapter<AutocompleteAdapter.ViewHolder> {

    public enum SuggestionType {
        KEYWORD,
        FUNCTION,
        VARIABLE,
        CLASS,
        SNIPPET
    }

    public static class Suggestion {
        public String text;
        public String description;
        public SuggestionType type;
        public String insertText;

        public Suggestion(String text, String description, SuggestionType type) {
            this.text = text;
            this.description = description;
            this.type = type;
            this.insertText = text;
        }

        public Suggestion(String text, String description, SuggestionType type, String insertText) {
            this.text = text;
            this.description = description;
            this.type = type;
            this.insertText = insertText;
        }
    }

    private List<Suggestion> suggestions = new ArrayList<>();
    private OnSuggestionClickListener listener;

    public interface OnSuggestionClickListener {
        void onSuggestionClick(Suggestion suggestion);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_autocomplete, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Suggestion suggestion = suggestions.get(position);
        holder.bind(suggestion);
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    public void setSuggestions(List<Suggestion> suggestions) {
        this.suggestions = suggestions;
        notifyDataSetChanged();
    }

    public void setOnSuggestionClickListener(OnSuggestionClickListener listener) {
        this.listener = listener;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvText;
        private TextView tvDescription;
        private ImageView ivIcon;

        ViewHolder(View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tvSuggestionText);
            tvDescription = itemView.findViewById(R.id.tvSuggestionDescription);
            ivIcon = itemView.findViewById(R.id.ivSuggestionIcon);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSuggestionClick(suggestions.get(pos));
                }
            });
        }

        void bind(Suggestion suggestion) {
            tvText.setText(suggestion.text);
            tvDescription.setText(suggestion.description);
            
            Context context = itemView.getContext();
            int iconRes;
            int iconColor;
            
            switch (suggestion.type) {
                case KEYWORD:
                    iconRes = R.drawable.ic_keyword;
                    iconColor = R.color.keyword_icon;
                    break;
                case FUNCTION:
                    iconRes = R.drawable.ic_function;
                    iconColor = R.color.function_icon;
                    break;
                case CLASS:
                    iconRes = R.drawable.ic_class;
                    iconColor = R.color.class_icon;
                    break;
                case SNIPPET:
                    iconRes = R.drawable.ic_snippet;
                    iconColor = R.color.snippet_icon;
                    break;
                case VARIABLE:
                default:
                    iconRes = R.drawable.ic_variable;
                    iconColor = R.color.variable_icon;
                    break;
            }
            
            ivIcon.setImageResource(iconRes);
            ivIcon.setColorFilter(ContextCompat.getColor(context, iconColor));
        }
    }
}
