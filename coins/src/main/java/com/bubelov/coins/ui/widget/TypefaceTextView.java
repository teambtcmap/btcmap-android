package com.bubelov.coins.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import com.bubelov.coins.R;
import com.bubelov.coins.TypefaceManager;

/**
 * Author: Igor Bubelov
 * Date: 13/05/15 14:48
 */

public class TypefaceTextView extends TextView {
    public TypefaceTextView(Context context) {
        super(context);
    }

    public TypefaceTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        applyAttributes(attrs);
    }

    public TypefaceTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        applyAttributes(attrs);
    }

    private void applyAttributes(AttributeSet attributes) {
        if (isInEditMode()) {
            return;
        }

        TypedArray array = getContext().getTheme().obtainStyledAttributes(
                attributes,
                R.styleable.TypefaceTextView,
                0, 0
        );

        try {
            int typeface = array.getInt(R.styleable.TypefaceTextView_typeface, -1);

            if (typeface != -1) {
                setTypeface(TypefaceManager.getTypeface(getContext(), typeface));
            }
        } finally {
            array.recycle();
        }

        getPaint().setSubpixelText(true);
    }
}
