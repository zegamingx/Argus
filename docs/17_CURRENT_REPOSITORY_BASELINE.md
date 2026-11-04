# Current Repository Baseline

**Repository:** `zegamingx/Argus` · **Documentation branch:** `main`  
**Repository HEAD cross-checked:** `ec67ac6dde0f2c49927e936da1a156c038679053`  
**Source inspection baseline:** `4d8104d4400dcd858e648d7b08ac0718385733d3`  
**Inspection date:** 2026-11-04

A Git comparison from the source inspection baseline to the cross-checked `main` HEAD found no changed application-source paths in that interval; the intervening changes are documentation and repository guidance. The implementation facts below therefore describe the source snapshot at `4d8104d`, with `ec67ac6dde0f2c49927e936da1a156c038679053` recorded as the documentation/repository revision used for this cross-check.

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

Inspected packages are `model/` (ArgusNeuralEngine, Tokenizer, TrainingManager, WorldModel), `sensor/` (HardwareProfile, DeviceGuard, ExcludedAppsManager, WebSearchEngine), `service/` (Accessibility and foreground services) and `ui/` (ViewModel and Compose screens).

The README's described `core/agent`, `core/memory`, `core/training`, native C++ and hardware-router tree is not present in the inspected source tree.

## 3. Hardware discovery baseline

`HardwareProfile.kt` reads Android SoC properties with fallbacks, identifies likely Qualcomm/MediaTek from manufacturer/board strings, checks a Vulkan hardware-version feature and assigns likely NPU/APU labels from vendor identity.

This is **preliminary heuristic identification**, not verified NPU access. It does not prove that a third-party APK can load a runtime, that a model/operator/precision is supported, that a GPU compute queue is usable, or that a model actually executed on the accelerator. A Vulkan feature flag is not a substitute for queue/feature/driver validation and a compute test.

## 4. Neural, training and memory baseline

`ArgusNeuralEngine.kt` is a Kotlin experimental transformer-like implementation (inspected dimensions include dModel 64, 4 heads and context 128). It is not evidence of a production large-model runtime or native CPU/GPU/NPU execution.

`TrainingManager.kt` has a queue/worker prototype. Correctness, optimizer coverage, checkpoint recovery, foreground lifecycle, data governance and sustained performance require independent verification.

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
