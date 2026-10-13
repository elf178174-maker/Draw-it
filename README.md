# Draw it

An Android app that reminds you to draw, gives you somewhere to look for
ideas, and keeps a dated album of everything you finish. A streak flame that
evolves, chests of ink, an avatar to dress up and Foxy the fox make keeping the
habit feel good.

Everything lives on the phone. There is no account, no server, and nothing is
uploaded anywhere.

## What it does

**Today** is the home screen. It shows your streak flame, this week, today's
drawing prompt (or a Pinterest pin you picked to draw), the next reminder with
a live countdown, and your latest drawings. Your avatar, streak and ink sit in
the bar along the top.

**Streak** grows every day you add a drawing. Saving a drawing plays a
celebration: the flame ignites, the number rolls over, each day of the week
pops in on a rising scale, and Foxy the fox cheers you on. Extra drawings on a
day that already counted get their own shorter "saved" moment.

**The flame evolves** as the streak grows. There are nine looks in all: Ember
(not drawn yet), Spark, Blaze (7 days), Inferno (14), Azure (28), Cosmic (50),
Solar (100), Phoenix (200) and Eternal (365). Each new tier gets a full
evolution scene with a charge-up, a flash and the reveal.

**Goals** are at 3, 7, 14, 28, 50, 100, 150, 200, 365, 500, 730 and 1000 days.
Each one is a medal on the streak screen's goal path. Reaching it pays out a
chest of ink: the chest drops in, rattles, leaks light and bursts open.

**Ink** is the app's currency, earned by drawing:

- 20 for the day's first drawing, plus a streak bonus that grows with your flame
- 5 for each extra drawing, up to 3 a day
- 50 for a perfect Monday-to-Sunday week
- the goal chests

On first launch after updating, you get a welcome gift and back pay for every
drawing already in your album.

**Profile and avatar** gives you your own character, dressed from a shop of
over a hundred items: backgrounds, wings, skin tones, hairstyles and colours,
eyes, mouths, tops, glasses and face paint, hats, things to hold, and animated
auras. Items come in five rarities. Eight can only be won by streak, from the
Fire Aura at 7 days up to the Royal Robe at 365. You can try anything on before
buying. The shop also sells streak freezes.

**Streak freezes** protect your streak. You earn one a week and can buy more
with ink, up to three. Miss a day and a freeze is spent for you automatically:
the streak holds, it just doesn't grow that day, and the app tells you next
time you open it. If you run out, a missed day ends the run.

**Inspiration** is a native feed of Pinterest results with topic chips and a
search box. Tap a pin to see it big, save it, or set it as today's drawing.
Saved pins have their own tab. The full Pinterest site is still there under
**Browse** if the feed can't load.

**Album** keeps your finished drawings. Photograph a drawing (or pick a photo
you already took), name it, and it's saved with the date and time. The album
is a searchable grid; tap a drawing to see it full size, edit it or delete it.

**Reminder** lets you pick a time and your days. The app posts a notification
at that time. The alarm re-arms after each firing and survives reboots,
updates and time-zone changes.

**Backup** lives in Settings and exports everything to one `.zip`: every photo
with its name, description and date, plus your streak, ink, avatar, owned items
and saved pins. Import reads it back on a new phone.

- Backups made by Draw it 1.0 still import. Their drawings and streak come
  back, and you're paid the ink they would have earned.
- Drawings you already have are skipped, so importing the same file twice is
  safe.

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
- Sound and vibration are on by default. You can turn each off separately in
  Settings, which also has a sound check.
- The Inspiration feed needs an internet connection. Everything else works
  offline.
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

- Kotlin and Jetpack Compose, with a chunky Duolingo-inspired design system:
  the Nunito font, 3D-press buttons, and a palette built around the logo
  orange.
- All the characters and rewards are drawn in code on a Canvas, not shipped as
  images: Foxy, the avatar and its items, flames, chests, medals and the ink
  drop. One shared frame clock drives their animation, so they redraw without
  recomposing.
- Drawings are JPEGs in the app's private storage plus a small JSON index. The
  streak, wallet, profile and settings live in `SharedPreferences`. There is
  no database, account or server.
- The streak is settled lazily. Opening the app works out how many days were
  missed, then spends freezes if it can and otherwise resets the streak. Days
  are local epoch days, so the streak turns over at your midnight, not UTC.
- The ink rules live in `model/Economy.kt`. `data/ProgressManager.kt` applies
  them when a drawing is saved.
- Backups are a plain `.zip` (`manifest.json` plus a `photos/` folder), so the
  contents stay readable without the app. `data/BackupFormat.kt` writes
  format 2 and reads both 1 and 2.
- The Pinterest feed loads pinterest.com in a hidden WebView and reads its
  search results through a JavaScript bridge, then shows them in a native
  grid. If Pinterest changes its page, the feed falls back to reading the page
  itself, and the Browse tab always works.
- Every sound is synthesised for this project, not sampled: bells, marimba,
  glockenspiel and noise sweeps, rendered to OGG by `tools/make_sounds.py`.
  Haptics use Android's composition primitives where the phone supports them.
- `AlarmManager` drives the reminder as a one-shot alarm that re-arms after
  every firing, so day-of-week schedules stay correct across daylight saving.
  `BootReceiver` restores it after a reboot, an update or a clock change.

```
app/src/main/java/com/drawit/app/
├── data/         drawings, streak, wallet, profile, saved pins, backup zip
├── model/        flame tiers and the ink economy
├── feedback/     sounds and haptics
├── reminder/     alarm scheduling, notifications, boot restore
└── ui/
    ├── art/          Foxy, the avatar and its items, flames, chests, medals
    ├── celebrate/    the after-drawing celebration, welcome and streak notices
    ├── components/   buttons, cards, top bar, bottom bar, week strip, confetti
    ├── inspire/      the Pinterest feed
    ├── screens/      today, inspiration, album, detail, add, reminder,
    │                 streak, profile, shop, settings
    └── theme/        colour, type
```
