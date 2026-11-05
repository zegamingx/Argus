# Argus Android Project Structure and File-Placement Rules

**Status:** Target package structure accepted for bounded migration; package relocation and composition-root ingress work are in progress; dependency remediation remains open  
**Last reviewed:** 2026-11-05  
**Pre-migration source observation:** `24b26b061574700d82fecd5c2a1485353333cef8`  
**Source baseline reviewed for this update:** `4fea8fb2351868c7e6ec0464c8c58724d90eeed4`  
**Scope:** Android repository layout, Kotlin package ownership, subsystem boundaries, Gradle module extraction, native/JNI boundary, resource and test placement, build configuration ownership and migration rules  
**Dependencies:** Documents 01, 02, 13, 15, 17–22, 23, 35, 36, 43, 44, 46, 47 and 48  
**Boundary:** This document governs package ownership and structural migration. The project owner has authorized bounded package/path relocation and the required import/manifest alignment. It does not authorize behavior rewrites, neural architecture/tokenizer/backend selection, Gradle module extraction, build/dependency changes, tests, workflow execution or device experiments.

## 1. Purpose

Argus needs a project structure that can support an Android application, local model lifecycle, tokenizer, training and inference, device capability assessment, knowledge acquisition, policy enforcement, persistent data, Android services and a future native compute engine without coupling UI, platform APIs and numerical kernels.

This document defines the observed source layout, target Kotlin package organization, dependency direction, criteria for Gradle module extraction, Kotlin/native boundary, resource/test/configuration placement, prototype-file ownership and rules for a future migration. The contract-owner, permitted-consumer, provider/adaptor and authoritative-state mapping for those packages is maintained separately in [47 — Package Interface and Dependency Contract Matrix](47_PACKAGE_INTERFACE_AND_DEPENDENCY_CONTRACT_MATRIX.md).

The structure is architecture-neutral. It does not select Transformer, Mamba-2, RWKV-7, a hybrid, tokenizer family, optimizer, precision, GPU/NPU provider or model profile.

## 2. Observed repository structure

The repository contains one Android Gradle application module, `:app`, and no additional Gradle modules. The first source relocation is recorded at `b0441af8`; subsequent composition-root and runtime-ingress changes are recorded at `d9c3924` and `8a9014c`. The current source tree was re-read before this bounded file-ownership update.

The application namespace is `com.argus.agi`. Principal current paths are:

| Current path | Observed contents |
|---|---|
| `app/src/main/java/com/argus/agi/app/bootstrap/` | `ArgusContainer` composition root and `ArgusViewModelFactory` |
| `app/src/main/java/com/argus/agi/app/entry/` | `ArgusApp` and `MainActivity` |
| `app/src/main/java/com/argus/agi/app/navigation/` | `MainScreen` and destination switching |
| `app/src/main/java/com/argus/agi/application/runtime/` | Transitional runtime aggregation plus focused training, device, hardware, capture and accessibility port files |
| `app/src/main/java/com/argus/agi/feature/*/presentation/` | Feature screens and presentation-owned state; ChatMessage, SettingsUiState, LearningUiState and PerceptionUiState are feature-owned |
| `app/src/main/java/com/argus/agi/ui/ArgusViewModel.kt` | Transitional cross-feature Android ViewModel; decomposition remains open |
| `app/src/main/java/com/argus/agi/ui/design/` | Shared Compose theme, colors and typography |
| `app/src/main/java/com/argus/agi/model/` | Four mixed legacy prototype classes: engine, tokenizer, training manager and world model |
| `app/src/main/java/com/argus/agi/platform/android/capture/` | AccessibilityService, action bridge and installed-app/exclusion adapter |
| `app/src/main/java/com/argus/agi/platform/android/device/` | Device guard and hardware observations |
| `app/src/main/java/com/argus/agi/platform/android/lifecycle/` | Foreground service adapter |
| `app/src/main/java/com/argus/agi/platform/android/notification/` | Notification channel adapter |
| `app/src/main/java/com/argus/agi/data/acquisition/` | Web-search connector and result type |
| `app/src/test/` | One Robolectric-backed unit test file |
| `app/src/main/res/` | Android drawables, launcher icons, values and XML resources |

