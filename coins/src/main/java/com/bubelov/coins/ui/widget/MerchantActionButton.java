package com.bubelov.coins.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.bubelov.coins.R;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Author: Igor Bubelov
 * Date: 6/20/15 9:02 PM
 */

public class MerchantActionButton extends FrameLayout {
    @Bind(R.id.text) TextView text;

    public MerchantActionButton(Context context) {
        super(context);
        init();
    }

    public MerchantActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        applyAttributes(attrs);
    }

    public MerchantActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        applyAttributes(attrs);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        int color = enabled ? getResources().getColor(R.color.primary_dark) : getResources().getColor(R.color.secondary_text_or_icons);
        text.setTextColor(color);
        DrawableCompat.setTint(DrawableCompat.wrap(text.getCompoundDrawables()[1]), color);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return !isEnabled();
    }

    private void init() {
        inflate(getContext(), R.layout.widget_merchant_action_button, this);
        ButterKnife.bind(this);
    }

    private void applyAttributes(AttributeSet attrs) {
        TypedArray a = getContext().getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.MerchantActionButton,
                0, 0);

        try {
            text.setText(a.getString(R.styleable.MerchantActionButton_text));
            text.setCompoundDrawablesWithIntrinsicBounds(0, a.getResourceId(R.styleable.MerchantActionButton_mab_icon, 0), 0, 0);
        } finally {
            a.recycle();
        }
    }
}
