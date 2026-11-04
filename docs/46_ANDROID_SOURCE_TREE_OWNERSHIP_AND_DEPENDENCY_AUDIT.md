# Argus Android Source-Tree Ownership and Dependency Audit

**Status:** Static structural audit complete for inspected source tree; remediation not authorized  
**Audit date:** 2026-11-04  
**Repository:** `zegamingx/Argus` · **Branch:** `main`  
**Audit baseline:** `bd75ac1533e079bfca4d12e1af9ec3170d7187d9` (`docs: clarify structure proposal authority`)  
**Scope:** Production Kotlin files, Android manifest, source sets, resources, Gradle topology/configuration and file-to-target ownership  
**Authority:** Observed source facts and structural findings for this snapshot. Target package rules remain in document 45; logical subsystem contracts remain in document 36.  
**Boundary:** Static repository inspection only. No source file was moved, created or modified; no Gradle configuration was changed; no build, test, workflow or device experiment was run.

## 1. Executive result

The inspected Android project is a single-module prototype. Its package names identify broad areas, but the current structure does not enforce the boundaries described by the architecture and lifecycle contracts.

The main structural findings are:

- `ArgusViewModel` directly reaches chat, neural execution, training, memory, hardware, file import, network search, app exclusion and AccessibilityService actions.
- The observed package graph contains a source-level cycle: `com.argus.agi` → `com.argus.agi.ui` → `com.argus.agi.service` → `com.argus.agi`. It is hidden inside the single `:app` Gradle module and must be broken before those boundaries can be extracted.
- `TrainingManager` combines queue state, worker lifecycle, device admission, model execution and memory updates.
- `ArgusNeuralEngine` combines numerical execution, tokenizer mutation, benchmark reporting and Android checkpoint file I/O.
- `model/TrainingManager` imports `sensor.DeviceGuard`, reversing intended dependency direction.
- Android services access global objects through `ArgusApp`; AccessibilityService also exposes a static live-instance action path.
- Compose screens directly use CameraX, Android settings/permission APIs and platform service classes.
- The single test file does not represent most documented subsystem boundaries.

**Recommendation:** retain one Gradle application module during the first package-boundary migration. Establish package ownership and dependency direction first. Extract Gradle modules only after stable APIs and concrete build/test/ABI boundaries exist. Creating many empty modules now would add complexity without proving the boundaries are correct.

This is a static structural audit, not a mathematical correctness, privacy-compliance, API deprecation or product-readiness audit. Those remain governed by documents 15 and 17–19 and the relevant technical contracts.

## 2. Baseline and method

The recursive Git tree and all production Kotlin files were re-read at `main` HEAD `bd75ac1533e079bfca4d12e1af9ec3170d7187d9`. The current source inventory was compared with the earlier source snapshot `24b26b061574700d82fecd5c2a1485353333cef8`: the 24 intervening commits changed project/documentation guidance, README/CONTRIBUTING and added `public/assets/Reference_Booting.mp4`; no Kotlin source, Android manifest, Gradle build/settings/catalog or Android resource path changed. The target package proposal has since been revised in document 45, so the observed source graph was checked against that current target rather than assumed to have changed with the documents.

The audit read package declarations/imports/top-level declarations, direct references in principal orchestrator/service classes, Android manifest registrations, Gradle settings/catalog/build scripts, the existing test file and the Android resource tree. It then built the package graph from explicit imports and direct cross-package references, including the root package's two distinct roles (`ArgusApp` and `MainActivity`).

This is a static dependency audit. It identifies declared imports, direct references, manifest registrations and Gradle declarations. It does not prove every dynamic call path, reflection-based use or Android runtime behavior. No build or test result is claimed.

## 3. Current physical project layout

```text
Argus/
├── .gitignore
├── CONTRIBUTING.md
├── LICENSE / NOTICE / SECURITY.md
├── README.md
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
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
│       │   └── res/
│       └── test/java/com/argus/agi/
│           └── ArgusAppRobolectricTest.kt
├── docs/
│   └── engineering documents
└── public/assets/argus_icon.png
```