The inspected root also contains `settings.gradle.kts`, root `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, the Gradle wrapper, `docs/` and `public/assets/`.

Observed limitations in the current tree:

1. `model/` still combines numerical execution, tokenizer state, training orchestration and memory-like state; these files cannot be safely assigned to final owners by path rename alone.
2. `ui/ArgusViewModel.kt` remains cross-feature and directly knows about model, data and platform implementations.
3. `ArgusApp` now delegates construction to the process-scoped `app/bootstrap/ArgusContainer`; the old global companion-object service locator has been removed.
4. Android services now enter through `ArgusRuntimeOwner` and narrow runtime interfaces rather than static `ArgusApp` objects or a static AccessibilityService instance. The aggregate `application/runtime` contract package remains transitional and is not yet split by workflow ownership.
5. `ExcludedAppsManager` still combines Android installed-app discovery with exclusion state; durable policy ownership has not been extracted.
6. `WebSearchEngine` is correctly placed under `data/acquisition`, but connector provenance, rights and training admission remain separate responsibilities.
7. No native C++/JNI source or verified native runtime exists in the inspected tree.
8. The existing test file does not represent model, tokenizer, data, policy, lifecycle or backend contracts.
9. Repository assets under `public/assets/` are not Android runtime resources; their distribution consumer must remain explicit.

These are source-ownership findings, not authorization to create placeholder packages or mechanically rename mixed classes. The detailed file-by-file audit is maintained in [46 — Android Source-Tree Ownership and Dependency Audit](46_ANDROID_SOURCE_TREE_OWNERSHIP_AND_DEPENDENCY_AUDIT.md).

## 3. Structural principles

1. **Package boundaries before Gradle boundaries.** Define ownership and dependency direction in Kotlin packages first. A package is not automatically a Gradle module.
2. **One responsibility per layer.** UI renders state; application orchestration coordinates use cases; core owns domain rules/contracts; data owns persistence and source implementations; platform owns Android APIs; engines own numerical execution.
3. **Dependency inversion.** Core contracts must not depend on Android framework types, Compose, Room, OkHttp, vendor SDKs or JNI implementation classes.
4. **No global service locator.** The application composition root owns dependency construction and lifecycle. Feature code receives explicit dependencies or a documented DI mechanism.
5. **No god packages/classes.** `utils`, `manager`, `misc`, `common` and `helpers` must not become unbounded dumping grounds.
6. **Architecture-neutral core.** Core APIs describe behavior and data contracts without hardcoding a model family, tokenizer format or accelerator.
7. **Platform isolation.** Android services, permissions, sensors, schedulers, files and network APIs stay behind platform/data adapters.
8. **Numerical isolation.** Tensor math, forward/backward operations and optimizer kernels do not live in ViewModels, Composables, Android services or repositories.
9. **Explicit data trust.** Rights, consent, provenance and training eligibility are enforced by policy/application boundaries, never inferred by a connector or UI.
10. **No speculative scaffolding.** Do not add empty packages, placeholder Gradle modules, unused dependencies or fake backend classes.
11. **Stable package identity.** Moving a package to another Gradle module should not require arbitrary class/package renaming.
12. **No implementation implied.** The target tree is a placement contract; absent directories remain absent until work is authorized.

## 4. Target Kotlin package organization

This is a **logical package map** under the existing namespace com.argus.agi, initially within the existing :app module. It is not a physical top-level directory layout and does not prescribe Gradle module IDs. No package in this tree is required to exist before it owns real code. The mapping is derived from the subsystem contracts in document 36; the detailed crosswalk below is authoritative for the proposed placement.

~~~text
com.argus.agi
├── app/
│   ├── entry/                 # Android Application/Activity entry points
│   ├── bootstrap/             # Composition root and dependency graph construction
│   └── navigation/            # Routes and destination wiring
├── feature/
│   ├── chat/presentation/
│   ├── learning/presentation/ # User-facing model/training controls and status
│   ├── perception/presentation/
│   └── settings/presentation/
├── application/
│   ├── genesis/               # Initial generation workflow and readiness
│   ├── inference/             # Request/context workflow and cancellation
│   ├── learning/              # Training admission, session and candidate lifecycle
│   ├── acquisition/           # Discovery/fetch job orchestration and handoff
│   ├── knowledge/             # Retrieval and memory workflows
│   └── evaluation/            # Candidate evaluation and promotion coordination
├── core/
│   ├── identity/              # Instance, generation, session and stable IDs
│   ├── model/                 # Model config, generation and repository contracts
│   ├── tokenizer/             # Tokenizer identity, manifest and API contracts
│   ├── inference/             # Inference request/snapshot contracts
│   ├── training/              # Training session, update and recovery contracts
│   ├── compute/               # Execution capability and backend contracts
│   ├── device/                # Stable capability and live-condition contracts
│   ├── policy/                # Consent, purpose, authorization and retention rules
│   ├── knowledge/             # Knowledge, memory and provenance domain contracts
│   ├── acquisition/           # Source, operation-scope and job contracts
│   ├── evaluation/            # Metrics, gates and promotion invariants
│   └── events/                # Typed lifecycle/audit event envelopes and schemas
├── data/
│   ├── model/                 # Generation, tensor and checkpoint repositories
│   ├── corpus/                # Dataset manifests, splits and readers
│   ├── knowledge/             # Durable records and retrieval indexes
│   ├── acquisition/           # Concrete source connectors, fetch and parsing
│   ├── policy/                # Durable consent, exclusion and policy records
│   └── audit/                 # Sanitized durable audit/diagnostic records
├── platform/
│   └── android/
│       ├── lifecycle/         # WorkManager, foreground service and OS callbacks
│       ├── permissions/       # Android permission state and platform request flows
│       ├── capture/           # Accessibility, camera and authorized capture
│       ├── device/            # Android hardware and runtime-condition observations
│       ├── storage/           # SAF/filesystem/key APIs and platform storage access
│       ├── connectivity/      # Android connectivity and network-state observation
│       ├── notification/      # Android notification adapters
│       └── compute/           # Android/Vulkan/vendor runtime and capability adapters
├── engine/
│   ├── reference/             # Correctness-first numerical reference path
│   ├── model/                 # Selected family graph/initialization implementation; after G4
│   └── tokenizer/             # Selected concrete tokenizer implementation; after G1-D
└── ui/
    └── design/                # Shared Compose design system and visual primitives
~~~

The tree deliberately does **not** include a generic application/usecase plus application/orchestration double hierarchy, app/lifecycle, a generic data/local catch-all, or a mandatory feature/knowledge screen. Workflow-specific use cases and coordinators live together under their owning `application/<workflow>` package. A user-facing feature package is added only when a real screen or interaction is approved. The engine/model and engine/tokenizer packages remain absent until their respective selection gates close.

### 4.1 Package ownership

| Package root | Owns | Must not own |
|---|---|---|
| app/entry | Android Application and Activity entry points, process/window host and platform launchers | Dependency graph internals or business workflows |
| app/bootstrap | Application composition root, explicit manual dependency graph construction, constructor-injection factories and startup wiring | Android component lifecycle, business policy, model math or repository internals |
| app/navigation | Stable route identity/encoding, navigation state and destination composition; internal learning-session route is `argus://app/learning/session/{sessionId}` | Activity ownership, business rules, data access or authoritative workflow/session state |
| feature/*/presentation | Feature UI, immutable feature-specific UI state, ViewModels, lifecycle-aware observation and user intent mapping | Concrete repositories, engine classes, connector policy, JNI or authoritative workflow state |
| `application/<workflow>` | Workflow-specific use cases, coordination, admission, cancellation, readiness and result mapping | Android framework implementation, tensor mathematics or direct file/database manipulation |
| core/identity | Instance/generation/session IDs and identity invariants | Android Context, database entities or UI models |
| core/model, core/tokenizer, core/inference, core/training | Architecture-neutral model/tokenizer/session contracts and invariants | Selected family implementation, persistence code or Android lifecycle |
| core/device, core/compute | Capability evidence, runtime-condition records and execution contracts | Chipset-name guesses, vendor calls or false acceleration claims |
| core/policy | Purpose-scoped authorization, consent, retention and fail-closed decisions | Network fetching, capture APIs or UI-owned consent decisions |
| core/knowledge, core/acquisition | Knowledge/provenance and source-operation/job contracts | Concrete HTTP clients, source self-authorization or training eligibility inferred from retrieval |
| core/evaluation, core/events | Evaluation invariants, promotion criteria and typed event schemas | Direct active-generation mutation or a global mutable event bus |
| `data/<domain>` | Concrete persistence, serialization, query/index and source-connector implementations for that domain | Changing domain policy, granting consent or exposing database/network DTOs to UI |
| platform/android/* | Android framework, permission, service, filesystem, connectivity, device and runtime-provider integration | Product policy, model mathematics or persistent domain decisions |
| engine/reference | Correctness-oriented numerical operations and reference behavior | Android APIs, storage, rights policy or presentation |
| engine/model / engine/tokenizer | Selected architecture/tokenizer implementations behind stable contracts | UI, Android services, consent or source rights |
| ui/design | Shared Compose tokens, components, typography, shapes and theme | Feature workflows or direct data access |

### 4.2 Crosswalk from document 36 logical subsystems

Document 36 remains authoritative for logical responsibilities and lifecycle guarantees. This table assigns proposed Kotlin ownership without changing those contracts.

| Document 36 logical subsystem | Contract/domain owner | Workflow or concrete implementation owner |
|---|---|---|
| Experience/UI | None | feature/*/presentation, ui/design, app/entry and app/navigation |
| Application orchestrator | core contracts used by each workflow | application/genesis, inference, learning, acquisition, knowledge and evaluation; app/bootstrap wires dependencies |
| Trust and policy | core/policy | Policy checks in application workflows; data/policy persists grants/revocations |
| Device capability service | core/device | platform/android/device gathers observations; application workflows perform resource admission |
| Genesis coordinator | core/model, core/device, core/tokenizer | application/genesis; initialization math in a selected engine; data/model commits generation state |
| Model-generation repository | Repository contract in core/model | data/model; platform storage/key APIs in platform/android/storage |
| Tokenizer service | core/tokenizer | engine/tokenizer after G1-D; immutable artifact reference/persistence through data/model |
| Inference service | core/inference, core/compute | application/inference; numerical execution in engine; runtime providers in platform/android/compute |
| Learning coordinator | core/training, core/device, core/policy | application/learning; numerical updates in engine; OS scheduling in platform/android/lifecycle |
| Training engine/backend | core/model, core/training, core/compute | engine/reference and later selected engine/model; Android runtime adapters in platform/android/compute |
| Corpus and knowledge store | core/knowledge, core/acquisition, core/training | data/corpus, data/knowledge and data/model as appropriate |
| Personal experience intake | core/policy and core/knowledge | application/knowledge owns intake, retention routing and review orchestration; platform/android/capture supplies observations; data/knowledge stores approved personal-experience records; data/policy stores consent/revocation/review evidence; application/learning independently validates training admission |
| Acquisition/connectors | core/acquisition, core/policy | application/acquisition orchestrates; data/acquisition fetches/parses; platform connectivity is observed under platform/android/connectivity |
| Evaluation/promotion | core/evaluation, core/model | application/evaluation; generation commit/promotion persistence in data/model |
| Event/diagnostic contracts | core/events | Emission stays with owning workflow; data/audit persists sanitized records; no generic event-bus service is required |

