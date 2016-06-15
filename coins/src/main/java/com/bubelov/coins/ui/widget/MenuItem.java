package com.bubelov.coins.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.bubelov.coins.R;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Author: Igor Bubelov
 * Date: 06/04/15 19:48
 */

public class MenuItem extends FrameLayout {
    @BindView(R.id.icon)
    View icon;

    @BindView(R.id.text)
    TextView text;

    public MenuItem(Context context) {
        super(context);
        init();
    }

    public MenuItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        applyAttributes(attrs);
    }

    public MenuItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        applyAttributes(attrs);
    }

    public String getText() {
        return text.getText().toString();
    }

    public void setSelected(boolean selected) {
        setBackgroundColor(selected
                ? getResources().getColor(R.color.drawer_list_item_background_selected)
                : getResources().getColor(R.color.drawer_list_item_background));

        text.setTextColor(selected
                ? getResources().getColor(R.color.primary_dark)
                : getResources().getColor(R.color.drawer_list_item_text));

        if (icon.getBackground() != null) {
            if (selected) {
                DrawableCompat.setTint(icon.getBackground(), getResources().getColor(R.color.drawer_icon_active));
            } else {
                DrawableCompat.setTint(icon.getBackground(), getResources().getColor(R.color.drawer_icon_inactive));
            }
        }
    }

    private void init() {
        inflate(getContext(), R.layout.widget_menu_item, this);
        ButterKnife.bind(this);
        setSelected(false);
    }

    private void applyAttributes(AttributeSet attrs) {
        TypedArray a = getContext().getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.MenuItem,
                0, 0);

        try {
            icon.setBackgroundResource(a.getResourceId(R.styleable.MenuItem_mi_icon, 0));
            text.setText(a.getString(R.styleable.MenuItem_text));
        } finally {
            a.recycle();
        }
    }
}
