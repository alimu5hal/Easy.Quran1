# Easy Quran (Android)

Open this folder in Android Studio (Koala or newer), let Gradle sync, then Run on a phone (Android 8.0+).
Command line: run `gradle wrapper` once, then `./gradlew assembleDebug`.

## Features
- **Reader like Islam360**: Quran home, Surah / Juz / Bookmarks index, search, full reader with Arabic (Amiri Quran font) and Urdu (Noto Nastaliq), text size, continuous audio, bookmarks, notes, share/copy, resume where you stopped
- Whole Quran, one ayah at a time, in order (6,236 ayat), progress remembered
- Wallpaper (lock, home or both) moves to the next ayah with a new scene on every unlock
- Scenes: Kaaba, Masjid an-Nabawi, Dome of the Rock, Badshahi Mosque, Blue Mosque (original illustrations) + your own photos
- Text colour and a soft panel are chosen from the brightness behind the text
- Daily reminder notification, search (Urdu/Arabic/surah/2:255), bookmarks, personal notes, audio recitation (Alafasy)

## Kanz ul Iman translation
The app ships with the full Arabic text and Maududi's Urdu translation (Tanzil edition), so it works at once.
To show Kanz ul Iman: put your text in the format `surah|ayah|urdu|optional tafseer` (one ayah per line), run
`python3 tools/make_kanz_json.py kanz.txt kanz.json "Kanz ul Iman - Ahmad Raza Khan"`, copy kanz.json to the phone and use
**Import translation / tafseer** in the app. A tafseer text in the 4th column shows under each ayah.

## Notes
- Unlock mode needs a small ongoing notification (Android requirement). On some phones (Xiaomi, Oppo, Vivo, Samsung) also turn off battery optimisation for Easy Quran.
- Urdu Nastaliq style depends on the phone's installed fonts. To force it, add a Nastaliq .ttf to `res/font` and set it in `Scene.lay`.

## Namaz
- Times are computed on the phone for 18 Pakistani cities (Karachi method, Hanafi Asr, PKT). No internet needed.
- Alarm per prayer, tracker with 7-day history and streak, Qibla compass, Hijri date, Tasbeeh counter.
- On Android 12+ allow "exact alarms" from the button on the Namaz screen so alarms fire on time.

## Build without Android Studio (free, in the cloud)
1. Create a GitHub account and a new empty repository.
2. Upload every file in this folder (including the hidden `.github` folder) to it.
3. Open the **Actions** tab, run **Build APK** (it also runs on every upload).
4. When it finishes, download **EasyQuran-debug-apk** from the run page, unzip it, and install `app-debug.apk` on your phone (allow "install unknown apps").
If the build fails, copy the red error text and send it to Claude to fix.

## Publishing
See `store/CHECKLIST.md` (signing key, release bundle, Play Console steps), `store/listing.md` and `docs/privacy.html`.
The project targets Android 16 (API 36), AGP 8.9.1, Gradle 8.11.1, Kotlin 2.0.21.