| Item | Current state |
|---|---|
| Gradle project name | `A.R.G.U.S` |
| Included modules | `:app` only |
| Namespace/application ID | `com.argus.agi` |
| Native Android source | No `src/main/cpp/` in inspected tree |
| Unit-test source | One Kotlin test file |
| Checked-in instrumentation tests | No `app/src/androidTest/` files |
| Separate core/data/platform/engine modules | None |
| Benchmark module | None |
| Version catalog | `gradle/libs.versions.toml` |

A one-module prototype is not automatically wrong. The limitation is that package dependencies compile inside one application module and are enforced only by convention.

## 4. File-by-file ownership audit

Target paths below refer to document 45. They are design destinations, not existing folders or move instructions.

### 4.1 Application entry and navigation

| Current file | Current responsibility | Target owner | Finding |
|---|---|---|---|
| `ArgusApp.kt` | Application subclass, notification-channel setup, global lazy objects for engine, world model, device guard, hardware profile and training manager | `app/entry` + `app/bootstrap` | Keep the Android `Application` entry point separate from dependency-graph construction. Static companion properties act as a global service locator and must be replaced only in an approved refactor. |
| `MainActivity.kt` | Activity, `ArgusScreen`, `MainScreen`, bottom navigation, Compose root and runtime permission requests | `app/entry` + `app/navigation` | Keep the Activity as the Android host; move route definitions/destination wiring to `app/navigation` and keep permission launchers at the Android presentation/platform edge. |

### 4.2 Current `model/` package

| Current file | Current responsibility | Target owner | Finding |
|---|---|---|---|
| `model/ArgusNeuralEngine.kt` | `BenchmarkResult`, model parameters, forward/training/generation, benchmark and checkpoint load/save | `engine/reference`; contracts in `core/model`, `core/inference`, `core/training`; persistence in `data/model` | Combines numerical execution, tokenizer growth, benchmark reporting and Android `Context`/file I/O. High-priority ownership split. Experimental, not a selected production engine. |
| `model/Tokenizer.kt` | `Tokenizer`, special-token inventory, token maps, encode/decode and token registration | `core/tokenizer` contract + selected `engine/tokenizer` implementation | Tokenizer identity/API must be distinct from implementation. Mutable runtime vocabulary registration remains gated by G1-D; do not freeze it through a package move. |
| `model/TrainingManager.kt` | `TrainingStatus`, `TrainingData`, queue/state flows, coroutine worker, device-safety check, neural step and world-model update | `application/learning` + `core/training`; Android scheduling in `platform/android/lifecycle`; numerical step in `engine/reference` or the later selected engine | Mixes workflow orchestration, queue state, resource admission and numerical execution. Imports `sensor.DeviceGuard`, reversing intended dependency direction. |
| `model/WorldModel.kt` | `WorldEntity`, `ReflectionEvaluation`, in-memory entities, screen/camera context and response reflection | `core/knowledge` + `data/knowledge`; updates through application use cases | Memory/knowledge concept sits in `model/` and accepts Android `Context`. Domain records/policy and durable repository ownership are not separated. |

### 4.3 Current `sensor/` package

| Current file | Current responsibility | Target owner | Finding |
|---|---|---|---|
| `sensor/DeviceGuard.kt` | `DeviceSafetyState`, battery broadcasts, charging/power observations and training-safety flow | `platform/android/device` + `core/device`; admission in the owning application workflow | Android broadcast code is platform-specific. Stable hardware capability and short-lived runtime conditions must be separate contracts. |
| `sensor/HardwareProfile.kt` | `SystemHardwareInfo`, Android system properties, SoC/CPU heuristics and Vulkan feature flag | `platform/android/device` | Platform ownership is appropriate, but output is heuristic identification rather than verified GPU/NPU execution capability. Core capability records belong in `core/device`. |
| `sensor/ExcludedAppsManager.kt` | `InstalledAppItem`, installed-app enumeration and exclusion state | `platform/android/capture` + `data/policy` + `core/policy` | Mixes PackageManager metadata with user exclusion state. Separate platform discovery, durable user exclusions and policy decisions. |
| `sensor/WebSearchEngine.kt` | `SearchResultItem`, OkHttp requests to public search endpoints and result parsing | `data/acquisition` + `core/acquisition` | Concrete network transport/parser is misclassified as a sensor. It must not own source permission, retention or training eligibility; Android connectivity observation is a separate platform adapter. |

