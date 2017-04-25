package com.bubelov.coins.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bubelov.coins.DataStorage;
import com.bubelov.coins.PlacesCache;
import com.bubelov.coins.R;
import com.bubelov.coins.api.CoinsApi;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.Place;
import com.bubelov.coins.util.AuthController;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import retrofit2.Response;

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

    private Place place;

    private GoogleMap map;

    private LatLng pickedLocation;

    private AuthController authController;

    private DataStorage dataStorage;

    public static void startForResult(Activity activity, long placeId, CameraPosition mapCameraPosition, int requestCode) {
        Intent intent = new Intent(activity, EditPlaceActivity.class);
        intent.putExtra(ID_EXTRA, placeId);
        intent.putExtra(MAP_CAMERA_POSITION_EXTRA, mapCameraPosition);
        activity.startActivityForResult(intent, requestCode, ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        authController = Injector.INSTANCE.mainComponent().authController();
        dataStorage = Injector.INSTANCE.mainComponent().dataStorage();

        place = dataStorage.getPlace(getIntent().getLongExtra(ID_EXTRA, -1));

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

    private Map<String, Object> getRequestArgs() {
        Map<String, Object> args = new HashMap<>();

        if (pickedLocation == null && place != null) {
            pickedLocation = new LatLng(place.latitude(), place.longitude());
        }

        if ((place == null && name.length() > 0) || (place != null && !TextUtils.equals(name.getText(), place.name()))) {
            args.put("name", name.getText().toString());
        }

        if (place == null || place.latitude() != pickedLocation.latitude) {
            args.put("latitude", pickedLocation.latitude);
        }

        if (place == null || place.longitude() != pickedLocation.longitude) {
            args.put("longitude", pickedLocation.longitude);
        }

        if ((place == null && phone.length() > 0) || (place != null && !TextUtils.equals(phone.getText(), place.phone()))) {
            args.put("phone", phone.getText().toString());
        }

        if ((place == null && website.length() > 0) || (place != null && !TextUtils.equals(website.getText(), place.website()))) {
            args.put("website", website.getText().toString());
        }

        if ((place == null && description.length() > 0) || (place != null && !TextUtils.equals(description.getText(), place.description()))) {
            args.put("description", description.getText().toString());
        }

        if ((place == null && openingHours.length() > 0) || (place != null && !TextUtils.equals(openingHours.getText(), place.openingHours()))) {
            args.put("opening_hours", openingHours.getText().toString());
        }

        if (closedSwitch.isChecked()) {
            args.put("visible", false);
        } else {
            args.put("visible", true);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("place", args);
        return result;
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
        private Map<String, Object> requestArgs;

        @Override
        protected void onPreExecute() {
            requestArgs = getRequestArgs();
            showProgress();
        }

        @Override
        protected Boolean doInBackground(Void... args) {
            CoinsApi api = Injector.INSTANCE.mainComponent().api();

            try {
                Response<Place> response = api.addPlace(authController.getToken(), requestArgs).execute();

                if (response.isSuccessful()) {
                    Place place = response.body();
                    dataStorage.insertPlaces(Collections.singleton(place));
                    Currency bitcoin = dataStorage.getCurrency("BTC");
                    dataStorage.insertCurrencyForPlaces(place, Collections.singleton(bitcoin));
                    PlacesCache cache = Injector.INSTANCE.mainComponent().placesCache();
                    cache.invalidate();
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            hideProgress();

            if (success) {
                setResult(RESULT_OK);
                supportFinishAfterTransition();
            } else {
                Toast.makeText(EditPlaceActivity.this, "Couldn't add place", Toast.LENGTH_LONG).show();
            }
        }
    }

    private class UpdatePlaceTask extends AsyncTask<Void, Void, Boolean> {
        private Map<String, Object> requestArgs;

        @Override
        protected void onPreExecute() {
            requestArgs = getRequestArgs();
            showProgress();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            CoinsApi api = Injector.INSTANCE.mainComponent().api();

            try {
                Response<Place> response = api.updatePlace(place.id(), authController.getToken(), requestArgs).execute();

                if (response.isSuccessful()) {
                    Place place = response.body();
                    dataStorage.insertPlaces(Collections.singleton(place));
                    Currency bitcoin = dataStorage.getCurrency("BTC");
                    dataStorage.insertCurrencyForPlaces(place, Collections.singleton(bitcoin));
                    PlacesCache cache = Injector.INSTANCE.mainComponent().placesCache();
                    cache.invalidate();
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
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