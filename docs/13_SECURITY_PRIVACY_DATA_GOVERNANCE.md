# Security, Privacy and Data Governance

**Status:** Proposed security baseline; threat-model review required  
**Last reviewed:** 2026-11-03

## 1. Security objectives

Minimize collection and persistence; prevent private data leaving the device without an explicit purpose; treat captured/imported content as untrusted; enforce permissions and user consent; protect models, memories, datasets and keys; make security behavior visible and auditable.

## 2. Data classification

| Class | Examples | Default handling |
|---|---|---|
| Public | Public documentation or user-selected public pages | Cache only under documented policy |
| User content | Chats, files and images | App-private; no telemetry |
| Sensitive | Credentials, OTP, banking, health, private communications | Exclude/minimize; never train by default |
| Derived | Embeddings, summaries and entities | Inherit source sensitivity/deletion |
| Model artifacts | Weights, tokenizers, adapters, checkpoints | Integrity-checked and license-aware |
| Secrets | API keys, signing keys, certificates | Never commit or log |

## 3. Consent and capture

Screen capture, camera and AccessibilityService are distinct high-risk features. Each needs clear purpose, platform consent/permission, visible active state, pause/stop and revocation behavior. Exclude secure surfaces and sensitive apps by default where technically possible. If secure-surface status cannot be determined reliably, fail closed rather than promise perfect exclusion.

## 4. Network policy

"Local-first" allows explicit exceptions without ambiguity:
- local inference and memory work offline;
- user-triggered web fetch sends a request to the selected destination;
- model downloads contact configured sources;
- telemetry, analytics and silent private-content upload are prohibited;
- sync requires separate opt-in, scope, encryption and deletion design.

Document destinations, DNS/HTTP behavior, redirects, TLS validation and local audit records.

## 5. Prompt injection and tool safety

Web, OCR, files, retrieval and model outputs are untrusted data. Separate data from system policy. Validate tool arguments, enforce allowlists, apply per-tool permissions, and require confirmation for consequential actions. A model confidence claim is not authorization.

## 6. Cryptography and secrets

Use platform-backed key stores where appropriate. Document generation, wrapping, rotation, backup/restore, recovery and deletion. Do not implement custom cryptography. Never store secrets in source, Gradle files, screenshots, crash logs or benchmarks. Keep signing material outside version control.

## 7. Baseline audit observations

The inspected manifest declares INTERNET, ACCESS_NETWORK_STATE, CAMERA and AccessibilityService-related permissions/components; the Gradle catalog/build file includes a Firebase AI dependency. These are audit facts, not proof of data transmission. They require an explicit dependency/network audit before a zero-cloud claim is treated as verified.

The public repository tree also contains `my-upload-key.jks`. Private signing material must not be committed publicly. Treat this as a high-priority exposure requiring owner-led investigation, key rotation/replacement as appropriate, review of its use and Git history cleanup where feasible. Do not copy its contents into documentation or logs.

## 8. Incident and deletion model

Define procedures for lost device, exposed signing key, malicious model package, data export, device transfer and user-requested deletion. Deletion must cover source records and derived artifacts subject to disclosed backup/retention rules.
