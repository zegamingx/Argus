# GPU Architecture and Compute

**Status:** Research/design baseline  
**Last reviewed:** 2026-11-03

## 1. GPU ecosystems

| Ecosystem | Families | Candidate software path |
|---|---|---|
| Qualcomm | Adreno | Vulkan; supported framework/vendor delegates |
| Arm | Mali, Immortalis | Vulkan; compatible LiteRT GPU path |
| Samsung | Xclipse (AMD RDNA-derived) | Android graphics/compute APIs; validate driver |
| Imagination | PowerVR | Vulkan/OpenCL support varies by product |
| NVIDIA | GeForce/RTX, Jetson | CUDA, TensorRT, Vulkan by platform |
| AMD | Radeon, Instinct | ROCm/HIP on supported combinations; Vulkan |
| Intel | integrated/discrete Arc | OpenVINO, oneAPI/Level Zero, Vulkan |
| Apple | Apple GPU | Metal/Core ML on Apple platforms, not Android |

GPU brand and advertised API version do not guarantee support for the required compute features.

## 2. Android Vulkan validation

Before selecting Vulkan, check:
- compute-capable queue family;
- required storage formats, shader features and memory limits;
- descriptor, synchronization and dispatch constraints;
- device/driver identity and known defects;
- allocation under real application memory pressure;
- model correctness and numerical tolerances.

A Vulkan feature flag is only a discovery hint. It is not proof that a model kernel is supported or stable.

LiteRT GPU acceleration is a separate framework-managed backend, not the same thing as Argus-owned Vulkan kernels. Benchmark both on named devices before choosing a default.

## 3. Suitable and unsuitable workloads

Candidate workloads: matrix multiplication, attention, image transforms, embeddings, normalization and selected training kernels. Workload partitioning must account for data movement.

GPU may lose to CPU for tiny models/batches, serial decode without KV-cache optimization, unsupported operators causing repeated transfers, or thermally constrained long jobs.

## 4. Driver reliability

Record GPU vendor/device IDs, driver, OS build, Vulkan API and extension set. Workarounds/denylist entries need reproducible evidence and review dates. Device-loss recovery must release resources and retry only on a compatible backend.

## 5. Desktop/embedded distinction

CUDA, ROCm/HIP, OpenVINO/Level Zero, Metal and Vulkan are not interchangeable. They have different product/OS matrices, drivers, licenses and packaging.

References: [Vulkan](https://www.vulkan.org/), [LiteRT CompiledModel](https://developers.google.com/edge/litert/android/compiled_model), [NVIDIA Jetson](https://developer.nvidia.com/embedded/jetson), [AMD ROCm](https://rocm.docs.amd.com/en/latest/), [Intel OpenVINO](https://docs.openvino.ai/).
