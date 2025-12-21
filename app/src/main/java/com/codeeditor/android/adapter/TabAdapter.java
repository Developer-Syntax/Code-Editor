package com.codeeditor.android.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.codeeditor.android.R;
import com.codeeditor.android.model.OpenFile;

import java.util.ArrayList;
import java.util.List;

public class TabAdapter extends RecyclerView.Adapter<TabAdapter.ViewHolder> {
    
    private List<OpenFile> tabs = new ArrayList<>();
    private int selectedPosition = -1;
    private OnTabClickListener listener;
    
    public interface OnTabClickListener {
        void onTabClick(OpenFile file, int position);
        void onTabClose(OpenFile file, int position);
    }
    
    public void setOnTabClickListener(OnTabClickListener listener) {
        this.listener = listener;
    }
    
    public void setTabs(List<OpenFile> tabs) {
        this.tabs = tabs;
        notifyDataSetChanged();
    }
    
    public void addTab(OpenFile file) {
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).path.equals(file.path)) {
                setSelectedPosition(i);
                return;
            }
        }
        tabs.add(file);
        notifyItemInserted(tabs.size() - 1);
        setSelectedPosition(tabs.size() - 1);
    }
    
    public void removeTab(int position) {
        if (position >= 0 && position < tabs.size()) {
            int oldSelectedPosition = selectedPosition;
            
            tabs.remove(position);
            notifyItemRemoved(position);
            
            if (tabs.isEmpty()) {
                selectedPosition = -1;
            } else if (position < selectedPosition) {
                selectedPosition--;
            } else if (position == selectedPosition) {
                if (selectedPosition >= tabs.size()) {
                    selectedPosition = tabs.size() - 1;
                }
            }
            
            int adjustedOldPosition = position < oldSelectedPosition ? oldSelectedPosition - 1 : oldSelectedPosition;
            if (adjustedOldPosition >= 0 && adjustedOldPosition < tabs.size() && adjustedOldPosition != selectedPosition) {
                notifyItemChanged(adjustedOldPosition);
            }
            if (selectedPosition >= 0 && selectedPosition < tabs.size()) {
                notifyItemChanged(selectedPosition);
            }
        }
    }
    
    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        if (oldPosition >= 0 && oldPosition < tabs.size()) {
            notifyItemChanged(oldPosition);
        }
        if (position >= 0 && position < tabs.size()) {
            notifyItemChanged(position);
        }
    }
    
    public int getSelectedPosition() {
        return selectedPosition;
    }
    
    public OpenFile getSelectedTab() {
        if (selectedPosition >= 0 && selectedPosition < tabs.size()) {
            return tabs.get(selectedPosition);
        }
        return null;
    }
    
    public void updateTabModified(int position, boolean modified) {
        if (position >= 0 && position < tabs.size()) {
            tabs.get(position).isModified = modified;
            notifyItemChanged(position);
        }
    }
    
    public void updateTabSha(int position, String sha) {
        if (position >= 0 && position < tabs.size()) {
            tabs.get(position).sha = sha;
        }
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tab, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OpenFile file = tabs.get(position);
        holder.bind(file, position == selectedPosition, listener, position);
    }
    
    @Override
    public int getItemCount() {
        return tabs.size();
    }
    
    public List<OpenFile> getTabs() {
        return tabs;
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameView;
        private final ImageButton closeButton;
        private final View indicator;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.tabName);
            closeButton = itemView.findViewById(R.id.tabClose);
            indicator = itemView.findViewById(R.id.tabIndicator);
        }
        
        public void bind(OpenFile file, boolean isSelected, OnTabClickListener listener, int position) {
            String displayName = file.isModified ? "● " + file.name : file.name;
            nameView.setText(displayName);
            
            indicator.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);
            itemView.setSelected(isSelected);
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTabClick(file, position);
                }
            });
            
            closeButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTabClose(file, position);
                }
            });
        }
    }
}
