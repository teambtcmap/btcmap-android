package com.bubelov.coins.ui.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bubelov.coins.Constants;
import com.bubelov.coins.PlacesCache;
import com.bubelov.coins.R;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.model.PlaceCategory;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.Place;
import com.bubelov.coins.model.PlaceNotification;
import com.bubelov.coins.model.NotificationArea;
import com.bubelov.coins.provider.NotificationAreaProvider;
import com.bubelov.coins.ui.adapter.PlaceCategoriesAdapter;
import com.bubelov.coins.ui.dialog.MapPopupMenu;
import com.bubelov.coins.ui.dialog.PlaceCategoryDialog;
import com.bubelov.coins.ui.widget.PlaceDetailsView;
import com.bubelov.coins.util.AuthController;
import com.bubelov.coins.util.MapMarkersCache;
import com.bubelov.coins.util.OnCameraChangeMultiplexer;
import com.bubelov.coins.util.StaticClusterRenderer;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.maps.android.clustering.ClusterManager;

import java.util.Collection;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * @author Igor Bubelov
 */

public class MapActivity extends AbstractActivity implements OnMapReadyCallback, Toolbar.OnMenuItemClickListener, PlaceCategoriesAdapter.Listener, MapPopupMenu.Listener, PlacesCache.PlacesCacheListener {
    private static final String PLACE_ID_EXTRA = "place_id";
    private static final String NOTIFICATION_AREA_EXTRA = "notification_area";
    private static final String CLEAR_PLACE_NOTIFICATIONS_EXTRA = "clear_place_notifications";

    private static final int REQUEST_CHECK_LOCATION_SETTINGS = 10;
    private static final int REQUEST_ACCESS_LOCATION = 20;
    private static final int REQUEST_FIND_PLACE = 30;
    private static final int REQUEST_ADD_PLACE = 40;
    private static final int REQUEST_EDIT_PLACE = 50;

