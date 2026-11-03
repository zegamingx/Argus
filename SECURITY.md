# Security & Privacy Policy

Argus is an experimental local-first Android AI framework. Because the design includes high-trust capabilities such as screen capture, camera and AccessibilityService, privacy and security are primary engineering requirements.

Local inference is designed to run on-device. Web retrieval is a separate network feature: it sends the submitted search query to public search endpoints and retrieves their responses. It must not silently transmit captured screens, camera frames, imported files, conversation history, memory or model weights.

## 🛡️ The Argus Threat Model

1. **Zero Exfiltration:** Argus must NEVER send user data (screens, camera frames, files, chat history, memory dumps) to external servers. There is no telemetry, no analytics, and no crash reporting to third parties.
2. **Prompt Injection Defense:** External content (web pages, OCR'd text, imported files) is treated as **untrusted data**. It must never be executed as system instructions or tool calls without explicit user approval and sandboxing.
3. **Silent Capture Prevention:** MediaProjection and Camera access must always show a system-level indicator. A global "kill switch" must instantly sever all perception pipelines.
4. **Data at Rest (Design Requirement):** Persistent memory, LoRA adapters and training candidates must use a documented platform-backed encryption design before these stores are introduced. This is a target requirement, not a claim that the current prototype already provides it.
5. **Action Authorization (Design Requirement):** Any future action that changes state outside the app (for example, sending a message, making a purchase or deleting a file) must require explicit user authorization. Biometric confirmation may provide an additional gate but does not replace platform permissions.

## 🐛 Reporting a Vulnerability

We take security and privacy bugs extremely seriously. Since Argus handles highly sensitive local data, please **DO NOT** open a public GitHub issue for security vulnerabilities (e.g., prompt injection bypasses, sandbox escapes, local data exfiltration vectors, or silent capture bugs).

**How to report:**
1. Email the core maintainer directly via the contact method listed on the GitHub profile (`zegamingx`).
2. Use the subject line: `[ARGUS SECURITY] - Brief description`.
3. Include steps to reproduce, potential impact, and (if possible) a proof-of-concept.

**What to expect:**
- Acknowledgment within 48 hours.
- Triage and patch development within 7 days for critical local-escape or privacy-leak vulnerabilities.
- Full credit in the release notes (if you wish to be named) once the patch is merged and published.

## 🔐 Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 0.1.x (Pre-alpha) | :white_check_mark: Active Development |
