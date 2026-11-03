# SoC and Hardware Compatibility Matrix

**Status:** Planning watchlist — no row means Argus compatibility is verified.  
**Last reviewed:** 2026-11-03

## 1. Interpretation

This is a research/test inventory, not an exhaustive catalogue. Product variants, GPU IP, NPU blocks and SDK access can differ by SKU, region and board. Add exact data only from primary sources. Every entry begins as **UNVERIFIED for Argus** until a repeatable device test passes.

Do not freeze TOPS figures without a vendor datasheet that states datatype, sparsity assumptions, power mode and methodology. TOPS is not model tokens/sec.

## 2. Mobile SoC watchlist

| Vendor | Representative families | Compute notes | Candidate path | Argus status |
|---|---|---|---|---|
| Qualcomm | Snapdragon 8 Elite family, 8 Gen 3, 7s Gen 3, 6 Gen 1 and later | Oryon/Kryo variants, Adreno, Hexagon/HTP | QAIRT/QNN/GENIE; LiteRT; Vulkan | Unverified per SKU |
| MediaTek | Dimensity 9400/9300 families; Helio G99/G100 | Cortex CPU, Mali/Immortalis; APU varies; do not infer a programmable APU on Helio G | NeuroPilot delegate/LiteRT where supported; CPU/GPU | Unverified |
| UNISOC | T612, T616, T760, T820/T8200 variants | GPU and AI block differ by exact SKU | CPU first; Vulkan only after test; vendor path if public | Unverified |
| Samsung | Exynos 2400, 1480, 1330 and later | Xclipse or Mali by SoC; NPU access varies | LiteRT GPU/CPU; OEM path only if documented | Unverified |
| Google | Tensor G3/G4 and later | Arm CPU, Mali, TPU/AICore ecosystem | AICore for supported features; LiteRT for custom models | Unverified |
| Huawei | Kirin 9000S/9010 and later | Maleoon/Mali variants; Da Vinci ecosystem | HiAI/Ascend where available; otherwise CPU/GPU | Unverified |
| Xiaomi | XRING O1 and future XRING products | Confirm released SKU, GPU IP, NPU claims and public SDK | Public standard path or vendor adapter only after evidence | Research |

## 3. Embedded and SBC watchlist

| Vendor | Products to research | Ecosystem | Important boundary |
|---|---|---|---|
| Rockchip | RK3588, RK3576, RK356x, RV families | RKNPU / RKNN Toolkit and Runtime | Board-specific Linux/Android image and runtime versions |
| Amlogic | A311D and future edge SoCs | Vendor stack; community driver work | Mainline GPU support does not prove NPU API availability |
| Allwinner | T527, A733 and later | Product-specific SDK | Verify compiler/runtime and OS image per product |
| NXP | i.MX 8M Plus, i.MX 93 and later | eIQ, Ethos-U and SoC-specific accelerators | Do not equate MCU/MPU software paths |
| Texas Instruments | AM62A, AM67A and related TDA families | TIDL / Edge AI SDK | Embedded Linux SDK matrix |
| Renesas | RZ/V2L, RZ/V2H, RZ/V2N; RZ/G2L separately | DRP-AI on applicable RZ/V devices | RZ/G2L is not equivalent to RZ/V DRP-AI |
| STMicroelectronics | STM32MP23/25 | OpenVX stack for GCNanoUltra31-VIP AI core | Exact STM32MP2 line and software stack |

Sources: [Rockchip RKNN Toolkit2](https://github.com/rockchip-linux/rknn-toolkit2), [NXP eIQ](https://www.nxp.com/design/design-center/software/eiq-ai-development-environment:EIQ), [TI Edge AI](https://github.com/TexasInstruments/edgeai), [Renesas RZ/V AI SDK](https://renesas-rz.github.io/rzv_ai_sdk/ai-sdk), [STM32MP2 NPU](https://wiki.st.com/stm32mpu/wiki/STM32MP2_NPU_description).

## 4. Desktop, workstation and server watchlist

| Vendor/platform | Families | Software path to assess |
|---|---|---|
| Intel | Core/Core Ultra, Xeon, Arc | OpenVINO CPU/GPU/NPU; oneAPI/Level Zero; Vulkan |
| AMD | Ryzen/Ryzen AI, Radeon, Instinct, EPYC | ROCm/HIP on supported combinations; Vulkan |
| NVIDIA | RTX/GeForce, Jetson Orin/Thor, data-center GPU | CUDA, TensorRT, JetPack on Jetson; Vulkan |
| Apple | M-series systems | Core ML, Metal, Neural Engine via platform frameworks |
| Arm | Neoverse and Arm servers | CPU/SVE/SME kernels, vendor accelerators |
| RISC-V | RV64 systems and accelerator platforms | Ratified ISA extensions and vendor toolchains |

NVIDIA Tegra must not be treated as one obsolete category: historical Tegra products and current Jetson generations are separate lines.

## 5. Compatibility record

For each exact device capture: commercial SKU, SoC revision, CPU cluster/ISA, GPU ID/driver/API, NPU/DSP block, ABI, OS build, vendor SDK, model/compiler, operators, precision, memory, thermal state, benchmark, date, evidence, fallback and known issues.
