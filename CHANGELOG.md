# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

- Show the desktop map's sync indicator again, and re-run the sync every time the map is entered rather than only at launch, so returning from a sub-screen refreshes the data
- Support large screens across the account flow: the signed-in profile and its edit forms stay in a centred column instead of stretching across a wide window, and the sign-in form is capped, headed and centred
- Label the account form's password-visibility control for screen readers, and keep its submit button tappable so the required-field messages appear
- Cap the map search field and its results at the place sheet's width and centre them, so they no longer stretch across a large screen
- Follow OS dark-mode changes in the running desktop app on Linux instead of keeping the launch-time theme until a restart
- Apply a map style change immediately instead of only after a restart: the map, its offline packs, the attribution line and the system-bar icons now all follow the newly picked style
- Fix the basemap rendering blank over regions whose labels need CJK or Hangul glyphs (for example Beijing or Seoul): the unbundled glyph ranges are fetched online, falling back to an empty range offline so the tiles still draw
- Show cities in the map's area chips, below countries and communities, and give cities a city-buildings icon in both the chips and the search results
- Jump straight to a search result instead of animating across the distance, and leave search mode once a result is picked
- Change the map search field's hint to "Places, events, areas"
- Hide the website row on the city area screen
- Move every screen into the shared Compose Multiplatform UI: the Android app now hosts one Compose root instead of the Views fragments, dialog fragments and XML layouts, so Android and the desktop render the same screens from `:ui`
- Show the area's header image as a plain header in the body instead of the collapsing toolbar
- Make the area screen's website address tappable, and list its upcoming events above the boosted merchants
- Tighten the area header's spacing: 8dp above the image, 16dp below when a description follows and 28dp before the website when it does not
- Stop the map camera from moving when the search field is focused
- Keep the map's add-location and settings actions hidden while a search query or its results are showing
- Rebuild the add-place screen on the shared Compose UI so Android and the desktop app render the same screen: a Material 3 top app bar, the map's merchant pin as the positioning pin and a confirmation state
- Fix the add-place screen showing the literal `location_on` text instead of the pin, the MapLibre logo and attribution pill, and a blank top bar
- Restore the add-place form's example hints, mark the required fields, chain the keyboard's Next key, show a spinner while submitting, scroll to the first invalid field and surface submit errors in a snackbar
- Float a hint over the add-place map so it is clear the map can be dragged to set the exact location
- Add an add-event screen on the shared Compose UI, mirroring add-place: a positioning map, name and website fields, Material 3 date and time pickers for the start and optional end, and a confirmation state. Submitted times are the event's local wall-clock time, with the zone resolved from its location
- Offer a choice of adding a place or an event when the map's add-location action is tapped, and submit events to the new `POST /v4/events` endpoint
- Add a "My events" screen to the profile: each submitted event is an outlined card with a full-width map preview, its date and review state, a duplicate action that opens a pre-filled add-event screen to repeat a recurring event, and, while pending, a revoke action that soft-deletes the submission. Fetched from `GET /v4/users/me/events` and ordered with the date closest to today first
- Add event review to the map for event managers, admins and roots: a right-side button badged with the number of pending submissions opens a queue where each event shows a map preview, its date and a clickable website link, and can be approved or rejected (`GET /v4/events?status=pending`, `PUT /v4/events/{id}/status`)
- Add an admin-only infrastructure dashboard, opened from a button on the map, showing live analytics (unique clients, places, imports, request logs, API calls, Lightning node, OSM syncs and wallets) with tap-to-open detail dialogs and a refresh action
- Let the account sign-in and sign-up forms be filled by password managers, and add a password visibility toggle
- Cap and centre the desktop account form's width, show a busy indicator while signing in, restyle its error message and drop the redundant sign-in hint
- Show a place's photos at full resolution in the viewer, captioned with the uploader's username
- Let users delete their own place photos from the gallery; admins and root may delete any
- Add an Uploaded images screen to the profile, listing the account's photos with a delete action
- Show the area's header image on the desktop area screen
- Align the area screen's Read more/Collapse button with the text above it
- Open the desktop map's area chips and search results on the shared area and event screens
- Fix a crash when an area screen renders a boosted merchant's expiry date
- Fix a crash when the map loads places and events whose API timestamp omits the zero seconds (`2025-02-03T00:00Z`)
- Fix a crash from concurrent database access, caused by the multiplatform database refactor dropping the shared connection's serialization lock
- Add a WebAssembly (wasmJs) target to the shared core, moving its database layer onto a suspending API
- Move the shared locale, number-formatting, locking and dispatcher services onto multiplatform code, groundwork for a future web target
- Move the shared date/time handling off java.time onto kotlinx-datetime, groundwork for a future web target
- Move the shared HTTP layer off OkHttp onto Ktor, groundwork for a future web target
- Move the shared core off Gson and java.io onto kotlinx.serialization and Okio, groundwork for a future web target
- Attach optional photo evidence when verifying or reporting a place
- Show a place's photos on the place screen, with a fullscreen viewer
- Let signed-in users upload photos to a place
- Restore the map's OpenStreetMap attribution line at the bottom of the screen
- Show the sync indicator above the map's marker filter buttons
- Restore the map controls' pre-rewrite sizes, colours and positions
- Open the place details sheet half-expanded on the map, so the map stays visible behind it
- Drop the redundant mini map from the place sheet opened from the map
- Stop a dismissed place from reopening when returning to the map
- Make the map honour the "Allow map rotation" setting
- Show the soft keyboard in the login, sign-up and change-password dialogs
- Bring the desktop settings screen to parity with Android, adding map style, colours, verification filter and database stats
- Seed the desktop app from the bundled snapshots, so it works offline on first run instead of downloading the whole history
- Open the desktop map at the default Curaçao view, matching the Android app, and follow the map centre in the activity feed
- Share the boost, payment, account, report and saved-items logic between the Android and desktop apps, so the desktop gains the same request timeouts and error handling
- Share the boost plans, saved-item name localization and database stats cards between the Android and desktop apps
- Move the add-place, report and account screens into shared Compose UI, so the desktop and Android render the same forms
- Move the profile, settings, colour and database stats screens into shared Compose UI
- Move the boost, comment and invoice-payment screens into shared Compose UI and generate invoice QR codes in shared code
- Move the Android settings, colour and event screens onto the shared Compose UI, dropping the colour picker library
- Move the Android add-place, report, account, comments, boost and add-comment screens onto the shared Compose UI
- Move the Android image-stats, place and area screens onto the shared Compose UI, and share the image-stats reader and area section loaders
- Move the Android database-stats screen, the activity-feed state machine, the offline-map dialog and the map sync/update controls onto the shared Compose UI
- Move the Android offline map packs, image-load stats and update check onto shared code, and drop the MapLibre Android SDK in favour of the shared Compose map runtime
- Show a place's photos at the top of its sheet, with an add-photo tile in the photo carousel, and make its phone, address, website, email and social links tappable
- Replace a place's Comments button with a Watch/Unwatch action, drop Save from its overflow menu and remove the separate comments screen
- Show a place's opening hours as a week, one line per weekday with today underlined, instead of the raw OpenStreetMap value
- Move the place action buttons directly below the photo carousel and give them icons
- Load and show a place's photos in the desktop place sheet, matching Android
- Rebuild the map search field on Material 3, with a spinner while a search runs and a message when it finds nothing
- Keep the map still when the keyboard opens, and stop it turning black behind the keyboard
- Draw boosted merchants in the settings' boosted marker colour on the area screen and in the search results
- Theme the app from the `#1F2D61` brand seed on Android and desktop, replacing the wallpaper-based dynamic colours
- Drop the fractional distance in search results beyond 10 km

