# Map

The map is the home screen of BTC Map and the starting point for everything
else. It shows the places, events and areas around wherever you are looking.

## Filters

Three filter chips at the bottom switch what the map is showing:

- **Merchants** — shops, cafés, restaurants and other businesses that accept
  bitcoin.
- **Exchanges** — bitcoin ATMs and currency exchanges.
- **Events** — meetups and other events, drawn as event markers.

The selected chip is highlighted. Changing the filter rebuilds the markers for
the current view; moving the map loads the markers for the new area as you pan.

## Markers

Each marker is drawn in the map's marker colors. A few variations matter:

- **Boosted** places are drawn in Bitcoin orange so they stand out.
- **Outdated** places — ones that have not been verified for a while — are
  highlighted so you know they may need checking.
- **Bundled** places come from the offline snapshot that ships with the app.
  They are read-only until a sync has replaced them with the live record.

Tap a marker to open the place card at the bottom of the screen. See
[Places](places.md) for what the card contains.

## Your location

BTC Map asks for location permission only when you use the **locate button** in
the corner of the map. Once granted, it centres the map on you and shows your
position. Denying permission does not affect any other feature.

## Area chips

Just above the filter chips, the map shows a **country** chip followed by the
**community** chips for the area you are looking at. Tap a chip to open that
area's page, where you will find its description, upcoming events and issues.
See [Areas and communities](areas.md).

Area chips come from the local cache, so they appear instantly and work
offline.

## Activity feed

The activity button opens a feed of recent changes near the current view. See
[Activity feed](activity-feed.md).

## Map styles

You can change the look of the map under **Settings → Map style**. The options
are:

- **Auto** — a light style during the day and a dark style at night.
- **OpenFreeMap Liberty**, **Positron**, **Bright** and **Dark**.
- **Carto Dark Matter**.

Offline map downloads are tied to the style you were using, so if you change
style after downloading an area, download it again to use the offline tiles
with the new style. See [Offline maps](offline-maps.md).

## Links into the app

Opening one of these links on a device with BTC Map installed opens the app
directly:

- `https://btcmap.org/merchant/<id>` — a place.
- `https://btcmap.org/event/<id>` — an event.

When the target is not in the local cache yet, the app fetches it.

---

Back to the [documentation index](../index.md).
