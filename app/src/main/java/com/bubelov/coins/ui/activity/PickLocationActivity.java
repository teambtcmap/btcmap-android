package com.bubelov.coins.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.Toolbar;

import com.bubelov.coins.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author Igor Bubelov
 */

public class PickLocationActivity extends AbstractActivity implements OnMapReadyCallback {
    public static final String LOCATION_EXTRA = "location";

    public static final String MAP_CAMERA_POSITION_EXTRA = "map_camera_position";

    private static final float DEFAULT_ZOOM = 13;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private GoogleMap map;

    private LatLng initialLocation;

    public static void startForResult(Activity activity, LatLng initialLocation, CameraPosition mapCameraPosition, int requestCode) {
        Intent intent = new Intent(activity, PickLocationActivity.class);
        intent.putExtra(LOCATION_EXTRA, initialLocation);
        intent.putExtra(MAP_CAMERA_POSITION_EXTRA, mapCameraPosition);
        activity.startActivityForResult(intent, requestCode, ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick_location);
        ButterKnife.bind(this);

        initialLocation = getIntent().getParcelableExtra(LOCATION_EXTRA);

        toolbar.setNavigationOnClickListener(view -> supportFinishAfterTransition());
        toolbar.inflateMenu(R.menu.pick_location);

        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_done) {
                Intent data = new Intent();
                data.putExtra(LOCATION_EXTRA, map.getCameraPosition().target);
                setResult(RESULT_OK, data);
                supportFinishAfterTransition();
                return true;
            }

            return false;
        });

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        if (initialLocation != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, DEFAULT_ZOOM));
        } else {
            CameraPosition cameraPosition = getIntent().getParcelableExtra(MAP_CAMERA_POSITION_EXTRA);

            if (cameraPosition != null) {
                map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        }
    }
}