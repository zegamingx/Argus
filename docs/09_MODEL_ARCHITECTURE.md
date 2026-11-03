# Model Architecture and Artifact Compatibility

**Status:** Proposed model governance and compatibility design  
**Last reviewed:** 2026-11-03

## 1. Model task families

Argus may manage language models (LLM/SLM), vision-language models, image encoders, embeddings, speech recognition/synthesis, audio understanding, OCR/document layout, rerankers, classifiers and deterministic policy/utility components. One model should not be forced to perform every task. Registry entries must state their actual task capability.

## 2. Architecture metadata

Record: encoder/decoder/encoder-decoder; dense or mixture-of-experts; attention type; GQA/MQA; sliding-window behavior; context length; positional encoding; tokenizer family; special tokens; image/audio input path; cache format; adapter compatibility; operator coverage; supported runtime; model license.

An architecture name does not guarantee that a runtime can execute it.

## 3. Artifact formats

| Format | Purpose | Caveat |
|---|---|---|
| GGUF | Packed/quantized LLM artifacts, commonly used by llama.cpp | Runtime-specific tensor and operator support |
| ONNX | Interchange/deployment graphs | Execution provider and operator support vary |
| TFLite/LiteRT | Mobile and edge inference | Delegate/compiler/operator support varies |
| SafeTensors | Tensor weight container | Not an executable runtime/model by itself |
| Vendor formats | QAIRT/QNN, RKNN and other compiled artifacts | Tied to target, compiler and runtime versions |

Conversion must record provenance and numerical validation. Do not assume conversion preserves tokenizer semantics or model output.

## 4. Quantization and precision

Track FP32, FP16, BF16, INT8, INT4 and other precisions only when the selected runtime/model supports them. Record weight and activation precision separately, scale granularity, group size, calibration dataset, outlier handling, KV-cache precision and measured quality impact. INT8 TOPS cannot be used as an FP16 or tokens/sec promise.

## 5. Model manifest

Every model package should contain:
- stable model ID and revision;
- architecture/task tags;
- artifact names, sizes and SHA-256;
- tokenizer assets, vocabulary and special-token map;
- format, quantization and context constraints;
- runtime/backend requirements and minimum versions;
- license, attribution and redistribution constraints;
- source and conversion provenance;
- safety notes and known limitations;
- benchmark and evaluation reports.

## 6. Loading and prompt contract

Validate checksum, manifest schema, tokenizer consistency, runtime compatibility, memory estimate and license metadata before loading. Never silently substitute a tokenizer, vocabulary or model revision. Failed loads must leave the previous active model intact.

Context assembly must preserve role boundaries, source provenance and truncation policy. Web/file/screen content is isolated as untrusted context. Dynamic token registration is not acceptable for a fixed pretrained model unless vocabulary and model tensors are explicitly extended and validated together.

## 7. Evaluation and promotion

Evaluate instruction following, language coverage, factuality, safety, tool behavior, retrieval quality, latency and resource cost. Quantized or adapter-modified variants must be compared with a known baseline before promotion.
