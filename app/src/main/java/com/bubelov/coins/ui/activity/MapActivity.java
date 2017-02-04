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
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.bubelov.coins.Constants;
import com.bubelov.coins.PlacesCache;
import com.bubelov.coins.R;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.event.DatabaseSyncedEvent;
import com.bubelov.coins.model.Amenity;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.Place;
import com.bubelov.coins.model.PlaceNotification;
import com.bubelov.coins.model.NotificationArea;
import com.bubelov.coins.provider.NotificationAreaProvider;
import com.bubelov.coins.ui.widget.DrawerMenu;
import com.bubelov.coins.ui.widget.PlaceDetailsView;
import com.bubelov.coins.util.AuthUtils;
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
import com.squareup.otto.Subscribe;

import java.util.Collection;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * @author Igor Bubelov
 */

public class MapActivity extends AbstractActivity implements OnMapReadyCallback, DrawerMenu.OnItemClickListener {
    private static final String PLACE_ID_EXTRA = "place_id";
    private static final String NOTIFICATION_AREA_EXTRA = "notification_area";
    private static final String CLEAR_PLACE_NOTIFICATIONS_EXTRA = "clear_place_notifications";

    private static final int REQUEST_CHECK_LOCATION_SETTINGS = 10;
    private static final int REQUEST_ACCESS_LOCATION = 20;
    private static final int REQUEST_FIND_PLACE = 30;
    private static final int REQUEST_SIGN_IN_TO_ADD_PLACE = 40;
    private static final int REQUEST_SIGN_IN_TO_EDIT_PLACE = 50;

