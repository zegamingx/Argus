# Device-Adaptive Model and Training

**Status:** Accepted adaptive-design direction; thresholds and implementation remain open  
**Last reviewed:** 2026-11-05

## 1. Two different adaptation problems

Argus uses two separate control loops:

1. **Genesis-time capability adaptation** selects a model configuration that the device can construct, store, train and execute.
2. **Runtime workload adaptation** adjusts or pauses active work according to current device conditions.

The first defines a validated capability envelope. The second stays inside that envelope. Neither is permission to mutate model architecture in the middle of training.

## 2. Argus Device Capability Profile

The profile should include evidence and confidence, not just device marketing metadata.

| Domain | Inputs |
|---|---|
| CPU | ABI, supported ISA features, core topology where exposed, measured kernel/compute performance |
| GPU | Vulkan loader/driver presence; physical-device identity; actual API version; compute-capable queue family; queried core features/extensions/limits; buffer/storage formats; FP32/FP16 arithmetic and storage support separately; operator validation, transfer cost and sustained benchmark |
| NPU/DSP | Optional vendor runtime/SDK availability, APK access rules, supported operators and precision, compile/load behavior; absence is not a configuration failure when a verified Vulkan-GPU or CPU path fits |
| Memory | App memory class, process limits, measured allocation behavior and baseline headroom; current free memory/pressure belongs to the runtime-condition snapshot |
| Thermal | Availability and semantics of OS thermal signals plus recorded sustained-behavior evidence; current thermal status belongs to the runtime-condition snapshot |
| Power | Availability/semantics of battery and power-policy telemetry; current charge/charging/power-saver state belongs to the runtime-condition snapshot |
| Storage | App-private storage limits and measured I/O behavior; current free space and checkpoint reserve belong to the runtime-condition snapshot |
| Tokenizer preparation | CPU capability, available memory and storage, seed-corpus size, trainer/runtime availability, vocabulary/head cost and bounded preparation budget; accelerator support is UNKNOWN unless measured |
| Platform | Android API level, lifecycle restrictions, ABI, Vulkan loader/driver versions, runtime/provider versions and permission state |

Unknown telemetry must remain unknown. Missing temperature or memory data is not a measured safe state. The versioned Device Capability Profile records stable hardware/runtime capability and measured baseline evidence; current thermal, battery, free-memory and storage-pressure values are short-lived Runtime Condition observations, not permanent device properties.

### 2.1 Android memory evidence and interpretation

The Device Capability Profile must keep three different memory observations separate:

| Signal | Interpretation | Use in Argus admission |
|---|---|---|
| ActivityManager.getMemoryClass() | Approximate per-application managed-heap class in MB; a sizing hint, not total process/native memory headroom | Record as platform evidence only. Never treat it as the complete model-training budget |
| ActivityManager.getMemoryInfo() | System-wide available/total memory, low-memory flag and threshold at which Android begins reclaiming/killing processes | Capture a timestamped workload-admission observation; do not confuse system-wide availability with a guaranteed app allocation |
| Anonymous RSS + swap, native/managed allocation telemetry, and file-backed page residency | Anonymous RSS + swap measures non-file-backed process memory, including managed/native heaps, anonymous mmap allocations and thread stacks. File-backed mapped pages are a separate residency category and may be reclaimed by the OS under pressure | Track native training tensors, runtime pools, anonymous allocations and file-backed model/asset page residency separately; do not merge file-backed pages into anonymous RSS + swap |
| ComponentCallbacks2.onTrimMemory() | OS pressure notification/trim signal; documented preferred response path rather than frequent getMemoryInfo() polling | Release rebuildable caches, reduce/defer work at a safe boundary and persist the event; do not use it to mutate model identity |

Android documents that heap limits vary by device and reaching the app heap limit can cause OutOfMemoryError; system memory pressure can trigger low-memory process kills. The memory class is therefore not a substitute for measured native-process peak or the device's changing system pressure. Keep process baseline, app/heap limit evidence, current system availability/threshold, anonymous RSS + swap, file-backed resident pages, native/runtime workspace and application reserve as separate fields. Capture expensive system observations at workload admission and relevant lifecycle/trim events rather than continuous high-frequency polling. The trim callback is a lifecycle/pressure hint, not a guaranteed byte count. Android's current guidance focuses on UI_HIDDEN and BACKGROUND events; beginning with Android 14, the other legacy trim notifications are no longer delivered, and those legacy constants were formally deprecated in Android 15. Do not make correctness or admission depend on receiving an obsolete trim-level callback.

