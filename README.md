# Draw it

A small Android app that reminds you to draw, gives you somewhere to look for
ideas, and keeps a dated album of everything you finish.

Everything lives on the phone. There is no account, no server, and nothing is
uploaded anywhere.

## What it does

**Reminder** — pick a time and pick your days, either every day or a specific
set of them. At that time the app posts a notification telling you it's time to
draw. The alarm re-arms itself after each firing and survives reboots, app
updates and time-zone changes.

**Inspiration** — Pinterest opens inside the app, with a row of topics
(sketchbook, portraits, hands, anatomy, perspective, and more) so you can scroll
for reference without leaving. Your session is kept while the app is open, and
there's a button to hand the current page to your browser.

**Album** — when a drawing is done, photograph it (or pick a photo you already
took), give it a name and an optional description, and it's saved with the date
and time. The album is a searchable grid; tapping a drawing opens it full size
with its details, which you can edit or delete later.

**Today** — the home screen shows the next reminder with a live countdown, a
daily drawing prompt, your streak, and the drawings you added most recently.

## Getting the APK

Every push builds the app on GitHub's runners.

1. Open the repository's **Actions** tab and pick the most recent
   **Build APK** run, or go straight to **Releases**.
2. Download **`draw-it.apk`** — from the release, or from the run's
   `draw-it-apk` artifact.
3. Open the file on your phone. Android will ask you to allow installs from
   whichever app you downloaded it with; allow it, then install.

`draw-it-debug.apk` is the same app built in debug mode under a separate
application id, so it can sit next to the release build if you want to compare.

## Requirements

- Android 8.0 (API 26) or newer.
- Notification permission, which the app asks for on first launch. Without it
  the reminder has no way to reach you; the Reminder screen will tell you and
  link straight to the setting.
- On Android 12 and newer, "alarms & reminders" permission makes the reminder
  land exactly on time. Without it Android may shift the reminder by a few
  minutes to save battery — the app still works, and the Reminder screen offers
  to fix it.

## Signing

Release builds are signed so the APK installs without extra steps. By default
the build uses the convenience keystore committed at
`keystore/drawit-release.jks` (store and key password `drawitdrawit`, alias
`drawit`). That keystore is not a secret and is only meant for personal builds.

To sign with your own key instead, add these repository secrets and the
workflow will use them automatically:

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | your keystore file, base64-encoded |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | key alias |
| `KEY_PASSWORD` | key password |

Locally, the same thing works by writing a `keystore.properties` file in the
project root with `storeFile`, `storePassword`, `keyAlias` and `keyPassword`.

Note that an APK signed with a different key cannot be installed over one signed
with the old key — you have to uninstall first.

## Building it yourself

```sh
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk
./gradlew assembleDebug     # app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 17 and the Android SDK (compile SDK 34).

## How it's put together

- Kotlin and Jetpack Compose, Material 3 with a custom warm paper-and-ink
  palette built around the logo orange.
- Drawings are JPEGs in the app's private storage plus a small JSON index;
  reminder preferences live in `SharedPreferences`. No database, no network
  layer.
- `AlarmManager` drives the reminder as a one-shot alarm that re-arms after
  every firing, so day-of-week schedules stay correct across daylight saving.
- `BootReceiver` restores the alarm after a reboot, an app update, or a clock
  change.

```
app/src/main/java/com/drawit/app/
├── data/         drawings + reminder settings
├── reminder/     alarm scheduling, notifications, boot restore
└── ui/
    ├── components/   buttons, cards, fields, bottom bar
    ├── screens/      today, inspiration, album, detail, add, reminder
    └── theme/        colour, type, shapes
```
