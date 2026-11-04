# AI Runtime and Backend Architecture

**Status:** Proposed runtime strategy  
**Last reviewed:** 2026-11-03

## 1. Backend contract

A common contract should expose model validation, compile/load, tensor allocation, execution, cancellation, profiling and structured error reporting. Preserve backend-specific capability metadata rather than pretending every backend is equivalent.

Record platform/ABI; runtime/driver/compiler; formats/operators; precision/quantization; dynamic shapes; memory; async/cancellation; profiling; and known defects.

## 2. Runtime landscape

| Runtime | Target and purpose | Planning note |
|---|---|---|
| Portable CPU kernels | Android ARM64 first; later x86-64/RISC-V | Mandatory correctness path |
| LiteRT CompiledModel — standalone | Android supported configurations | Current LiteRT v2.3.2 release lists CompiledModel for CPU/GPU/NPU paths; exact provider, operator and release maturity must be pinned |
| LiteRT in Google Play services | Android devices with Google Play services | `play-services-litert` CompiledModel CPU/GPU path; smaller and auto-updated runtime, but provider telemetry/terms and service dependency require explicit privacy/offline review |
| Legacy LiteRT Interpreter | Compatibility path | Android LiteRT v2.x lists Interpreter as CPU-only; do not plan new GPU/NPU work on the legacy API |
| Qualcomm QAIRT/QNN/GENIE | Supported Qualcomm devices | Vendor compile/runtime adapter |
| MediaTek NeuroPilot | Supported MediaTek products | Honor APK/vendor partition restrictions |
| Vulkan | Suitable GPU platforms | Custom kernels and driver validation |
| llama.cpp | Supported builds/platforms | GGUF LLM path; not universal training runtime |
| ONNX Runtime | Supported Android/Linux/desktop providers | Provider support varies |
| RKNN Runtime | Rockchip boards | Vendor embedded path |
| TIDL | TI edge devices | TI-specific toolchain |
| OpenVINO | Supported Intel/CPU/GPU/NPU devices | Check exact OS/driver/model matrix |
| Core ML/Metal | Apple systems | Separate platform adapter |
| CUDA/TensorRT | NVIDIA desktop/server/Jetson | Separate NVIDIA deployment |
| ROCm/HIP | Supported AMD GPUs | Verify exact GPU/OS/framework tuple |

## 3. Android migration context

NNAPI was deprecated in Android 15. Google's 2026-10-28 announcement states that NNAPI removal is planned to begin in 2027; new Argus integrations must not depend on NNAPI as their forward path. Keep legacy compatibility only where a named device requirement justifies it.

As of 2026-11-04, the standalone Android LiteRT release table identifies v2.3.2 (2026-10-26) as latest and lists the CompiledModel API; its legacy Interpreter API is CPU-only. Google Play services offers the separate `play-services-litert` package and CompiledModel CPU/GPU execution, with GPU execution powered by ML Drift. Its NPU path is still documented as coming soon. The standalone LiteRT documentation exposes additional NPU provider paths, but the NPU overview and the newer `/next/` provider guides do not present identical maturity/status. Therefore do not treat a vendor name in an evolving guide as universal production support: pin the exact LiteRT release, provider package, SoC SKU, operator/precision set and execution mode.

CompiledModel is documented as an **inference** API. LiteRT's separate on-device-training guide describes train/infer/save/restore signatures for a specific converted training graph (for example, Fashion-MNIST); that is not proof of arbitrary LLM autograd or full-backprop support. Training runtime selection remains a separate research decision.

Sources: [NNAPI migration guide](https://developer.android.com/ndk/guides/neuralnetworks/migration-guide), [LiteRT Android release/API matrix](https://developers.google.com/edge/litert/android), [LiteRT CompiledModel inference and operator matrix](https://developers.google.com/edge/litert/inference), [LiteRT in Google Play services](https://developers.google.com/edge/litert/android/play_services), [NNAPI removal announcement (2026-10-28)](https://developers.googleblog.com/smaller-apps-and-faster-gpu-inference-with-litert-in-google-play-services/), [standalone LiteRT NPU guide](https://developers.google.com/edge/litert/next/npu).

## 4. Runtime privacy, service dependency and offline behavior

The Google Play services LiteRT documentation states that model inputs are processed on-device and are not sent to Google servers by the API. It also states that the runtime may contact Google services for updates/compatibility information and sends diagnostics/usage metrics. The listed metrics include device information and device identifier, app/package details, runtime configuration, event/error data and performance measurements.

This does not mean user prompts or model weights are uploaded, but it is still a network/metadata-processing dependency. Argus must not describe this backend as having zero network activity or zero telemetry. Before selecting it, complete a documented privacy/legal review, user-disclosure assessment, offline behavior check, and decision on whether an opt-in or standalone-runtime path is required. Standalone LiteRT and each vendor runtime need their own network/dependency audit; do not assume their behavior from the Play services policy.

Source: [LiteRT in Google Play services — privacy and data collection](https://developers.google.com/edge/litert/android/play_services).

## 5. Selection algorithm

Two stages:
1. **Eligibility:** OS/ABI, installed runtime, graph/operators, datatype, shapes, memory and driver.
2. **Optimization:** measured latency, throughput and energy under current thermal/concurrency conditions.

Task classes may differ: prompt prefill, token decode, embeddings, vision, audio, OCR, evaluation and training need not select the same backend.

## 6. Fallback and graph partitioning

Measure transfer and synchronization cost when graph segments execute on different devices. Reject a partition if it is slower, unsupported or numerically unsafe. Report full-device, partitioned or fallback execution with stable reason codes. Never infer that listing a dependency proves a backend is integrated.
