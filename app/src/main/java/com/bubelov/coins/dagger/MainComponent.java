package com.bubelov.coins.dagger;

import android.content.Context;

import com.bubelov.coins.data.repository.area.NotificationAreaRepository;
import com.bubelov.coins.data.repository.currency.CurrenciesRepository;
import com.bubelov.coins.data.repository.notification.PlaceNotificationsRepository;
import com.bubelov.coins.data.repository.place.PlacesRepository;
import com.bubelov.coins.data.repository.placecategory.PlaceCategoriesDataSourceDb;
import com.bubelov.coins.data.repository.placecategory.PlaceCategoriesDataSourceMemory;
import com.bubelov.coins.data.repository.placecategory.PlaceCategoriesDataSourceNetwork;
import com.bubelov.coins.data.repository.placecategory.PlaceCategoriesRepository;
import com.bubelov.coins.data.repository.rate.ExchangeRatesRepository;
import com.bubelov.coins.ui.activity.EditPlaceActivity;
import com.bubelov.coins.ui.activity.ExchangeRatesActivity;
import com.bubelov.coins.ui.activity.FindPlaceActivity;
import com.bubelov.coins.ui.activity.MapActivity;
import com.bubelov.coins.ui.activity.NotificationAreaActivity;
import com.bubelov.coins.ui.activity.ProfileActivity;
import com.bubelov.coins.ui.fragment.SettingsFragment;
import com.bubelov.coins.ui.fragment.SignInFragment;
import com.bubelov.coins.ui.fragment.SignUpFragment;
import com.bubelov.coins.service.DatabaseSync;
import com.bubelov.coins.util.PlaceNotificationManager;
import com.bubelov.coins.util.MapMarkersCache;
import com.google.firebase.analytics.FirebaseAnalytics;

import javax.inject.Singleton;

import dagger.Component;

/**
 * @author Igor Bubelov
 */

@Singleton
@Component(modules = {MainModule.class})
public interface MainComponent {
    Context context();

    MapMarkersCache markersCache();

    DatabaseSync databaseSync();

    FirebaseAnalytics analytics();

    PlaceNotificationManager notificationManager();

    NotificationAreaRepository notificationAreaRepository();
    PlacesRepository placesRepository();
    CurrenciesRepository currenciesRepository();
    ExchangeRatesRepository exchangeRatesRepository();
    PlaceNotificationsRepository placeNotificationsRepository();

    PlaceCategoriesRepository placeCategoriesRepository();
    PlaceCategoriesDataSourceNetwork placeCategoriesDataSourceNetwork();
    PlaceCategoriesDataSourceDb placeCategoriesDataSourceDb();
    PlaceCategoriesDataSourceMemory placeCategoriesDataSourceMemory();

    void inject(MapActivity target);
    void inject(EditPlaceActivity target);
    void inject(FindPlaceActivity target);
    void inject(NotificationAreaActivity target);
    void inject(ProfileActivity target);
    void inject(ExchangeRatesActivity target);

    void inject(SignInFragment target);
    void inject(SignUpFragment target);
    void inject(SettingsFragment target);

    void inject(DatabaseSync sync);
}