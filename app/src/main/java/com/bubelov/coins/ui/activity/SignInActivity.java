package com.bubelov.coins.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.bubelov.coins.BuildConfig;
import com.bubelov.coins.R;
import com.bubelov.coins.data.repository.user.UserRepository;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.firebase.analytics.FirebaseAnalytics;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * @author Igor Bubelov
 */

public class SignInActivity extends AbstractActivity implements GoogleApiClient.OnConnectionFailedListener {
    private static final int REQUEST_GOOGLE_SIGN_IN = 10;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.view_switcher)
    ViewSwitcher viewSwitcher;

    @BindView(R.id.sign_in_with_google)
    View googleSignInButton;

    @Inject
    UserRepository userRepository;

    @Inject
    FirebaseAnalytics analytics;

    private GoogleApiClient googleApiClient;

    public static Intent newIntent(Context context) {
        return new Intent(context, SignInActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        ButterKnife.bind(this);

        toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());

        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(BuildConfig.GOOGLE_CLIENT_ID)
                .requestEmail()
                .build();

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, signInOptions)
                .addApi(Auth.CREDENTIALS_API)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        OptionalPendingResult<GoogleSignInResult> pendingResult = Auth.GoogleSignInApi.silentSignIn(googleApiClient);

        if (pendingResult.isDone()) {
            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
            // and the GoogleSignInResult will be available instantly.
            GoogleSignInResult result = pendingResult.get();
            handleSignInResult(result);
        } else {
            // If the user has not previously signed in on this device or the sign-in has expired,
            // this asynchronous branch will attempt to sign in the user silently. Cross-device
            // single sign-on will occur in this branch.
            googleSignInButton.setEnabled(false);
            pendingResult.setResultCallback(googleSignInResult -> {
                googleSignInButton.setEnabled(true);
                handleSignInResult(googleSignInResult);
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_GOOGLE_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, R.string.cant_connect_to_google_services, Toast.LENGTH_LONG).show();
    }

    @OnClick(R.id.sign_in_with_google)
    public void onGoogleSignInClick() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        startActivityForResult(signInIntent, REQUEST_GOOGLE_SIGN_IN);
    }

    @OnClick(R.id.sign_in_with_email)
    public void onEmailSignInClick() {
        Intent intent = new Intent(this, EmailSignInActivity.class);
        startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle());
    }

    private void handleSignInResult(GoogleSignInResult result) {
        if (result.isSuccess()) {
            GoogleSignInAccount account = result.getSignInAccount();
            new AuthWithGoogleTask(account.getIdToken()).execute();
        }
    }

    private void setLoading(boolean loading) {
        viewSwitcher.setDisplayedChild(loading ? 1 : 0);
    }

    private class AuthWithGoogleTask extends AsyncTask<Void, Void, Boolean> {
        private final String token;

        public AuthWithGoogleTask(String token) {
            this.token = token;
        }

        @Override
        protected void onPreExecute() {
            setLoading(true);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return userRepository.signIn(token);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result == null) {
                setLoading(false);
                Toast.makeText(SignInActivity.this, R.string.could_not_connect_to_server, Toast.LENGTH_SHORT).show();
                return;
            }

            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.SIGN_UP_METHOD, "google");
            analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle);

            setResult(RESULT_OK);
            supportFinishAfterTransition();
        }
    }
}