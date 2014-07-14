package com.bubelov.coins.ui;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.bubelov.coins.Constants;
import com.bubelov.coins.manager.UserNotificationManager;
import com.bubelov.coins.service.MerchantsSyncService;
import com.bubelov.coins.util.OnCameraChangeMultiplexer;
import com.bubelov.coins.loader.PlacesLoader;
import com.bubelov.coins.R;
import com.bubelov.coins.model.Merchant;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

import java.util.Collection;

public class MapActivity extends Activity implements LoaderManager.LoaderCallbacks<Collection<Merchant>> {
    private static final String SHOW_MERCHANT_EXTRA = "show_merchant";
    private static final String MERCHANT_LOCATION_EXTRA = "merchant_location";
    private static final String MY_LOCATION_EXTRA = "my_location";

    private static final int MERCHANTS_LOADER = 0;

    private GoogleMap map;
    private ClusterManager<Merchant> placesManager;

    private SwipeRefreshLayout refreshLayout;

    public static Intent makeIntent(Context context, double latitude, double longitude) {
        Intent intent = new Intent(context, MapActivity.class);
        intent.putExtra(SHOW_MERCHANT_EXTRA, true);
        intent.putExtra(MERCHANT_LOCATION_EXTRA, new LatLng(latitude, longitude));
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getActionBar().setBackgroundDrawable(null);

        refreshLayout = (SwipeRefreshLayout)findViewById(R.id.container);
        refreshLayout.setColorScheme(R.color.silver, R.color.gold, R.color.silver, R.color.gold);
        refreshLayout.setEnabled(false);

        MapFragment mapFragment = (MapFragment)getFragmentManager().findFragmentById(R.id.map);
        map = mapFragment.getMap();
        map.setMyLocationEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(false);

        if (getIntent().getBooleanExtra(SHOW_MERCHANT_EXTRA, false)) {
            showMerchant();
        } else {
            findMyLocationIfNotFound();
        }

        initClustering();

        map.setOnCameraChangeListener(new OnCameraChangeMultiplexer(placesManager, new CameraChangeListener()));

        LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
        getLoaderManager().initLoader(MERCHANTS_LOADER, PlacesLoader.prepareArguments(bounds), this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_refresh:
                refreshData();
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Loader<Collection<Merchant>> onCreateLoader(int id, Bundle args) {
        return new PlacesLoader(this, args);
    }

    @Override
    public void onLoadFinished(Loader<Collection<Merchant>> loader, final Collection<Merchant> data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                placesManager.clearItems();
                placesManager.addItems(data);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        placesManager.cluster();
                    }
                });
            }
        }).start();

        placesManager.cluster();
    }

    @Override
    public void onLoaderReset(Loader<Collection<Merchant>> loader) {
        // Nothing to do here
    }

    private void showMerchant() {
        LatLng location = getIntent().getParcelableExtra(MERCHANT_LOCATION_EXTRA);

        if (location != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 12));
            getIntent().removeExtra(MERCHANT_LOCATION_EXTRA);
        }
    }

    private void findMyLocationIfNotFound() {
        LatLng myLocation = getIntent().getParcelableExtra(MY_LOCATION_EXTRA);

        if (myLocation == null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(Constants.SAN_FRANCISCO_LATITUDE, Constants.SAN_FRANCISCO_LONGITUDE) , 10));

            map.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
                @Override
                public void onMyLocationChange(Location location) {
                    map.setOnMyLocationChangeListener(null);
                    LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    map.moveCamera(CameraUpdateFactory.newLatLng(myLocation));
                    getIntent().putExtra(MY_LOCATION_EXTRA, myLocation);

                    UserNotificationManager notificationManager = new UserNotificationManager(getApplicationContext());

                    if (notificationManager.getNotificationAreaCenter() == null) {
                        notificationManager.setNotificationAreaCenter(myLocation);
                    }
                }
            });
        }
    }

    private void initClustering() {
        placesManager = new ClusterManager<>(this, map);
        placesManager.setRenderer(new PlacesRenderer(this, map, placesManager));

        map.setOnCameraChangeListener(placesManager);
        map.setOnMarkerClickListener(placesManager);
    }

    private void refreshData() {
        Handler refreshCompletedHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                refreshLayout.setRefreshing(false);
                LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
                getLoaderManager().restartLoader(MERCHANTS_LOADER, PlacesLoader.prepareArguments(bounds), MapActivity.this).forceLoad();
            }
        };

        refreshLayout.setRefreshing(true);
        startService(MerchantsSyncService.makeIntent(this, refreshCompletedHandler));
    }

    private class PlacesRenderer extends DefaultClusterRenderer<Merchant> {
        public PlacesRenderer(Context context, GoogleMap map, ClusterManager<Merchant> clusterManager) {
            super(context, map, clusterManager);
        }

        @Override
        protected void onBeforeClusterItemRendered(Merchant item, MarkerOptions markerOptions) {
            super.onBeforeClusterItemRendered(item, markerOptions);

            if (!TextUtils.isEmpty(item.getName())) {
                markerOptions.title(item.getName());
            }

            if (!TextUtils.isEmpty(item.getDescription())) {
                markerOptions.snippet(item.getDescription());
            }
        }
    }

    private class CameraChangeListener implements GoogleMap.OnCameraChangeListener {
        @Override
        public void onCameraChange(CameraPosition cameraPosition) {
            LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
            getLoaderManager().restartLoader(MERCHANTS_LOADER, PlacesLoader.prepareArguments(bounds), MapActivity.this).forceLoad();
        }
    }
}
