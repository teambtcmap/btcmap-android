package com.bubelov.coins.ui.widget;

import android.content.Context;
import android.support.v7.internal.widget.TintImageView;
import android.util.AttributeSet;

import com.bubelov.coins.R;

/**
 * Author: Igor Bubelov
 * Date: 04/05/15 17:58
 */

public class ClickableImageView extends TintImageView {
    public ClickableImageView(Context context) {
        super(context);
    }

    public ClickableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ClickableImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (enabled) {
            setColorFilter(getResources().getColor(R.color.primary));
        } else {
            setColorFilter(getResources().getColor(R.color.icons));
        }
    }
}
