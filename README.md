# Do.it

A privacy-first daily task manager for Android.  
All data stays on device, encrypted at rest with SQLCipher + Android Keystore.

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

---

## Features

| Tab | Description |
|---|---|
| **Tasks** | Create, prioritise, complete or cancel up to 10 tasks per day. Swipe left to delete, right to complete. |
| **Routines** | Define recurring tasks (daily / weekdays / weekly / custom days). Injected automatically at the start of each day. |
| **Profile** | Display name, avatar colour, notification reminders, biometric lock, share daily progress. |
| **Widget** | Home-screen widget showing X/Y tasks done + current streak. Adapts to light/dark mode automatically. |

---

## Architecture

```
app/
├── data/
│   ├── db/                     Room database (SQLCipher AES-256)
│   │   ├── dao/                DAO interfaces (suspend functions)
│   │   ├── entity/             @Entity data classes
│   │   └── migration/          Migrations.kt — all DDL migrations
│   ├── LocalDataSource.kt      Single DB access point (singleton)
│   ├── TaskRepository.kt       Tasks + recurring injection logic
│   ├── RecurringTaskRepository.kt
│   └── UserRepository.kt       Profile + streak
├── model/                      Pure Kotlin domain models
│   ├── RecurrenceRule.kt       Frequency enum + bitmask isDueOn()
│   └── ...
├── ui/                         Fragments + ViewModels (StateFlow, no LiveData)
├── util/
│   ├── DateUtils.kt            ISO-8601 date helpers
│   ├── InjectionPrefs.kt       Guard preventing double injection per day
│   └── ...
├── widget/                     TaskWidgetProvider (coroutine-safe IO scope)
└── worker/                     MidnightResetWorker (WorkManager)
```

**Key choices:**
- **StateFlow** everywhere — no LiveData, collected via `repeatOnLifecycle(STARTED)`
- **SQLCipher** — AES-256 page-level encryption, key managed by Android Keystore
- **WorkManager** — reliable midnight reset; no BroadcastReceiver needed
- **activityViewModels()** — `TaskViewModel` shared between `TasksFragment` and `ProfileFragment`
- **Batch injection query** — recurring task injection uses one `SELECT DISTINCT` to avoid N+1

---

## Database versions

| Version | Change |
|---|---|
| 1 | Initial schema (tasks, sessions, profile, streak) |
| 2 | Add `tasks.priority` |
| 3 | Add `profile.biometric_lock_enabled` |
| 4 | Add `tasks.recurring_task_id` + create `recurring_tasks` table |
| 5 | Drop `friends` table (social feature removed) |

---

## Building

### Debug (no keystore needed)

```bash
./gradlew assembleDebug
```

### Release

1. Generate a keystore (one-time):

```bash
keytool -genkey -v \
  -keystore release.keystore \
  -alias doit \
  -keyalg RSA -keysize 2048 \
  -validity 10000
```

2. Copy `keystore.properties.example` → `keystore.properties` and fill in the values.

3. Build:

```bash
./gradlew bundleRelease   # signed AAB for Play Store
./gradlew assembleRelease # signed APK
```

---

## Tests

```bash
./gradlew test                        # JVM unit tests (no device)
./gradlew connectedAndroidTest        # instrumented tests (device/emulator)
./gradlew lint                        # lint
```

**Unit tests** (`src/test/`):
- `RecurrenceRuleTest` — all Frequency variants, edge cases
- `DateUtilsTest` — ISO date parsing, consecutive-day logic

**Migration tests** (`src/androidTest/`):
- `MigrationTest` — MIGRATION_3_4 and MIGRATION_4_5 verified with plain SQLite (no SQLCipher)

---

## CI/CD

GitHub Actions (`.github/workflows/ci.yml`):

| Job | Trigger | What it does |
|---|---|---|
| `test` | every push + PR | lint + unit tests, uploads reports as artifacts |
| `release` | push to `main`/`master` | builds signed AAB, uploads as artifact (30 days) |

See `.github/SECRETS.md` for the four required repository secrets.

---

## Localisations

| Locale | File |
|---|---|
| French (default) | `res/values/strings.xml` |
| English | `res/values-en/strings.xml` |

---

## Security notes

- `allowBackup="false"` — no ADB backup
- SQLCipher DB excluded from cloud backup and device transfer (`data_extraction_rules.xml`)
- `FLAG_SECURE` on the main window — no screenshot / recent apps preview
- No network permissions — 100% local, no Firebase, no analytics
- `applicationId` is `io.doit.app`
