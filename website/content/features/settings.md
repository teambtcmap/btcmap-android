# Settings and appearance

Open **Settings** from the menu next to the search field on the map.

## Account

Sign in, create an account, open your profile or log out. See
[Accounts and saved items](accounts.md).

## Map style

Choose the map's look:

- **Auto** — light during the day, dark at night.
- **OpenFreeMap Liberty**, **Positron**, **Bright** and **Dark**.
- **Carto Dark Matter**.

Changing the style takes effect on the map immediately. Offline downloads are
tied to the style they were made with; see [Offline maps](offline-maps.md).

## Colors

You can customise how the map and its controls look. Each color has a picker
with an alpha channel, so you can make elements translucent. Several of them
offer a **Reset** action to return to the default.

- **Marker background** and **marker icon** — the pins that are not boosted.
- **Boosted marker background** — the pins of boosted places (Bitcoin orange by
  default).
- **Badge background** and **badge text** — the small labels drawn on markers.
- **Button background**, **button icon** and **button border** — the on-map
  controls.

With **Use adaptive colors** enabled, the colors follow your device's Material
You palette instead of the defaults.

## Map options

- **Only show places** — filters the places shown by how recently they were
  verified: **within 1 year**, **within 2 years** or **within 3 years**.
- **Show attribution** — show or hide the OpenStreetMap attribution on the map.
  Tapping it opens the OpenStreetMap website.
- **Allow map rotation** — let the map rotate with a two-finger gesture. Off by
  default.
- **Show debug info** — display extra diagnostic information. On by default in
  debug builds only.

## Diagnostics

Two screens in settings report on what the app has stored on your device:

- **Database** — the database version and file, its size, and per-table counts
  including visible and deleted (tombstoned) rows, the bundled snapshots, and
  the current sync state and its source.
- **Image cache** — Coil's memory and disk cache usage and location, plus
  lifetime load counts by source (memory, disk, network), the cache hit rate,
  errors, cancels and the average load time.

These are useful when reporting a problem: they show whether data is present and
up to date without exposing anything personal.

---

Back to the [documentation index](../index.md).
