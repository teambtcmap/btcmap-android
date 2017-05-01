package com.bubelov.coins.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bubelov.coins.R;
import com.bubelov.coins.domain.User;
import com.bubelov.coins.data.repository.user.UserRepository;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.GoogleApiClient;
import com.squareup.picasso.Picasso;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author Igor Bubelov
 */

public class ProfileActivity extends AbstractActivity implements Toolbar.OnMenuItemClickListener {
    public static final int RESULT_SIGN_OUT = 10;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.avatar)
    ImageView avatar;

    @BindView(R.id.user_name)
    TextView userName;

    @Inject
    UserRepository userRepository;

    private GoogleApiClient googleApiClient;

    public static Intent newIntent(Context context) {
        return new Intent(context, ProfileActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dependencies().inject(this);
        setContentView(R.layout.activity_profile);
        ButterKnife.bind(this);

        toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());
        toolbar.inflateMenu(R.menu.profile);
        toolbar.setOnMenuItemClickListener(this);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .enableAutoManage(this, connectionResult -> Toast.makeText(ProfileActivity.this, connectionResult.getErrorMessage(), Toast.LENGTH_SHORT).show())
                .build();

        User user = userRepository.getUser();

        if (!TextUtils.isEmpty(user.avatarUrl())) {
            Picasso.with(this).load(user.avatarUrl()).into(avatar);
        } else {
            avatar.setImageResource(R.drawable.ic_no_avatar);
        }

        if (!TextUtils.isEmpty(user.firstName())) {
            userName.setText(String.format("%s %s", user.firstName(), user.lastName()));
        } else {
            userName.setText(user.email());
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_sign_out) {
            signOut();
            return true;
        } else {
            return false;
        }
    }

    private void signOut() {
        if ("google".equalsIgnoreCase(userRepository.getUserAuthMethod())) {
            googleSignOut();
        } else {
            onSignOut();
        }
    }

    private void googleSignOut() {
        if (!googleApiClient.isConnected()) {
            showAlert("Couldn't connect to Google services.");
            return;
        }

        Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback(status -> {
            onSignOut();
        });
    }

    private void onSignOut() {
        userRepository.setUser(null);
        userRepository.setUserAuthToken(null);
        userRepository.setUserAuthMethod(null);
        setResult(RESULT_SIGN_OUT);
        supportFinishAfterTransition();
    }
}