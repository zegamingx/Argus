# Current Repository Baseline

**Repository:** `zegamingx/Argus` · **Documentation branch:** `main`  
**Behavioral/source-logic baseline:** `4d8104d4400dcd858e648d7b08ac0718385733d3`  
**Latest structural migration checkpoint:** `b0441af8df127897f3ef579c22116d85337feea6`  
**Latest source-continuity cross-check:** `09c72df12371670fc0417d47f5a0e6644537aa60`  
**Latest startup/bootstrap source HEAD:** `c805118a5001a30ff1b7e69025efc2865dd8a1bd`  
**Inspection date:** 2026-11-05

The Kotlin implementation and build configuration were behaviorally unchanged by the initial package migration, but source paths, package declarations and manifest component names did change. The source-logic observations below remain based on `4d8104d`; current physical paths are recorded at structural checkpoint `b0441af8`. No build or test was run for the migration, so compilation and manifest/component resolution remain unverified.

This record separates what was found in the inspected source snapshot from proposed architecture.

## 1. Build and platform facts

Observed in `app/build.gradle.kts`, `gradle/libs.versions.toml` and `gradle/wrapper/gradle-wrapper.properties`:

| Item | Value |
|---|---|
| Namespace/application ID | `com.argus.agi` |
| minSdk | 33 |
| compileSdk | 36, minor API level 1 |
| targetSdk | 36 |
| AGP catalog | 9.1.1 |
| Kotlin catalog | 2.2.10 |
| Gradle wrapper | 9.3.1 |
| Java compile source/target | Java 11 |
| README JDK expectation | JDK 21 |
| Native CMake/JNI source | No `app/src/main/cpp/CMakeLists.txt` found in inspected tree |

README badges match the checked-in AGP, Gradle and Kotlin versions. Release signing is optional and uses externally supplied configuration; no private signing artifact is stored in the current source tree.

## 2. Source layout

Current physical packages at structural checkpoint `b0441af8` are: `app/entry` (ArgusApp and MainActivity), `app/navigation` (MainScreen), `feature/*/presentation` (four feature screens), `ui/design` (shared theme), `platform/android/capture` (AccessibilityService and installed-app adapter), `platform/android/device` (DeviceGuard and HardwareProfile), `platform/android/lifecycle` (foreground service), and `data/acquisition` (WebSearchEngine).

Three mixed prototype classes remain in `model/` (ArgusNeuralEngine, Tokenizer, WorldModel), and the cross-feature Android ViewModel remains in `ui/ArgusViewModel.kt`. The former `TrainingManager` has been removed with its raw-string queue/worker; no `application/learning` coordinator replaces it. These are explicit migration debt, not target architecture owners. The README's described `core/agent`, `core/memory`, `core/training`, native C++ and hardware-router implementation is not present in the source tree. The file-by-file pre-migration ownership audit and current migration checkpoint are maintained in [46 — Android Source-Tree Ownership and Dependency Audit](46_ANDROID_SOURCE_TREE_OWNERSHIP_AND_DEPENDENCY_AUDIT.md).

## 3. Hardware discovery baseline

`HardwareProfile.kt` reads Android SoC properties with fallbacks, identifies likely Qualcomm/MediaTek from manufacturer/board strings, checks a Vulkan hardware-version feature and assigns likely NPU/APU labels from vendor identity.

This is **preliminary heuristic identification**, not verified NPU access. It does not prove that a third-party APK can load a runtime, that a model/operator/precision is supported, that a GPU compute queue is usable, or that a model actually executed on the accelerator. A Vulkan feature flag is not a substitute for queue/feature/driver validation and a compute test.

## 4. Neural, training and memory baseline

`ArgusNeuralEngine.kt` is a Kotlin experimental transformer-like implementation (inspected dimensions include dModel 64, 4 heads and context 128). It is not evidence of a production large-model runtime or native CPU/GPU/NPU execution.

The legacy queue/worker prototype and `ArgusNeuralEngine.trainStep(String)` have been removed from the current tree. Training is unavailable pending a verified `application/learning` workflow; the old algorithm was retired, not repaired. Genesis, inference, tokenizer, checkpoint and device behavior still require independent verification.