The core packages contain contracts and domain invariants, not every workflow implementation. The application packages coordinate those contracts. data, platform/android and engine implement distinct ports and must not depend on one another directly unless a later reviewed module contract proves a narrow adapter dependency is unavoidable.

application owns process-independent workflow lifecycle (for example, training-session pause/resume and Genesis readiness). platform/android/lifecycle owns Android process/service callbacks, WorkManager/foreground-service integration and OS scheduling constraints. These are related but not interchangeable responsibilities.

platform/android/compute owns Android- and vendor-specific runtime/provider integration. core/compute owns provider-neutral capability/execution contracts. engine owns numerical model operations. HTTP acquisition connectors remain in data/acquisition; platform/android/connectivity reports Android connectivity rather than owning all network clients. Personal capture, intake, consent, review, retention and the memory–training handoff are specialized by [48 — Personal Capture and Experience Intake Contract](48_PERSONAL_CAPTURE_AND_EXPERIENCE_INTAKE_CONTRACT.md); this does not add a new package or authorize source movement.

Do not add empty packages to match this tree. Add a package only when a real declaration has a clear owner, callers and tests. Subpackages such as internal, serialization or repository are introduced only when they represent an actual boundary.

## 5. Dependency direction and forbidden dependencies

The intended compile-time direction is:

