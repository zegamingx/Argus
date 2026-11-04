# Argus Android Project Structure and File-Placement Rules

**Status:** Proposed target repository/package structure; no source-tree migration authorized  
**Last reviewed:** 2026-11-04  
**Repository observation:** `main` at commit `c3d9a47d5bb02f28349345a92b1434d8ea77665f`  
**Scope:** Android repository layout, Kotlin package ownership, subsystem boundaries, Gradle module extraction, native/JNI boundary, resource and test placement, build configuration ownership and migration rules  
**Dependencies:** Documents 01, 02, 15, 17–21, 23, 35, 36, 43 and 44  
**Boundary:** Structural design only. This document does not create folders, move or rename source files, add Gradle modules, select the neural architecture/tokenizer/backend, change build configuration or authorize implementation, builds, tests, workflows or device experiments.

## 1. Purpose

Argus needs a project structure that can support an Android application, local model lifecycle, tokenizer, training and inference, device capability assessment, knowledge acquisition, policy enforcement, persistent data, Android services and a future native compute engine without coupling UI, platform APIs and numerical kernels.

This document defines the observed source layout, target Kotlin package organization, dependency direction, criteria for Gradle module extraction, Kotlin/native boundary, resource/test/configuration placement, prototype-file ownership and rules for a future migration.

The structure is architecture-neutral. It does not select Transformer, Mamba-2, RWKV-7, a hybrid, tokenizer family, optimizer, precision, GPU/NPU provider or model profile.

## 2. Observed repository structure

The inspected `main` tree at the repository observation commit contains one Android Gradle application module, `:app`, and no additional Gradle modules.

The current application namespace is `com.argus.agi`. Principal source paths are:

| Current path | Observed contents |
|---|---|
| `app/src/main/java/com/argus/agi/ArgusApp.kt` | Application subclass and global lazy service access |
| `app/src/main/java/com/argus/agi/MainActivity.kt` | Activity, app navigation selection and Compose shell |
| `app/src/main/java/com/argus/agi/model/` | Experimental neural engine, tokenizer, training manager and in-memory world model |
| `app/src/main/java/com/argus/agi/sensor/` | Hardware profile, device guard, excluded-app manager and web search |
| `app/src/main/java/com/argus/agi/service/` | Accessibility and foreground services |
| `app/src/main/java/com/argus/agi/ui/` | Broad ViewModel, screens and theme |
| `app/src/test/` | One Robolectric application test |
| `app/src/main/res/` | Android drawables, launcher icons, values and XML resources |

