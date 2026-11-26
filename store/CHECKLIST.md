# Publishing checklist

## 0. Before anything: things only you can confirm
- [ ] **Translation and text permission.** The Arabic text and Maududi's Urdu translation come from Tanzil / quran-json (CC BY-SA 4.0 for the package). Check the Tanzil terms and whether the rights holder of Maududi's translation allows distribution in a published app. If unsure, ask the publisher or use text you hold rights to.
- [ ] **Audio.** Streaming from everyayah.com: check their terms, or remove the audio button.
- [ ] Take 2+ real phone screenshots (Play requires them). I could not make these.

## 1. Privacy policy URL (free, GitHub Pages)
1. In your GitHub repository: Settings > Pages > Source: "Deploy from a branch", branch `main`, folder `/docs`.
2. Edit `docs/privacy.html` and put your email in.
3. Your URL will be `https://YOUR-NAME.github.io/easy-quran/privacy.html`.

## 2. Signing key (one time, KEEP IT SAFE – losing it blocks updates)
On a PC with Java installed (or use Android Studio's terminal):
```
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias easyquran
```
Then turn it into text (Windows PowerShell):
```
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.jks"))
```
In GitHub: Settings > Secrets and variables > Actions > New repository secret. Add:
`KEYSTORE_BASE64` (the long text), `KEYSTORE_PASSWORD`, `KEY_ALIAS` (easyquran), `KEY_PASSWORD`.
Never upload release.jks to GitHub. Back it up somewhere private.

## 3. Build the bundle
Actions tab > "Build signed release bundle (AAB)" > Run workflow. Download `EasyQuran-release-aab` > unzip > `app-release.aab`.

## 4. Play Console (play.google.com/console)
- [ ] Pay the US$25 developer fee, verify identity, turn on 2-step verification.
- [ ] Create app: name Easy Quran, App, Free. Accept Play App Signing.
- [ ] Upload `app-release.aab` to **Internal testing** first, install and test on your own phone.
- [ ] Store listing: text from `store/listing.md`, icon `store/icon-512.png`, feature graphic `store/feature-graphic-1024x500.png`, your screenshots.
- [ ] Privacy policy URL from step 1.
- [ ] Data safety: no data collected or shared (audio streaming sends an IP address to everyayah.com, mention it if the form asks about network requests).
- [ ] Content rating questionnaire, target audience (all ages), Ads: none.
- [ ] **Foreground service declaration** (special use). Suggested wording: "The user can turn on a feature that changes the device wallpaper to the next Quran ayah each time the screen is unlocked. A foreground service holds the receiver that detects the unlock; it runs only while the user keeps the feature switched on and shows a notification."
- [ ] **Exact alarm declaration.** Suggested wording: "Prayer-time alarms that alert the user at the calculated time of each of the five daily prayers; the user turns each one on or off." Google may reject this permission. The app falls back to approximate alarms (a few minutes late) if it is not granted.
- [ ] **Closed testing:** a personal account created after Nov 2023 needs at least 12 testers opted in for 14 days before requesting production access. Recheck Google's current rule.
- [ ] Then apply for production access and release.

## 5. Each update
Increase `versionCode` in `app/build.gradle.kts` by 1, rebuild the AAB, upload.
