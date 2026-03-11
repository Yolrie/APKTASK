# CLAUDE.md — AI Assistant Guide for APKTASK

## Project Overview

**APKTASK** (branded as "Do.it") is a native Android task manager application written in Java. It helps users manage daily tasks with a 24-hour lifecycle — all tasks reset automatically at midnight.

- **Language:** Java
- **Platform:** Android (min SDK 28 / Android 9, target SDK 36 / Android 15)
- **Architecture:** Single-Activity, no external architecture framework (no MVVM, no MVP)
- **UI:** Manual LinearLayout inflation (no RecyclerView, no Jetpack Compose)
- **Persistence:** SharedPreferences with hand-rolled JSON serialization (no Room, no SQLite library)
- **Build system:** Gradle 9.1.0 with Android Gradle Plugin 9.0.0

---

## Repository Structure

```
APKTASK/
├── app/
│   ├── build.gradle                    # App-level dependencies and SDK config
│   ├── proguard-rules.pro              # ProGuard rules (currently minimal)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml     # Permissions, activities, broadcast receivers
│       │   ├── java/com/example/apktask/
│       │   │   ├── MainActivity.java   # All UI logic and app state
│       │   │   ├── Task.java           # Task data model (POJO)
│       │   │   └── MidnightReceiver.java # BroadcastReceiver for daily reset
│       │   └── res/
│       │       ├── layout/
│       │       │   ├── activity_main.xml  # Main screen layout
│       │       │   └── item_task.xml      # Individual task row template
│       │       ├── values/             # Strings, colors, themes (light)
│       │       └── values-night/       # Dark theme overrides
│       ├── androidTest/                # Instrumentation tests (Espresso)
│       └── test/                       # Unit tests (JUnit)
├── gradle/
│   ├── libs.versions.toml              # Centralized dependency version catalog
│   └── wrapper/gradle-wrapper.properties
├── build.gradle                        # Root build config
├── settings.gradle                     # Module inclusion and repo config
├── gradle.properties                   # JVM flags, AndroidX opt-in
├── gradlew / gradlew.bat               # Gradle wrapper scripts
└── README.txt                          # User-facing documentation
```

---

## Build and Development Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumentation tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Full build (includes test compilation)
./gradlew build

# Clean build artifacts
./gradlew clean
```

The output APK is placed in `app/build/outputs/apk/`.

---

## Key Source Files

### `Task.java` — Data Model

A simple POJO with these fields:

| Field | Type | Description |
|-------|------|-------------|
| `id` | `int` | Unique identifier |
| `title` | `String` | Task text |
| `isDone` | `boolean` | Completion flag |
| `createAt` | `String` | Creation timestamp |
| `status` | `int` | Task state (see below) |

**Task status values:**
- `0` — Draft (being entered, not yet validated)
- `1` — In Progress (validated, active)
- `2` — Completed (marked done)
- `3` — Cancelled

### `MainActivity.java` — Core Logic

All UI management lives here. Key methods:

| Method | Purpose |
|--------|---------|
| `ajouterTache()` | Adds a new blank task input row to the UI |
| `afficherTaches()` | Re-renders all tasks into their correct zone based on status |
| `enregistrerTaches()` | Validates draft tasks and persists them |
| `reinitialiserTout()` | Clears all tasks and resets UI |
| `sauvegarderTaches()` | Serializes `ArrayList<Task>` to JSON in SharedPreferences |
| `chargerTaches()` | Deserializes tasks from SharedPreferences |
| `mettreAJourCompteurs()` | Updates the counter badges (in progress / done / cancelled) |
| `afficherTitresZones()` | Toggles zone header visibility based on task counts |

**State flag:** `isEnregistree` (boolean) tracks whether the current task list has unsaved draft rows.

### `MidnightReceiver.java` — Daily Reset

A `BroadcastReceiver` registered for `Intent.ACTION_DATE_CHANGED`. When fired at midnight, it clears all SharedPreferences, effectively resetting the task list.

---

## UI Architecture

The main layout has three **zones** (LinearLayouts), each acting as a container for task rows:

1. **Zone In Progress** — status 1 tasks
2. **Zone Completed** — status 2 tasks
3. **Zone Cancelled** — status 3 tasks

Draft tasks (status 0) appear in the In Progress zone while being entered.

Task rows are inflated from `item_task.xml` and added/removed programmatically. Each row contains:
- `EditText` for the task title
- An action `Button` that cycles through states: **Ajouter → Modifier → Valider → Done**
- A secondary `Button` for delete / cancel actions

Task views use `setTag(task.getId())` to safely identify tasks during deletion — do **not** match by title string.

---

## Code Conventions

- **Language of identifiers:** Method and variable names are in French (e.g., `ajouterTache`, `chargerTaches`, `reinitialiserTout`). Follow this convention for any new methods.
- **Package:** `com.example.apktask`
- **Naming:** PascalCase for classes, camelCase for methods and variables.
- **Java version:** Source/target compatibility set to Java 11.
- **No third-party JSON library:** JSON is built and parsed manually using string operations. Do not introduce GSON, Jackson, or Moshi unless explicitly requested.
- **No architecture library:** No ViewModel, LiveData, or Repository pattern. Keep logic in MainActivity unless refactoring is explicitly requested.
- **Strings:** User-visible strings go in `res/values/strings.xml` (in French). Do not hardcode UI strings in Java.

---

## Dependencies

Managed via `gradle/libs.versions.toml`:

| Library | Version | Purpose |
|---------|---------|---------|
| `androidx.appcompat` | 1.6.1 | Backwards-compatible Activity/Fragment support |
| `com.google.android.material` | 1.10.0 | Material Design components |
| `androidx.activity` | 1.8.0 | Modern Activity API |
| `androidx.constraintlayout` | 2.1.4 | ConstraintLayout |
| `junit` | 4.13.2 | Unit testing |
| `androidx.test.espresso` | 3.5.1 | Instrumentation testing |

To add a dependency, add the version to `libs.versions.toml` and reference it from `app/build.gradle`.

---

## Testing

- **Unit tests** live in `app/src/test/` — run with `./gradlew test`.
- **Instrumentation tests** live in `app/src/androidTest/` — run with `./gradlew connectedAndroidTest` (requires an attached device or running emulator).
- Currently minimal test coverage exists; new features should include unit tests where the logic is testable without Android framework dependencies.

---

## Git Workflow

- Main development branch: `main` / `initialfeat` (merged via PRs)
- Commit message style observed in history: `type: description` (e.g., `feat:`, `fix:`, `style:`, `docs:`)
- PRs are used to merge feature work back to main.

---

## Common Pitfalls

1. **Task deletion:** Always use `view.getTag()` to retrieve the task ID. Never delete by matching title strings — titles can be duplicated.
2. **SharedPreferences JSON:** The serialization is hand-rolled substring parsing. If adding new fields to `Task`, update both `sauvegarderTaches()` and `chargerTaches()` in MainActivity.
3. **Task limit:** The app enforces a maximum of 10 concurrent tasks. This check is in `ajouterTache()`.
4. **Empty task guard:** `enregistrerTaches()` rejects tasks with empty titles.
5. **MidnightReceiver registration:** The receiver is declared in `AndroidManifest.xml` — it does not need to be registered programmatically.
6. **Zone visibility:** After any task status change, call `afficherTitresZones()` and `mettreAJourCompteurs()` to keep headers and counters consistent.
