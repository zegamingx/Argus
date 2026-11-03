# Processor Architecture

**Status:** Research/design baseline  
**Last reviewed:** 2026-11-03

## 1. CPU families to track

| Family | Examples | Argus relevance |
|---|---|---|
| Arm application CPUs | Cortex-A, Cortex-X; Armv8-A/Armv9-A | Primary mobile and many SBC targets |
| Qualcomm custom CPUs | Kryo-branded generations, Oryon | Snapdragon mobile and PC products |
| Apple CPU | Apple Silicon performance/efficiency cores | Future Apple-platform research |
| x86-64 | Intel Core/Xeon, AMD Ryzen/EPYC | Desktop, workstation and server |
| Arm server | Neoverse and compatible systems | Future high-throughput deployment |
| RISC-V | RV64 and ratified extensions | Embedded and future compute research |
| MCU-class cores | Cortex-M and selected RISC-V MCUs | Specialized sensor/edge co-processors, not general LLM hosts |

## 2. ISA and optimization features

- **Arm Advanced SIMD/NEON:** important vector path for mobile kernels; build and runtime dispatch must match ABI/features.
- **SVE/SVE2:** scalable vectors on selected Arm implementations, more relevant to server/HPC than typical phones.
- **SME/SME2:** matrix-oriented extensions on selected newer CPUs; optional specialized kernels.
- **x86 SSE/AVX/AVX2/AVX-512:** feature subsets vary by CPU and OS state.
- **Intel AMX:** matrix acceleration on selected systems; requires OS/runtime support.
- **RISC-V Vector (V):** use ratified specs and actual hardware probing; do not infer vector support from RISC-V branding.
- **Crypto extensions:** useful for supported cryptographic primitives but do not replace key lifecycle design.

References: [Arm NEON](https://www.arm.com/technologies/neon), [Arm SVE](https://learn.arm.com/insights/sve/), [Arm SME](https://learn.arm.com/insights/sme/), [RISC-V ratified specifications](https://riscv.org/specifications/ratified/).

## 3. Capability discovery

Combine OS-reported ABI/bitness, supported runtime feature APIs, NDK mechanisms such as `getauxval(AT_HWCAP/AT_HWCAP2)` where applicable, compiler dispatch tables, core topology exposed by the OS, and measured microbenchmarks. Do not use `/proc/cpuinfo` or device marketing names as the sole source.

Record heterogeneous core topology and performance/efficiency class where exposed. A big core count does not imply all cores have equal performance or that all should be saturated.

## 4. Kernel strategy

- Keep a portable scalar correctness path.
- Add ISA-specific kernels behind runtime dispatch.
- Validate numerical equivalence, tail handling and alignment.
- Avoid building the whole app for the newest ISA when older targets are supported.
- Benchmark matrix multiplication, attention, normalization, activation and quantization kernels independently.
- Record compiler version, flags and ABI for every binary.

## 5. CPU workloads

CPU is the correctness baseline for orchestration, tokenization, policy, unsupported operators, small models and accelerator fallback. An accelerator is not always faster: launch cost, transfers and synchronization can dominate small workloads.
