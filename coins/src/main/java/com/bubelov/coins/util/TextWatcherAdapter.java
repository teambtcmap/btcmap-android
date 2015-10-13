package com.bubelov.coins.util;

import android.text.Editable;
import android.text.TextWatcher;

/**
 * Author: Igor Bubelov
 * Date: 10/12/15 2:24 PM
 */

public abstract class TextWatcherAdapter implements TextWatcher {
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Nothing to do here
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Nothing to do here
    }

    @Override
    public void afterTextChanged(Editable s) {
        // Nothing to do here
    }
}