~~~text
feature presentation ───────┐
app navigation ─────────────┤
                            v
                     application/*
                     workflow use cases
                            |
                            v
                      core contracts
             ^              ^               ^
             |              |               |
        data adapters  platform adapters  engine
             \              |               /
              \             |              /
               └──── app/bootstrap wires ─┘
~~~

The diagram shows package dependencies, not an execution call graph. app/entry hosts Android components; app/bootstrap is the composition root and may reference concrete implementations to bind them to contracts. The application layer must not import those concrete implementations.

Mandatory rules:

1. core/** MUST remain free of Android framework, Compose, Activity, Service, Context, Room, Retrofit/OkHttp, vendor SDK and JNI implementation dependencies.
2. feature/** MUST depend on application-facing workflow APIs/state contracts and ui/design, not concrete data, platform or engine implementation classes.
3. application/** MUST depend on core/** contracts and workflow ports. It MUST NOT depend on data/**, platform/android/**, Compose, Android framework or numerical engine implementations.
4. data/** MUST implement domain/application-facing persistence or connector contracts. It MUST NOT redefine consent, rights, retention or training eligibility.
5. platform/android/** MAY depend on Android APIs and narrow application entry-point contracts needed by Android callbacks/services, plus core/**. Application workflows MUST NOT depend back on Android implementations.
6. engine/** MUST implement declared model, tokenizer and compute contracts. Engine code MUST NOT know about Composables, Activity, ViewModel, navigation, Android Context or domain policy.
7. The native bridge, when separately approved, MUST be called through a typed Kotlin engine adapter. UI and domain code MUST NOT call JNI entry points.
8. The training engine MUST NOT select or authorize personal data. It receives only an approved immutable dataset manifest.
9. Acquisition connectors MUST NOT authorize their own source, retention or training permissions.
10. Repositories expose domain-facing results; database rows and network DTOs do not leak into feature presentation or core contracts.
11. Dependency cycles are prohibited at package and Gradle-module level. data, platform/android and engine do not depend on one another directly by default.
12. Cross-layer calls use explicit interfaces and typed outcomes, not reflection, static global service locators or global mutable registries.
13. Lifecycle status is owned by the workflow that owns the job/session; Android adapters observe/control it through narrow interfaces. Do not create a second state database in notifications or generic event streams.
14. core/events defines typed event contracts only. Durable audit persistence belongs to data/audit; in-process state should use the owning workflow state/result API rather than a mandatory global event bus.
15. Backend selection and fallback report the implementation that actually executed.
16. The current single-module composition root uses explicit manual constructor injection and scoped factories by default; do not add a DI framework or static service locator without a separate evidence-backed decision.
17. Notification routes are owned by app/navigation; platform notification adapters pass a stable session destination and never encode progress/state as an alternate source of truth.

## 6. Gradle module strategy

### 6.1 Current topology

The current Gradle topology is one Android application module:

```text
Argus/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
└── app/
    ├── build.gradle.kts
    └── src/
```

This is an observed fact. No module extraction is being performed in this documentation phase.

### 6.2 Package-first modularization

For the first authorized structural migration, logical package ownership in Section 4 is the required boundary. Keeping packages within `:app` temporarily is acceptable while the dependency graph and APIs are established.

A logical package MUST become a separate Gradle module only when at least one reason is demonstrated:

- a stable API needs to be consumed by more than one module;
- platform/native dependencies must be isolated from pure Kotlin/domain code;
- compilation, testing, build caching or ABI boundaries materially benefit from separation;
- ownership/security boundaries need Gradle enforcement rather than convention alone;
- the module can be tested and versioned independently with a stable contract.

Do not create one Gradle module per class, screen, repository or proposed neural architecture. Do not introduce empty modules to make the tree look complete.

### 6.3 Candidate extraction seams

These are candidate boundaries, not approved Gradle module declarations or a required extraction order:

| Candidate boundary | Why it may be extracted | Gate before extraction |
|---|---|---|
| :core | Enforce Android-free domain/contracts and shared identity/policy types | Contract ownership is stable; no Android or adapter dependency |
| :application | Isolate process-independent workflows and orchestration | Use-case APIs, cancellation and workflow ownership are stable |
| :data (or bounded data modules) | Isolate model/corpus/knowledge/policy/audit repositories and connectors | Storage, schema, encryption, deletion and provenance contracts are closed |
| :platform:android | Isolate Android lifecycle, capture, device, permission, storage and runtime-provider adapters | Exact permission/lifecycle/API and provider boundaries are stable |
| :engine:reference | Independently test correctness-first numerical behavior | Model/operator contracts and verification method are accepted |
| Feature modules | Enforce independent UI ownership/build boundaries when more than convention is needed | Feature APIs and navigation integration are stable; do not extract one module per screen |
| Native engine | Isolate JNI/NDK/CMake and ABI-specific binaries | G4 plus operator, ABI, memory-ownership and toolchain contracts are accepted |

The exact Gradle module names, granularity and order remain open. A module is extracted only when it enforces a demonstrated dependency, testing, build or ABI boundary.

### 6.4 Gradle ownership rules

- `settings.gradle.kts` is the canonical list of included modules and plugin/repository management.
- Root `build.gradle.kts` owns root plugin declarations and genuinely shared build logic.
- `gradle/libs.versions.toml` is the single version-catalog authority for shared plugin and library versions.
- Module `build.gradle.kts` files own only that module's Android/Kotlin settings and dependencies.
- Repeated conventions may move to an included `build-logic/` build only after they are stable and reused; do not create it preemptively.
- Version values MUST NOT be duplicated across module files when the version catalog can own them.
- Use `api` only when dependency public types intentionally form the module API; otherwise prefer `implementation`.
- `compileOnly`, `runtimeOnly`, debug, test and annotation-processor configurations must reflect actual use.
- Never place release signing credentials, API keys, tokens or personal data in tracked Gradle files.
- Build tasks MUST NOT copy release binaries into source-controlled paths as an implicit side effect. Publication/export must be separately named and explicitly invoked.
- Do not add dependencies solely because a research document mentions them.

No Gradle file is changed by this document.

## 7. Android source sets and resource placement

### 7.1 Main source

- `src/main/AndroidManifest.xml`: application components, permissions, services/providers/receivers and exported-state declarations.
- `src/main/java/`: Kotlin/Java production source arranged by package ownership.
- `src/main/res/`: Android resources: drawable, mipmap, values, XML configuration, fonts and other Android resource types.
- `src/main/assets/`: intentionally packaged immutable runtime assets that do not belong in typed Android resources. Large corpora, user files, checkpoints and generated training outputs do not belong here by default.
- `src/main/cpp/`: C/C++ source only inside an approved Android module that owns native code; not a dumping location under the app module.

### 7.2 Tests

- `src/test/`: local JVM unit tests for domain, policy, pure Kotlin engine, serialization and repository logic.
- `src/androidTest/`: instrumentation tests requiring Android framework behavior, app-process integration, platform services, permission flows or device APIs.
- `src/testFixtures/`: reusable test-only fixtures only when multiple tests/modules need them and the Gradle capability is deliberately enabled.
- Benchmark source sets/modules are introduced only after benchmark methodology and execution environment are defined.

Test package paths SHOULD mirror production package paths. Test names SHOULD describe the behavior or contract, not a private implementation detail.

### 7.3 Resource rules

- Android resource filenames use lowercase `snake_case`.
- User-visible strings belong in string resources, not hardcoded Composables.
- Colors, typography, shapes and component tokens are owned by the shared design system.
- Manifest XML configuration belongs under `res/xml/` when referenced by Android platform components.
- Launcher assets belong in `mipmap/`; ordinary illustrations/icons belong in appropriate drawable/vector resources.
- Localized resources use Android's `values-<language>` qualifiers.
- Engine/domain classes do not contain user-facing copy.

Existing resource files remain where they are until migration is separately authorized.

## 8. Native C++ and JNI boundary

Native code is a future implementation boundary, not an assumption that C++ is automatically faster or that all workloads should run outside the JVM.

When an Android native engine is approved, it SHOULD be an isolated Android library module at a path such as `engine/native/`, with its own Gradle and CMake configuration. Proposed internal layout:

```text
engine/native/
├── build.gradle.kts
└── src/main/
    ├── AndroidManifest.xml
    ├── java/com/argus/agi/engine/nativebridge/
    │   ├── NativeEngine.kt
    │   └── NativeEngineLoader.kt
    └── cpp/
        ├── CMakeLists.txt
        ├── include/argus/
        ├── src/
        ├── jni/
        ├── cpu/
        ├── vulkan/
        └── vendor/
```

This is illustrative and MUST NOT be created as empty scaffolding.

Native rules:

1. JNI is an adapter boundary, not the public domain/model API.
2. Kotlin owns orchestration, lifecycle, cancellation, typed error conversion and resource admission. C++ owns only assigned numerical/runtime responsibilities.
3. Native interfaces define tensor shape, dtype, layout, alignment, ownership, lifetime, mutability and error semantics.
4. Every native allocation has explicit ownership/release behavior. Handles are invalid after their owning generation/session closes.
5. C++ exceptions MUST NOT cross JNI boundaries; native errors become stable typed results.
6. Long-running native work supports bounded cancellation and never blocks the Android main thread.
7. CPU, Vulkan and vendor backends are separate capability implementations. A detected library/device is not proof of an executable training graph.
8. Vendor SDK headers, binaries and licensing stay isolated in their adapter boundary.
9. Native code retains a CPU correctness/reference route and parity tests before accelerator optimization is accepted.
10. ABI packaging, Android ABIs, NDK/CMake versions, STL, sanitizers and symbol policy are explicit decisions.
11. Do not duplicate model, tokenizer or optimizer semantics in Kotlin and C++ without a versioned parity contract.
12. The application reports the backend actually used and the fallback reason.

Architecture, native operator inventory and backend coverage remain governed by documents 35, 38, 42 and 43.

## 9. Kotlin source and naming rules

- Package names are lowercase and follow `com.argus.agi`.
- Types and Composable functions use `PascalCase`; functions/properties use `camelCase`; true constants use `UPPER_SNAKE_CASE`.
- File names follow the primary declaration. Avoid generic `Utils.kt`, `Manager.kt` or `Common.kt` without narrow documented ownership.
- Prefer one primary public type per file; tightly related private declarations may remain together.
- Public APIs use explicit types and stable domain terminology. Do not expose database entities, network DTOs, Android `Context`, JNI pointers or Compose state as cross-layer contracts.
- Expected failures use typed outcomes/errors; exceptions are not normal control flow.
- Coroutines have explicit ownership, cancellation and dispatcher policy. No detached `GlobalScope` jobs.
- ViewModels expose immutable UI state and accept events/intents. They do not own long-lived runtimes or database connections.
- Composables are state-driven; they do not directly perform network, file, training or native operations.
- Core numerical algorithms are deterministic under explicit seed/configuration where required by contract.
- Comments explain invariants, safety constraints or mathematical choices, not every line.

## 10. Mapping from the current prototype

This is a proposed ownership map, not an instruction to move mixed classes mechanically. Rows distinguish the current physical path from the eventual owner.

| Current file/package | Target owner(s) | Structural treatment |
|---|---|---|
| `app/entry/ArgusApp.kt` + `app/bootstrap/ArgusContainer.kt` | app/entry + app/bootstrap | Current composition root is separated from the Android Application host; retain explicit construction until workflow APIs are stable |
| `app/entry/MainActivity.kt` + `app/navigation/MainScreen.kt` | app/entry + app/navigation | Keep Activity as Android host; navigation shell owns destination switching, not business workflow |
| `application/runtime/ArgusRuntime.kt` | Workflow-specific application ports; core contracts where they own provider-neutral meaning | Transitional aggregate ingress; split only when each interface has a named workflow/contract owner and consumer |
| `feature/chat/presentation/ChatMessage.kt` | feature/chat/presentation | Chat-only immutable presentation model; not persisted conversation history |
| `feature/chat/presentation/ChatUiState.kt` | feature/chat/presentation | Immutable displayed conversation state; not durable conversation or model state |
| `feature/settings/presentation/SettingsUiState.kt` | feature/settings/presentation | Immutable display projections for installed apps, search previews and world-entity summaries; no Android or storage DTOs cross into the screen |
| `feature/learning/presentation/LearningUiState.kt` | feature/learning/presentation | Immutable training-display projections; queue item projections are not session manifests or authoritative workflow state |
| `feature/perception/presentation/PerceptionUiState.kt` | feature/perception/presentation | Immutable hardware/context/capture display projections; hardware labels do not claim verified accelerator execution |
| `ui/ArgusViewModel.kt` | Feature-specific feature/*/presentation state holders | Split by user-facing workflow after application-facing workflow APIs exist; do not move the monolith wholesale into one feature |
| `model/ArgusNeuralEngine.kt` | engine/reference initially; model persistence in data/model; Android file/key access through platform/android/storage | Separate numerical operations, checkpoint policy and Android I/O; experimental, not selected production engine |
| `model/Tokenizer.kt` | core/tokenizer contracts + engine/tokenizer only after G1-D | Separate immutable identity/API from concrete algorithm; preserve compatibility and token-ID semantics |
| `model/TrainingManager.kt` | application/learning, core/training, engine/reference/selected engine/model, platform/android/lifecycle | Decompose session coordination, resource admission, numerical updates and OS scheduling; do not merely rename the class |
| `model/WorldModel.kt` | core/knowledge, data/knowledge and application/knowledge | Separate domain records, retrieval/memory workflow and durable persistence; remove Android Context from domain records |
| `platform/android/device/HardwareProfile.kt` | platform/android/device + core/device | Separate raw Android evidence from versioned capability contract; chipset heuristics are not verified execution |
| `platform/android/device/DeviceGuard.kt` | platform/android/device, core/device and workflow admission | Separate Android battery/thermal observations from policy and resource admission |
| `platform/android/capture/ExcludedAppsManager.kt` | platform/android/capture + data/policy + core/policy | Separate installed-app discovery, durable user exclusions and authorization decisions |
| `data/acquisition/WebSearchEngine.kt` | data/acquisition + core/acquisition | Concrete HTTP fetch/parser; source permission, retention and training eligibility remain outside connector code |
| `platform/android/capture/ArgusAccessibilityService.kt` | platform/android/capture | Android entry adapter; submit only bounded, explicitly authorized capture observations through application-facing interfaces |
| `platform/android/lifecycle/ArgusForegroundService.kt` | platform/android/lifecycle + platform/android/notification | Android execution/notification adapter; session state remains application-owned |
| `feature/chat/presentation/ChatScreen.kt` | feature/chat/presentation | Keep picker/clipboard UI edges; delegate decoding and conversation workflow |
| `feature/learning/presentation/NeuralCoreScreen.kt` | feature/learning/presentation | User-facing learning/training status; avoid exposing engine DTOs |
| `feature/perception/presentation/PerceptionScreen.kt` | feature/perception/presentation + platform/android/capture/permissions | Keep UI/preview host at presentation edge; move camera/permission adapter operations behind contracts |
| `feature/settings/presentation/SkillsAndSettingsScreen.kt` | feature/settings/presentation + application/platform contracts | UI dispatches typed settings/exclusion intents; platform settings actions remain outside domain |
| `ui/design/*` | ui/design | Shared design system |

