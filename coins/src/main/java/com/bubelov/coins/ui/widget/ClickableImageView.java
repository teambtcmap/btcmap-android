package com.bubelov.coins.ui.widget;

import android.content.Context;
import android.support.v7.internal.widget.TintImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;

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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            setColorFilter(getResources().getColor(R.color.primary_semi_transparent));
        }

        if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            setColorFilter(getResources().getColor(R.color.primary));
        }

        return super.onTouchEvent(event);
    }
}
