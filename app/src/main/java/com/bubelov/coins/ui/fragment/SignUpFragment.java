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
import android.widget.ViewSwitcher;

import com.bubelov.coins.R;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.repository.user.UserRepository;
import com.bubelov.coins.ui.activity.MapActivity;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * @author Igor Bubelov
 */

public class SignUpFragment extends Fragment implements TextView.OnEditorActionListener {
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

    @Inject
    UserRepository userRepository;

    private Unbinder unbinder;

    private SignUpTask signUpTask;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Injector.INSTANCE.mainComponent().inject(this);
    }

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
        signUpTask = new SignUpTask(email, password, firstName, lastName);
        signUpTask.execute();
    }

    private void setState(int state) {
        stateSwitcher.setDisplayedChild(state);
    }

    private class SignUpTask extends AsyncTask<Void, Void, Boolean> {
        private final String email;

        private final String password;

        private final String firstName;

        private final String lastName;

        SignUpTask(String email, String password, String firstName, String lastName) {
            this.email = email;
            this.password = password;
            this.firstName = firstName;
            this.lastName = lastName;
        }

        @Override
        protected void onPreExecute() {
            setState(STATE_PROGRESS);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return userRepository.signUp(email, password, firstName, lastName);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (!success) {
                setState(STATE_FILL_FORM); // TODO
                return;
            }

            Intent intent = new Intent(getContext(), MapActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }
}