## [1.2.0] - 2026-09-30

- Add global search, covering places, communities, conferences, etc
- Full offline mode support, including bundled world map
- Let users download high-res region maps for offline use
- Add community pulse screen, listing recent events
- Add community/country screen, showing description and maintainance issues
- Add optional auth
- Open btcmap.org place URLs in-app
- Support per-app language selection on Android 13+
- Simplify add location workflow
- Simplify verify/report workflow
- Speed up app startup
- Add human readable opening hours
- Manage saved places and areas
- Add personalized timeline based on watched places and areas
- Show boosted places at the top of matching search results with a rocket icon
- Improve payment flow reliability
- Add missing OSM attribution
- Add database and image manager diagnostics screen
- Bundle map styles as APK assets
- Render outdated merchants with reduced opacity and gray icons
- Filter out outdated (3+ years) places by default
- Show an in-app update notification on beta builds
- Greatly improve test coverage
- Bug fixes and performance improvements

## [1.1.0] - 2026-03-30

- Add Carto Dark Matter map style
- Display current country and nearby communities on the map
- Enable map rotation
- Display localized place names
- Improve opening hours display and translation
- Improve clustering
- Make adaptive color scheme opt in, default to website colors
- Improve payment flow
- Add support for many new languages
- Improve testability and test coverage

