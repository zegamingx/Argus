# Current Repository Baseline

**Repository:** `zegamingx/Argus` · **Branch:** `main`  
**Inspected commit:** `e80afc8979fb6d51cbc46466d2b15a5a61506e00`  
**Inspection date:** 2026-11-03

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

README badges state AGP 8.7.x, Gradle 8.13 and Kotlin 2.1; these differ from checked-in build configuration.

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

The manifest declares INTERNET, ACCESS_NETWORK_STATE, CAMERA, notification/foreground-service and WAKE_LOCK permissions, and an AccessibilityService. Gradle includes a Firebase AI dependency. These are audit observations, not proof of data transmission; they require a dependency/network audit before a zero-cloud claim is considered verified.

## 6. Repository hygiene risk

The public repository tree contains `my-upload-key.jks`. Private signing material must not be tracked publicly. Treat this as a high-priority exposure for owner-led investigation: determine whether it is a live upload key, rotate/replace as appropriate, review where used and clean Git history where feasible. Never reproduce its contents.

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