Official references: [ActivityManager API](https://developer.android.com/reference/android/app/ActivityManager), [Manage your app's memory](https://developer.android.com/topic/performance/memory/manage-app-memory), and [Memory usage: anonymous RSS + swap](https://developer.android.com/topic/performance/issues/memory-usage). The ActivityManager reference says getMemoryClass() is approximate and getMemoryInfo() should not be polled continuously; the memory guide recommends onTrimMemory() and documents the current callback scope. These sources define Android signals; they do not provide a universal safe RAM number for Argus.


## 3. Configuration selection

The configuration engine evaluates candidate models against a bounded resource budget.

Account for parameters and tensor dtypes; embeddings/output head and architecture-specific state; activation/workspace memory for training; gradients and optimizer moments; context length and attention/recurrent cache; microbatch/concurrency and temporary copies; runtime/compiler allocation and OS headroom; model storage and checkpoint generations.

Do not derive model size from total RAM alone. Do not use a single fixed parameter count, tokenizer vocabulary size or training profile for all users.

An initial configuration is valid only when the selected architecture/operator set and tokenizer profile have supported execution paths and resource estimates with explicit safety margins. Account for tokenizer preparation separately from neural-model training, then account for the vocabulary-dependent embedding/output-head and optimizer-state costs in the selected model configuration. If no complete candidate bundle fits, Genesis must report the limitation rather than pretend a model was created.

### 3.1 Device-conditioned configuration portfolio

Argus maintains a reviewed portfolio of compatible configuration candidates rather than one globally fixed model/tokenizer configuration or a new ad hoc implementation for every phone. A candidate bundle binds at least:

- Architecture family/version and model dimensions.
- Tokenizer profile, exact artifact/configuration fingerprint and preparation mode.
- Vocabulary/head allocation and context/packing assumptions.
- Precision, supported operator set and eligible backend paths.
- Initial training objective/update profile and resource envelope.
- Minimum storage, checkpoint and recovery reserve.

Selection is a constrained decision, not a marketing-tier lookup. First reject candidates that fail correctness, rights, operator, storage or hard memory constraints. Then compare the remaining candidates across expected capability, memory, sustained compute, energy, thermal behavior and preparation cost. Record why the chosen bundle fits and which alternatives were rejected or unavailable. The tokenizer-specific staged eligibility, format/runtime, Genesis-preparation, model-fit, Pareto and deterministic tie-break procedure is proposed in [34 — Tokenizer Evaluation and Decision Framework](34_TOKENIZER_EVALUATION_AND_DECISION.md); the model-bundle portfolio uses the same hard-gate-before-ranking principle. Numeric tier thresholds must come from documented analysis and later device measurements, not chipset names or invented universal cutoffs.

Device capability does not decide which languages Argus is allowed or intended to support. Product language/domain policy and item-level rights determine the eligible tokenizer seed/evaluation data. Device capability determines which compatible tokenizer algorithm, vocabulary budget, artifact-preparation mode and model configuration can be afforded for that scope.

A profile may load a precomputed, versioned tokenizer artifact or train a tokenizer locally from a bounded, rights-cleared seed corpus. Local tokenizer training is an optional capability-gated preparation path, not a mandatory per-installation ritual and not neural-model pretraining. It has its own CPU, memory, storage and time budget. Do not assume GPU/NPU acceleration for tokenization training without measured support. Where local preparation does not fit, select a pre-approved compatible artifact/profile before model tensor allocation; never switch vocabulary or token IDs silently after Genesis.

A tokenizer may differ between installations. That is compatible with independent local model identity: each model generation pins its own exact tokenizer fingerprint, and tokenized datasets, checkpoints and evaluation reports must match it. It does not make model weights interchangeable across installations. Sharing an identical precomputed tokenizer artifact is still valid where two devices select the same profile.

A profile change caused by updated hardware/runtime capability may justify a new configuration candidate and generation migration. Transient conditions such as heat, battery state or temporary memory pressure only affect workload admission and safe runtime controls; they do not mutate the active model graph or tokenizer.

## 4. Runtime training-condition profile

While learning is active, monitor a timestamped Runtime Condition Profile using relevant OS signals and bounded measurements:
- Available memory and memory-pressure events.
- CPU load and sustainable thread capacity.
- Vulkan GPU/NPU availability, correctness and execution cost, including queue/device-loss and fallback events.
- Thermal status and sustained performance trend.
- Battery/power mode and charging state.
- Storage reserve and checkpoint capacity, including dataset shards, the temporary checkpoint being written, the last committed checkpoint retained for recovery, and candidate-generation staging.
- App foreground/background state, service/worker state and platform scheduling policy.
- User pause/stop preferences.

Admission must reserve simultaneous peak usage, not only final artifact size. Include active inference snapshots, candidate parameters, gradients, optimizer/scheduler/scaler state, activations/workspaces, runtime/provider allocations, streaming dataset buffers, checkpoint staging, and the previous durable checkpoint. A device is not admitted merely because model weights fit in RAM or a single checkpoint fits in free storage.

Sampling intervals must respect API cost and thermal overhead. Avoid high-frequency polling when a platform callback or coarser observation is sufficient.

## 5. Workload controls

Within validated limits, the controller may:
- Reduce worker concurrency or CPU thread count.
- Change microbatch size or accumulation strategy where the training profile defines equivalent semantics.
- Reduce the number of queued work units per execution window.
- Choose a validated backend for the current operation.
- Defer checkpoint work or pause at a safe update boundary.
- Pause training when memory, thermal, battery or OS constraints make it unsafe.
- Resume when admission conditions recover.

A change affecting the mathematical training definition, such as precision, optimizer, sequence packing or gradient reduction, must be versioned and validated. It must not be silently applied as an ordinary resource response. Changes to architecture dimensions, tokenizer identity, vocabulary, token IDs or model-state shape require a new generation/configuration contract, not a live training-session adjustment.

## 6. Training controller state model

~~~text
IDLE
  |
  v
ADMISSION_CHECK
  | accepted
  v
RUNNING
  | memory / thermal / battery / OS constraint
  v
PAUSING_AT_SAFE_BOUNDARY
  |
  v
PAUSED_OR_DEFERRED
  |
  | conditions recovered and policy permits
  v
ADMISSION_CHECK
~~~

A user stop is distinct from a temporary pause. A failed checkpoint or invalid numerical state is an error, not a pause.

## 7. Backend policy

Backend selection is per workload and per operator-capability contract, not a fixed assumption that every device owns an NPU.

**Portable execution roles:**

1. **CPU reference/fallback:** the required baseline for deterministic correctness and for devices without a usable accelerator. CPU ISA features such as ARM NEON are queried separately; CPU availability does not imply that every model profile fits or reaches a useful training rate.
2. **Vulkan GPU compute:** the primary general GPU-acceleration candidate when a Vulkan-capable GPU/driver exposes a compute queue and the required features. It does not require an NPU. Argus owns the compute pipelines/shaders and the forward/backward/update kernels; Vulkan itself is a low-level GPU API, not an autograd engine or an LLM-training framework.
3. **Vendor NPU/DSP runtime:** an optional backend selected only when the exact provider, driver, precision and complete required training graph are verified. NPU absence must never block a candidate that is valid on Vulkan or CPU.

**Vulkan admission contract:**

- Enumerate the actual physical device and query the loader/driver API version, device/driver identity, queue families and the compute-queue capability; reject a graphics-only queue for compute work.
- Query the exact core features, optional extensions, descriptor/storage limits, subgroup/workgroup limits and buffer alignment needed by each kernel. Do not infer Vulkan API/feature support from Android API level, Android release branding or GPU model strings alone.
- Use FP32 as the portable numerical/reference shader path. Android Vulkan requires FP32 shader arithmetic; FP16 arithmetic and 16-bit storage are optional capabilities and must be queried separately before compiling/selecting the corresponding pipeline. A supported FP16 arithmetic feature does not by itself prove 16-bit storage-buffer support.
- Compile/cache shader variants by GPU/driver/API and kernel revision. Pipeline creation, specialization, shader-cache storage, device-loss handling and driver changes are part of runtime identity and observability.
- Report per-operation execution: GPU Vulkan, CPU fallback, optional NPU provider, or unsupported. A partially accelerated graph must declare partition boundaries, synchronization, transfer/copy buffers and host/device residency; a CPU fallback inside a large tensor path may erase the expected GPU gain.
- For a full training path, verify forward, loss, backward, gradient accumulation/reduction, clipping, optimizer update, serialization and recovery—not only inference or matrix multiplication. At minimum audit embedding gather and gradient accumulation, linear/GEMM, QKᵀ/attention, softmax, value aggregation, RoPE, normalization, GELU/FFN, residuals, vocabulary projection/loss, optimizer kernels and all dtype conversions used by the selected graph.
- Measure peak shared/process memory, GPU allocations/workspaces, queue synchronization, host/device transfers, sustained throughput, energy and thermal behavior. GPU offload is rejected when the combined liveness or sustained execution budget does not fit.

The Android platform API version does not guarantee a particular Vulkan driver version on an already-upgraded device. Android's Vulkan documentation describes platform/launch requirements, while the runtime must still inspect the reported physical-device version and features. The current Android requirement matrix lists Vulkan 1.3 for devices launching with Android 13+ and Vulkan 1.4 for devices launching with Android 16+; those requirements are not permission to skip runtime feature discovery. Vulkan 1.1 devices also do not universally support FP16 storage buffers.

Do not make NNAPI the required Argus abstraction: Android deprecated its NDK API in Android 15. Treat any surviving NNAPI/vendor path as an optional provider adapter with explicit capability and fallback, not as the universal GPU/NPU selector.

The supported LiteRT GPU inference path is not proof of Argus training support. The current Google Play services documentation recommends the CompiledModel API for new GPU-accelerated work and marks the older Interpreter GPU delegate deprecated; an inference delegate does not satisfy Argus forward/backward/optimizer coverage by itself.

Official evidence: [Android Vulkan implementation requirements](https://source.android.com/docs/core/graphics/implement-vulkan), [Android Vulkan compute architecture](https://source.android.com/docs/core/graphics/arch-vulkan), [Vulkan compute shader dispatch](https://docs.vulkan.org/guide/latest/compute_shaders.html), [Android Vulkan precision guidance](https://developer.android.com/games/optimize/vulkan-reduced-precision), [NNAPI migration/deprecation](https://developer.android.com/ndk/guides/neuralnetworks/migration-guide), and [LiteRT Android GPU guidance](https://developers.google.com/edge/litert/android/java). These references establish APIs and capability rules, not Argus kernel availability or device performance.

## 8. Observability

Expose the actual active state and explain adaptations:
- Selected configuration and why it fits.
- Current backend for the work actually executed.
- Training throughput and recent measured interval.
- Memory, thermal and battery admission state.
- Whether work is running, paused, deferred, recovering or failed.
- Reason for latest pause/fallback.
- Last successful checkpoint and resumable progress.

The UI must not overstate precision of OS signals or claim training is real-time if displayed values are stale.

### 8.1 Android background execution and scheduling decision matrix

**Repository baseline observed 2026-11-05:** app minSdk 33, compileSdk 36 (minor API level 1), targetSdk 36 in app/build.gradle.kts. The requirements below are evaluated against that target; any targetSdk/compileSdk increase requires a fresh platform-policy review before implementation or release.

| Mechanism | Suitable workload | Platform constraints relevant to the current target | A.R.G.U.S. disposition |
|---|---|---|---|
| Direct Foreground Service | Explicitly user-started, ongoing, user-visible local training where a valid FGS type exists | Apps targeting API 31+ cannot generally start an FGS from the background without a listed exemption. Target API 34+ requires a declared service type and matching type permission. FGS raises visibility/priority but does not guarantee process survival or uninterrupted execution. | Candidate for a user-visible active session only after the exact service type, start path, notification and distribution-policy eligibility are established. Start from a visible user action; keep workflow state and checkpoints outside the Service. |
| WorkManager bounded/resumable work | Deferrable, constraint-aware, retryable or chunked work whose units can stop and resume from durable state | WorkManager supports long-running workers and explicitly lists local ML-model computation as an example. A long-running worker runs an FGS on WorkManager's behalf but still relies on JobScheduler. For target API 34+, runtime foreground info must specify a valid FGS type. On Android 16/API 36, long-running workers can exhaust app JobScheduler quota; direct FGS may be considered when quota behavior makes WorkManager unsuitable. | Suitable for bounded, independently resumable preparation/training chunks or deferred retry only after quota/type admission. It is not a guarantee of continuous training. A worker stop must preserve the last committed checkpoint. |
| Hybrid scheduling | WorkManager handles deferred eligibility/retry; a direct user-visible FGS handles an explicitly started active run | Each mechanism retains its own Android restrictions. Do not let both schedulers own or execute the same session concurrently. An FGS does not remove Android 16 JobScheduler quota from jobs that are still executing under JobScheduler/WorkManager. | A possible design, not a selected binding. One durable session owner arbitrates a single execution lease; only one engine writer may own a candidate/session. |
| User-initiated data-transfer job | User-requested data upload/download or other qualifying transfer | This is a transfer API, not a general-purpose compute execution exemption. | May serve a separately authorized dataset/model transfer if that product feature is later approved; it is not the training engine or a way to run local neural computation. |

#### 8.1.1 Foreground-service type and quota rules

- There is no dedicated Android foreground-service type named for neural-model training in the reviewed platform type catalog. The type must match the actual work; do not label local tensor computation as dataSync or mediaProcessing merely to obtain background execution.
- For apps targeting API 35+ (including this repository's current targetSdk 36), dataSync and mediaProcessing services each have a separate six-hour-in-24-hour background execution budget. Within each type, the budget is shared across all of the app's services using that type; returning the app to the foreground resets the applicable budget. When the limit is reached, the service has only a few seconds to stop itself. This callback is not a safe place to serialize an entire model checkpoint. Periodic durable checkpoints must already exist.
- specialUse covers valid FGS use cases not covered by another type. It requires a manifest subtype explanation and is reviewed in Google Play Console. It is not an automatic approval, not a generic compute exemption, and does not remove background-start or process-death risks. Do not select it until the precise user-facing use case and distribution route are reviewed.
- systemExempted is reserved for eligible system apps/roles/integrations and is not a general-purpose training type.
- If no compliant and distribution-eligible FGS type is established for sustained background training, the product must keep training foreground-only or use bounded resumable work that fits the available scheduler policy. It must not invent or misdeclare a service type.
- For WorkManager, onStopped() is a lightweight stop notification: its callback thread is not contractually fixed and it must not perform long/blocking checkpoint I/O. It may signal the process-scoped session owner to stop at the next safe update boundary, but it is not a recovery guarantee.
- If a service timeout or worker stop is received, signal the learning workflow to reach its next safe boundary when time permits. Do not start unbounded finalization in the timeout callback. WorkManager onStopped() is documented as a lightweight callback with no fixed callback-thread guarantee; it is not guaranteed to run after process death or force-stop. The main recovery guarantee remains the last periodic checkpoint already committed to durable storage.
- Direct FGS and WorkManager are execution/scheduling adapters only. Neither owns the session state, authoritative progress, candidate, checkpoint validity or active-generation pointer.

#### 8.1.2 Selection remains an explicit product decision

Before selecting a production execution binding, close these inputs: distribution route (Google Play or another channel), the precise user-visible purpose of continuous training, eligible FGS type and policy approval evidence, start restrictions, target API, quota behavior, OEM process-management behavior, expected session duration, pause latency, checkpoint interval, and resource/energy budget. Reassess whenever the target SDK or Android policy changes.

Official platform evidence (reviewed 2026-11-05):
- [Foreground-service types and per-type permissions](https://developer.android.com/develop/background-work/services/fgs/service-types)
- [Foreground-service start restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)
- [Foreground-service timeouts](https://developer.android.com/develop/background-work/services/fgs/timeout)
- [Long-running WorkManager workers, including local ML workloads](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running)
- [ListenableWorker.onStopped() contract](https://developer.android.com/reference/androidx/work/ListenableWorker#onStopped())

## 9. Acquisition resource boundary

The same measured Device Capability Profile informs autonomous knowledge acquisition, but model and acquisition workloads use separate resource reservations. Model weights, inference workspaces, optimizer state and checkpoint recovery capacity must be reserved before corpus storage, parser buffers, indexes or tokenization caches receive a budget.

See [32 — Device-Adaptive Knowledge Acquisition and Corpus Budgeting](32_DEVICE_ADAPTIVE_KNOWLEDGE_ACQUISITION.md) for the acquisition profile and device-specific corpus policy.

## 10. Post-implementation device verification plan

After implementation is separately authorized, use representative devices selected by measured capability envelopes rather than relying only on low/mid/high marketing labels. Verify profile selection, tokenizer preparation mode and cost, peak/sustained memory, training throughput, energy, thermal response, pause latency, checkpoint reliability and recovery after process death. Include cases where the same device selects a different workload profile under changed runtime conditions, without changing its active model/tokenizer identity. Keep exact device, OS, driver, runtime, model and dataset revisions with every result.

This section is an empirical verification protocol, not a documentation-stage blocker. The capability dimensions, source reliability and candidate resource envelopes can be researched and analytically bounded now. Physical measurements are required later to claim that a particular configuration actually works or that a named device tier is supported. Until that verification, runtime thresholds and device support remain explicitly **unverified**, rather than preventing completion of the design contract.


## 11. External evidence and transferability

The following evidence informs the design of the mobile training controller. It must be interpreted according to the workload actually studied.

- [MELON (ACM MobiSys 2022)](https://doi.org/10.1145/3498361.3538928) demonstrates memory-oriented techniques for training neural networks on commodity mobile devices, including recomputation, micro-batching and memory adaptation. It is useful systems evidence, but it is not an LLM-from-scratch result.
- [MobileFineTuner (2025)](https://arxiv.org/abs/2512.08211) reports full-parameter and parameter-efficient fine-tuning of pretrained LLMs on real mobile phones. Its memory, gradient-accumulation, sharding and scheduling techniques are relevant to Argus's training infrastructure; its results do not prove that broad language pretraining from random initialization is practical on a phone.
- [Geyko, Mosbach and Brinkmann (2026)](https://arxiv.org/abs/2610.06325) measure sustained LoRA fine-tuning of a quantized, frozen 3B-parameter base model on an iPhone 17 Pro. The work is valuable for complete-run, energy and thermal analysis. It is an iOS fine-tuning experiment, not an Android or from-scratch result.
- Google's [LiteRT on-device training guide](https://developers.google.com/edge/litert/conversion/tensorflow/build/ondevice_training) documents Android training of a deployed model through a training signature. It establishes an available incremental-training workflow, not a generic foundation-pretraining engine.
- The TensorFlow team's [2021 on-device training guidance](https://blog.tensorflow.org/2021/11/on-device-training-in-tensorflow-lite.html) explicitly distinguishes practical transfer learning/retraining from the much larger data and compute demand of training from scratch. Because it is older and stack-specific, it is a cautionary reference rather than a current benchmark.

### 11.1 Consequences for Argus

1. Preserve Local Knowledge Genesis as the accepted product direction. None of the references above authorizes replacing it with an externally pretrained foundation.
2. Do not infer from successful inference, adapter training or full fine-tuning that a model can be pretrained from random initialization on the same device.
3. Keep optimizer state, gradients, activations, temporary workspaces, runtime allocations, checkpoint reserve and Android process headroom in the same peak-memory accounting.
4. Require complete-run measurements: peak and sustained memory, optimizer updates, bytes/tokens processed, time-to-validation target, energy, thermal behavior, interruptions and recovery.
5. Use MELON and mobile fine-tuning papers for systems techniques; use narrow-domain from-scratch research and Argus-specific experiments to investigate learning from random initialization.
6. The **empirical device-feasibility claim** remains unverified until a defined model/data/task target is measured on representative Android devices. This does not block completion of research, analytical resource estimates or selection of a qualified pre-implementation design. No parameter ceiling or time budget is inferred from these papers alone.
