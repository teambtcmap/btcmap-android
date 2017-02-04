package com.bubelov.coins.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.bubelov.coins.R;
import com.bubelov.coins.api.CoinsApi;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.model.SessionResponse;
import com.bubelov.coins.util.AuthUtils;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;

/**
 * @author Igor Bubelov
 */

public class SignInActivity extends AbstractActivity implements GoogleApiClient.OnConnectionFailedListener {
    private static final int REQUEST_GOOGLE_SIGN_IN = 10;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.google_sign_in)
    SignInButton googleSignInButton;

    private GoogleApiClient googleApiClient;

    public static void startForResult(Activity activity, int requestCode) {
        Intent intent = new Intent(activity, SignInActivity.class);
        activity.startActivityForResult(intent, requestCode, ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        ButterKnife.bind(this);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                supportFinishAfterTransition();
            }
        });

        googleSignInButton.setSize(SignInButton.SIZE_WIDE);

        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.server_client_id))
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
            pendingResult.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(GoogleSignInResult googleSignInResult) {
                    googleSignInButton.setEnabled(true);
                    handleSignInResult(googleSignInResult);
                }
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

    @OnClick(R.id.google_sign_in)
    public void onGoogleSignInClick() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        startActivityForResult(signInIntent, REQUEST_GOOGLE_SIGN_IN);
    }

    private void handleSignInResult(GoogleSignInResult result) {
        if (result.isSuccess()) {
            GoogleSignInAccount account = result.getSignInAccount();
            CoinsApi api = Injector.INSTANCE.getAppComponent().provideApi();

            api.getSession(account.getIdToken()).enqueue(new Callback<SessionResponse>() {
                @Override
                public void onResponse(Call<SessionResponse> call, final retrofit2.Response<SessionResponse> response) {
                    if (response.isSuccessful()) {
                        new Handler().post(new Runnable() {
                            @Override
                            public void run() {
                                AuthUtils.setToken(response.body().token);
                                setResult(RESULT_OK);
                                supportFinishAfterTransition();
                            }
                        });
                    } else {
                        SessionResponse body = Injector.INSTANCE.getAppComponent().provideGson().fromJson(response.errorBody().charStream(), SessionResponse.class);

                        StringBuilder errorMessageBuilder = new StringBuilder();

                        for (String error : body.errors) {
                            errorMessageBuilder.append(error).append("\n");
                            Crashlytics.log(error);
                        }

                        new AlertDialog.Builder(SignInActivity.this)
                                .setMessage(errorMessageBuilder.toString())
                                .setPositiveButton(android.R.string.ok, null)
                                .show();

                        Crashlytics.logException(new Exception("Google sign in failed"));
                    }
                }

                @Override
                public void onFailure(Call<SessionResponse> call, Throwable t) {
                    Crashlytics.logException(t);
                    Toast.makeText(SignInActivity.this, R.string.failed_to_sign_in, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}