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
