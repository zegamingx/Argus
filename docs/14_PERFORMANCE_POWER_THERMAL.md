# Performance, Power and Thermal Engineering

**Status:** Proposed measurement and admission-control design  
**Last reviewed:** 2026-11-03

## 1. Required measurements

Measure separately: model load/compile time, time-to-first-token, prompt prefill throughput, decode tokens/sec, end-to-end p50/p95/p99 latency, embedding/vision/audio throughput, peak/steady memory, CPU/GPU/NPU utilization where exposed, energy per task/token where measurable, temperature/thermal state and output correctness.

A short peak run is not evidence of sustained mobile performance.

## 2. Reproducible benchmark manifest

Every result records device/SKU and SoC revision; OS/build; battery/charging and power mode; thermal state and ambient temperature if available; RAM pressure; driver/runtime/SDK; model checksum and quantization; context/batch/concurrency; prompt and generated token counts; warm-up/repetitions; measurement method; backend actually used.

Report median and tail latency, not only best run. Keep raw measurements and scripts where possible.

## 3. Workload-specific routing

Prefill, token decode, embeddings, vision, audio, OCR and training may prefer different backends. Account for transfer, compile and launch overhead. Compare backends using equivalent tasks and quality settings.

## 4. Thermal and battery policy

Use OS thermal/power signals where available. Define normal, constrained, paused and recovery states from observed device behavior and platform policy. Avoid universal hard-coded temperature thresholds as a substitute for thermal APIs.

Heavy inference/training must support cancellation, bounded concurrency, checkpointing and visible user state. Background work remains subject to Android scheduling and foreground-service rules.

## 5. Performance budgets

Final RAM, tokens/sec, latency, temperature and battery targets are TBD until device tiers and representative workloads are selected. First define protocol and sample-device plan.

## 6. Optimization order

1. Correctness and operator coverage.
2. Allocation and data-copy reduction.
3. Kernel and graph efficiency.
4. KV cache/context optimization.
5. Parallelism and batching.
6. Quantization and mixed precision.
7. Backend routing and thermal-aware scheduling.

Never trade correctness, privacy or device safety for a headline benchmark.

## 7. Genesis and training measurements

Add measurements for the from-scratch learning lifecycle, not only inference:

- Time to complete device assessment.
- Model graph construction and parameter initialization time.
- Initial tensor bytes written and peak memory during Genesis.
- Time from application process start to usable application state.
- Background general-learning tokens/sec and optimizer updates/sec.
- Training peak and sustained memory, including gradients and optimizer state.
- Energy per training token/update where measurable.
- Pause latency after thermal, memory, battery or user stop signals.
- Checkpoint duration, bytes written and recovery time after process death.

Report both the initial device capability profile and the live state that caused workload adaptation. Do not combine a short peak throughput result with a sustained training claim.

## 8. Adaptive-workload measurement rule

The device profile chooses a safe configuration envelope; the live controller selects a workload inside that envelope. A thermal pause or memory-pressure reduction must not silently alter the architecture, optimizer definition or already committed training history. Any change to sequence packing, precision, batch semantics or optimizer configuration must be versioned in the training session manifest and validated for numerical correctness.
