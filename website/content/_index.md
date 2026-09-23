# BTC Map Android

BTC Map helps you find places to spend sats wherever you are. Place data comes
from [OpenStreetMap](https://www.openstreetmap.org), where volunteers tag
merchants, ATMs and exchanges that accept bitcoin. BTC Map is a free and
open-source project.

This documentation is for people using the app. If you want to build the app or
contribute code, see [`AGENTS.md`](../../AGENTS.md) and the
[`README`](../../README.md) instead.

## Start here

- **[Install the app](getting-started/install.md)**. Start with the install,
  then learn your way around the map.

## Feature guides

Each guide focuses on a specific feature and shows you how to make the most of it.

| Feature | What it covers |
| --- | --- |
| [Merchants](features/merchants.md) | Merchant details, verifying and reporting, adding a place. |
| [Events](features/events.md) | Bitcoin meetups and conferences near you. |
| [Countries and communities](features/areas.md) | Community and country pages. |
| [Offline maps](features/offline-maps.md) | Select regions to download and use the app when offline. |
| [Comments](features/comments.md) | Read and add anonymous, spam-protected comments. |
| [Boosts](features/boosting.md) | Make a merchant stand out on the map. |
| [Activity feed](features/activity-feed.md) | Recent changes nearby and in the areas you follow. |
| [Accounts and saved items](features/accounts.md) | Sign up, manage your profile, save places and areas. |
| [Settings and appearance](features/settings.md) | Map style, custom colors and diagnostics. |

## Good to know

- **Where the data comes from.** Place data comes from OpenStreetMap, where
  volunteers tag merchants, ATMs and exchanges that accept bitcoin. Areas,
  events and comments are not permitted on OpenStreetMap, so BTC Map hosts
  them in its own database. Edits you make to a place are reviewed before they
  appear.
- **It works offline.** A snapshot of places, areas, events and comments is
  bundled with the app, so it is usable on first launch and without a
  connection. To go full off-grid, you also need to download map tiles for your
  areas of interest, see [Offline maps](features/offline-maps.md).
- **Payments use Lightning.** Comments and boosts are paid in sats through a
  Lightning wallet on your device.
- **Release signatures.** You can verify a release APK against the certificate
  fingerprint listed in the [README](../../README.md#verifying-signatures).