## [1.0.0] - 2025-11-06

- Add map style picker
- Show Bitcoin-related events and meetups
- Split map objects into 3 groups (merchants, events, exchanges)
- Improve boost/comment payment flow
- Add LINE links (popular in Thailand)
- Implement full theming support
- Support Material 3 Expressive Colors
- Switch to v4 API
- Add Slovak translation
- Improve Czech translation
- Improve Spanish translation
- Improve Polish translation
- Improve Portuguese translation
- Improve Brazilian translation

## [0.9.2] - 2025-01-25

- Add merchant boosts
- Add comments screen
- Speed up element search within an area
- Show user location marker
- Improve Material theme support
- Remove Google blob from APK
- Show more info on deleted elements
- Add Polish translation
- Add Brazilian translation
- Fix issue with reports
- Fix issue with distance units
- Fix crashes under certain conditions

- Allow users to post merchant comments
- Fix issue with cold sync
- Add special icon for debug builds
- Improve error handling

## [0.9.1] - 2025-01-18

- Allow users to post merchant comments
- Fix issue with cold sync
- Add special icon for debug builds
- Improve error handling

## [0.9.0] - 2025-01-13

- Switch to vector maps
- Change marker colors in light mode
- Fix verification reports

## [0.8.0] - 2024-10-24

- Show place comments
- Hide ATMs by default
- Show places offering delivery
- Migrate to faster and more efficient v3 API

## [0.7.3] - 2024-05-03

- Bundle the latest data snapshots
- Remove sync progress indicator from the map screen
- Add place directions
- Add share location button
- Add companion app warnings for the minority of places which require it
- Fix crashes during sync

## [0.7.2] - 2024-04-26

- Notify user of new places nearby
- Perform daily sync in background
- Handle API rate limiting

## [0.7.1] - 2024-03-17

- Fix issue with deleted places not being shown in a change log
- Fix issue with date format
- Warn the users about outdated places

## [0.7.0] - 2024-02-18

- Show community meetup locations
- Show community description, if available
- Add area issues screen
- Change default location to Curacao
- Zoom to current location
- Enable search by localized place categories
- Show percentage of verified places
- Show days since verified
- Add Portuguese translation
- Show place images, when available
- Hide deleted reports
- Show issues count on area screen
- Let debug builds coexist with the release builds

## [0.6.6] - 2023-06-06

- Allow users to hide ATMs permanently in settings
- Update verify link to single id param
- Change status bar color when place view is expanded
- Add more validations
- Improve Greek translation
- Improve German translation

## [0.6.5] - 2023-04-24

- Fix issue with sync status reporting
- Improve search
- Improve dark mode support
- Show translated place names, when available
- Improve French translation
- Improve Russian translation
- Improve Spanish translation
- Improve Thai translation

## [0.6.4] - 2023-04-09

- Add dark tiles
- Speed up initial sync
- Show last sync date in settings
- Improve Turkish translation

## [0.6.2] - 2023-03-04

- Add incremental sync
- Show sync indicator
- Update donation info

## [0.6.1] - 2023-02-05

- Fix issue with out of sync cache
- Improve place picker animation
- Cache downloaded images
- Speed up communities screen
- Speed up community screen
- Order places by verification date
- Use polygon bounding box when selecting communities

## [0.6.0] - 2023-01-20

- Show community reports
- Show more precise community bounds, when available
- Show boosted places
- Update search bar

## [0.5.10] - 2022-11-22

- Show more verification tags
- Prettify charts

## [0.5.9] - 2022-11-10

- Improve payment flow
- Fix issue with event parsing

