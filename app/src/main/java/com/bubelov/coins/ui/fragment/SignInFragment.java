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
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.model.AuthResponse;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import retrofit2.Response;

/**
 * @author Igor Bubelov
 */

public class SignInFragment extends AuthFragment implements TextView.OnEditorActionListener {
    @BindView(R.id.email)
    EditText email;

    @BindView(R.id.password)
    EditText password;

    @BindView(R.id.sign_in)
    Button signInButton;

    private Unbinder unbinder;

    private SignInTask signInTask;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign_in, container, false);
        unbinder = ButterKnife.bind(this, view);
        password.setOnEditorActionListener(this);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        password.setOnEditorActionListener(null);
        unbinder.unbind();

        if (signInTask != null) {
            signInTask.cancel(true);
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_GO) {
            signIn(email.getText().toString(), password.getText().toString());
            return true;
        }

        return false;
    }

    @OnClick(R.id.sign_in)
    void onSignInClick() {
        signIn(email.getText().toString(), password.getText().toString());
    }

    private void signIn(String email, String password) {
        signInTask = new SignInTask(email, password);
        signInTask.execute();
    }

    private class SignInTask extends AsyncTask<Void, Void, Void> {
        private String email;

        private String password;

        private Response<AuthResponse> response;

        public SignInTask(String email, String password) {
            this.email = email;
            this.password = password;
        }

        @Override
        protected void onPreExecute() {
            signInButton.setEnabled(false);
        }

        @Override
        protected Void doInBackground(Void... params) {
            CoinsApi api = Injector.INSTANCE.mainComponent().api();

            try {
                response = api.authWithEmail(email, password).execute();
            } catch (IOException e) {
                // TODO
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void success) {
            signInButton.setEnabled(true);
            onAuthResponse(response);
        }
    }
}