# Swalekhi — Finance Tracker

*Your money, Your records.*

A native Android app for tracking monthly income and expenses, predicting how much you'll
spend by the end of the month, and staying on top of budgets, recurring bills, and trends.

> Internally, the code still uses the project's original working name ("Spendly") for the
> package (`com.varnilix.spendly`) and a handful of class names (`SpendlyApp`, `SpendlyTheme`,
> etc.) — none of that is user-visible. Every screen, notification, and dialog the app actually
> shows now says **Swalekhi**. Ask if you'd like the internal names renamed to match too.

## Features

**Dashboard**
- Month-end spend projection, blending a whole-month run-rate with a recent-days rate
  (weighted more heavily) and per-weekday spending patterns learned from your history.
- Browse past months with the ‹ › month selector — not locked to the current month.
- Income / Expense / Net summary for the month.
- Optional overall monthly budget, plus **per-category budgets** with their own progress bars.
- Category breakdown with a donut chart.

**Transactions**
- Add/edit as Expense or Income, with amount, category, account, date, note, and an optional
  **receipt photo** (picked from gallery, copied into private app storage).
- Categories: built-in expense + income categories, plus your own (name, color, icon, optional
  monthly budget). Categories in use can't be deleted; unused custom ones can be.
- Accounts/wallets (Cash, Bank, or your own) to track where money moved from/to.
- **Recurring transactions** — rent, subscriptions, salary — auto-logged daily by a background
  worker, with weekly or monthly cadence.

**History**
- Every transaction grouped by day, filterable by category and account, searchable by note.
- Swipe left to delete, with an Undo snackbar.

**Insights**
- Month-over-month bar chart (last 6 months) and a per-category trend chart.

**Currency**
- Pick from 15 currencies (defaults to Indian Rupee ₹) — changes the displayed symbol only,
  no conversion.

**Security & reminders** (all in Settings, all optional)
- App lock via fingerprint/face/PIN (`BiometricPrompt`, gated at app start).
- Daily reminder notification to log expenses, at a time you choose.
- Budget alerts at 80% / 90% / 100% of your monthly budget, once per threshold per month.

**Data**
- Export/import everything as CSV (portable backup, also opens in a spreadsheet).
- Share a given month's report (CSV with a totals summary) via any share target.
- Everything is stored locally on-device (Room/SQLite) — no account, no internet required.

**Other**
- A 3-page onboarding walkthrough on first launch.
- A home-screen widget showing this month's projected spend and spend-so-far.

## How the prediction works

See `prediction/SpendingPredictor.kt`. In short:

1. **Recent rate** = average daily spend over the last 7 days (or fewer, early in the month).
2. **Month rate** = average daily spend over the whole month so far.
3. **Blended rate** = 60% recent + 40% month (pure month rate until 7 days of data exist).
4. Each *remaining* day of the month is projected at `blended rate × that weekday's learned
   multiplier` (e.g. if Saturdays run 40% above your average day historically, remaining
   Saturdays are scaled up) — multipliers default to 1.0 until ~2 weeks of history exist.
5. `projected total = spent so far + sum of projected remaining days`.

## Project structure

```
app/src/main/java/com/varnilix/spendly/
├── data/              Room entities, DAOs, database, repository, settings
├── prediction/         SpendingPredictor — the month-end projection math
├── util/               Date/currency formatting, CSV backup, biometric auth, receipt storage
├── work/                WorkManager workers (recurring transactions, daily reminder),
│                        notifications, recurrence date math, boot receiver
├── widget/              Home-screen widget (Glance)
└── ui/
    ├── theme/           Material 3 theme (light/dark, dynamic color on Android 12+)
    ├── navigation/      Nav graph + bottom bar (Home / History / Insights / Settings)
    ├── components/      Shared composables (category badges, donut/bar charts, stat cards)
    ├── dashboard/        Home screen
    ├── addexpense/       Add/Edit transaction screen
    ├── history/          Transaction history (grouped, filterable, searchable, swipeable)
    ├── categories/        Category list + create/edit
    ├── accounts/          Account list + create/edit
    ├── recurring/         Recurring rule list + create/edit
    ├── insights/          Trend charts
    ├── settings/          Settings hub (budget, currency, lock, reminders, export/import)
    ├── onboarding/        First-run walkthrough
    └── lock/              App-lock screen
```

Stack: Kotlin, Jetpack Compose, Material 3, Room, Navigation Compose, WorkManager, Biometric,
Glance (widget). No backend, no third-party analytics. minSdk 26 (Android 8.0+).

## Building the APK

Standard Gradle Android project — open in **Android Studio** and hit Run, or from the command line:

```bash
# Windows
gradlew.bat assembleDebug

# macOS/Linux
./gradlew assembleDebug
```

The debug APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

## Installing on your phone

**Option A — USB (recommended for development):**
1. On your phone: Settings → About phone → tap "Build number" 7 times to enable
   Developer options, then Settings → Developer options → enable **USB debugging**.
2. Connect the phone via USB and accept the "Allow USB debugging?" prompt.
3. Run: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

**Option B — copy the APK directly:**
1. Copy `app-debug.apk` to the phone (USB file transfer, email, Drive, etc.).
2. Open it on the phone, allow installs from that source once, tap Install.

**Option C — release build for wider distribution:**
```bash
gradlew.bat assembleRelease
```
Unsigned by default; sign it with your own keystore before distributing outside your own devices.

## Notes / known trade-offs

- The Room schema uses `fallbackToDestructiveMigration()` — appropriate pre-release, but means
  a future schema change on an already-installed app will wipe local data unless a proper
  `Migration` is added first.
- The home-screen widget is intentionally plain (no custom colors) to stay within Glance's
  RemoteViews-based styling constraints, which are stricter than regular Compose.
- The daily reminder and recurring-transaction check use `PeriodicWorkRequest` at ~24h
  intervals; exact fire time can drift by the OS's discretion (Doze/battery optimization),
  which is expected WorkManager behavior for non-critical background work.
