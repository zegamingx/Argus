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
| LiteRT CompiledModel | Android supported devices | Current Google direction for managed CPU/GPU inference |
| Standalone LiteRT | Android/Linux supported configurations | Consider where custom delegates/NPU or no Play services are required |
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

NNAPI was deprecated in Android 15. Google's migration documentation points developers toward current on-device runtime options. As of 2026-11-03, LiteRT in Google Play services documents CPU and GPU execution through CompiledModel; NPU support in that Play services package is described as coming soon, while standalone LiteRT has separate delegate options. Google's 2026-10-28 announcement says NNAPI removal is planned to begin in 2027. Recheck this time-sensitive lifecycle before implementation.

Sources: [NNAPI migration guide](https://developer.android.com/ndk/guides/neuralnetworks/migration-guide), [LiteRT CompiledModel](https://developers.google.com/edge/litert/android/compiled_model), [LiteRT Play services](https://developers.google.com/edge/litert/android/play_services), [Google announcement (2026-10-28)](https://developers.googleblog.com/smaller-apps-and-faster-gpu-inference-with-litert-in-google-play-services/).

## 4. Selection algorithm

Two stages:
1. **Eligibility:** OS/ABI, installed runtime, graph/operators, datatype, shapes, memory and driver.
2. **Optimization:** measured latency, throughput and energy under current thermal/concurrency conditions.

Task classes may differ: prompt prefill, token decode, embeddings, vision, audio, OCR, evaluation and training need not select the same backend.

## 5. Fallback and graph partitioning

Measure transfer and synchronization cost when graph segments execute on different devices. Reject a partition if it is slower, unsupported or numerically unsafe. Report full-device, partitioned or fallback execution with stable reason codes. Never infer that listing a dependency proves a backend is integrated.
