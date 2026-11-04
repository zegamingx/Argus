# Argus Repository Structure

This file describes the **physical repository layout**. It is not a second definition of Kotlin package ownership or a proposal for Gradle modules.

## Current physical layout

```text
Argus/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/argus/agi/
│       │   │   ├── ArgusApp.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── model/
│       │   │   ├── sensor/
│       │   │   ├── service/
│       │   │   └── ui/
│       │   │       ├── ArgusViewModel.kt
│       │   │       ├── screens/
│       │   │       └── theme/
│       │   └── res/
│       └── test/
├── docs/
│   ├── 01–48 numbered engineering documents
│   └── README.md
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── public/
│   └── assets/
│       ├── DigiTransID.zip
│       ├── Reference_Booting.mp4
│       └── argus_icon.png
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

The source/resource/test subtrees above are intentionally summarized. Their exact current paths are visible in the repository tree.

## Gradle project topology

The repository currently contains one Android application Gradle project:

- `:app` — the Android application and its current production/test source sets.
- Root Gradle files — shared plugin declarations, dependency repositories, version catalog and wrapper configuration.

There are no included `:core`, `:application`, `:data`, `:engine`, `:feature`, `:platform` or `:ui` Gradle projects. Those names must not be represented as root-level module directories or created as empty module scaffolding.

## Kotlin package ownership

The intended logical package hierarchy belongs under the existing namespace:

```text
app/src/main/java/com/argus/agi/
```

The authoritative target package map, package responsibilities, dependency direction, source-file ownership crosswalk and Gradle extraction criteria are maintained in:

- [45 — Android Project Structure and File-Placement Rules](docs/45_ANDROID_PROJECT_STRUCTURE_AND_FILE_PLACEMENT_RULES.md)
- [46 — Android Source-Tree Ownership and Dependency Audit](docs/46_ANDROID_SOURCE_TREE_OWNERSHIP_AND_DEPENDENCY_AUDIT.md)
- [47 — Package Interface and Dependency Contract Matrix](docs/47_PACKAGE_INTERFACE_AND_DEPENDENCY_CONTRACT_MATRIX.md)

Those documents describe logical ownership; they do not imply that every proposed package or class already exists. A package directory is added only when real source is assigned to it. Gradle modules are extracted only when a demonstrated dependency, testing, build or ABI boundary justifies extraction.

## File-placement rules

1. Android entry points and manifest-declared components stay in the Android application source set.
2. Production Kotlin/Java source belongs under `app/src/main/java/`, organized by the agreed package ownership.
3. Android resources belong under `app/src/main/res/`; tests belong under the matching test source set.
4. Version-catalog and wrapper configuration remain under `gradle/`; shared build configuration remains at the root or in reviewed build logic.
5. Public project assets belong under `public/assets/` only when they are repository/distribution assets. Android runtime resources must be placed under `app/src/main/res/`.
6. Numbered engineering documents remain directly under `docs/`; their stable paths and numbering are governed by [44 — Documentation Governance and Change Control](docs/44_DOCUMENTATION_GOVERNANCE_AND_CHANGE_CONTROL.md).

## Migration status

The existing Kotlin implementation still uses its legacy `model/`, `sensor/`, `service/` and `ui/` packages. The ownership audit identifies classes that mix responsibilities and must be decomposed before they can be safely assigned to the target packages. In particular, a package rename alone does not remove the current dependency cycle or separate numerical execution, persistence, platform services and workflow state.

The empty placeholder trees that previously appeared at the repository root (and empty `app/bootstrap`, `app/entry` and `app/navigation` directories outside the Android source set) are not source packages or Gradle projects. They are removed rather than kept as speculative scaffolding.

This structural cleanup does not change application behavior, Kotlin implementation, Android manifest entries, resources, dependencies or Gradle project topology. Source-file migration remains a separate, dependency-aware structural change and must preserve the contracts in documents 36 and 45–48.
