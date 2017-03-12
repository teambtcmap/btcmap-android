package com.bubelov.coins.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

import com.bubelov.coins.R;
import com.bubelov.coins.TypefaceManager;

/**
 * @author Igor Bubelov
 */

public class TypefaceTextView extends AppCompatTextView {
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
