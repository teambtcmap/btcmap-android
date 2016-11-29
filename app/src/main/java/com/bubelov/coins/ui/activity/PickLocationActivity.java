package com.bubelov.coins.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.bubelov.coins.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author Igor Bubelov
 */

public class PickLocationActivity extends AbstractActivity implements OnMapReadyCallback {
    public static final String LOCATION_EXTRA = "location";

    private static final float DEFAULT_ZOOM = 13;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private GoogleMap map;

    private LatLng initialLocation;

    public static void startForResult(Activity activity, LatLng initialLocation, int requestCode) {
        Intent intent = new Intent(activity, PickLocationActivity.class);
        intent.putExtra(LOCATION_EXTRA, initialLocation);
        activity.startActivityForResult(intent, requestCode, ActivityOptionsCompat.makeSceneTransitionAnimation(activity).toBundle());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick_location);
        ButterKnife.bind(this);

        initialLocation = getIntent().getParcelableExtra(LOCATION_EXTRA);

        if (initialLocation == null) {
            toolbar.setTitle(R.string.set_location);
        } else {
            toolbar.setTitle(R.string.change_location);
        }

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                supportFinishAfterTransition();
            }
        });

        toolbar.inflateMenu(R.menu.menu_pick_location);

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_done) {
                    Intent data = new Intent();
                    data.putExtra(LOCATION_EXTRA, map.getCameraPosition().target);
                    setResult(RESULT_OK, data);
                    supportFinishAfterTransition();
                    return true;
                }

                return false;
            }
        });

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        if (initialLocation != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, DEFAULT_ZOOM));
        }
    }
}