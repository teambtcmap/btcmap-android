package com.bubelov.coins.ui.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.bubelov.coins.R;
import com.bubelov.coins.api.CoinsApi;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.model.Merchant;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * @author Igor Bubelov
 */

public class EditPlaceActivity extends AbstractActivity {
    private static final String ID_EXTRA = "id";

    @BindView(R.id.closed_switch)
    Switch closedSwitch;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.name)
    TextView name;

    @BindView(R.id.phone)
    TextView phone;

    @BindView(R.id.website)
    TextView website;

    @BindView(R.id.description)
    TextView description;

    @BindView(R.id.opening_hours)
    TextView openingHours;

    private Merchant place;

    public static void start(Activity activity, long placeId) {
        Intent intent = new Intent(activity, EditPlaceActivity.class);
        intent.putExtra(ID_EXTRA, placeId);
        activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle());
        Answers.getInstance().logCustom(new CustomEvent("Edit place"));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_place);
        ButterKnife.bind(this);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                supportFinishAfterTransition();
            }
        });

        toolbar.inflateMenu(R.menu.menu_edit_place);

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_send) {
                    StringBuilder suggestionBuilder = new StringBuilder();
                    suggestionBuilder.append("ID: ").append(place.getId()).append("\n");

                    if (closedSwitch.isChecked()) {
                        suggestionBuilder.append("Closed");
                    } else {
                        suggestionBuilder.append("Name: ").append(name.getText()).append("\n");
                        suggestionBuilder.append("Phone: ").append(phone.getText()).append("\n");
                        suggestionBuilder.append("Website: ").append(website.getText()).append("\n");
                        suggestionBuilder.append("Description: ").append(description.getText()).append("\n");
                        suggestionBuilder.append("Opening hours: ").append(openingHours.getText());
                    }

                    CoinsApi api = Injector.INSTANCE.getAppComponent().provideApi();

                    api.addPlaceSuggestion(suggestionBuilder.toString()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                showAlert(R.string.thanks_for_suggestion, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int i) {
                                        dialog.dismiss();
                                        supportFinishAfterTransition();
                                    }
                                });
                            } else {
                                showAlert(R.string.could_not_send_suggestion, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int i) {
                                        dialog.dismiss();
                                    }
                                });
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, final Throwable t) {
                            showAlert(R.string.could_not_send_suggestion, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int i) {
                                    dialog.dismiss();
                                    Crashlytics.logException(t);
                                }
                            });
                        }
                    });

                    return true;
                }

                return false;
            }
        });

        place = Merchant.find(getIntent().getLongExtra(ID_EXTRA, -1));

        name.setText(place.getName());
        phone.setText(place.getPhone());
        website.setText(place.getWebsite());
        description.setText(place.getDescription());
        openingHours.setText(place.getOpeningHours());
    }

    private void showAlert(@StringRes int messageId, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(messageId)
                .setPositiveButton(android.R.string.ok, okListener)
                .show();
    }

    @OnCheckedChanged(R.id.closed_switch)
    public void onClosedSwitchChanged() {
        boolean closed = closedSwitch.isChecked();

        name.setEnabled(!closed);
        phone.setEnabled(!closed);
        website.setEnabled(!closed);
        description.setEnabled(!closed);
        openingHours.setEnabled(!closed);
    }
}