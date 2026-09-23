# Install the app

BTC Map runs on [Android 10 and newer](../../../app/build.gradle.kts). Every
official download is listed on [btcmap.org/apps](https://btcmap.org/apps); pick
whichever source you prefer:

- **F-Droid** — install from the
  [BTC Map page on F-Droid](https://f-droid.org/packages/org.btcmap/). The store
  manages updates for you.
- **Direct APK** — download the APK and open it:
  - [Latest release](https://static.btcmap.org/android/apk/latest.apk) —
    the stable build, also linked as **APK** on
    [btcmap.org/apps](https://btcmap.org/apps).
  - [Latest beta](https://static.btcmap.org/android/apk/beta.apk) — the
    pre-release build, also linked as **APK (Beta)**. See
    [Enrolling in beta](#enrolling-in-beta) below.

  When installing a direct APK you may need to allow your browser or file
  manager to install unknown apps. If you install this way, the app also
  notifies you when a newer build is available and offers a **Get APK** button.
- **Obtainium** — install and update BTC Map straight from its release page.
  First install [Obtainium](https://obtainium.imranr.dev/), then add BTC Map's
  [GitHub releases page](https://github.com/teambtcmap/btcmap-android/releases)
  as a source; Obtainium checks it and notifies you when a new release is out.
  To follow the beta instead, add the direct APK URL
  `https://static.btcmap.org/android/apk/beta.apk` as the source.
- **GitHub releases** — browse every published build on the
  [releases page](https://github.com/teambtcmap/btcmap-android/releases).
- **Zapstore** — install from
  [zapstore.dev/apps/org.btcmap](https://zapstore.dev/apps/org.btcmap).

The APKs distributed directly by the BTC Map team — the direct downloads and
GitHub releases — are signed with the team's release key, and you can check the
signature as described in the
[README](../../../README.md#verifying-signatures). Builds installed from F-Droid
are signed by F-Droid with its own key instead, so that fingerprint does not
apply to them.

## Enrolling in beta

The stable release is the safe choice for everyday use. If you would like to
help shape BTC Map, the beta build is for you: it gets new features and fixes
before the stable release, and we depend on beta testers to catch problems
early. There is no sign-up, invite or waiting list — just install it.

- **Download it at**
  [static.btcmap.org/android/apk/beta.apk](https://static.btcmap.org/android/apk/beta.apk),
  or use the **APK (Beta)** link on
  [btcmap.org/apps](https://btcmap.org/apps).
- **It sits next to the stable app; it does not replace it.** The beta is a
  separate app with its own Android application id (`org.btcmap.beta`, versus
  `org.btcmap` for the stable release), so both can be installed at the same
  time. You can keep using the stable app for everyday navigation and open the
  beta whenever you want to try what is coming next.
- **It is easy to tell apart.** The beta is named **BTC Map Beta** and uses a
  distinct app icon.
- **Each app updates on its own.** Installing, updating or uninstalling one has
  no effect on the other, and removing the beta leaves your stable app and its
  data untouched.
- **What we ask of testers.** Use it like you normally would, and when something
  looks wrong or you have an idea, tell us. Report bugs or suggestions on
  [GitHub](https://github.com/teambtcmap/btcmap-android/issues) or say hello in
  our [Matrix room](https://matrix.to/#/#btcmap:matrix.org).

The stable and beta APKs from the direct downloads above share the same release
key, so you can verify either one as described in the
[README](../../../README.md#verifying-signatures).

---

Next: [First launch](first-launch.md).

Back to the [documentation index](../index.md).
