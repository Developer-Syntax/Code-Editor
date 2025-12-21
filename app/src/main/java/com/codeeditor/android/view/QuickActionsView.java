package com.codeeditor.android.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.codeeditor.android.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class QuickActionsView extends FloatingActionButton {

    public enum Action {
        UNDO,
        REDO,
        COPY,
        CUT,
        PASTE,
        SELECT_ALL,
        FIND,
        REPLACE,
        GO_TO_LINE,
        DUPLICATE_LINE,
        TOGGLE_COMMENT,
        FORMAT_CODE,
        SNIPPETS,
        SAVE
    }

    private PopupWindow popupWindow;
    private OnQuickActionListener listener;

    public interface OnQuickActionListener {
        void onQuickAction(Action action);
    }

    public QuickActionsView(Context context) {
        super(context);
        init(context);
    }

    public QuickActionsView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public QuickActionsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setImageResource(R.drawable.ic_quick_action);
        setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.fab_background));

        setOnClickListener(v -> showQuickActionsMenu());
    }

    private void showQuickActionsMenu() {
        Context context = getContext();
        
        LinearLayout menuLayout = new LinearLayout(context);
        menuLayout.setOrientation(LinearLayout.VERTICAL);
        menuLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.quick_action_background));
        menuLayout.setPadding(8, 8, 8, 8);
        menuLayout.setElevation(8);

        addActionButton(menuLayout, "Undo", R.drawable.ic_undo, Action.UNDO);
        addActionButton(menuLayout, "Redo", R.drawable.ic_redo, Action.REDO);
        addDivider(menuLayout);
        addActionButton(menuLayout, "Find", R.drawable.ic_search, Action.FIND);
        addActionButton(menuLayout, "Replace", R.drawable.ic_find_replace, Action.REPLACE);
        addActionButton(menuLayout, "Go to Line", R.drawable.ic_go_to_line, Action.GO_TO_LINE);
        addDivider(menuLayout);
        addActionButton(menuLayout, "Duplicate Line", R.drawable.ic_add, Action.DUPLICATE_LINE);
        addActionButton(menuLayout, "Toggle Comment", R.drawable.ic_collapse, Action.TOGGLE_COMMENT);
        addDivider(menuLayout);
        addActionButton(menuLayout, "Snippets", R.drawable.ic_snippet, Action.SNIPPETS);
        addActionButton(menuLayout, "Save", R.drawable.ic_save, Action.SAVE);

        popupWindow = new PopupWindow(menuLayout, 
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true);

        popupWindow.setElevation(16);
        popupWindow.setOutsideTouchable(true);

        int[] location = new int[2];
        getLocationOnScreen(location);
        popupWindow.showAsDropDown(this, -200, -menuLayout.getChildCount() * 48 - 100);
    }

    private void addActionButton(LinearLayout parent, String text, int iconRes, Action action) {
        Context context = getContext();
        
        MaterialButton button = new MaterialButton(context, null,
            com.google.android.material.R.attr.borderlessButtonStyle);
        button.setText(text);
        button.setTextColor(ContextCompat.getColor(context, R.color.on_surface));
        button.setIconResource(iconRes);
        button.setIconTint(ContextCompat.getColorStateList(context, R.color.on_surface));
        button.setIconGravity(MaterialButton.ICON_GRAVITY_START);
        button.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        button.setPadding(32, 16, 48, 16);
        button.setMinWidth(200);
        button.setAllCaps(false);

        button.setOnClickListener(v -> {
            if (listener != null) {
                listener.onQuickAction(action);
            }
            if (popupWindow != null) {
                popupWindow.dismiss();
            }
        });

        parent.addView(button);
    }

    private void addDivider(LinearLayout parent) {
        View divider = new View(getContext());
        divider.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.divider));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1);
        params.setMargins(16, 4, 16, 4);
        parent.addView(divider, params);
    }

    public void setOnQuickActionListener(OnQuickActionListener listener) {
        this.listener = listener;
    }

    public void dismissPopup() {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
    }
}