The inspected root also contains `settings.gradle.kts`, root `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, the Gradle wrapper, `docs/` and `public/assets/argus_icon.png`.

Observed limitations:

1. `model/` combines domain concepts, tokenizer, experimental numerical code, training orchestration and memory-like state.
2. `sensor/` mixes stable hardware identification, transient device conditions and network retrieval.
3. `ui/ArgusViewModel.kt` coordinates multiple features and directly knows about engine, service, network and device classes.
4. `ArgusApp` exposes process-wide lazy objects through a global companion object, complicating ownership, lifecycle and testing.
5. Android-specific classes and model logic are not separated by enforceable dependency boundaries.
6. No native C++/JNI source or verified native runtime exists in the inspected tree.
7. Current tests do not represent model, tokenizer, data, policy, lifecycle or backend contracts.
8. `public/assets/argus_icon.png` is outside Android's resource path; its runtime/build consumer was not established by this audit.

These are observations about the prototype, not instructions to copy its package structure. Existing paths remain intact until a separately approved migration.

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

The following is the target logical package hierarchy under the existing namespace `com.argus.agi`. During the first authorized structural migration, these boundaries may initially remain inside `:app`. This is not a statement that every folder currently exists.

```text
com.argus.agi
├── app/
│   ├── bootstrap/          # Application entry, startup and composition root
│   ├── lifecycle/          # App readiness and process lifecycle coordination
│   └── di/                 # Dependency graph construction, if DI is used
├── navigation/             # Routes and destination wiring
├── feature/
│   ├── chat/presentation/
│   ├── neuralcore/presentation/
│   ├── perception/presentation/
│   ├── knowledge/presentation/
│   └── settings/presentation/
├── core/
│   ├── identity/           # Instance, generation, session and stable IDs
│   ├── policy/             # Consent, authorization, retention and trust rules
│   ├── device/             # Capability and runtime-condition contracts
│   ├── genesis/            # First-generation lifecycle and readiness contracts
│   ├── model/              # Model configuration and generation contracts
│   ├── tokenizer/          # Tokenizer API, identity and manifest contracts
│   ├── inference/          # Inference session and request contracts
│   ├── training/           # Training session, update and recovery contracts
│   ├── compute/            # Backend capability and execution contracts
│   ├── knowledge/          # Knowledge-domain and memory contracts
│   ├── acquisition/        # Discovery/fetch job and source-operation contracts
│   └── evaluation/         # Evaluation definitions and promotion criteria
├── data/
│   ├── local/              # Local metadata and transactional persistence
│   ├── model/              # Generations, tensor files and checkpoint repositories
│   ├── corpus/             # Corpus manifests and dataset readers
│   ├── knowledge/          # Knowledge records, indexes and retrieval repositories
│   └── acquisition/        # Source registries and concrete connectors
├── platform/
│   └── android/
│       ├── lifecycle/      # Android lifecycle and background scheduling
│       ├── capture/        # Accessibility, camera and authorized capture
│       ├── device/         # Android hardware/OS capability observation
│       ├── network/        # Android networking and connectivity
│       ├── storage/        # Android filesystem, SAF and platform keys
│       └── notification/   # Foreground-service and notification adapters
├── engine/
│   ├── reference/          # Correctness-first Kotlin reference operations
│   ├── tokenizer/          # Concrete tokenizer engines selected after G1-D
│   └── backend/            # Compute dispatch and backend adapters
└── ui/
    └── design/             # Shared Compose design system and visual primitives
