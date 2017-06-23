package com.bubelov.coins.ui.fragment;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.bubelov.coins.R;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.repository.user.UserRepository;
import com.bubelov.coins.ui.activity.AbstractActivity;
import com.bubelov.coins.ui.activity.MapActivity;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * @author Igor Bubelov
 */

public class SignInFragment extends Fragment implements TextView.OnEditorActionListener {
    @BindView(R.id.email)
    EditText email;

    @BindView(R.id.password)
    EditText password;

    @BindView(R.id.sign_in)
    Button signInButton;

    @Inject
    UserRepository userRepository;

    private Unbinder unbinder;

    private SignInTask signInTask;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Injector.INSTANCE.getMainComponent().inject(this);
    }

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

    private class SignInTask extends AsyncTask<Void, Void, Boolean> {
        private String email;

        private String password;

        SignInTask(String email, String password) {
            this.email = email;
            this.password = password;
        }

        @Override
        protected void onPreExecute() {
            signInButton.setEnabled(false);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return userRepository.signIn(email, password);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            signInButton.setEnabled(true);

            if (!success) { // TODO
                if (getActivity() != null) {
                    AbstractActivity abstractActivity = (AbstractActivity) getActivity();
                    abstractActivity.showAlert(R.string.could_not_connect_to_server);
                }

                return;
            }

            Intent intent = new Intent(getContext(), MapActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }
}