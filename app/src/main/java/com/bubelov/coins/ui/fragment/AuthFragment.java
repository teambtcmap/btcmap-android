package com.bubelov.coins.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.bubelov.coins.R;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.model.AuthResponse;
import com.bubelov.coins.ui.activity.AbstractActivity;
import com.bubelov.coins.ui.activity.MapActivity;
import com.bubelov.coins.util.AuthController;
import com.bubelov.coins.util.Utils;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

import retrofit2.Response;

/**
 * @author Igor Bubelov
 */

public abstract class AuthFragment extends Fragment {
    protected final void onAuthResponse(Response<AuthResponse> response) {
        if (response == null) {
            if (getActivity() != null) {
                AbstractActivity abstractActivity = (AbstractActivity) getActivity();
                abstractActivity.showAlert(R.string.could_not_connect_to_server);
            }

             return;
        }

        if (response.isSuccessful()) {
            AuthController authController = Injector.INSTANCE.mainComponent().authController();
            authController.setToken(response.body().getToken());

            Intent intent = new Intent(getContext(), MapActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);

            FirebaseAnalytics analytics = Injector.INSTANCE.mainComponent().analytics();
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.SIGN_UP_METHOD, "email");
            analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle);
        } else {
            Gson gson = Injector.INSTANCE.mainComponent().gson();
            List<String> errors = gson.fromJson(response.errorBody().charStream(), new TypeToken<List<String>>(){}.getType());
            Utils.showErrors(getContext(), errors);
        }
    }
}