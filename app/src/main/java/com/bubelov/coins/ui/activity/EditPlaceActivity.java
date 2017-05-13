package com.bubelov.coins.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bubelov.coins.R;
import com.bubelov.coins.repository.place.PlacesRepository;
import com.bubelov.coins.model.Place;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

/**
 * @author Igor Bubelov
 */

public class EditPlaceActivity extends AbstractActivity implements OnMapReadyCallback {
    private static final String ID_EXTRA = "id";

    private static final String MAP_CAMERA_POSITION_EXTRA = "map_camera_position";

    private static final int REQUEST_PICK_LOCATION = 10;

    private static final float DEFAULT_ZOOM = 13;

    @BindView(R.id.closed_switch)
    Switch closedSwitch;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.name)
    TextView name;

    @BindView(R.id.change_location)
    Button changeLocation;

    @BindView(R.id.phone)
    TextView phone;

    @BindView(R.id.website)
    TextView website;

    @BindView(R.id.description)
    TextView description;

    @BindView(R.id.opening_hours)
    TextView openingHours;

    @Inject
    PlacesRepository placesRepository;

    private Place place;

    private GoogleMap map;

    private LatLng pickedLocation;

    public static void startForResult(Activity activity, long placeId, CameraPosition mapCameraPosition, int requestCode) {
        Intent intent = new Intent(activity, EditPlaceActivity.class);
        intent.putExtra(ID_EXTRA, placeId);
        intent.putExtra(MAP_CAMERA_POSITION_EXTRA, mapCameraPosition);
        activity.startActivityForResult(intent, requestCode, ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dependencies().inject(this);
        setContentView(R.layout.activity_edit_place);
        ButterKnife.bind(this);

        toolbar.setNavigationOnClickListener(view -> supportFinishAfterTransition());
        toolbar.inflateMenu(R.menu.edit_place);

        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_send) {
                if (place == null) {
                    if (name.length() == 0) {
                        showAlert(R.string.name_is_not_specified);
                        return true;
                    }

                    if (pickedLocation == null) {
                        showAlert(R.string.location_is_not_specified);
                        return true;
                    }
                }

                if (place == null) {
                    new AddPlaceTask().execute();
                    return true;
                } else {
                    new UpdatePlaceTask().execute();
                    return true;
                }
            }

            return false;
        });

        place = placesRepository.getPlace(getIntent().getLongExtra(ID_EXTRA, -1));

        if (place == null) {
            toolbar.setTitle(R.string.action_add_place);
            closedSwitch.setVisibility(View.GONE);
            changeLocation.setText(R.string.set_location);
        } else {
            name.setText(place.name());
            changeLocation.setText(R.string.change_location);
            phone.setText(place.phone());
            website.setText(place.website());
            description.setText(place.description());
            openingHours.setText(place.openingHours());
        }

        SupportMapFragment mapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PICK_LOCATION && resultCode == RESULT_OK) {
            pickedLocation = data.getParcelableExtra(PickLocationActivity.LOCATION_EXTRA);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(pickedLocation, DEFAULT_ZOOM));
            changeLocation.setText(R.string.change_location);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        map.getUiSettings().setAllGesturesEnabled(false);

        if (place != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(place.getPosition(), DEFAULT_ZOOM));
        }
    }

    private Place getEditedPlace() {
        return Place.builder()
                .id(place == null ? 0 : place.id())
                .name(name.getText().toString())
                .description(description.getText().toString())
                .latitude(pickedLocation.latitude)
                .longitude(pickedLocation.longitude)
                .categoryId(place == null ? 0 : place.categoryId())
                .phone(phone.getText().toString())
                .website(website.getText().toString())
                .openingHours(openingHours.getText().toString())
                .address(place == null ? "" : place.address())
                .visible(!closedSwitch.isChecked())
                .build();
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

    @OnClick(R.id.change_location)
    public void onChangeLocationClick() {
        PickLocationActivity.startForResult(this, place == null ? null : place.getPosition(), getIntent().getParcelableExtra(MAP_CAMERA_POSITION_EXTRA), REQUEST_PICK_LOCATION);
    }

    private class AddPlaceTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            showProgress();
        }

        @Override
        protected Boolean doInBackground(Void... args) {
            return placesRepository.add(getEditedPlace());
        }

        @Override
        protected void onPostExecute(Boolean success) {
            hideProgress();

            if (success) {
                setResult(RESULT_OK);
                supportFinishAfterTransition();
            } else {
                Toast.makeText(EditPlaceActivity.this, R.string.could_not_add_place, Toast.LENGTH_LONG).show();
            }
        }
    }

    private class UpdatePlaceTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            showProgress();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            return placesRepository.update(getEditedPlace());
        }

        @Override
        protected void onPostExecute(Boolean success) {
            hideProgress();

            if (success) {
                setResult(RESULT_OK);
                supportFinishAfterTransition();
            } else {
                Toast.makeText(EditPlaceActivity.this, R.string.could_not_update_place, Toast.LENGTH_LONG).show();
            }
        }
    }
}