```

### 4.1 Package ownership

| Package root | Owns | Must not own |
|---|---|---|
| `app` | Process entry, startup, dependency construction and app lifecycle | Neural math, rights decisions or storage implementation |
| `navigation` | Typed destinations and navigation wiring | Business rules or persistence |
| `feature/*/presentation` | Screen state, events, ViewModels and UI mapping | Direct SQL/file access, JNI, tensor mutation or connector policy |
| `core/identity` | Stable IDs and identity invariants | Android Context, database entities or UI models |
| `core/policy` | Purpose-scoped permission and policy decisions | Direct fetching, capture implementation or UI state |
| `core/device` | Capability/condition models and admission contracts | Guessing accelerator support from branding |
| `core/genesis` / `core/model` | Initialization and immutable-generation contracts | Neural family implementation or direct file I/O |
| `core/tokenizer` | Tokenizer contract, identity and deterministic behavior requirements | An unselected tokenizer implementation or implicit vocabulary mutation |
| `core/training` / `core/inference` | Session/request semantics and lifecycle contracts | Android scheduling, presentation and raw storage writes |
| `core/compute` | Backend interfaces and execution coverage contracts | Vendor SDK calls or false acceleration claims |
| `core/knowledge` / `core/acquisition` | Knowledge/source rules and job contracts | Source permission self-approval or hidden network access |
| `core/evaluation` | Metrics, candidate acceptance and promotion rules | Direct mutation of an active generation |
| `data` | Persistence, queries, mapping and concrete external-data implementations | Changing domain policy or granting consent |
| `platform/android` | Android framework integration and lifecycle adapters | Product decisions or neural mathematics |
| `engine` | Tokenizer and numerical execution behind stable contracts | UI, consent, source rights or navigation |
| `ui/design` | Shared Compose components, typography, shapes, colors and theme | Feature business logic or direct data access |

Subpackages such as `api`, `internal`, `repository` or `serialization` may be added when they represent real ownership. Do not create them preemptively.

## 5. Dependency direction and forbidden dependencies

The intended direction is:

```text
feature presentation ───────┐
navigation ─────────────────┤
                            v
                  app / application use cases
                            |
             +--------------+----------------+
             v              v                v
         core policy     core domain     core lifecycle
             ^              ^                ^
             |              |                |
       platform adapters  data adapters   engine adapters
             |              |                |
       Android APIs       Room/files      Kotlin/native kernels
```

The diagram is conceptual. Concrete wiring is owned by the application composition root.

Mandatory rules:

1. `core/**` MUST remain free of Android framework, Compose, Activity, Service, Context, Room, Retrofit/OkHttp, vendor SDK and JNI implementation dependencies.
2. `feature/**` MUST depend on application-facing use cases/state contracts, not concrete data or engine implementation classes.
3. `data/**` MUST implement contracts and MUST NOT redefine consent, rights, retention or training eligibility.
4. `platform/android/**` MAY depend on Android APIs and core contracts. Core MUST NOT depend back on platform packages.
5. `engine/**` MUST implement declared tokenizer/compute/model contracts. Engine code MUST NOT know about Composables, Activity, ViewModel or navigation.
6. The native bridge MUST be called through a typed Kotlin engine adapter. UI and domain code MUST NOT call JNI entry points.
7. The training engine MUST NOT select or authorize personal data. It receives only an approved immutable dataset manifest.
8. Acquisition connectors MUST NOT authorize their own source, retention or training permissions.
9. Repositories expose domain-facing results; database rows and network DTOs do not leak into feature presentation.
10. Dependency cycles are prohibited at package and Gradle-module level.
11. Cross-layer calls use explicit interfaces and typed outcomes, not reflection or global mutable registries.
12. Backend selection and fallback report the implementation that actually executed.

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

These are candidate boundaries, not approved Gradle module declarations:

| Candidate boundary | Why it may be extracted | Gate before extraction |
|---|---|---|
| Android application shell | Entry, navigation, feature composition and Android packaging | Stable feature/use-case contracts |
| Core contracts/policy | Pure Kotlin APIs and policy-independent data types | Stable cross-subsystem contracts and dependency audit |
| Local data adapters | Room, file repositories, manifests and migrations | Storage, encryption, deletion and schema policy closure |
| Android platform adapters | OS services, capture, notifications, scheduling and hardware observation | Exact permission/lifecycle/API contracts |
| Reference engine | Numerical correctness path independent of UI | Selected model/operator contracts and verification method |
| Native engine | JNI/NDK/CMake build and backend-specific binaries | Architecture, operator subset, ABI, memory ownership and build/toolchain contract |

The exact Gradle module names and count remain open. Select them in the implementation plan using the actual dependency graph.

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

This is a future ownership map, not an instruction to move files now.

| Current file/package | Target owner | Structural treatment |
|---|---|---|
| `ArgusApp.kt` | `app/bootstrap` | Application composition root; global service-locator ownership is removed only in an approved refactor |
| `MainActivity.kt` | `app` plus `navigation` | Activity remains at app entry; route definitions move out of the Activity |
| `model/ArgusNeuralEngine.kt` | `engine/reference` or quarantined legacy area | Experimental numerical code; not the selected production engine |
| `model/Tokenizer.kt` | `core/tokenizer` contract plus selected `engine/tokenizer` implementation | Format/algorithm remains open until G1-D closes |
| `model/TrainingManager.kt` | `core/training` contract plus engine/platform adapters | Decompose by responsibility; do not just rename the file |
| `model/WorldModel.kt` | `core/knowledge` plus `data/knowledge` | In-memory prototype is not durable memory architecture |
| `sensor/HardwareProfile.kt` | `platform/android/device` | Capability evidence provider; heuristic branding is not verified support |
| `sensor/DeviceGuard.kt` | `platform/android/device` plus `core/device` | Separate Android observations from admission policy |
| `sensor/ExcludedAppsManager.kt` | `platform/android/capture` and/or policy-backed repository | Final owner depends on permission and persistence contract |
| `sensor/WebSearchEngine.kt` | `core/acquisition` plus `data/acquisition` | Network client does not own source permission or training eligibility |
| `service/ArgusAccessibilityService.kt` | `platform/android/capture` | Android platform adapter |
| `service/ArgusForegroundService.kt` | `platform/android/lifecycle` / `notification` | Android execution adapter; job ownership remains in application orchestration |
| `ui/ArgusViewModel.kt` | Individual `feature/*/presentation` owners | Split by feature; do not create another monolithic ViewModel |
| `ui/screens/*` | Corresponding `feature/*/presentation` | Screens and UI state remain feature-owned |
| `ui/theme/*` | `ui/design` | Shared design system |

Before any move, inspect usages, DI construction, manifest references, serialization names, tests, reflection/ProGuard rules and resource references. This mapping does not authorize a bulk package rename.

## 11. Top-level repository ownership

| Path | Purpose and rule |
|---|---|
| `app/` | Existing Android application module and production/test source sets |
| `docs/` | Requirements, architecture, evidence, decisions, lifecycle and governance |
| `gradle/` | Version catalog and wrapper; no project source or secrets |
| `tools/` | Future reproducible maintenance/research tooling only with named owner, inputs, outputs and environment |
| `benchmarks/` | Future benchmark harnesses/results only with defined methodology and provenance |
| `public/` | Existing repository static assets; identify the current consumer of `public/assets/argus_icon.png` before moving or duplicating |
| root `engine/`, `core/`, `data/`, `platform/` | Future Gradle module roots only after extraction approval; do not create empty directories |
| `build/`, `.gradle/`, generated outputs | Local/generated state, never source-of-truth inputs or committed runtime artifacts |

Do not add `datasets/`, `models/`, `checkpoints/`, `secrets/` or personal-data directories as casual source-tree storage. Distributable fixtures/datasets require documented provenance, rights, size, checksum, update and packaging policy. User checkpoints, memory and training data are runtime user data, not Git assets.

## 12. Test and verification organization

| Test category | Preferred location | Required focus |
|---|---|---|
| Core/domain/policy | `src/test/.../core/` | Invariants, typed errors, consent defaults and fail-closed behavior |
| Tokenizer | `src/test/.../tokenizer/` | Fixtures, Unicode, special-token boundaries, determinism and fingerprint |
| Numerical engine | `src/test/.../engine/` | Reference parity, finite values, gradients, optimizer updates and deterministic initialization |
| Training lifecycle | `src/test/.../training/` | Pause/resume, cursor consistency, checkpoint/recovery and cancellation |
| Data/persistence | `src/test/.../data/` | Transactions, checksums, migrations, partial writes and deletion |
| Android platform | `src/androidTest/.../platform/` | Permissions, process lifecycle, foreground scheduling and platform integration |
| UI | `src/androidTest/.../feature/` or supported local Compose tests | User-visible states, controls, privacy review and error presentation |
| Native/JNI | Native module tests plus Android instrumentation | ABI, memory ownership, concurrency, crash safety and Kotlin/native parity |

A test result is tied to the exact code revision, device/software environment and test data. File placement or a passing test that does not cover the claimed behavior does not establish support.

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

No such migration is part of the current documentation-only phase.

## 14. Unresolved structural decisions

- exact Gradle module count, IDs and extraction order;
- DI library versus a small explicit composition root;
- whether core contracts share one module or several independently tested modules;
- local persistence split, Room/DataStore/file formats and encryption boundary;
- native engine module, C++ ABI, NDK/CMake/STL versions and supported ABIs;
- backend module split for CPU, Vulkan and vendor providers;
- location and packaging model for future bundled corpus/tokenizer assets;
- whether `public/` has an active non-Android tooling/branding consumer;
- build convention plugin strategy and static-analysis toolchain;
- benchmark module and physical-device result format.

These decisions depend on architecture, tokenizer, storage/security, backend and verification contracts. Their open state does not prevent establishing package ownership and file-placement rules now.

## 15. Current disposition and authorization

- **Current physical source layout:** one Android Gradle module, `:app`, with the legacy package structure in Section 2.
- **Target logical package hierarchy:** proposed in Section 4.
- **Dependency direction:** defined in Section 5.
- **Gradle module extraction:** intentionally deferred until stable contracts and concrete dependency boundaries justify it.
- **Native C++/JNI structure:** documented as a future isolated boundary; no native implementation exists in the inspected tree.
- **Source movement, new packages/modules, build-configuration changes and code changes:** not authorized.
- **Build, test, GitHub Actions, artifact upload and device execution:** not authorized by this document.

This document may be used to audit/classify current files and prepare a future structural migration plan. Actual changes require separate explicit project-owner authorization and the applicable implementation phase.

## 16. Change history

- 2026-11-04 — Initial Android project structure and file-placement contract prepared from the inspected `main` source tree. No application files or Gradle configuration were changed.
