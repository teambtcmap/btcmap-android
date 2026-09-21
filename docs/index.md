# BTC Map for Android

BTC Map helps you find places to spend sats wherever you are. The data comes
from [OpenStreetMap](https://www.openstreetmap.org), where volunteers tag
merchants, ATMs and exchanges that accept bitcoin. BTC Map is a free and
open-source project.

This documentation is for people using the app. If you want to build the app or
contribute code, see [`AGENTS.md`](../AGENTS.md) and the
[`README`](../README.md) instead.

## Start here

- **[Getting started](getting-started.md)** — install the app, grant the
  permissions you want, and learn your way around the map.

## Feature guides

Each guide below is a standalone page focused on one part of the app.

| Feature | What it covers |
| --- | --- |
| [Map](features/map.md) | Filters, markers, your location, area chips and map styles. |
| [Search](features/search.md) | Find places, areas and events by name. |
| [Places](features/places.md) | Merchant details, verifying and reporting, adding a place. |
| [Events](features/events.md) | Bitcoin meetups and other events near you. |
| [Areas and communities](features/areas.md) | Community and country pages, issues, and their events. |
| [Offline maps](features/offline-maps.md) | Download an area's map and use the app without a connection. |
| [Comments](features/comments.md) | Read and add anonymous, spam-protected comments. |
| [Boosting](features/boosting.md) | Make a merchant stand out on the map. |
| [Activity feed](features/activity-feed.md) | Recent changes nearby and in the areas you saved. |
| [Accounts and saved items](features/accounts.md) | Sign up, manage your profile, save places and areas. |
| [Settings and appearance](features/settings.md) | Map style, custom colors and the built-in diagnostics screens. |

## Good to know

- **Where the data comes from.** Place, area, event and comment data is provided
  by OpenStreetMap and the BTC Map community. Edits you make to a place are
  reviewed before they appear.
- **It works offline.** A snapshot of places, areas, events and comments is
  bundled with the app, so it is usable on first launch and without a
  connection. See [Offline maps](features/offline-maps.md).
- **Payments use Lightning.** Comments and boosts are paid in sats through a
  Lightning wallet on your device. The app never asks for personal data.
- **Release signatures.** You can verify a release APK against the certificate
  fingerprint listed in the [README](../README.md#verifying-signatures).