    private static final float MAP_DEFAULT_ZOOM = 13;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.drawer_layout)
    DrawerLayout drawerLayout;

    @BindView(R.id.drawer_menu)
    DrawerMenu drawerMenu;

    @BindView(R.id.fab)
    FloatingActionButton actionButton;

    @BindView(R.id.place_details)
    PlaceDetailsView placeDetails;

    private ActionBarDrawerToggle drawerToggle;

    private GoogleMap map;

    private ClusterManager<Place> placesManager;

    private Amenity selectedAmenity;

    private Place selectedPlace;

    private GoogleApiClient googleApiClient;

    private PlacesCache placesCache;

    private boolean firstLaunch;

    private BottomSheetBehavior bottomSheetBehavior;

    public static Intent newShowPlaceIntent(Context context, long placeId, boolean clearNotifications) {
        Intent intent = new Intent(context, MapActivity.class);
        intent.putExtra(PLACE_ID_EXTRA, placeId);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(CLEAR_PLACE_NOTIFICATIONS_EXTRA, clearNotifications);
        return intent;
    }

    public static Intent newShowNotificationAreaIntent(Context context, NotificationArea notificationArea, boolean clearNotifications) {
        Intent intent = new Intent(context, MapActivity.class);
        intent.putExtra(NOTIFICATION_AREA_EXTRA, notificationArea);
        intent.putExtra(CLEAR_PLACE_NOTIFICATIONS_EXTRA, clearNotifications);
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

        bottomSheetBehavior = BottomSheetBehavior.from(placeDetails);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                placeDetails.setFullScreen(newState == BottomSheetBehavior.STATE_EXPANDED);
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                actionButton.setVisibility(slideOffset > 0.5f ? View.GONE : View.VISIBLE);
            }
        });

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                bottomSheetBehavior.setPeekHeight(placeDetails.getHeaderHeight());
            }
        }, 1000);

        placeDetails.setListener(new PlaceDetailsView.Listener() {
            @Override
            public void onEditPlaceClick(Place place) {
                if (AuthUtils.isAuthorized()) {
                    EditPlaceActivity.start(MapActivity.this, place.getId(), null);
                } else {
                    SignInActivity.startForResult(MapActivity.this, REQUEST_SIGN_IN_TO_EDIT_PLACE);
                }
            }

            @Override
            public void onDismissed() {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
    }

    @OnClick(R.id.place_details)
    public void onPlaceDetailsClick() {
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

        if (requestCode == REQUEST_FIND_PLACE && resultCode == RESULT_OK) {
            drawerMenu.setAmenity(null);
            selectPlace(data.getLongExtra(FindPlaceActivity.PLACE_ID_EXTRA, -1));
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedPlace.getPosition(), MAP_DEFAULT_ZOOM));
        }

        if (requestCode == REQUEST_SIGN_IN_TO_ADD_PLACE && resultCode == RESULT_OK) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    EditPlaceActivity.start(MapActivity.this, 0, map.getCameraPosition());
                }
            });
        }

        if (requestCode == REQUEST_SIGN_IN_TO_EDIT_PLACE && resultCode == RESULT_OK) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    EditPlaceActivity.start(MapActivity.this, selectedPlace.getId(), null);
                }
            });
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem settingsMenuItem = menu.findItem(R.id.action_settings);
        SpannableString string = new SpannableString(settingsMenuItem.getTitle());
        string.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.black)), 0, string.length(), 0);
        settingsMenuItem.setTitle(string);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        int id = item.getItemId();

        switch (id) {
            case R.id.action_add:
                if (AuthUtils.isAuthorized()) {
                    EditPlaceActivity.start(this, 0, map.getCameraPosition());
                } else {
                    SignInActivity.startForResult(this, REQUEST_SIGN_IN_TO_ADD_PLACE);
                }

                return true;
            case R.id.action_search:
                Location lastLocation = null;

                if (googleApiClient != null && googleApiClient.isConnected() && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                }

                FindPlaceActivity.startForResult(this, lastLocation, REQUEST_FIND_PLACE);
                return true;
            case R.id.action_settings:
                SettingsActivity.start(this);
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

        placesCache = Injector.INSTANCE.getAppComponent().getPlacesCache();
        drawerMenu.setAmenity(selectedAmenity);

        handleIntent(getIntent());
    }

    @Override
    public void onAmenitySelected(Amenity amenity, String title) {
        drawerLayout.closeDrawer(GravityCompat.START);
        getSupportActionBar().setTitle(title);
        this.selectedAmenity = amenity;
        reloadPlaces();
    }

    @Subscribe
    public void onDatabaseSynced(DatabaseSyncedEvent e) {
        reloadPlaces();
    }

    private void handleIntent(final Intent intent) {
        if (intent.getBooleanExtra(CLEAR_PLACE_NOTIFICATIONS_EXTRA, false)) {
            PlaceNotification.deleteAll();
        }

        if (intent.hasExtra(PLACE_ID_EXTRA)) {
            selectPlace(intent.getLongExtra(PLACE_ID_EXTRA, -1));

            if (selectedPlace != null) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedPlace.getPosition(), MAP_DEFAULT_ZOOM));
            }
        }

        if (intent.hasExtra(NOTIFICATION_AREA_EXTRA)) {
            NotificationArea notificationArea = (NotificationArea) intent.getSerializableExtra(NOTIFICATION_AREA_EXTRA);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(notificationArea.getCenter(), MAP_DEFAULT_ZOOM));
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

    private void onPlacesLoaded(Collection<Place> places) {
        placesManager.clearItems();
        placesManager.addItems(places);
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
                selectedPlace = null;
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });
    }

    private void reloadPlaces() {
        if (map == null) {
            return;
        }

        final LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;

        if (placesCache.isInitialized()) {
            onPlacesLoaded(placesCache.getPlaces(bounds, selectedAmenity));
        } else {
            placesCache.getListeners().add(new PlacesCache.PlacesCacheListener() {
                @Override
                public void onPlacesCacheInitialized() {
                    onPlacesLoaded(placesCache.getPlaces(bounds, selectedAmenity));
                    placesCache.getListeners().remove(this);
                }
            });
        }
    }

    private void selectPlace(long placeId) {
        selectedPlace = Place.find(placeId);
        selectedPlace.setCurrencies(Currency.findByPlace(selectedPlace));
        placeDetails.setPlace(selectedPlace);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        Answers.getInstance().logContentView(new ContentViewEvent()
                .putContentName(selectedPlace.getName())
                .putContentType("place")
                .putContentId(String.valueOf(selectedPlace.getId())));
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

    private class PlacesRenderer extends StaticClusterRenderer<Place> {
        private MapMarkersCache cache;

        public PlacesRenderer(Context context, GoogleMap map, ClusterManager<Place> clusterManager) {
            super(context, map, clusterManager);
            cache = Injector.INSTANCE.getAppComponent().getMarkersCache();
        }

        @Override
        protected void onBeforeClusterItemRendered(Place item, MarkerOptions markerOptions) {
            super.onBeforeClusterItemRendered(item, markerOptions);
            markerOptions.icon(cache.getMarker(item.getAmenity())).anchor(Constants.MAP_MARKER_ANCHOR_U, Constants.MAP_MARKER_ANCHOR_V);
        }
    }

    private class CameraChangeListener implements GoogleMap.OnCameraChangeListener {
        @Override
        public void onCameraChange(CameraPosition cameraPosition) {
            reloadPlaces();
        }
    }

    private class ClusterItemClickListener implements ClusterManager.OnClusterItemClickListener<Place> {
        @Override
        public boolean onClusterItemClick(Place place) {
            selectPlace(place.getId());
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