Before any move, inspect usages, app composition, manifest references, Android component FQCNs, serialization names, tests, reflection/ProGuard rules and resource references. This mapping does not authorize a bulk package rename.

## 11. Top-level repository ownership

The current repository remains a single Android application module. Section 4 describes packages under the namespace inside that module; a package tree must not be mistaken for root-level Gradle modules.

| Path | Purpose and rule |
|---|---|
| app/ | Existing Android application module and production/test source sets; current Kotlin packages remain under com.argus.agi until module extraction is approved |
| docs/ | Requirements, architecture, evidence, decisions, lifecycle and governance |
| gradle/ | Version catalog and wrapper; no project source or secrets |
| tools/ | Future reproducible maintenance/research tooling only with named owner, inputs, outputs and environment |
| benchmarks/ | Future benchmark harnesses/results only with defined methodology and provenance |
| public/ | Existing repository static assets; identify the current consumer of public/assets/argus_icon.png before moving or duplicating |
| root core/, application/, data/, platform/, engine/ or feature/ | Potential Gradle module roots only after extraction approval; do not create them merely to mirror the package diagram |
| build/, .gradle/, generated outputs | Local/generated state, never source-of-truth inputs or committed runtime artifacts |

Do not add datasets/, models/, checkpoints/, secrets/ or personal-data directories as casual source-tree storage. Distributable fixtures/datasets require documented provenance, rights, size, checksum, update and packaging policy. User checkpoints, memory and training data are runtime user data, not Git assets.

