package com.bubelov.coins.ui.dialog;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;

import com.bubelov.coins.R;
import com.bubelov.coins.util.AuthController;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * @author Igor Bubelov
 */

public class MapPopupMenu extends PopupWindow {
    @BindView(R.id.settings)
    View settings;

    @BindView(R.id.sign_in)
    View signIn;

    @BindView(R.id.sign_out)
    View signOut;

    private Listener listener;

    public MapPopupMenu(Context context) {
        super(context);

        if (context instanceof Listener) {
            listener = (Listener) context;
        }

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.menu_map_popup, null);
        ButterKnife.bind(this, view);

        setFocusable(true);
        setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        setContentView(view);

        setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        if (new AuthController().isAuthorized()) {
            signIn.setVisibility(View.GONE);
        } else {
            signOut.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.settings)
    public void onSettingsClick() {
        listener.onSettingsClick();
        dismiss();
    }

    @OnClick(R.id.exchange_rates)
    public void onExchangeRatesClick() {
        listener.onExchangeRatesClick();
        dismiss();
    }

    @OnClick(R.id.sign_in)
    public void onSignInClick() {
        listener.onSignInClick();
        dismiss();
    }

    @OnClick(R.id.sign_out)
    public void onSignOutClick() {
        listener.onSignOutClick();
        dismiss();
    }

    public interface Listener {
        void onSettingsClick();

        void onExchangeRatesClick();

        void onSignInClick();

        void onSignOutClick();
    }
}