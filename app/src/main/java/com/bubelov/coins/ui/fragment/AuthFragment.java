package com.bubelov.coins.ui.fragment;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.bubelov.coins.R;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.model.AuthResponse;
import com.bubelov.coins.ui.activity.MapActivity;
import com.bubelov.coins.util.AuthController;
import com.bubelov.coins.util.Utils;
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
            Toast.makeText(getContext(), R.string.could_not_connect_to_server, Toast.LENGTH_SHORT).show();
            return;
        }

        if (response.isSuccessful()) {
            new AuthController().setToken(response.body().getToken());

            Intent intent = new Intent(getContext(), MapActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else {
            Gson gson = Injector.INSTANCE.getCoreComponent().gson();
            List<String> errors = gson.fromJson(response.errorBody().charStream(), new TypeToken<List<String>>(){}.getType());
            Utils.showErrors(getContext(), errors);
        }
    }
}