## 12. Test and verification organization

| Test category | Preferred location | Required focus |
|---|---|---|
| Core/domain/policy/events | src/test/.../core/ | Identity invariants, typed outcomes, consent defaults, event schema compatibility and fail-closed behavior |
| Application workflows | src/test/.../application/ | Genesis readiness, learning admission, pause/resume, cancellation, acquisition handoff and promotion ordering |
| Tokenizer | src/test/.../engine/tokenizer/ or contract-focused core/tokenizer tests | Fixtures, Unicode, special-token boundaries, determinism and fingerprint |
| Numerical engine | src/test/.../engine/ | Reference parity, finite values, gradients, optimizer updates and deterministic initialization |
| Data/persistence/audit | src/test/.../data/ | Transactions, checksums, migrations, partial writes, rights records and deletion lineage |
| Android platform | src/androidTest/.../platform/ | Permissions, process lifecycle, foreground scheduling, capture safeguards and runtime/provider integration |
| Feature UI | Supported local Compose tests and/or src/androidTest/.../feature/ | User-visible states, controls, privacy review and error presentation |
| Native/JNI | Native module tests plus Android instrumentation | ABI, memory ownership, concurrency, crash safety and Kotlin/native parity |

Test package paths SHOULD mirror production package paths. Cross-layer tests may live in the narrowest owning module's test source set or in a dedicated integration suite only when multiple modules are extracted. A test result is tied to the exact code revision, device/software environment and test data. File placement or a passing test that does not cover the claimed behavior does not establish support.

