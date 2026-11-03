# NPU, DSP and Dedicated AI Acceleration

**Status:** Research/design baseline  
**Last reviewed:** 2026-11-03

## 1. Core rule

An NPU is not a universal Android accelerator. Access depends on SoC, firmware, OS build, driver, SDK, app partition, compiler, operator set, datatype, memory and license. The presence of an NPU in a product sheet does not mean a third-party APK can execute arbitrary models on it.

## 2. Ecosystems

| Vendor | Technology | Potential route | Constraint |
|---|---|---|---|
| Qualcomm | Hexagon DSP/HTP | QAIRT, QNN, GENIE; framework delegates | Target-specific compiler/runtime and operator support |
| MediaTek | APU/NeuroPilot/Neuron | TFLite Shim + Neuron Delegate where supported; newer LiteRT route where documented | Some direct Neuron Runtime APIs are vendor-partition-only |
| Samsung | Exynos NPU | OEM/platform routes; delegates where supported | Third-party access varies |
| Google | Tensor TPU/AICore | Supported AICore features; LiteRT CPU/GPU for custom models | AICore is not a public arbitrary TPU graph API |
| Huawei | Da Vinci/HiAI | HiAI/Ascend ecosystem where available | Device/region and SDK restrictions |
| Rockchip | RKNPU | RKNN Toolkit/Runtime | Board OS, driver and artifact version alignment |
| NXP | Ethos-U/SoC accelerators | eIQ, Ethos-U compiler/runtime | MCU/MPU and OS differences |
| TI | C7 NPU/TIDL | Edge AI SDK/TIDL | Embedded Linux SDK matrix |
| Renesas | DRP-AI | Renesas AI SDK | Specific RZ/V product families |
| ST | GCNanoUltra31-VIP AI core | OpenVX stack on STM32MP2 | Specific board and OS stack |
| Intel | selected Core Ultra NPU | OpenVINO NPU plugin | OS/driver/model matrix |
| Apple | Neural Engine | Core ML on Apple OS | Not an Android API |

## 3. Qualcomm

Qualcomm AI Engine Direct (QNN) provides lower-level APIs and backend access; QAIRT documents GENIE for generative AI workloads. Deployment involves model preparation/conversion, target runtimes, application integration and benchmarking. One compiled model must not be presumed portable across Snapdragon generations.

Sources: [Qualcomm AI Engine Direct](https://www.qualcomm.com/developer/software/qualcomm-ai-engine-direct-sdk), [QAIRT overview](https://docs.qualcomm.com/bundle/publicresource/topics/80-63442-10/general_overview.html), [QAIRT HTP](https://docs.qualcomm.com/bundle/publicresource/topics/80-63442-10/htp_backend.html).

## 4. MediaTek

Official NeuroPilot documentation distinguishes third-party APK paths from vendor-partition applications. TFLite Shim with Neuron Delegate can be an APK route on supported devices; direct Neuron Runtime API is restricted to vendor-partition use. Recheck the current LiteRT MediaTek guide before implementation.

Sources: [MediaTek NeuroPilot guide](https://developer.mediatek.com/ai/6423c916c612745b3a4baa2d.html), [TFLite Neuron Delegate](https://github.com/MediaTek-NeuroPilot/tflite-neuron-delegate), [LiteRT MediaTek guidance](https://ai.google.dev/edge/litert/next/mediatek).

## 5. Google and Android

AICore provides supported system GenAI capabilities, not a universal NPU driver for arbitrary user models. As of 2026-11-03, LiteRT in Google Play services documents CPU and GPU paths and describes NPU support as coming soon; standalone LiteRT has separate delegate/accelerator options. Re-verify this fast-moving status before coding.

## 6. Adapter contract

Every NPU adapter must document supported device/OS; SDK/runtime/driver and license; model format/compiler; operator coverage; precision/quantization; tensor limits; memory; graph partitioning; compile/load time; cancellation/synchronization; profiler/error codes; and whether training/backprop is actually supported.

## 7. Training caution

Mobile NPU paths are commonly designed for inference. Successful forward execution does not prove backward, gradient or optimizer support. Training is a separate capability requiring explicit tests.
