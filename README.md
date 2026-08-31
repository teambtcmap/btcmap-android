<p align="center"><img src="https://github.com/bubelov/btcmap-android/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="100"></p> 
<h2 align="center"><b>BTC Map</b></h2>
<h4 align="center">Find places to spend sats wherever you are</h4>

<p align="center">
  <a href="https://f-droid.org/packages/org.btcmap/">
    <img src="graphics/get-it-on-fdroid.svg" alt="Get it on F-Droid" height="60">
  </a>
</p>

<p align="center">
<a href="https://github.com/bubelov/btcmap-android/releases" alt="GitHub release"><img src="https://img.shields.io/github/release/bubelov/btcmap-android.svg" ></a>
<a href="https://www.gnu.org/licenses/gpl-3.0" alt="License: GPLv3"><img src="https://img.shields.io/badge/License-AGPL%20v3-blue.svg"></a>
</p>

## Screenshots

<div>
<img alt="" src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="204">
<img alt="" src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="204">
<img alt="" src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="204">
<img alt="" src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width="204">
</div>

## Support BTC Map

[btcmap.org/support-us](https://btcmap.org/support-us)

## 🌍 Help with Translation

Click on a language link below and press an edit button if you want to help with translations.

- [Afrikaans](app/src/main/res/values-af/strings.xml)
- [Arabic](/app/src/main/res/values-ar/strings.xml)
- [Bengali](/app/src/main/res/values-bn/strings.xml)
- [Bulgarian](/app/src/main/res/values-bg/strings.xml)
- [Catalan](/app/src/main/res/values-ca/strings.xml)
- [Chinese](/app/src/main/res/values-zh/strings.xml)
- [Czech](/app/src/main/res/values-cs/strings.xml)
- [Danish](/app/src/main/res/values-da/strings.xml)
- [Dutch](/app/src/main/res/values-nl/strings.xml)
- [English](/app/src/main/res/values/strings.xml)
- [Finnish](/app/src/main/res/values-fi/strings.xml)
- [French](/app/src/main/res/values-fr/strings.xml)
- [German](/app/src/main/res/values-de/strings.xml)
- [Greek](/app/src/main/res/values-el/strings.xml)
- [Hebrew](/app/src/main/res/values-iw/strings.xml)
- [Hindi](/app/src/main/res/values-hi/strings.xml)
- [Hungarian](/app/src/main/res/values-hu/strings.xml)
- [Italian](/app/src/main/res/values-it/strings.xml)
- [Japanese](/app/src/main/res/values-ja/strings.xml)
- [Korean](/app/src/main/res/values-ko/strings.xml)
- [Norwegian](/app/src/main/res/values-no/strings.xml)
- [Persian](/app/src/main/res/values-fa/strings.xml)
- [Polish](/app/src/main/res/values-pl/strings.xml)
- [Portuguese](/app/src/main/res/values-pt/strings.xml)
- [Portuguese (Brazil)](/app/src/main/res/values-pt-rBR/strings.xml)
- [Romanian](/app/src/main/res/values-ro/strings.xml)
- [Russian](/app/src/main/res/values-ru/strings.xml)
- [Serbian](/app/src/main/res/values-sr/strings.xml)
- [Spanish](/app/src/main/res/values-es/strings.xml)
- [Swedish](/app/src/main/res/values-sv/strings.xml)
- [Thai](/app/src/main/res/values-th/strings.xml)
- [Turkish](/app/src/main/res/values-tr/strings.xml)
- [Ukrainian](/app/src/main/res/values-uk/strings.xml)
- [Urdu](/app/src/main/res/values-ur/strings.xml)
- [Vietnamese](/app/src/main/res/values-vi/strings.xml)

Feel free to open an issue if you can't find your language or regional dialect.

## FAQ

### Where does BTC Map take its data from?

The data is provided by OpenStreetMap:

https://www.openstreetmap.org

### Can I add or edit places?

Absolutely, you are very welcome to do that. This is a good place to start:

[Tagging Instructions](https://wiki.btcmap.org/Tagging-Merchants)

### BTC Map shows a place which doesn't exist, how can I delete it?

You can delete such places from OpenStreetMap and BTC Map will pick up all your changes within 10
minutes.

### I've found a place on BTC Map but it doesn't accept bitcoins

OpenStreetMap might have outdated information about some places, you can delete the `currency:XBT`
tag or set it to `no` in order to remove this place from BTC Map.

## Verifying signatures

Release APKs are signed by a long-lived release key owned by the BTC Map team.
You can verify the signature by checking the certificate's SHA-256 fingerprint:

```
a63de0923b0c27e3de6ef4e90f3a0d81cca917f9689d2638f1b31949657f1435
```

### With apksigner

The Android SDK build-tools include `apksigner`:

```
apksigner verify --print-certs path/to/app.apk
```

The `SHA-256 digest` line for `Signer #1 certificate` should match the
fingerprint above. The certificate subject is `O=Team BTC Map`.

### With keytool

If you'd rather inspect the certificate from a downloaded APK directly, unzip it
and pass the META-INF signature block to `keytool` (the exact filename under
`META-INF/` varies between builds):

```
unzip -p app.apk META-INF/*.RSA | keytool -printcert | grep SHA256
```
