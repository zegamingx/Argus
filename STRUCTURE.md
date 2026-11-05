# Argus Repository Structure

This file records the **physical repository and Android source layout** at `main`. Kotlin package ownership and dependency direction are governed by documents 45–48; Gradle project topology is defined by the settings and build files.

## Physical repository layout

```text
Argus/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── java/com/argus/agi/
│       │       ├── app/
│       │       │   ├── bootstrap/
│       │       │   │   ├── ArgusContainer.kt
│       │       │   │   └── ArgusViewModelFactory.kt
│       │       │   ├── entry/
│       │       │   │   ├── ArgusApp.kt
│       │       │   │   └── MainActivity.kt
│       │       │   └── navigation/
│       │       │       └── MainScreen.kt
│       │       ├── application/
│       │       │   └── runtime/
│       │       │       ├── ArgusRuntime.kt
│       │       │       ├── AccessibilityRuntime.kt
│       │       │       ├── CaptureRuntime.kt
│       │       │       ├── DeviceRuntime.kt
│       │       │       ├── HardwareRuntime.kt
│       │       │       └── TrainingRuntime.kt
│       │       ├── data/
│       │       │   └── acquisition/
│       │       │       └── WebSearchEngine.kt
│       │       ├── feature/
│       │       │   ├── chat/presentation/
│       │       │   │   ├── ChatMessage.kt
│       │       │   │   ├── ChatScreen.kt
│       │       │   │   └── ChatUiState.kt
│       │       │   ├── learning/presentation/
│       │       │   │   ├── LearningUiState.kt
│       │       │   │   └── NeuralCoreScreen.kt
│       │       │   ├── perception/presentation/
│       │       │   │   ├── PerceptionUiState.kt
│       │       │   │   └── PerceptionScreen.kt
│       │       │   └── settings/presentation/
│       │       │       ├── SettingsUiState.kt
│       │       │       └── SkillsAndSettingsScreen.kt
│       │       ├── model/              # Four transitional mixed prototype classes
│       │       ├── platform/android/
│       │       │   ├── capture/
│       │       │   │   ├── AccessibilityActionRegistry.kt
│       │       │   │   ├── ArgusAccessibilityService.kt
│       │       │   │   └── ExcludedAppsManager.kt
│       │       │   ├── device/
│       │       │   │   ├── DeviceGuard.kt
│       │       │   │   └── HardwareProfile.kt
│       │       │   ├── lifecycle/
│       │       │   │   └── ArgusForegroundService.kt
│       │       │   └── notification/
│       │       │       └── ArgusNotificationChannels.kt
│       │       └── ui/
│       │           ├── ArgusViewModel.kt  # Transitional cross-feature ViewModel
│       │           └── design/
│       │               ├── Color.kt
│       │               ├── Theme.kt
│       │               └── Type.kt
│       ├── res/
│       └── test/java/com/argus/agi/
│           └── ArgusAppRobolectricTest.kt
├── docs/                         # Numbered engineering documents and index
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── public/assets/                # README/project assets, not Android runtime resources
├── .gitignore
├── CONTRIBUTING.md
├── LICENSE
├── NOTICE
├── README.md
├── SECURITY.md
├── STRUCTURE.md
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```

## Gradle project topology

The repository contains one Android application Gradle module:

- `:app` — Android application, production source, Android resources and current tests.
- Root Gradle files — plugin declarations and shared build configuration.
- `gradle/` — dependency version catalog and Gradle wrapper.

There are no separate Gradle modules named `:core`, `:application`, `:data`, `:engine`, `:feature`, `:platform` or `:ui`. These are logical package roots under `com.argus.agi`, not top-level Gradle projects.

## Current structural state

The repository has moved beyond the first package relocation:

- `app/bootstrap` owns the process-scoped composition root and ViewModel factory.
- `app/entry` contains the Android Application and Activity.
- `app/navigation` contains the current app shell and destination switching.
- Feature screens and presentation-owned immutable state types live under their respective `feature/*/presentation` packages. Chat owns `ChatMessage` and `ChatUiState`; Settings owns `SettingsUiState`; Learning owns `LearningUiState`; Perception owns `PerceptionUiState`.
- `application/runtime` keeps the transitional aggregate entry contract in `ArgusRuntime.kt` and separates training, device, hardware, capture and accessibility ports into focused files. It is not yet the final workflow-specific application API layout.
- Android capture, device, foreground lifecycle and notification adapters live under `platform/android/*`.
- The concrete web-search connector lives under `data/acquisition`.
- `model/` still contains four mixed legacy prototype classes. Their responsibilities must be separated before assigning final owners.
- `ui/ArgusViewModel.kt` remains a transitional cross-feature ViewModel and still reaches concrete model, data and platform classes. Settings, Learning and Perception now receive feature-owned immutable UI state projections; command handling and full ViewModel ownership remain to be migrated after workflow APIs are established.
- `ui/design/` contains shared Compose theme, color and typography resources.

The current layout is an **intermediate migration state**, not proof that every dependency boundary is enforced. The source ownership and dependency audit is recorded in [46 — Android Source-Tree Ownership and Dependency Audit](docs/46_ANDROID_SOURCE_TREE_OWNERSHIP_AND_DEPENDENCY_AUDIT.md); package placement rules and target ownership remain in [45](docs/45_ANDROID_PROJECT_STRUCTURE_AND_FILE_PLACEMENT_RULES.md) and [47](docs/47_PACKAGE_INTERFACE_AND_DEPENDENCY_CONTRACT_MATRIX.md).

## File-placement rules

1. Android components and manifest-declared classes stay in the Android application source set.
2. Production Kotlin/Java source belongs under `app/src/main/java/`, arranged by package ownership.
3. Android runtime resources belong under `app/src/main/res/`; tests belong in the matching test source set.
4. Version-catalog and wrapper configuration remain under `gradle/`; shared build configuration remains at the root or in reviewed build logic.
5. `public/assets/` is for repository/distribution assets. Files consumed by Android at runtime belong under `app/src/main/res/` or `app/src/main/assets/`.
6. Numbered engineering documents remain directly under `docs/`; stable paths and numbering are governed by [44 — Documentation Governance and Change Control](docs/44_DOCUMENTATION_GOVERNANCE_AND_CHANGE_CONTROL.md).
7. Add a Kotlin package only when a real declaration has a clear owner and callers. Do not recreate empty placeholder trees or one Gradle module per logical package.

## Migration checkpoints

- Initial package/path relocation: [commit b0441af8](https://github.com/zegamingx/Argus/commit/b0441af8df127897f3ef579c22116d85337feea6).
- Composition root and runtime ingress: [commit d9c3924](https://github.com/zegamingx/Argus/commit/d9c39243dac32570fdd4882aa8b07c1393933472).
- Removal of old service-locator/static-service access paths and transient-only capture ingress: [commit 8a9014c](https://github.com/zegamingx/Argus/commit/8a9014c7728c65124caa0a6204e16baab84c5cfd).

The runtime-port file split preserves package names, public type names and method signatures; it is a source-organization change only. No build or test result is implied. Further work remains on feature ViewModel decomposition, workflow API ownership, mixed model prototypes, platform permission/camera adapters, persistence and verification.