### 4.4 Current `service/` package

| Current file | Current responsibility | Target owner | Finding |
|---|---|---|---|
| `service/ArgusAccessibilityService.kt` | Accessibility events/node traversal, exclusion checks, screen-context forwarding and training enqueue | `platform/android/capture` | Directly constructs `ExcludedAppsManager`, accesses `ArgusApp.worldModel/trainingManager`, and exposes static live-instance click actions. Future integration should use app-owned capture/action facades and explicit policy checks. |
| `service/ArgusForegroundService.kt` | Foreground notification, service actions, periodic notification refresh and pause/resume | `platform/android/lifecycle` + `platform/android/notification` | Directly reads global training/device/hardware objects and controls the concrete manager. It should observe an application-owned session facade; scheduling and training policy remain separate. |

### 4.5 Current `ui/` package

| Current file | Current responsibility | Target owner | Finding |
|---|---|---|---|
| `ui/ArgusViewModel.kt` | `ChatMessage`, chat/generation, training, benchmark, file/image import, camera context, web search, accessibility click, app exclusion and UI state flows | Split into `feature/*/presentation`; workflows in `application` | Highest-priority presentation coupling. It creates concrete services and directly reaches engine, memory, training, ContentResolver, Bitmap decoding, web search and AccessibilityService. Split into feature state holders; UI should depend on use-case contracts. Avoid exposing Android `Uri` as a core contract. |
| `ui/screens/ChatScreen.kt` | Chat rendering, message item, attachment picker, clipboard and Toast | `feature/chat/presentation` | Feature ownership is clear. Keep picker/clipboard interaction at the presentation edge; decoding, conversation orchestration and model work belong behind use cases. |
| `ui/screens/NeuralCoreScreen.kt` | Training status, queue cards, metrics and loss chart | `feature/learning/presentation` | Imports `TrainingData` from `model/`, coupling UI to an implementation DTO. Replace with stable UI state/domain projections. |
| `ui/screens/PerceptionScreen.kt` | Perception UI, permissions, CameraX provider/analysis/preview, Android settings and accessibility state | `feature/perception/presentation` + `platform/android/permissions` + `platform/android/capture` | Direct CameraX lifecycle integration and import of `ArgusAccessibilityService` cross the feature/platform boundary. Preview may be hosted by UI; capture/session ownership and service state should be exposed through contracts. |
| `ui/screens/SkillsAndSettingsScreen.kt` | Settings, app exclusion, Context/Intent/PowerManager and Android settings actions | `feature/settings/presentation` + platform/policy use cases | Power/settings intents and exclusion operations are embedded in the screen. Route platform actions through typed contracts; screen renders state and dispatches intent. |
| `ui/theme/Color.kt` | Color tokens | `ui/design` | Shared design-system ownership. |
| `ui/theme/Theme.kt` | Compose theme and system-bar integration | `ui/design` with Activity/window host boundary | Theme belongs in shared design; window-specific effects remain at Android host boundary. |
| `ui/theme/Type.kt` | Typography | `ui/design` | Shared design-system ownership. |

## 5. Manifest, resources and tests

The manifest registers `.ArgusApp`, the exported launcher `.MainActivity`, a non-exported foreground service and the AccessibilityService with its binding permission and XML metadata. It declares INTERNET, ACCESS_NETWORK_STATE, CAMERA, POST_NOTIFICATIONS, FOREGROUND_SERVICE, FOREGROUND_SERVICE_DATA_SYNC, FOREGROUND_SERVICE_SPECIAL_USE, WAKE_LOCK, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS and VIBRATE permissions; camera hardware is optional.

