package com.bubelov.coins.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.bubelov.coins.Constants;
import com.bubelov.coins.MerchantsCache;
import com.bubelov.coins.R;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.model.Amenity;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.Merchant;
import com.bubelov.coins.model.MerchantNotification;
import com.bubelov.coins.model.NotificationArea;
import com.bubelov.coins.provider.NotificationAreaProvider;
import com.bubelov.coins.ui.widget.DrawerMenu;
import com.bubelov.coins.ui.widget.MerchantDetailsView;
import com.bubelov.coins.util.MapMarkersCache;
import com.bubelov.coins.util.OnCameraChangeMultiplexer;
import com.bubelov.coins.util.StaticClusterRenderer;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;

import java.util.Collection;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * @author Igor Bubelov
 */

public class MapActivity extends AbstractActivity implements OnMapReadyCallback, DrawerMenu.OnItemClickListener {
    private static final String MERCHANT_ID_EXTRA = "merchant_id";
    private static final String NOTIFICATION_AREA_EXTRA = "notification_area";
    private static final String CLEAR_MERCHANT_NOTIFICATIONS_EXTRA = "clear_merchant_notifications";

    private static final int REQUEST_CHECK_LOCATION_SETTINGS = 10;
    private static final int REQUEST_ACCESS_LOCATION = 20;
    private static final int REQUEST_FIND_MERCHANT = 30;

