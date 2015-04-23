package com.bubelov.coins.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bubelov.coins.R;

/**
 * Author: Igor Bubelov
 * Date: 06/04/15 19:48
 */

public class MenuItem extends FrameLayout {
    private ImageView icon;

    private TextView text;

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
                ? getResources().getColor(R.color.drawer_list_item_text_selected)
                : getResources().getColor(R.color.drawer_list_item_text));
    }

    private void init() {
        inflate(getContext(), R.layout.widget_menu_item, this);
        icon = (ImageView) findViewById(R.id.icon);
        text = (TextView) findViewById(R.id.text);
        setSelected(false);
    }

    private void applyAttributes(AttributeSet attrs) {
        TypedArray a = getContext().getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.MenuItem,
                0, 0);

        try {
            icon.setImageResource(a.getResourceId(R.styleable.MenuItem_mi_icon, 0));
            text.setText(a.getString(R.styleable.MenuItem_text));
        } finally {
            a.recycle();
        }
    }
}