The app module manifest remains the packaging/registration authority. Moving an Application or service class requires coordinated FQCN and manifest review. The manifest does not justify placing service behavior in domain code.

The audited resource set includes launcher/vector drawables, mipmap icons, strings, a platform Activity theme, AccessibilityService configuration, backup rules and data-extraction rules. The named manifest XML resource references exist in the inspected tree.

The root-level `public/assets/argus_icon.png` is separate from Android resources. Its consumer is unresolved; do not move or duplicate it until an actual consumer is identified.

There is one test file: `app/src/test/java/com/argus/agi/ArgusAppRobolectricTest.kt`. Its class is named `ArgusAppUnitTest`, imports the current `model.Tokenizer`, and contains two tokenizer tests. It does not currently represent an application lifecycle test despite the filename. No checked-in `androidTest` source was found.

The visible test gap includes core policy, model identity, numerical reference operations, training lifecycle/recovery, data transactions, Android services and feature UI contracts.

## 6. Dependency-direction findings

Observed direct relationships:

```text
MainActivity
  └── imports UI ViewModel, screens and theme

ArgusApp
  ├── constructs model classes
  ├── constructs sensor classes
  └── exposes them as global companion properties

ArgusViewModel
  ├── global ArgusApp services
  ├── model engine/training/memory DTOs
  ├── sensor hardware/device/search/exclusion classes
  └── AccessibilityService static action

TrainingManager
  ├── ArgusNeuralEngine
  ├── sensor.DeviceGuard
  └── WorldModel

ArgusAccessibilityService
  ├── constructs ExcludedAppsManager
  └── global ArgusApp world/training objects

ArgusForegroundService
  ├── global ArgusApp training/device/hardware objects
  └── MainActivity for notification navigation

PerceptionScreen
  ├── CameraX and Android settings APIs
  └── ArgusAccessibilityService

SkillsAndSettingsScreen
  └── Android Context/Intent/PowerManager APIs
```

### 6.1 Verified package dependency matrix

The matrix below records observed source edges at the current audit baseline. It distinguishes a legitimate host-to-destination edge from dependencies that bypass the intended application/core contracts.

| Current source | Observed destination | Source evidence | Assessment |
|---|---|---|---|
| `com.argus.agi.MainActivity` | `ui.ArgusViewModel`, `ui.screens.*`, `ui.theme.*` | `MainActivity.kt:49–58, 67–69` | Entry-to-presentation hosting is expected; the Activity should not become a workflow owner. |
| `com.argus.agi.ArgusApp` | `model.*`, `sensor.*` | `ArgusApp.kt:7–11, 17–24` | Composition belongs in bootstrap, but the current `Application` exposes static lazy objects as a service locator. |
| `com.argus.agi.ui.ArgusViewModel` | root `ArgusApp`, `model`, `sensor` and `service` | `ArgusViewModel.kt:9–20, 43–49, 62–69, 88, 280` | High coupling: presentation constructs concrete adapters, consumes implementation DTOs/state and invokes concrete service actions instead of application-facing APIs. |
| `com.argus.agi.ui.screens.NeuralCoreScreen` | `model.TrainingData` | `NeuralCoreScreen.kt` imports `TrainingData` directly | Feature presentation is bound to a model-package queue DTO; expose stable learning UI state instead. |
| `com.argus.agi.ui.screens.PerceptionScreen` | `service.ArgusAccessibilityService` and CameraX/Android settings APIs | `PerceptionScreen.kt:9–13, 69, 84–89` | The screen reads a concrete service static instance. Preview hosting may remain at the UI edge, but capture/session ownership and service state need narrow contracts. |
| `com.argus.agi.service.ArgusAccessibilityService` | root `ArgusApp` and `sensor.ExcludedAppsManager` | `ArgusAccessibilityService.kt:7–8, 35, 42, 79–80` | Android callback code constructs a platform adapter and reaches application state through globals. |
| `com.argus.agi.service.ArgusForegroundService` | root `ArgusApp`, `MainActivity` and training/device state | `ArgusForegroundService.kt:12–14, 58, 110–147` | The service reads global workflow objects and imports the concrete Activity for notification navigation. |
| `com.argus.agi.model.TrainingManager` | `sensor.DeviceGuard` | `TrainingManager.kt:3–4, 35–42, 143` | Dependency inversion: the training coordinator imports an Android-oriented sensor implementation for admission/safety. Depend on a device/resource contract instead. |
| `model.ArgusNeuralEngine` and `model.WorldModel` | Android `Context` and file APIs | `ArgusNeuralEngine.kt:3–9, 26`; `WorldModel.kt:3–10, 27` | Engine/domain-adjacent code is not platform-independent; persistence and Android access must be separated from numerical/domain behavior. |

