package com.bubelov.coins.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.bubelov.coins.R;
import com.bubelov.coins.util.Utils;

/**
 * Author: Igor Bubelov
 * Date: 13/05/15 16:04
 */

public class DrawerMenuRootLayout extends ScrimInsetsFrameLayout {
    public DrawerMenuRootLayout(Context context) {
        super(context);
    }

    public DrawerMenuRootLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DrawerMenuRootLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int actionBarHeight = 0;

        TypedValue typedValue = new TypedValue();

        if (getContext().getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(typedValue.data, getResources().getDisplayMetrics());
        }

        int optimalWidth = Utils.getScreenWidth(getContext()) - actionBarHeight;
        int maxWidth = getResources().getDimensionPixelSize(R.dimen.drawer_menu_max_width);

        getLayoutParams().width = Math.min(optimalWidth, maxWidth);
        forceLayout();
    }
}
