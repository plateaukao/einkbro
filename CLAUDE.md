# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

EinkBro is an Android web browser specifically designed for E-ink devices like e-readers. It's built with Kotlin and uses modern Android development practices including Jetpack Compose, Room database, and MVVM architecture with Koin dependency injection.

## Build Commands

### Development
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Build releaseDebuggable variant (for debugging release builds)
./gradlew assembleReleaseDebuggable

# Install debug on connected device
./gradlew installDebug

# Clean build
./gradlew clean
```

### Testing
```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Run lint checks
./gradlew lint
```

### Code Quality
```bash
# Run ProGuard analysis (implicit in release builds)
./gradlew assembleRelease

# Check for lint issues
./gradlew lintDebug
```

## Architecture Overview

### Multi-Module Structure
- **app/**: Main browser application module
- **ad-filter/**: Custom ad-blocking library with ViewModel architecture  
- **adblock-client/**: Native C++ ad-blocking implementation using CMake

### Core Architecture Patterns
- **MVVM**: ViewModels with LiveData/StateFlow for UI state management
- **Repository Pattern**: Data layer abstraction (e.g., `OpenAiRepository.kt`)
- **Dependency Injection**: Koin for DI container management
- **Room Database**: Local data persistence with KSP annotation processing

### Key Components
- **EinkBroApplication.kt**: Application class with Koin DI setup
- **BrowserActivity.kt**: Main activity (singleInstance launch mode)
- **EBWebView.kt**: Custom WebView with E-ink optimizations
- **ChatGPT Integration**: AI-powered web content interaction via OpenAI API

### E-ink Specific Features
- Gesture-based navigation (tap left/right for page navigation)
- Volume key page turning functionality
- Reader mode with custom CSS rendering
- Vertical reading mode for CJK languages
- High contrast UI elements optimized for E-ink displays

## Development Workflow

### Module Dependencies
The main app module depends on the ad-filter module, which contains:
- JavaScript scriptlets for ad filtering
- FilterViewModel for managing filter state
- Custom ad-blocking logic integrated with the native adblock-client

### Build Configuration
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14) 
- **JVM Target**: Java 17
- **Kotlin**: 2.0.0 with KSP for annotation processing
- **ABI Splits**: Supports arm64-v8a, armeabi-v7a, x86, x86_64

### Database Schema
Room database schemas are stored in `app/schemas/` directory. When modifying database entities, ensure schema migration paths are properly defined.

### Internationalization
The project supports 25+ languages managed through Crowdin. Translation files are in standard Android `res/values-*/strings.xml` format.

### ProGuard Configuration
Release builds use ProGuard for code obfuscation and optimization. Rules are defined in:
- `app/proguard-rules.txt` (release builds)
- `app/proguard-rules.pro` (releaseDebuggable builds)

## Key Development Areas

### WebView Customization
E-ink specific WebView optimizations are implemented in `EBWebView.kt` including:
- Custom scroll behavior for E-ink refresh patterns
- Reader mode CSS injection
- JavaScript interface for native feature integration

### AI Integration
ChatGPT functionality is implemented through:
- `GptActionsActivity.kt`: UI for AI interactions
- `OpenAiRepository.kt`: API communication layer
- `ChatGptQuery.kt`: Database entity for query persistence

### EPUB Processing
Complete EPUB reading functionality using EPubLib:
- EPUB parsing and content extraction
- Custom rendering for E-ink displays
- Export web content to EPUB format

### Translation Services
Multi-provider translation support with integration for various translation APIs.

## Testing Strategy

The project has limited test coverage but includes:
- AndroidJUnitRunner configuration for instrumented tests
- Test resources in the ad-filter module
- Debug tools including optional LeakCanary integration (commented out)

## Release Process

The project uses Fastlane for deployment automation and supports F-Droid distribution. Version codes follow the pattern `MM_mm_pp` (major_minor_patch) with current version 14.7.0 (versionCode: 14_07_00).