`ArgusViewModel` also directly constructs `ExcludedAppsManager(application)` and `WebSearchEngine()` rather than receiving application-facing ports from the composition root. The `sensor` package has no observed imports back into `model`, `service` or `ui` in the audited production files; the problematic edge is `model` → `sensor`, not a reciprocal `sensor` dependency.

### 6.2 Confirmed source-level package cycle

The direct imports/references produce this package-level cycle:

```text
com.argus.agi.MainActivity
        │ imports screens and ViewModel
        ▼
com.argus.agi.ui
        │ ViewModel / PerceptionScreen import concrete services
        ▼
com.argus.agi.service
        │ services import ArgusApp and MainActivity
        └──────────────────────────────────────────────► com.argus.agi
```

The root package is itself a mixed boundary: `MainActivity` is the Android entry/UI host, while `ArgusApp` is currently used as the global dependency container. The cycle is created by the combination of `MainActivity` → UI, UI → concrete service classes, and service → `ArgusApp`/`MainActivity`. `ArgusViewModel` also imports `ArgusApp` directly.

This is a **source/package dependency cycle**, not evidence of a runtime crash or a Gradle cycle. The current build has only one included Gradle module (`:app`), so Gradle cannot enforce or expose the package boundary. Extracting `app/entry`, `feature/*/presentation` and `platform/android/*` into separate modules without first inverting these edges would create a module dependency cycle.

Required inversion before extraction:

1. UI/ViewModels call application-facing workflow APIs and consume feature-owned immutable state; they do not import `ArgusAccessibilityService` or `ArgusForegroundService`.
2. Android services call narrow application ingress/control contracts for capture, training-session observation and pause/resume; they do not read global `ArgusApp` properties.
3. Notification navigation must not require a compile-time reference from the foreground service to `MainActivity`; route through a launcher/navigation boundary owned by the app host.
4. `app/bootstrap` constructs/binds concrete implementations, while `app/entry` only hosts Android components. No static global service locator is part of the target contract.

Target logical direction:

```text
feature presentation / navigation
              |
              v
      application use cases
              |
              v
       core contracts/domain
          ^      ^      ^
          |      |      |
        data  platform engine
      adapters adapters adapters

app composition root wires implementations to contracts.
```

The graph does not imply one Gradle module for every box.

