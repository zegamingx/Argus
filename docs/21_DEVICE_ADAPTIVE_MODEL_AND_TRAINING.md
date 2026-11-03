# Device-Adaptive Model and Training

**Status:** Accepted adaptive-design direction; thresholds and implementation remain open  
**Last reviewed:** 2026-11-03

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

## 9. Verification plan

Use representative low-, mid- and high-capability Android devices. Measure configuration selection, peak/sustained memory, training throughput, energy, thermal response, pause latency, checkpoint reliability and recovery after process death. Keep exact device, OS, driver, runtime, model and dataset revisions with every result.

No parameter tiers or thresholds are finalized until this research is complete.
