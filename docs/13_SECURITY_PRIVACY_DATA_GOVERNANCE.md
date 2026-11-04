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

"Local-first" allows explicit, bounded network operations without treating the local instance as a remote training service:

- Local inference and memory work offline.
- User-triggered web fetch sends a request to the selected destination.
- Autonomous public-source acquisition may schedule outbound requests only after the owner has been informed and the feature/network policy has been enabled.
- Autonomous acquisition uses general topic queries and approved public-source scopes. It must not silently attach private conversations, personal files, screenshots, memory records or profile details to search requests.
- Source allowlists, terms, robots directives, rate limits, redirect validation, TLS validation, response-size limits and local provenance records apply to every acquisition job.
- Model downloads contact configured sources.
- Argus-owned telemetry and analytics that expose private content are prohibited. Third-party runtime telemetry must be inventoried, privacy-reviewed, disclosed and accepted as a dependency before the runtime is selected.
- Acquired corpora, local model weights, optimizer state, checkpoints and gradients are not uploaded to a central Argus service or external AI customer.
- A third-party ML runtime may process model inputs locally while still sending non-content diagnostics or device/app metadata. This is not equivalent to zero-network operation and must be assessed separately from prompt/weight transfer.
- Any future sync or export requires separate opt-in, scope, encryption and deletion design.

Network requests necessarily disclose ordinary request metadata to remote destinations. Document configured providers, destination domains, DNS/HTTP behavior, redirects, TLS validation, request metadata and local audit records. See [24 — Autonomous Knowledge Acquisition](24_AUTONOMOUS_KNOWLEDGE_ACQUISITION.md).

### 4.1 Third-party runtime telemetry and Play services

The current LiteRT in Google Play services documentation states that model input processing remains on-device and is not sent to Google, while the runtime may contact Google servers for service updates and compatibility data and may transmit diagnostics/usage metrics. The listed metrics include device identifiers, manufacturer/model/OS information, app/package information, accelerator configuration, event/error types and performance metrics.

This is a separate data path from Argus web acquisition and does not establish that user content is uploaded. It nevertheless means the runtime is not a zero-telemetry dependency. Before adoption, record the exact API/package version, data fields, purpose, service terms, retention/controls where documented, user disclosure and whether the product's local-first/offline requirements permit it. Prefer an alternative only after its exact runtime and vendor dependencies receive the same audit; do not infer that standalone means telemetry-free without evidence.

Source: [LiteRT in Google Play services — privacy and data collection](https://developers.google.com/edge/litert/android/play_services).

## 5. Prompt injection and tool safety

Web, OCR, files, retrieval and model outputs are untrusted data. Separate data from system policy. Validate tool arguments, enforce allowlists, apply per-tool permissions, and require confirmation for consequential actions. A model confidence claim is not authorization.

## 6. Cryptography and secrets

Use platform-backed key stores where appropriate. Document generation, wrapping, rotation, backup/restore, recovery and deletion. Do not implement custom cryptography. Never store secrets in source, Gradle files, screenshots, crash logs or benchmarks. Keep signing material outside version control.

## 7. Baseline audit observations

The inspected manifest declares INTERNET, ACCESS_NETWORK_STATE, CAMERA and AccessibilityService-related permissions/components. Google AI Studio-specific Secrets and Google Services plugins, Firebase AI/App Check dependencies, and generated environment/metadata files have been removed from the current main branch. Android build tooling and KSP remain. The web-retrieval feature performs outbound HTTP requests to public search services.

A previously tracked private signing artifact has been removed from the current main tree, but historical Git objects, other branches and forks may still retain it. Treat the credential as potentially exposed: verify whether it was active, rotate or replace it as appropriate, and plan Git-history cleanup. Never copy its contents into documentation or logs. Release signing now requires externally supplied configuration.

## 8. Incident and deletion model

Define procedures for lost device, exposed signing key, malicious model package, data export, device transfer and user-requested deletion. Deletion must cover source records and derived artifacts subject to disclosed backup/retention rules.

## 9. Local Knowledge Genesis and instance isolation

The accepted model-origin policy is local initialization, not import of third-party pretrained weights as the Argus foundation.

- Parameter initialization occurs on the user's device.
- Model weights, optimizer state, checkpoints, memory and personal learning data remain in the app's local data boundary by default.
- No central training server, federated learning, cross-user gradient aggregation, automatic model synchronization or cross-user weight exchange is permitted by the current design.
- A model manifest must identify the instance and generation without exposing private conversation content.
- Diagnostic export must be user-initiated and content-minimized. It must not silently attach weights, checkpoints or personal datasets.

## 10. Bundled corpus governance

Bundled general corpus data is not user personal data, but it still requires provenance and redistribution review. Record exact repository revision, source dataset, license and layered terms, attribution, preprocessing, filtering, split construction, file checksums and package version. Do not ship a dataset merely because a hosting page labels it open.

The Small C4 dataset card states that its license inherits from original C4. The original C4 card identifies ODC-BY and also references Common Crawl terms. Distribution approval must evaluate those terms and provide required notices.

## 11. Personal learning governance

Personal learning is separate from general bootstrap learning. A user's interaction with Argus is not blanket training consent. The user must be able to understand, inspect, approve, reject, pause, revoke and delete personal learning candidates under the policy in [22 — General Corpus and Personal Learning](22_GENERAL_CORPUS_AND_PERSONAL_LEARNING.md).

Do not promise exact unlearning from already modified neural parameters unless a tested rollback or reconstruction procedure can prove it. Retain generation lineage and known-good states.
