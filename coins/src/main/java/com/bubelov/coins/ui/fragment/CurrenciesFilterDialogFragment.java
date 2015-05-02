package com.bubelov.coins.ui.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

/**
 * Author: Igor Bubelov
 * Date: 02/05/15 10:07
 */

public class CurrenciesFilterDialogFragment extends DialogFragment {
    public static final String TAG = CurrenciesFilterDialogFragment.class.getSimpleName();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new CurrenciesFilterDialog(getActivity());
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (getActivity() instanceof Listener) {
            ((Listener) getActivity()).onCurrenciesFilterDismissed();
        }

        super.onDismiss(dialog);
    }

    public interface Listener {
        void onCurrenciesFilterDismissed();
    }
}