## 13. Rules for changing project structure

Every proposed package move, new module, source-set change or build-logic extraction must include:

1. current owner and target owner of every affected file;
2. why the existing boundary is insufficient;
3. dependency graph before and after;
4. source-set, manifest, resource, DI, serialization and test impact;
5. public package/API or persisted-schema impact;
6. rollback strategy and compatibility impact;
7. required tests and exact verification scope;
8. updates to this document, document 36, document 17 and other direct dependents.

A structure migration must be mechanical and reviewable. Do not combine a large package move with neural math rewrites, dependency upgrades, model-architecture changes, UI redesign or data-schema migrations in one unbounded change.

Future migration order:

1. approve target package and module ownership;
2. map declarations/usages and external references;
3. establish dependency graph and forbidden-dependency checks;
4. move one bounded subsystem at a time;
5. update tests, DI and manifest/resource references;
6. verify the moved subsystem against the same contract;
7. update the repository baseline and this structure contract.

The project owner has authorized bounded package/path relocation and the required import/manifest alignment. That authorization does not extend to Gradle module extraction, dependency upgrades, behavior rewrites, storage-schema changes or architecture/backend selection.

## 14. Unresolved structural decisions

- exact Gradle module count, IDs and extraction order;
- No DI framework is selected or required. Explicit manual constructor injection through `app/bootstrap` is the default for the current single-module topology; a future framework decision is optional and must be justified by measured graph complexity or a concrete module boundary;
- whether core contracts share one module or several independently tested modules;
- local persistence split, Room/DataStore/file formats and encryption boundary;
- native engine module, C++ ABI, NDK/CMake/STL versions and supported ABIs;
- exact split between platform/android/compute provider integration and engine-side numerical execution;
- location and packaging model for future bundled corpus/tokenizer assets;
- whether `public/` has an active non-Android tooling/branding consumer;
- build convention plugin strategy and static-analysis toolchain;
- benchmark module and physical-device result format.

