# Copilot Instructions for EinkBro

EinkBro is an Android web browser for E-ink devices (e-readers). Built with Kotlin, Jetpack Compose, Room, and Koin DI.

## Build & Test Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build (with ProGuard)
./gradlew assembleRelease

# Debuggable release (no minification)
./gradlew assembleReleaseDebuggable

# Install on connected device
./gradlew installDebug

# Unit tests
./gradlew test

# Single module test
./gradlew :app:test
./gradlew :ad-filter:test

# Instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lintDebug
```

## Module Structure

- **`app/`** — Main browser application
- **`ad-filter/`** — Ad-blocking library (JS scriptlets + FilterViewModel)
- **`adblock-client/`** — Native C++ ad-blocking via CMake; `app` depends on `ad-filter`

## Architecture

### MVVM + Koin DI
All singletons are registered in `EinkBroApplication.kt` in the `myModule` koin module. Classes that need injection implement `KoinComponent` and use `by inject()`:

```kotlin
class MyClass : KoinComponent {
    private val config: ConfigManager by inject()
}
```

ViewModels also use `by inject()` rather than constructor injection in most cases.

### BrowserController Interface
`BrowserController` (`browser/BrowserController.kt`) is the central interface between `EBWebView` and `BrowserActivity`. WebView components call back to the activity via this interface — don't add direct activity references to WebView classes.

### ConfigManager & Preferences
All user settings go through `ConfigManager` (`preference/ConfigManager.kt`), which uses Kotlin property delegate classes (`BooleanPreference`, `StringPreference`, etc.) backed by `SharedPreferences`. Add new settings as delegated properties here, not raw SharedPreferences calls.

### Settings UI
Settings screens are driven by data — `SettingItemInterface` implementations (`BooleanSettingItem`, `ActionSettingItem`, `ListSettingWithEnumItem`, etc. in `setting/SettingComposeData.kt`) map directly to `ConfigManager` properties using `KMutableProperty0<T>` references.

### Enums for Configurable Actions
- `GestureType` (`view/GuestureType.kt`) — all configurable gesture actions
- `ToolbarAction` (`view/toolbaricons/ToolbarAction.kt`) — all toolbar button actions

When adding a new user-configurable action, add it to the appropriate enum.

### Mixed UI: Compose + Traditional Views
The app uses both Jetpack Compose and traditional Android Views. Compose components live in `view/compose/` and `view/viewControllers/` (via `AbstractComposeView`). Theme is defined in `view/compose/MyTheme.kt`. New UI should prefer Compose.

### Database
Two persistence layers exist:
- **Room** (`AppDatabase`) — modern entities: `Bookmark`, `Highlight`, `DomainConfiguration`, `ChatGptQuery`, `TranslationCache`. Schema files stored in `app/schemas/`. Always provide Room migrations when changing entities.
- **Legacy SQLite** (`RecordDb`) — history records via raw SQLite helper.

### Version Code Format
`versionCode` uses `MM_mm_pp` (e.g., `15_05_00` = v15.5.0). Update both `versionCode` and `versionName` together in `app/build.gradle.kts`.

## Key Conventions

- **Lint baseline**: `app/lint-baseline.xml` suppresses pre-existing lint issues. Only add to the baseline for pre-existing issues, not new ones.
- **`MissingTranslation` lint disabled** — translations are managed via Crowdin; never add string keys directly to non-default locale files.
- **ABI splits**: Release builds produce per-ABI APKs (arm64-v8a, armeabi-v7a, x86, x86_64) plus a universal APK.
- **ProGuard**: `proguard-rules.txt` for release; `proguard-rules.pro` for releaseDebuggable. If adding new reflective classes, update the appropriate rules file.
- **LeakCanary** is present but commented out in `build.gradle.kts` — do not enable in committed code.
- **`GlobalScope.launch`** is used in several legacy places; prefer `lifecycleScope` or `viewModelScope` in new code.
