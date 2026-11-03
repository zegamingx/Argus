# Platform Strategy and Expansion

**Status:** Proposed platform roadmap  
**Last reviewed:** 2026-11-03

## 1. Platform tiers

| Tier | Platform | Intended order | Constraints |
|---|---|---|---|
| 1 | Android ARM64 phones/tablets | First | Android lifecycle, permissions, ABI, vendor SDK and thermal variation |
| 2 | More Android device tiers | With same app, capability-dependent | No compatibility guarantee based on brand |
| 3 | Embedded Linux/SBC | Later | Board images, kernel/driver, root/vendor services, cross-compilation |
| 4 | Desktop Linux/Windows | Later | x86-64/ARM64, CUDA/ROCm/OpenVINO, packaging and drivers |
| 5 | Apple platforms | Research | Core ML/Metal, Apple toolchain and app sandbox |
| 6 | Server/edge cluster | Research | Multi-user isolation, scheduling, network security and operations |
| 7 | RISC-V/specialized accelerator | Research | Toolchain/runtime maturity varies by implementation |

## 2. Shared and platform-specific code

Potential shared contracts: model metadata, tokenizer interfaces, orchestration schemas, memory models, evaluation definitions and backend capability vocabulary.

Platform-specific behavior: permissions/lifecycle, filesystem/key store, capture/accessibility, foreground execution, native ABI/compiler, GPU API, vendor NPU SDK, installation/updates and model provisioning.

Do not commit to JNI/NDK, Rust FFI, C++ core or another cross-platform framework before comparing them against requirements and deployment costs.

## 3. Embedded deployment

Embedded targets may require vendor kernels, device trees, root-owned services, cross-compilation, host-side model conversion and board-specific libraries. Android APK APIs cannot be assumed to work. Each board family needs its own deployment profile.

## 4. Desktop deployment

CUDA, ROCm/HIP, OpenVINO/Level Zero, Vulkan and Metal have different hardware/OS matrices. Model metadata may be shared, but runtime adapters, packaging and profiling are platform-native.

## 5. Platform entry gate

Add a platform only after user value, OS security model, toolchain/license, compute/memory path, model/runtime, build/test automation, update/rollback and maintenance ownership are documented.