## [0.5.7] - 2022-11-09

- Bring back 32-bit ARM support
- Add pay with Pouch button
- Exclude certain users and bots from the leaderboard

## [0.5.6] - 2022-11-05

- Speed-up sync
- Enable Brotli compression
- Upgrade Material components

## [0.5.5] - 2022-11-03

- Fix cluster placement
- Fix app freezes
- Reduce APK size

## [0.5.4] - 2022-11-01

- Change place icon style
- Show more icons

## [0.5.3] - 2022-10-30

- Fix issue with clustering
- Add verify place button
- Prettify social links

## [0.5.2] - 2022-10-29

- Improve clustering

## [0.5.1] - 2022-10-26

- Show more contact details
- Improve dynamic theme switching

## [0.5.0] - 2022-10-25

- Connect OpenStreetMap account (optional)
- Add built-in tag editor

## [0.4.13] - 2022-10-23

- Show more contact methods
- Prettify links
- Allow users to go back to search results
- Save scroll position on taggers screen
- Fix issue with Australia

## [0.4.12] - 2022-10-22

- Show community contact details
- Speed up community screen
- Show last verification date for every place

## [0.4.11] - 2022-10-19

- Speed up map
- Allow users to report outdated places
- Switch to new areas API
- Fix a few bugs

## [0.4.10] - 2022-10-17

- Speed up events loading
- Show Facebook links
- Don't count deleted events
- Speed up area screen
- Show area icons
- Make user edits clickable

## [0.4.9] - 2022-10-15

- Speed up sync
- Re-design pin clusters
- Save scroll position on recent changes screen
- Speed up recent changes loading
- Show pins on place screen
- Sort communities by distance
- Add more place actions

## [0.4.8] - 2022-10-14

- Show last verification dates

## [0.4.7] - 2022-10-13

- Add element screen
- Improve screen transitions
- Re-arrange menu items
- Change event timeline icons
- Fix a few minor bugs
- Include all contributions made by Bill on Bitcoin Island

## [0.4.6] - 2022-10-13

- Flush users cache

## [0.4.5] - 2022-10-12

- Add user profile screen
- Add area screen
- Remove sync indicator
- Improve location permissions handling
- Speed up sync

## [0.4.4] - 2022-10-11

- Fix signature

## [0.4.3] - 2022-10-11

- Prettify charts
- Speed up DB queries to make all screens instantly loadable

## [0.4.2] - 2022-10-10

- Prettify charts
- Add top mappers screen
- Make areas selectable
- Cache areas
- Cache users
- Cache events

## [0.4.1] - 2022-10-08

- Add OSM link on element screen
- Hide deleted places
- Add more icons
- Don't show countries on communities screen

## [0.4.0] - 2022-10-07

- Show daily reports in offline mode
- Add OSM attribution
- Make element events clickable
- Bug fixes and performance improvements

## [0.3.11] - 2022-10-06

- Improve LNURL parsing

## [0.3.10] - 2022-10-06

- Add Lightning tips
- Re-design latest changes screen

## [0.3.9] - 2022-10-05

- Save last location
- Make icons easier to click

## [0.3.8] - 2022-10-05

- Add communities screen
- Change default location

## [0.3.7] - 2022-10-04

- Add trends screen
- Simplify donation flow
- Fix minor issue with navigation

## [0.3.6] - 2022-10-04

- Add themed pins
- Add dark map
- Add settings screen
- Speed up search
- Speed up sync

## [0.3.5] - 2022-09-16

- Change launcher icon
- Provide monochrome icon
- Introduce minimal sync interval
- Change pin appearance

## [0.3.4] - 2022-09-04

- Highlight LN-enabled places
- Sync elements every time the map is shown

## [0.3.3] - 2022-07-11

- Add more place icons
- Show Lightning-enabled places
- Fix issue with pin size

## [0.3.2] - 2022-06-09

- Provide self-signed APK for each release
- Provide self-signed APKs for latest commits
- Add more icons
- Minor bugfixes

## [0.2.0] - 2022-05-27

- Add dark mode support for map pins
- Shrink APK size
- Cluster pins in dense areas
- Support bundled data (optional)
- Add more place icons
- Improve sync

## [0.1.0] - 2022-05-14
