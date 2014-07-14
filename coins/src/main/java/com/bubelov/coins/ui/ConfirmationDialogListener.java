package com.bubelov.coins.ui;

/**
 * Author: Igor Bubelov
 * Date: 12/07/14 21:28
 */

public interface ConfirmationDialogListener {
    void onConfirmed(String tag);

    void onCancelled(String tag);
}
