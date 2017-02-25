package com.bubelov.coins.ui.fragment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.bubelov.coins.R;
import com.bubelov.coins.api.CoinsApi;
import com.bubelov.coins.api.UserParams;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.model.AuthResponse;
import com.crashlytics.android.Crashlytics;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import retrofit2.Response;
import timber.log.Timber;

/**
 * @author Igor Bubelov
 */

public class SignUpFragment extends AuthFragment  implements TextView.OnEditorActionListener {
    @BindView(R.id.email)
    EditText email;

    @BindView(R.id.password)
    EditText password;

    @BindView(R.id.first_name)
    EditText firstName;

    @BindView(R.id.last_name)
    EditText lastName;

    @BindView(R.id.sign_up)
    Button signUpButton;

    private Unbinder unbinder;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign_up, container, false);
        unbinder = ButterKnife.bind(this, view);
        lastName.setOnEditorActionListener(this);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        lastName.setOnEditorActionListener(null);
        unbinder.unbind();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_GO) {
            signUp(email.getText().toString(), password.getText().toString(), firstName.getText().toString(), lastName.getText().toString());
            return true;
        }

        return false;
    }

    @OnClick(R.id.sign_up)
    void onSignUpClick() {
        signUp(email.getText().toString(), password.getText().toString(), firstName.getText().toString(), lastName.getText().toString());
    }

    private void signUp(String email, String password, String firstName, String lastName) {
        new SignUpTask().execute(new UserParams(email, password, firstName, lastName));
    }

    private class SignUpTask extends AsyncTask<UserParams, Void, Void> {
        private Response<AuthResponse> response;

        @Override
        protected void onPreExecute() {
            signUpButton.setEnabled(false);
        }

        @Override
        protected Void doInBackground(UserParams... params) {
            CoinsApi api = Injector.INSTANCE.getAndroidComponent().provideApi();

            try {
                response = api.createUser(params[0]).execute();
            } catch (IOException e) {
                Timber.e(e, "Couldn't sign up");
                Crashlytics.logException(e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            signUpButton.setEnabled(true);
            onAuthResponse(response);
        }
    }
}