    private static final float MAP_DEFAULT_ZOOM = 13;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.category)
    TextView categoryView;

    @BindView(R.id.fab)
    View actionButton;

    @BindView(R.id.place_details)
    PlaceDetailsView placeDetails;

    private GoogleMap map;

    private ClusterManager<Place> placesManager;

    private PlaceCategory selectedCategory;

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

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .addConnectionCallbacks(new LocationApiConnectionCallbacks())
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Toast.makeText(MapActivity.this, connectionResult.getErrorMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .build();

        googleApiClient.connect();

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
                if (new AuthController().isAuthorized()) {
                    EditPlaceActivity.startForResult(MapActivity.this, place.getId(), null, REQUEST_EDIT_PLACE);
                } else {
                    SignInActivity.start(MapActivity.this);
                }
            }

            @Override
            public void onDismissed() {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        toolbar.inflateMenu(R.menu.menu_map_activity);
        toolbar.setOnMenuItemClickListener(this);
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
            selectPlace(data.getLongExtra(FindPlaceActivity.PLACE_ID_EXTRA, -1));
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedPlace.getPosition(), MAP_DEFAULT_ZOOM));
        }

        if (requestCode == REQUEST_ADD_PLACE && resultCode == RESULT_OK) {
            reloadPlaces();
        }

        if (requestCode == REQUEST_EDIT_PLACE && resultCode == RESULT_OK) {
            reloadPlaces();

            if (selectedPlace != null) {
                selectPlace(selectedPlace.getId());
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ACCESS_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }

                    map.setMyLocationEnabled(true);
                    moveToLastLocation();
                }
        }
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
            map.setMyLocationEnabled(true);
            moveToLastLocation();
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

        placesCache = Injector.INSTANCE.mainComponent().placesCache();
        placesCache.getListeners().add(this);

        handleIntent(getIntent());
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_add:
                if (new AuthController().isAuthorized()) {
                    EditPlaceActivity.startForResult(this, 0, map.getCameraPosition(), REQUEST_ADD_PLACE);
                } else {
                    SignInActivity.start(this);
                }

                return true;
            case R.id.action_search:
                Location lastLocation = null;

                if (googleApiClient != null && googleApiClient.isConnected() && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                }

                FindPlaceActivity.startForResult(this, lastLocation, REQUEST_FIND_PLACE);
                return true;
            case R.id.action_popup:
                MapPopupMenu popupMenu = new MapPopupMenu(MapActivity.this);
                popupMenu.showAsDropDown(findViewById(R.id.top_right_corner));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        switch (bottomSheetBehavior.getState()) {
            case BottomSheetBehavior.STATE_EXPANDED:
            case BottomSheetBehavior.STATE_SETTLING:
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                break;
            case BottomSheetBehavior.STATE_COLLAPSED:
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                break;
            case BottomSheetBehavior.STATE_HIDDEN:
                super.onBackPressed();
                break;
        }
    }

    @Override
    public void onPlaceCategorySelected(PlaceCategory category) {
        selectedCategory = category;
        @StringRes int textResId = category == null ? R.string.all_places : category.getPluralStringId();
        categoryView.setText(textResId);
        reloadPlaces();
    }

    @Override
    public void onSettingsClick() {
        SettingsActivity.start(this);
    }

    @Override
    public void onExchangeRatesClick() {
        Intent intent = new Intent(this, ExchangeRatesActivity.class);
        startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle());
    }

    @Override
    public void onSignInClick() {
        SignInActivity.start(this);
    }

    @Override
    public void onSignOutClick() {
        final AuthController authController = new AuthController();

        if ("google".equalsIgnoreCase(authController.getMethod())) {
            Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    authController.setUser(null);
                    authController.setToken(null);
                    authController.setMethod(null);
                    Toast.makeText(MapActivity.this, status.getStatusMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            authController.setUser(null);
            authController.setToken(null);
            authController.setMethod(null);
        }
    }

    @Override
    public void onPlacesCacheInitialized() {
        reloadPlaces();
    }

    private void handleIntent(final Intent intent) {
        if (intent.getBooleanExtra(CLEAR_PLACE_NOTIFICATIONS_EXTRA, false)) {
            PlaceNotification.deleteAll();
        }

        if (intent.hasExtra(PLACE_ID_EXTRA)) {
            selectPlace(intent.getLongExtra(PLACE_ID_EXTRA, -1));

            if (selectedPlace != null && map != null) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedPlace.getPosition(), MAP_DEFAULT_ZOOM));
            }
        }

        if (intent.hasExtra(NOTIFICATION_AREA_EXTRA)) {
            NotificationArea notificationArea = (NotificationArea) intent.getSerializableExtra(NOTIFICATION_AREA_EXTRA);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(notificationArea.getCenter(), MAP_DEFAULT_ZOOM));
        }
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
            onPlacesLoaded(placesCache.getPlaces(bounds, selectedCategory));
        } else {
            placesCache.getListeners().add(new PlacesCache.PlacesCacheListener() {
                @Override
                public void onPlacesCacheInitialized() {
                    onPlacesLoaded(placesCache.getPlaces(bounds, selectedCategory));
                    placesCache.getListeners().remove(this);
                }
            });
        }
    }

    private void selectPlace(long placeId) {
        selectedPlace = Place.find(placeId);

        if (selectedPlace == null) {
            return;
        }

        selectedPlace.setCurrencies(Currency.findByPlace(selectedPlace));
        placeDetails.setPlace(selectedPlace);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        logSelectPlaceEvent(selectedPlace);
    }

    private void logSelectPlaceEvent(@NonNull Place place) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, String.valueOf(place.getId()));
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, place.getName());
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "place");
        Injector.INSTANCE.mainComponent().analytics().logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    @OnClick(R.id.fab)
    public void onActionButtonClick() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            moveToLastLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_ACCESS_LOCATION);
        }
    }

    @OnClick(R.id.categories_spinner)
    public void onCategoriesSpinnerClick() {
        new PlaceCategoryDialog().show(getSupportFragmentManager(), PlaceCategoryDialog.TAG);
    }

    @OnClick(R.id.place_details)
    public void onPlaceDetailsClick() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    private class PlacesRenderer extends StaticClusterRenderer<Place> {
        private MapMarkersCache cache;

        public PlacesRenderer(Context context, GoogleMap map, ClusterManager<Place> clusterManager) {
            super(context, map, clusterManager);
            cache = Injector.INSTANCE.mainComponent().markersCache();
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
                firstLaunch = false;
                moveToLastLocation();
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            // Nothing to do here
        }
    }
}