`WorldModel.kt` stores entities in an in-memory concurrent map and exposes state flows. It is not yet a durable episodic/semantic/procedural/vector memory store.

## 5. Permission/network observations

The manifest declares INTERNET, ACCESS_NETWORK_STATE, CAMERA, notification/foreground-service and WAKE_LOCK permissions, and an AccessibilityService. The Google AI Studio-specific Secrets and Google Services plugins, Firebase AI/App Check dependencies, environment example and generated metadata file have been removed from the current main branch. Ordinary Android/AndroidX tooling and KSP remain. Web retrieval makes outbound HTTP requests to public search endpoints.

## 6. Repository hygiene risk

A previously tracked private signing artifact has been removed from the current main tree. This does not erase historical Git objects, other branches or forks. Treat the credential as potentially exposed: verify whether it was active, rotate or replace it as appropriate, and plan Git-history cleanup. Do not reproduce its contents. Release signing now depends on externally supplied configuration.

## 7. Scope classification

| Area | Observed status |
|---|---|
| Android Compose app shell | Implemented prototype |
| Kotlin experimental neural engine | Implemented prototype; not certified |
| Preliminary hardware profile | Implemented heuristic only |
| Verified Vulkan compute | Not evidenced in inspected source |
| Qualcomm QAIRT/QNN | Not evidenced |
| MediaTek NeuroPilot | Not evidenced |
| Native C++/JNI runtime | Not evidenced |
| Persistent encrypted memory | Not evidenced by current WorldModel |
| Multi-agent orchestrator described in README | Not evidenced in inspected source |
| Full continual-learning pipeline described in README | Not evidenced in inspected source |
| Test-backed compatibility matrix | Not present |

"Not evidenced" means the inspected snapshot did not show the capability; it is not a claim that no external experiment exists.

## 8. Maintenance

Reinspect before implementation planning and whenever source/build dependencies change. Update this record with commit SHA, date, paths and test evidence. Do not silently convert a README design statement into an implementation claim.


### Current learning-path delta — 2026-11-05

Commits `6df8603` and `9802085` removed the legacy `TrainingManager` worker and the raw-text `ArgusNeuralEngine.trainStep(String)` entry point. The current app has no autonomous learning worker. The Learning UI and foreground notification expose the capability as unavailable until the approved-candidate, session-admission and recovery contracts are implemented. No build or test was run for these commits.


### Current startup-flow delta — 2026-11-05

The app entry now uses `ArgusStartupViewModel` to perform two concrete startup operations before constructing the feature `ArgusViewModel`:

1. Read the preliminary Android hardware profile on `Dispatchers.IO`.
2. Construct the existing experimental `ArgusNeuralEngine` on `Dispatchers.Default`.

The resulting hardware profile is cached by `ArgusContainer` and passed into the feature ViewModel, avoiding a second hardware-profile read on the UI thread. The startup UI reports only these real operations and opens the current prototype workspace after the runtime constructor returns. Startup failure is surfaced with a retry action.

This is **not Local Knowledge Genesis**: it does not select an approved architecture/tokenizer bundle, commit a complete initial model generation, or schedule general foundation learning. The UI does not emit `GENESIS_READY` or claim that background learning has started. The prototype limitation is stated on the boot screen.

No Gradle build, automated test, emulator test or physical-device test was run for these changes. Compilation and Android runtime behavior therefore remain unverified.

### Startup side-effect boundary — 2026-11-05

The startup entry no longer starts `ArgusForegroundService` or requests camera/notification permissions merely because the preliminary runtime constructor completed. Those actions must be initiated by the feature or user action that actually needs them. This prevents prototype startup from implying that background learning or sensory capture has begun, and avoids requesting permissions before the user enters the relevant feature.

The startup coordinator still performs only preliminary hardware-profile retrieval and construction of the experimental `ArgusNeuralEngine`. It is not Genesis and does not publish a committed model generation.