| ID | Finding | Priority | Consequence | Required design correction |
|---|---|---|---|---|
| STR-001 | `ArgusViewModel` spans features and directly owns service/engine/data access | HIGH | Broad blast radius; UI tests require concrete services | Split feature ViewModels; move workflows into application use cases; pass typed contracts |
| STR-002 | `TrainingManager` combines queue, worker, device admission, engine and memory | HIGH | Training lifecycle cannot be isolated from Android/numerical implementation | Separate learning coordinator/session lifecycle, scheduler, resource admission, repositories and engine |
| STR-003 | `ArgusNeuralEngine` owns Android Context and checkpoint file I/O | HIGH | Numerical engine is tied to Android storage | Operate on explicit model/session state; persistence belongs in `data/model` |
| STR-004 | `model/TrainingManager` imports `sensor.DeviceGuard` | HIGH | Lower-level training depends on platform-oriented package | Define device/admission contracts in core/application; Android observations stay in platform |
| STR-005 | Services access global `ArgusApp` objects; AccessibilityService has a static instance path | HIGH | Platform entry points bypass explicit dependency/lifecycle boundaries | Application-owned service facades, explicit lifecycle and policy checks |
| STR-006 | Perception/settings screens directly use CameraX, Android settings and service implementation | MEDIUM | Feature presentation is coupled to platform internals | Platform adapters and application-facing contracts |
| STR-007 | `WorldModel` is in `model/` and accepts Android Context | MEDIUM | Knowledge domain and persistence are conflated | Split domain contracts from data repository/platform updates |
| STR-008 | `HardwareProfile` and `DeviceGuard` are both in `sensor/` | MEDIUM | Stable capability and transient conditions risk conflation | Separate core records and platform observation providers |
| STR-009 | `WebSearchEngine` is in `sensor/` | MEDIUM | Connector transport is misclassified | Connector in data/acquisition; policy/job contracts in core/application |
| STR-010 | One test file covers two tokenizer checks; filename/class imply a different scope | HIGH | Domain, lifecycle, persistence and platform contracts lack visible test representation | Establish package-mirrored tests by contract |
| STR-011 | Large feature screens contain platform integration and multiple UI responsibilities | MEDIUM | Presentation code has a broad change surface | State-driven UI; split platform ownership by responsibility |
| STR-012 | No native source/module exists | INFORMATIONAL | Research intent could be mistaken for current capability | Add native boundary only after architecture/operator/ABI contracts |
| STR-013 | `com.argus.agi` → `com.argus.agi.ui` → `com.argus.agi.service` → `com.argus.agi` source/package cycle | HIGH | Package boundaries cannot be extracted independently; platform services and UI are mutually coupled through concrete entry/service classes | Move composition to `app/bootstrap`; UI depends on application workflow APIs; services depend on narrow application ingress/control contracts and must not import `MainActivity` or global `ArgusApp` state |

Priority describes structural risk and migration order, not proof of a crash or functional defect.

## 7. Build and dependency topology

### 7.1 Gradle module recommendation

**Do not split Gradle modules yet.** Correct package ownership and stable interfaces first, inside the existing `:app` module.

Candidate extraction sequence, not approved module IDs:

| Order | Candidate boundary | Reason | Prerequisite |
|---|---|---|---|
| 1 | `:core:domain` or smaller pure-Kotlin core contracts | Enforce Android-free contracts and identity/policy types | Package APIs stabilize |
| 2 | `:application` | Isolate use cases/lifecycle orchestration | Contracts and composition accepted |
| 3 | `:data:local` | Isolate Room/file persistence, generations, corpus and knowledge repositories | Storage, encryption, schema and deletion contracts closed |
| 4 | `:platform:android` | Isolate services, capture, scheduling, device and notification adapters | Android lifecycle/permission contracts stable |
| 5 | `:engine:api` + `:engine:reference` | Independently test numerical contracts/reference implementation | Model, tokenizer and operator contracts selected |
| 6 | `:engine:native` | Isolate NDK/CMake/JNI and ABI-specific backends | G4 and native operator/ABI/memory contracts accepted |

Do not create one Gradle module per screen. Feature packages may remain in `:app` until compile-time isolation or independent delivery provides measurable value.

### 7.2 Build configuration observations

| File | Current responsibility | Observation |
|---|---|---|
| `settings.gradle.kts` | Plugins/repositories, project name and included modules | Correct root ownership; only `:app` included |
| Root `build.gradle.kts` | Root plugin aliases with `apply false` | Appropriate root role |
| `gradle/libs.versions.toml` | Shared plugin/library versions | Correct canonical version-catalog location |
| `gradle.properties` | Shared Gradle/Kotlin settings | Correct root configuration ownership |
| `gradle/wrapper/gradle-wrapper.properties` | Wrapper distribution pin | Correct wrapper ownership |
| `app/build.gradle.kts` | Android app configuration, dependencies, signing inputs and APK copy task | Mixes module configuration with repository-root export side effect |

