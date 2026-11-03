# Device-Adaptive Model and Training

**Status:** Accepted adaptive-design direction; thresholds and implementation remain open  
**Last reviewed:** 2026-11-04

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
| GPU | Device/driver, compute queue/features, storage formats, runtime availability, operator validation and benchmark |
| NPU/DSP | Vendor/runtime/SDK availability, APK access rules, supported operators and precision, compile/load behavior |
| Memory | Total and available memory, app memory class, observed pressure, expected tensor/activation/optimizer allocation |
| Thermal | OS thermal status/headroom where available, sustained benchmark behavior, sensor availability |
| Power | Battery level, charging state, power-saving mode and system policy |
| Storage | Available app-private storage, dataset package size, model tensors, checkpoint and recovery reserve |
| Platform | Android API level, lifecycle restrictions, ABI, driver/runtime versions and permission state |

Unknown telemetry must remain unknown. Missing temperature or memory data is not a measured safe state.

## 3. Configuration selection

The configuration engine evaluates candidate models against a bounded resource budget.

Account for parameters and tensor dtypes; embeddings/output head and architecture-specific state; activation/workspace memory for training; gradients and optimizer moments; context length and attention/recurrent cache; microbatch/concurrency and temporary copies; runtime/compiler allocation and OS headroom; model storage and checkpoint generations.

Do not derive model size from total RAM alone. Do not use a single fixed parameter count for all users.

An initial configuration is valid only when the selected architecture/operator set has a supported execution path and a memory estimate with safety margin. If no candidate fits, Genesis must report the limitation rather than pretend a model was created.

## 4. Runtime training-condition profile

While learning is active, monitor relevant OS signals and bounded measurements:
- Available memory and memory-pressure events.
- CPU load and sustainable thread capacity.
- GPU/NPU availability, correctness and execution cost.
- Thermal status and sustained performance trend.
- Battery/power mode and charging state.
- Storage reserve and checkpoint capacity.
- App foreground/background state and platform scheduling policy.
- User pause/stop preferences.

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

A change affecting the mathematical training definition, such as precision, optimizer, sequence packing or gradient reduction, must be versioned and validated. It must not be silently applied as an ordinary resource response.

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

Backend choice is per workload, not a fixed preference order. CPU, GPU and NPU may serve different operations. For every path, measure transfer cost, compile/load overhead, operator coverage, numerical tolerance, sustained throughput and power.

An NPU must not be selected for training unless the complete required forward/backward/optimizer path is verified. A successful inference run is not evidence of training support.

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

## 9. Acquisition resource boundary

The same measured Device Capability Profile informs autonomous knowledge acquisition, but model and acquisition workloads use separate resource reservations. Model weights, inference workspaces, optimizer state and checkpoint recovery capacity must be reserved before corpus storage, parser buffers, indexes or tokenization caches receive a budget.

See [32 — Device-Adaptive Knowledge Acquisition and Corpus Budgeting](32_DEVICE_ADAPTIVE_KNOWLEDGE_ACQUISITION.md) for the acquisition profile and device-specific corpus policy.

## 10. Verification plan

Use representative low-, mid- and high-capability Android devices. Measure configuration selection, peak/sustained memory, training throughput, energy, thermal response, pause latency, checkpoint reliability and recovery after process death. Keep exact device, OS, driver, runtime, model and dataset revisions with every result.

No parameter tiers or thresholds are finalized until this research is complete.


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
6. The feasibility question remains open until a defined model/data/task target is measured on representative Android devices. No parameter ceiling or time budget is inferred from these papers alone.
