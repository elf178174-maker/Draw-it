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

**Streak** — every day you add a drawing, the streak grows, and the save turns
into a small celebration: rings push out, paper confetti scatters, the number
rolls up, a warm chime plays and the phone buzzes in a pattern that builds as
the number lands. Milestones (3, 7, 14, 30, 50, 100, a year…) get a longer
version of all of it. Tap the streak card for the full picture — this week,
your longest run, the last ten weeks as a grid, and every milestone.

**Streak freezes** — you earn one freeze a week, up to three. Miss a day and a
freeze is spent for you automatically: the streak holds, it just doesn't grow
that day, and the app tells you what happened next time you open it. Run out of
freezes and a missed day ends the run, so they still matter.

**Backup** — Settings can export your whole album to a single `.zip`: every
photo, its name, its description, its date, and your streak. Import reads one
back on a new phone. Drawings you already have are skipped, so importing the
same file twice is safe.

**Today** — the home screen shows your streak and week at a glance, the next
reminder with a live countdown, a daily drawing prompt, and the drawings you
added most recently.

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
- Sound and vibration for the streak celebration are on by default and can be
  turned off independently in Settings.
- Exact alarms. On Android 13 and newer this is granted on install. On Android
  12 it is a setting, and the Reminder screen links straight to it. Without it
  Android parks the reminder while the phone is idle and only delivers it when
  something wakes the app, which is not a reminder.
- Because the reminder uses an alarm-clock alarm (the only kind Doze cannot
  defer), an alarm icon sits in the status bar while one is scheduled.
- If your phone is aggressive about battery — Samsung, Xiaomi, Oppo and others
  are — the Reminder screen offers to exempt the app. Use **Test the real
  alarm** there to confirm: it arms a genuine alarm 30 seconds out, so you can
  close the app and lock the phone to check it really arrives.

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
  reminder preferences, the streak and its freezes live in `SharedPreferences`.
  No database, no network layer.
- The streak is settled lazily: opening the app works out how many days were
  missed, spends that many freezes if it can, and otherwise resets. Days are
  local epoch days, so the streak turns over at your midnight, not UTC.
- Backups are a plain `.zip` — `manifest.json` plus a `photos/` folder — so the
  contents stay readable without the app.
- The celebration sounds are synthesised mallet tones generated for this
  project, not sampled audio; see `tools/make_sounds.py`.
- `AlarmManager` drives the reminder as a one-shot alarm that re-arms after
  every firing, so day-of-week schedules stay correct across daylight saving.
- `BootReceiver` restores the alarm after a reboot, an app update, or a clock
  change.

```
app/src/main/java/com/drawit/app/
├── data/         drawings, reminder settings, streak, backup zip
├── feedback/     the celebration's sound and haptics
├── reminder/     alarm scheduling, notifications, boot restore
└── ui/
    ├── components/   buttons, cards, fields, bottom bar, streak celebration
    ├── screens/      today, inspiration, album, detail, add, reminder,
    │                 streak, settings
    └── theme/        colour, type, shapes
```
