package com.bubelov.coins.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bubelov.coins.R;
import com.bubelov.coins.model.Merchant;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author Igor Bubelov
 */

public class EditPlaceActivity extends AbstractActivity {
    private static final String ID_EXTRA = "id";

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
                    String suggestion = "ID: " + place.getId() + "\n" +
                            "Name: " + name.getText() + "\n" +
                            "Phone: " + phone.getText() + "\n" +
                            "Website: " + website.getText() + "\n" +
                            "Description: " + description.getText() + "\n" +
                            "Opening hours: " + openingHours.getText();

                    Toast.makeText(EditPlaceActivity.this, suggestion, Toast.LENGTH_SHORT).show();
                    supportFinishAfterTransition();
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
}