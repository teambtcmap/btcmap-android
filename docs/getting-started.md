# Getting started

This guide takes you from installing BTC Map to finding your first place that
accepts bitcoin.

## Install the app

BTC Map runs on **Android 10 and newer**. Pick whichever source you prefer:

- **F-Droid** — install from the
  [BTC Map page on F-Droid](https://f-droid.org/packages/org.btcmap/). Updates
  are managed by the store.
- **GitHub releases** — download an APK from the
  [releases page](https://github.com/bubelov/btcmap-android/releases) and open
  it. You may need to allow installing apps from your browser or file manager.
- **Direct APK** — if you installed this way, the app can tell you when a newer
  build is available and offer a **Get APK** button.

Release APKs are signed by the BTC Map team. You can check the signature as
described in the [README](../README.md#verifying-signatures).

## First launch

When the app opens for the first time, it shows the map with a snapshot of
places, areas, events and comments that ships with the app. That means it is
usable immediately, even before the first sync finishes or while you are
offline.

The app then syncs in the background to bring the data up to date. A small
progress indicator appears while a sync is running; it is normal for this to
happen on the first launch or after a while without a connection.

## Permissions

- **Network** — required to sync data and load map tiles and images.
- **Location (optional)** — used only to show where you are on the map and to
  centre it on you. The app works fine if you deny it, and you can grant it
  later.

You can grant location from the **locate button** on the map; the app asks only
when you use it.

## Finding your way around

The **map** is the home screen. From top to bottom:

- The **search field** at the top: type a place, area or event name to search.
  The menu next to it has **Add location** and **Settings**.
- The **filter chips** at the bottom: **Merchants**, **Events** and
  **Exchanges** switch what is shown on the map.
- **Area chips** above the filters: the community or country you are currently
  looking at. Tap one to open its page.
- The **locate button** to centre the map on your position.
- The **activity button** to see recent changes nearby.

Tap any marker to open a card with the place's details at the bottom of the
screen.

## Try it out

A short path through the most common tasks:

1. **Find a nearby merchant.** Tap the locate button, then tap a marker. The
   card shows the name, address, opening hours, whether it was recently
   verified, and how to get there.
2. **Get directions.** Open a place and choose **Directions** from its menu to
   hand the coordinates to your maps app.
3. **Confirm a place still accepts bitcoin.** Open a place and tap **Verify**.
   Your report is reviewed by the BTC Map community.
4. **Add a missing place.** Tap **Add location** in the top menu, fill in the
   details and drag the map to set the exact spot, then submit it for review.
5. **Leave a note for others.** Open a place and tap **Comment**. Comments are
   anonymous but carry a small fee in sats as spam protection, paid with a
   Lightning wallet.
6. **Support a merchant.** Open a place and tap **Boost** to make it stand out
   on the map, in search and in the boosted section.

An account is needed for verifying, reporting, adding places, saving places and
areas, and following other users. Comments and boosts do not require one.
See [Accounts and saved items](features/accounts.md) for details.

## Using the app offline

All of the app's data is cached on your device, so search, place details, areas
and events keep working without a connection. To also keep the map itself, open
an area and download its tiles for offline use — see
[Offline maps](features/offline-maps.md).

## Where to go next

- Browse the [feature guides](index.md#feature-guides).
- Learn how to help improve the data with the
  [BTC Map tagging instructions](https://wiki.btcmap.org/Tagging-Merchants).
- Support the project at
  [btcmap.org/support-us](https://btcmap.org/support-us).

Back to the [documentation index](index.md).