These decisions depend on architecture, tokenizer, storage/security, backend and verification contracts. Their open state does not prevent establishing package ownership and file-placement rules now.

## 15. Current disposition and authorization

- **Current physical source layout:** one Android Gradle module, `:app`; current packages include `app/bootstrap`, `app/entry`, `app/navigation`, `application/runtime`, `feature/*/presentation`, `data/acquisition`, `platform/android/*` and `ui/design`.
- **Target logical package hierarchy:** defined in Section 4 and cross-mapped to the logical subsystems in document 36.
- **Dependency direction:** defined in Section 5; concrete source dependencies still need remediation.
- **Composition strategy:** `app/bootstrap/ArgusContainer` uses explicit manual constructor injection. `ArgusApp` is the Android host and no longer exposes the former global companion-object service locator.
- **Runtime ingress:** Android services consume the transitional `application/runtime` interfaces through `ArgusRuntimeOwner`; aggregation and focused port declarations are separated by file, while final workflow-specific API ownership remains open.
- **Presentation ownership:** ChatMessage, ChatUiState, SettingsUiState, LearningUiState and PerceptionUiState reside in their feature presentation packages. The shared ViewModel still coordinates several features and must not be mechanically moved wholesale into one feature.
- **Gradle module extraction:** intentionally deferred until stable contracts and concrete dependency boundaries justify it.
- **Native C++/JNI structure:** documented as a future isolated boundary; no native implementation exists in the inspected tree.
- **File ownership/dependency audit:** recorded in [46 — Android Source-Tree Ownership and Dependency Audit](46_ANDROID_SOURCE_TREE_OWNERSHIP_AND_DEPENDENCY_AUDIT.md).
- **Package/interface ownership and consumer/provider mapping:** proposed in [47 — Package Interface and Dependency Contract Matrix](47_PACKAGE_INTERFACE_AND_DEPENDENCY_CONTRACT_MATRIX.md).
- **Personal capture and experience intake:** proposed in [48 — Personal Capture and Experience Intake Contract](48_PERSONAL_CAPTURE_AND_EXPERIENCE_INTAKE_CONTRACT.md); final API, surface allowlist, retention and storage bindings remain open.
- **Source movement:** bounded package/path relocation and import/manifest alignment are authorized. The initial move and subsequent composition/runtime-ingress changes are recorded in Git history; further moves must follow file ownership and preserve manifest/resource/serialization compatibility.
- **Gradle modules, build configuration, dependency inventory, model algorithms and storage schemas:** unchanged in this bounded file-placement update.
- **Build, test, GitHub Actions, artifact upload and device execution:** not run or claimed for this update.

The initial migration is not complete architecture remediation. The monolithic ViewModel, mixed model prototypes, aggregate runtime-port package and remaining platform/presentation coupling stay open for separately bounded work.

## 16. Change history

- 2026-11-04 — Initial Android project structure and file-placement contract prepared from the inspected `main` source tree. No application files or Gradle configuration were changed.
- 2026-11-04 — Linked the package/interface contract crosswalk in document 47. Target ownership and dependency rules remain unchanged; no source or build files were changed.
- 2026-11-05 — Added the document 48 personal-experience intake crosswalk to clarify application/knowledge ownership and the separate application/learning admission boundary. No package or source tree was created or moved.
- 2026-11-05 — Closed the design-level UI state, notification route and manual composition contracts in alignment with documents 36 and 47. No package, source file or Gradle module was created or moved.
- 2026-11-05 — Project owner authorized the first bounded source-structure migration. Android entry/navigation, feature screens, shared UI design, Android capture/device/lifecycle adapters and the web-search connector were moved to their assigned Kotlin package roots. Import and Android manifest class references were aligned. The migration did not change Gradle module topology, dependencies, model algorithms or workflow behavior; no build/test result is claimed.

- 2026-11-05 — Follow-up source structure reviewed after composition-root/runtime-ingress commits `d9c3924` and `8a9014c`. Extracted the chat presentation message type into `feature/chat/presentation/ChatMessage.kt` and aligned the ViewModel import. Refreshed the physical-path crosswalk; no behavior, model, Gradle or schema changes were intended. No build/test result is claimed.

- 2026-11-05 — Split the transitional runtime contract source file into focused interface/snapshot files under the same `application/runtime` package. Package names, public type names and method signatures were preserved; no runtime behavior or dependency direction was changed. Updated the physical structure crosswalk. No build/test result is claimed.

- 2026-11-05 — Added `feature/settings/presentation/SettingsUiState.kt` and changed the shared ViewModel to project installed-app, search-result and world-entity records into immutable Settings-owned UI records. The Settings screen now collects one feature state object; the underlying adapters and data/model records were not changed. No build/test result is claimed.

- 2026-11-05 — Added feature-owned Learning and Perception UI state projections. The shared ViewModel maps legacy training, device-safety and hardware records into those immutable types; both screens now collect one feature state each. Command routing remains transitional, and no model/backend behavior was changed. No build/test result is claimed.

- 2026-11-05 — Added `feature/chat/presentation/ChatUiState.kt`; ChatScreen now collects the immutable chat feature state rather than separate message and generation flows. Message contents and generation behavior were preserved. No build/test result is claimed.
