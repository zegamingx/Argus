# Argus Repository Structure

This file describes the **physical repository and Android source layout** at `main`. Kotlin package ownership and dependency direction are governed by documents 45–48; Gradle project topology is defined by the settings and build files.

## Physical repository layout

```text
Argus/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/argus/agi/
│       │   │   ├── app/
│       │   │   │   ├── entry/       # ArgusApp, MainActivity
│       │   │   │   └── navigation/  # MainScreen and screen routing
│       │   │   ├── data/
│       │   │   │   └── acquisition/ # WebSearchEngine prototype connector
│       │   │   ├── feature/
│       │   │   │   ├── chat/presentation/
│       │   │   │   ├── learning/presentation/
│       │   │   │   ├── perception/presentation/
│       │   │   │   └── settings/presentation/
│       │   │   ├── model/           # Legacy mixed prototype; not target ownership
│       │   │   ├── platform/android/
│       │   │   │   ├── capture/     # Accessibility and installed-app adapters
│       │   │   │   ├── device/      # Device and hardware observations
│       │   │   │   └── lifecycle/   # Foreground service adapter
│       │   │   └── ui/
│       │   │       ├── ArgusViewModel.kt  # Transitional monolithic ViewModel
│       │   │       └── design/     # Shared Compose theme and typography
│       │   └── res/
│       └── test/
├── docs/                            # Numbered engineering documents and index
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── public/assets/                   # Repository/distribution assets
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

The repository contains one Android application Gradle project:

- `:app` — Android application, production source and tests.
- Root Gradle files — shared plugin declarations, dependency repositories and root build settings.
- `gradle/` — dependency version catalog and wrapper configuration.

There are no separate Gradle projects named `:core`, `:application`, `:data`, `:engine`, `:feature`, `:platform` or `:ui`. These are logical package roots under `com.argus.agi` when source is assigned to them; they are not top-level Gradle modules.

## Package ownership and migration state

The initial bounded source migration placed the Android entry/navigation layer, feature screens, shared UI design, Android capture/device/lifecycle adapters and the concrete web-search connector under their agreed logical package roots.

Four legacy model files remain under `model/`: `ArgusNeuralEngine`, `Tokenizer`, `TrainingManager` and `WorldModel`. They combine responsibilities that must be separated before final ownership can be assigned safely. `ui/ArgusViewModel.kt` also remains as a transitional monolithic Android ViewModel; feature screens still consume it and are not yet backed by separate workflow APIs.

The current package layout is therefore an **intermediate migration state**, not a claim that the full architecture or dependency inversion is implemented. The source ownership audit and migration order are in:

- [45 — Android Project Structure and File-Placement Rules](docs/45_ANDROID_PROJECT_STRUCTURE_AND_FILE_PLACEMENT_RULES.md)
- [46 — Android Source-Tree Ownership and Dependency Audit](docs/46_ANDROID_SOURCE_TREE_OWNERSHIP_AND_DEPENDENCY_AUDIT.md)
- [47 — Package Interface and Dependency Contract Matrix](docs/47_PACKAGE_INTERFACE_AND_DEPENDENCY_CONTRACT_MATRIX.md)
- [48 — Personal Capture and Experience Intake Contract](docs/48_PERSONAL_CAPTURE_AND_EXPERIENCE_INTAKE_CONTRACT.md)

## File-placement rules

1. Android entry points and manifest-declared components stay in the Android application source set.
2. Production Kotlin/Java source belongs under `app/src/main/java/`, organized by package ownership.
3. Android resources belong under `app/src/main/res/`; tests belong under the matching test source set.
4. Version-catalog and wrapper configuration remain under `gradle/`; shared build configuration remains at the root or in reviewed build logic.
5. Public project assets belong under `public/assets/` only when they are repository/distribution assets. Android runtime resources belong under `app/src/main/res/`.
6. Numbered engineering documents remain directly under `docs/`; stable paths and numbering are governed by [44 — Documentation Governance and Change Control](docs/44_DOCUMENTATION_GOVERNANCE_AND_CHANGE_CONTROL.md).
7. Add a Kotlin package only when a real declaration has an owner and callers. Do not recreate empty placeholder trees or one Gradle module per logical package.

## Migration checkpoint

The initial package relocation is recorded in commit [`b0441af8`](https://github.com/zegamingx/Argus/commit/b0441af8df127897f3ef579c22116d85337feea6). It changed source paths/package declarations and Android manifest class names, but did not intentionally change runtime behavior, dependencies, Gradle topology, model algorithms or data schemas.

No build or test result is claimed for this checkpoint. The remaining work is dependency inversion and decomposition of the mixed prototype classes, followed by verification of each bounded change.
