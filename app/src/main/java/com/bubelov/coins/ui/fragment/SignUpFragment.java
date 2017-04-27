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
import android.widget.ViewSwitcher;

import com.bubelov.coins.R;
import com.bubelov.coins.data.api.coins.CoinsApi;
import com.bubelov.coins.data.api.coins.UserParams;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.data.api.coins.model.AuthResponse;

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
    private static final int STATE_FILL_FORM = 0;
    private static final int STATE_PROGRESS = 1;

    @BindView(R.id.state_switcher)
    ViewSwitcher stateSwitcher;

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

    private SignUpTask signUpTask;

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

        if (signUpTask != null) {
            signUpTask.cancel(true);
        }
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
        signUpTask = new SignUpTask();
        signUpTask.execute(new UserParams(email, password, firstName, lastName));
    }

    private void setState(int state) {
        stateSwitcher.setDisplayedChild(state);
    }

    private class SignUpTask extends AsyncTask<UserParams, Void, Void> {
        private Response<AuthResponse> response;

        @Override
        protected void onPreExecute() {
            setState(STATE_PROGRESS);
        }

        @Override
        protected Void doInBackground(UserParams... params) {
            CoinsApi api = Injector.INSTANCE.mainComponent().dataManager().coinsApi();

            try {
                response = api.createUser(params[0]).execute();
            } catch (IOException e) {
                Timber.e(e, "Couldn't sign up");
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (response == null || !response.isSuccessful()) {
                setState(STATE_FILL_FORM);
            }

            onAuthResponse(response);
        }
    }
}