### 7.3 Implicit APK copy task

The app build script declares a `CopyApkTask` that copies `app/build/outputs/apk/release/app-release.apk` to repository-root `ARGUS.apk`, then finalizes `assembleRelease` with that copy task.

This conflicts with the repository rule that release export/publication must be explicit rather than an implicit side effect of assembly.

**Future disposition:** remove implicit copy finalization from the standard assemble lifecycle. Provide a separately named explicit export task only if requested by the project owner. No build script was changed.

### 7.4 Dependency inventory candidates

The app declares Compose, Lifecycle, Navigation, CameraX, Room, Moshi, Retrofit, OkHttp, Coroutines, Accompanist permissions, Coil and test tooling.

Static inspection found direct OkHttp use in `WebSearchEngine` and CameraX use in `PerceptionScreen`. No production Room database/DAO/entity, DataStore, Retrofit service or Moshi adapter declarations/imports were found in the audited Kotlin files. These become dependency-inventory review candidates; this observation alone does not authorize removal. Confirm transitive, annotation-processing, build-logic and generated use before pruning.

Commented-out DataStore and Play services dependencies are not active declarations. No third-party native training backend is declared in the inspected app module.

## 8. Consolidated ownership map

The target names below follow the revised package hierarchy and subsystem crosswalk in document 45. This table is a structural recommendation only; the current audited files stay in their existing paths.

