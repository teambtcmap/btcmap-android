package com.bubelov.coins.util;

import android.widget.SeekBar;

/**
 * Author: Igor Bubelov
 */

public abstract class OnSeekBarChangeAdapter implements SeekBar.OnSeekBarChangeListener {
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        // Nothing to do here
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // Nothing to do here
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // Nothing to do here
    }
}