    private static final float MAP_DEFAULT_ZOOM = 13;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.drawer_layout)
    DrawerLayout drawerLayout;

    @BindView(R.id.drawer_menu)
    DrawerMenu drawerMenu;

    @BindView(R.id.fab)
    FloatingActionButton actionButton;

    @BindView(R.id.merchant_details)
    MerchantDetailsView merchantDetails;

    private ActionBarDrawerToggle drawerToggle;

    private GoogleMap map;

    private ClusterManager<Merchant> placesManager;

    private Amenity selectedAmenity;

    private Merchant selectedMerchant;

    private GoogleApiClient googleApiClient;

    private MerchantsCache merchantsCache;

    private boolean firstLaunch;

    private BottomSheetBehavior bottomSheetBehavior;

    public static Intent newShowMerchantIntent(Context context, long merchantId, boolean clearNotifications) {
        Intent intent = new Intent(context, MapActivity.class);
        intent.putExtra(MERCHANT_ID_EXTRA, merchantId);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(CLEAR_MERCHANT_NOTIFICATIONS_EXTRA, clearNotifications);
        return intent;
    }

    public static Intent newShowNotificationAreaIntent(Context context, NotificationArea notificationArea, boolean clearNotifications) {
        Intent intent = new Intent(context, MapActivity.class);
        intent.putExtra(NOTIFICATION_AREA_EXTRA, notificationArea);
        intent.putExtra(CLEAR_MERCHANT_NOTIFICATIONS_EXTRA, clearNotifications);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        drawerToggle = new DrawerToggle(this, drawerLayout, android.R.string.ok, android.R.string.ok);
        drawerLayout.setDrawerListener(drawerToggle);

        drawerMenu.setItemSelectedListener(this);

        firstLaunch = savedInstanceState == null;

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        bottomSheetBehavior = BottomSheetBehavior.from(merchantDetails);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                merchantDetails.setFullScreen(newState == BottomSheetBehavior.STATE_EXPANDED);
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                actionButton.setVisibility(slideOffset > 0.5f ? View.GONE : View.VISIBLE);
            }
        });

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                bottomSheetBehavior.setPeekHeight(merchantDetails.getHeaderHeight());
            }
        }, 1000);

        merchantDetails.setListener(new MerchantDetailsView.Listener() {
            @Override
            public void onDismissed() {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
    }

    @OnClick(R.id.merchant_details)
    public void onMerchantDetailsClick() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CHECK_LOCATION_SETTINGS && resultCode == RESULT_OK) {
            moveToLastLocation();
        }

        if (requestCode == REQUEST_FIND_MERCHANT && resultCode == RESULT_OK) {
            drawerMenu.setAmenity(null);
            selectMerchant(data.getIntExtra(MerchantsSearchActivity.MERCHANT_ID_EXTRA, -1));
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedMerchant.getPosition(), MAP_DEFAULT_ZOOM));
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ACCESS_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initLocation();
                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_map_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                SettingsActivity.start(this);
                return true;
            case R.id.action_search:
                MerchantsSearchActivity.startForResult(this, map.getMyLocation(), REQUEST_FIND_MERCHANT);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setCompassEnabled(false);
        map.getUiSettings().setMapToolbarEnabled(false);

        initClustering();

        map.setOnCameraChangeListener(new OnCameraChangeMultiplexer(placesManager, new CameraChangeListener()));

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            initLocation();
        } else {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                moveToDefaultLocation();

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_ACCESS_LOCATION);
            } else {
                moveToDefaultLocation();
            }
        }

        merchantsCache = Injector.INSTANCE.getAppComponent().getMerchantsCache();
        drawerMenu.setAmenity(selectedAmenity);

        handleIntent(getIntent());
    }

    @Override
    public void onAmenitySelected(Amenity amenity, String title) {
        drawerLayout.closeDrawer(GravityCompat.START);
        getSupportActionBar().setTitle(title);
        this.selectedAmenity = amenity;
        reloadMerchants();
    }

    private void handleIntent(final Intent intent) {
        if (intent.getBooleanExtra(CLEAR_MERCHANT_NOTIFICATIONS_EXTRA, false)) {
            MerchantNotification.deleteAll();
        }

        if (intent.hasExtra(MERCHANT_ID_EXTRA)) {
            selectMerchant(intent.getLongExtra(MERCHANT_ID_EXTRA, -1));
        }

        if (intent.hasExtra(NOTIFICATION_AREA_EXTRA)) {
            NotificationArea notificationArea = (NotificationArea) intent.getSerializableExtra(NOTIFICATION_AREA_EXTRA);
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(notificationArea.getCenter(), MAP_DEFAULT_ZOOM));
        }
    }

    private void initLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        map.setMyLocationEnabled(true);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new LocationApiConnectionCallbacks())
                .build();

        googleApiClient.connect();
    }

    private void onMerchantsLoaded(Collection<Merchant> merchants) {
        placesManager.clearItems();
        placesManager.addItems(merchants);
        placesManager.cluster();
    }

    private void moveToLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        if (lastLocation != null) {
            onUserLocationReceived(lastLocation);
        } else {
            moveToDefaultLocation();
        }
    }

    private void moveToDefaultLocation() {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(Constants.DEFAULT_LOCATION, MAP_DEFAULT_ZOOM));
    }

    private void onUserLocationReceived(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, MAP_DEFAULT_ZOOM));

        NotificationAreaProvider notificationAreaProvider = new NotificationAreaProvider(this);

        if (notificationAreaProvider.get() == null) {
            notificationAreaProvider.save(new NotificationArea(latLng));
        }
    }

    private void initClustering() {
        placesManager = new ClusterManager<>(this, map);
        PlacesRenderer renderer = new PlacesRenderer(this, map, placesManager);
        placesManager.setRenderer(renderer);
        renderer.setOnClusterItemClickListener(new ClusterItemClickListener());

        map.setOnCameraChangeListener(placesManager);
        map.setOnMarkerClickListener(placesManager);
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                selectedMerchant = null;
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });
    }

    private void reloadMerchants() {
        if (map == null) {
            return;
        }

        final LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;

        if (merchantsCache.isInitialized()) {
            onMerchantsLoaded(merchantsCache.getMerchants(bounds, selectedAmenity));
        } else {
            merchantsCache.getListeners().add(new MerchantsCache.MerchantsCacheListener() {
                @Override
                public void onMerchantsCacheInitialized() {
                    onMerchantsLoaded(merchantsCache.getMerchants(bounds, selectedAmenity));
                    merchantsCache.getListeners().remove(this);
                }
            });
        }
    }

    private void selectMerchant(long merchantId) {
        selectedMerchant = Merchant.find(merchantId);
        selectedMerchant.setCurrencies(Currency.findByMerchant(selectedMerchant));
        merchantDetails.setMerchant(selectedMerchant);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        Answers.getInstance().logContentView(new ContentViewEvent()
                .putContentName(selectedMerchant.getName())
                .putContentType("Merchant")
                .putContentId(String.valueOf(selectedMerchant.getId())));
    }

    public void onActionButtonClicked(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            moveToLastLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_ACCESS_LOCATION);
        }
    }

    private class DrawerToggle extends ActionBarDrawerToggle {
        public DrawerToggle(Activity activity, DrawerLayout drawerLayout, int openDrawerContentDescRes, int closeDrawerContentDescRes) {
            super(activity, drawerLayout, openDrawerContentDescRes, closeDrawerContentDescRes);
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            super.onDrawerOpened(drawerView);
            invalidateOptionsMenu();
        }

        @Override
        public void onDrawerClosed(View drawerView) {
            super.onDrawerClosed(drawerView);
            invalidateOptionsMenu();
        }
    }

    private class PlacesRenderer extends StaticClusterRenderer<Merchant> {
        private MapMarkersCache cache;

        public PlacesRenderer(Context context, GoogleMap map, ClusterManager<Merchant> clusterManager) {
            super(context, map, clusterManager);
            cache = Injector.INSTANCE.getAppComponent().getMarkersCache();
        }

        @Override
        protected void onBeforeClusterItemRendered(Merchant item, MarkerOptions markerOptions) {
            super.onBeforeClusterItemRendered(item, markerOptions);
            markerOptions.icon(cache.getMarker(item.getAmenity())).anchor(Constants.MAP_MARKER_ANCHOR_U, Constants.MAP_MARKER_ANCHOR_V);
        }
    }

    private class CameraChangeListener implements GoogleMap.OnCameraChangeListener {
        @Override
        public void onCameraChange(CameraPosition cameraPosition) {
            reloadMerchants();
        }
    }

    private class ClusterItemClickListener implements ClusterManager.OnClusterItemClickListener<Merchant> {
        @Override
        public boolean onClusterItemClick(Merchant merchant) {
            selectMerchant(merchant.getId());
            return false;
        }
    }

    private class LocationApiConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle bundle) {
            if (firstLaunch && !getIntent().hasExtra(NOTIFICATION_AREA_EXTRA)) {
                moveToLastLocation();
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            // Nothing to do here
        }
    }
}