| Current area | Target owner(s) | Future responsibility |
|---|---|---|
| Application/Activity entry points | app/entry | Android process entry, Activity host and platform launchers |
| Dependency graph composition | app/bootstrap | Explicit construction and dependency wiring; no static global service locator |
| Activity destinations | app/navigation | Route definitions and destination composition |
| Broad ViewModel and screens | feature/*/presentation | Feature-specific state holders and user intent mapping |
| Shared Compose theme | ui/design | Shared visual tokens and components |
| Genesis workflow | application/genesis, core/model, core/device | Initial configuration, initialization orchestration and readiness |
| Inference workflow | application/inference, core/inference | Request assembly, cancellation, snapshot identity and result state |
| Learning/training lifecycle | application/learning, core/training | Session admission, pause/resume, candidate/checkpoint lifecycle |
| Numerical operations | engine/reference and later selected engine/model | Correctness path and selected architecture implementation; no Android I/O |
| Tokenizer contract/implementation | core/tokenizer, later engine/tokenizer | Immutable identity/API separated from selected implementation after G1-D |
| Device/capability contract | core/device, core/compute | Versioned capability and execution evidence; no chipset-name inference |
| Android device observations | platform/android/device | Android properties, battery and thermal observations |
| Android runtime/provider APIs | platform/android/compute | Runtime/provider discovery and concrete Android/vendor API integration |
| Model/checkpoint persistence | core/model, data/model, platform/android/storage | Repository contract, durable generation/checkpoint transaction and platform file/key access |
| Corpus and tokenizer-training data | core/acquisition, core/training, data/corpus | Immutable manifests, split/reader behavior and lineage |
| Knowledge and memory | core/knowledge, application/knowledge, data/knowledge | Domain contracts, retrieval workflow, persistence and indexes |
| Source policy and user consent | core/policy, application/*, data/policy | Fail-closed decisions, purpose checks, durable grants/revocation |
| Acquisition jobs/connectors | core/acquisition, application/acquisition, data/acquisition | Job policy/orchestration separated from HTTP fetch and parsing |
| Evaluation and promotion | core/evaluation, application/evaluation, data/model | Fixed-candidate evaluation and durable promotion transaction |
| Lifecycle event/audit | core/events, owning application workflows, data/audit | Typed event schemas, workflow-owned state and sanitized durable audit |
| Accessibility and camera | platform/android/capture | Android capture integration and platform safeguards |
| Android permissions/settings | platform/android/permissions | OS permission state, launchers and settings integration |
| Foreground scheduling/notification | platform/android/lifecycle, platform/android/notification | OS-compliant job/service adapters; session state stays in application |

The package crosswalk does not imply a global event bus, one module per subsystem, or a mandatory feature screen for every backend capability.

## 9. Recommended migration order

When implementation and structural migration are separately authorized:

1. Define/verify core contracts and application-facing workflow ports for Genesis, inference, learning, acquisition, knowledge, evaluation, device/resource admission, capture ingress and session control. Preserve document 36 lifecycle/transaction invariants.
2. Break the confirmed root ↔ UI ↔ service cycle: feature presentation must stop importing concrete service implementations; Android services must stop importing `MainActivity` and reading global `ArgusApp` objects. Separate `ArgusApp` composition wiring into `app/bootstrap` and Android entry hosting into `app/entry`.
3. Split the broad ViewModel into feature-owned presentation state holders; use feature/learning for the current NeuralCore UI rather than retaining an implementation-oriented package name.
4. Move Android service, permission, camera, PackageManager, battery, notification, connectivity, storage and runtime-provider operations behind platform/android adapters. Keep OS scheduling separate from application session ownership.
5. Separate engine numerical execution from tokenizer identity, model-generation persistence, data ingestion and Android Context/file I/O. Do not create the selected family engine before G4 or the concrete tokenizer before G1-D.
6. Isolate local persistence, checkpoint/generation transactions, policy/consent records, source manifests, audit events and knowledge repositories in their owning data domains.
7. Add contract-mirrored unit and integration tests for each bounded change; establish forbidden-dependency checks before Gradle module extraction.
8. Add a native module only after architecture/operator/ABI/memory contracts justify it and the required project-owner authorization is given.
9. Extract Gradle boundaries only when stable APIs and measured build/test/security/ABI benefits justify them.
10. Update documents 17–19, 36, 45 and relevant architecture/verification references after each bounded migration.

Do not combine package moves with optimizer changes, tokenizer selection, architecture rewrites, database migrations, dependency upgrades or UI redesign in one broad commit.

## 10. Open decisions and disposition

Open: DI framework selection (with an explicit composition root required); final workflow API shapes; exact package/module granularity; persistence/encryption/schema; test framework/source-set split; benchmark module; tokenizer/runtime after G1-D; native engine after G4; supported ABIs/NDK/CMake/STL; consumer for `public/assets/argus_icon.png`; dependency removals after full usage review; disposition of the implicit APK copy task.

| Area | Disposition |
|---|---|
| Current file inventory | Audited |
| File ownership mapping | Documented |
| High-risk dependency couplings | Identified and rechecked at current HEAD |
| Explicit package dependency graph | Documented from production imports and direct references |
| Root/UI/service package cycle | Confirmed; must be inverted before Gradle module extraction |
| Target package ownership | Reworked in document 45 and reconciled here against the complete document 36 logical subsystem inventory |
| Immediate Gradle module extraction | Not recommended |
| Future extraction seams | Candidate sequence documented |
| Source movement/build changes | Not performed or authorized |
| Build/test/Actions/device execution | Not performed |
| Implementation authorization | Not granted |

This closes only the static file-ownership/dependency-inventory task for the inspected snapshot. It does not close G1-D, G4, P0 remediation, implementation readiness or device verification.

## 11. Change history

- 2026-11-04 — Static file-by-file ownership and dependency audit added against `main` at `24b26b061574700d82fecd5c2a1485353333cef8`. No source files or build configuration were changed.
- 2026-11-04 — Target-owner references reconciled with the revised document 45 package hierarchy and the document 36 subsystem crosswalk. The observed source inventory and findings were not changed.
- 2026-11-04 — Re-audited `main` at `bd75ac1533e079bfca4d12e1af9ec3170d7187d9`; added an explicit cross-package dependency matrix and confirmed the `com.argus.agi` → `ui` → `service` → `com.argus.agi` cycle. The comparison with the earlier source snapshot found no intervening Kotlin, manifest, Gradle or Android-resource changes. No source/build files were changed.
