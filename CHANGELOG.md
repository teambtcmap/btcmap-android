# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

- Fix the sign-in, sign-up and change-password forms doing nothing when submitted after the device was rotated
- Keep the account chooser open when the device is rotated
- Submit the account and change-password forms from the keyboard
- Keep what was typed in the change-password form when the device is rotated
- Clear a corrected field's error in the account and change-password forms
- Fix the legacy settings import being permanently skipped when reading the old values fails
- Stop saving a session from blocking other settings reads on the main thread
- Confirm the password and require at least 8 characters when creating an account
- Confirm the new password and require at least 8 characters when changing the password
- Fix the map not releasing its native resources when the map screen is closed
- Keep what was typed in the sign-in or sign-up form when the device is rotated
- Let a slow sign-in or sign-up be cancelled and time it out instead of leaving the spinner stuck
- Read the signed-in session from memory so the app never opens the database on the main thread
- Fix signing out dropping an account that was signed in again in the meantime
- Clear a leftover session on the settings screen when the cached account is missing
- Document that the session token is intentionally stored unencrypted in the private app database
- Show a required-field error when saving a blank username or password in the profile instead of silently doing nothing
- Clear the cached account when an outdated encrypted session token is dropped during an upgrade
- Read the account status on the settings screen off the main thread
- Fix signing in as a different account leaving the previously cached account behind
- Reject a sign-in response that does not carry a session token instead of storing an empty one
- Fix the "following" activity feed showing as signed in after the stored session was lost
- Revoke the session token on the server when signing out, so it can no longer be reused
- Only send the stored session token to the configured API host
- Dismiss sign-in dialogs when leaving the screen instead of leaking the activity window
- Avoid exposing internal server or network details in the profile password and username dialogs
- Fix the stored session token being read as stale or missing when several requests run at once
- Fix a late rejected request from an old session signing out an account that was signed in again in the meantime
- Never leave an account only partly signed in when the session token cannot be saved
- Show a progress indicator while signing in or creating an account
- Show actionable sign-in errors instead of exposing internal server or network details
- Show the logged-in confirmation when signing in to an existing account, not just after creating one
- Only send the stored session token to API endpoints that require authentication
- Reopen the sign-in screen with the username filled in when account creation succeeds but the automatic sign-in fails
- Translate the beta update description into all supported languages
- Fix a crash when an API request fails instead of showing the error message
- Fix a rejected sign-in attempt clearing the currently signed-in account
- Allow long-running API requests so large place syncs no longer time out
- Sign out and clear the cached account automatically when the server rejects the stored session token
- Fix sign-in being sent with a stale stored token instead of the entered credentials
- Show a sign-in specific error message when signing in fails instead of an account creation error
- Fix a crash when opening the profile screen without a cached account
- Keep the cached account when changing the username is interrupted
- Keep events without a usable website instead of dropping them, and hide the website link for those
- Ignore unknown search result types returned by the API instead of failing to parse the response
- Fix place and comment sync skipping entries when more than one batch shares the same update timestamp
- Remove deleted comments from the local cache during sync
- Load an area's upcoming events from the API instead of a stale snapshot so newly added events show up immediately
- Fix text clipping in the upcoming event and place issue cards on the area screen
- Disable the area screen save action until the area has finished loading
- Localize event dates on the area screen
- Keep the selected map filter when returning to the map from other screens
- Show a dedicated event screen with a map, a directions action and a link to the event website instead of opening the website directly
- Open btcmap.org event links directly in the app
- Fix event links opening a duplicate event screen that required two back presses to dismiss
- Fix a crash when the map finishes loading after navigating away from it
- Localize area names and descriptions to the device language
- Collapse long area descriptions to the first paragraph with a read more toggle
- Show how many place issues are displayed when the list is truncated
- Show place issues with their place icons on the area screen below upcoming events and open the OSM editor when tapped
- Extend the area header image behind the status bar and toolbar
- Fix crashes when toggling or loading saved places and areas
- Fix crashes when navigating away while a screen or action is still loading
- Show a loading indicator on the area screen and return to the map with an error dialog when the area fails to load
- Show a retryable error instead of the empty state when the activity feed fails to load
- Fix a crash when tapping a map marker and its place or event can't be loaded
- Keep the area list on screen when refreshing nearby areas fails
- Speed up marker rendering for large areas by generating marker images and GeoJSON off the main thread
- Render any place icon present in the bundled font without a hardcoded list
- Stop showing places as boosted after their boost expires
- Draw and select overlapping map markers by depth and ignore taps on transparent marker areas
- Allow posting paid place comments without signing in
- Tighten place details bottom sheet header and action button spacing
- Show server-provided error messages and avoid retrying paid actions on rate limits
- Add a "How to help?" link to the area issues section
- Localize the area screen strings into all supported languages
- Localize the activity feed into all supported languages
- Translate the account, add-place and verify/report screens into all supported languages
- Open btcmap.org merchant links directly in the app
- Add in-app screens for submitting, verifying and reporting places
- Add activity feed with Local and Following tabs
- Show upcoming events as clickable cards and area count badges
- Add saved places
- Add saved areas management
- Add native area screen
- Rework account creation and authentication flow
- Allow changing username from profile screen
- Add OpenStreetMap attribution with settings toggle
- Search places and areas via the v4/search API
- Support per-app language selection on Android 13+
- Keep forms and search results above the keyboard
- Bundle map styles as APK assets
- Render outdated merchants with reduced opacity and gray icons
- Filter out outdated places by default
- Make map rotation opt in
- Show wide place icons when available
- Add optional debug info display
- Improve map loading performance
- Improve database layer testability and observability
- Update to Kotlin 2.4, AGP 9.4, targetSdk 37 and latest dependencies
- Update Gradle to 9.7.1 and AndroidX libraries to latest stable releases
- Skip in-app update prompts on debug builds
- Show an in-app update notification on beta builds
- Move settings and the stored session into the database so signing in and out updates the token and cached account atomically

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
