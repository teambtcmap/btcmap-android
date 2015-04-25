package com.bubelov.coins.ui.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Author: Igor Bubelov
 * Date: 12/07/14 21:22
 */

public class ConfirmationDialog extends DialogFragment {
    private static final String TEXT_RESOURCE_ID_KEY = "text_resource_id";

    public static ConfirmationDialog newInstance(int textResourceId) {
        ConfirmationDialog dialog = new ConfirmationDialog();
        Bundle arguments = new Bundle();
        arguments.putInt(TEXT_RESOURCE_ID_KEY, textResourceId);
        dialog.setArguments(arguments);
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int titleId = getArguments().getInt(TEXT_RESOURCE_ID_KEY);

        return new AlertDialog.Builder(getActivity())
                .setTitle(titleId)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ((ConfirmationDialogListener)getActivity()).onConfirmed(getTag());
                            }
                        }
                )
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ((ConfirmationDialogListener)getActivity()).onCancelled(getTag());
                            }
                        }
                )
                .create();
    }
}
