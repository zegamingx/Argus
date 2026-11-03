# Security & Privacy Policy

Argus is an on-device AGI framework. Because it operates with high-level permissions (Screen Capture, Camera, Accessibility, Storage), security and privacy are the foundational pillars of this project. 

We adhere to a strict **Zero-Exfiltration** policy. User data never leaves the device unless explicitly requested by the user (e.g., fetching a specific web page).

## 🛡️ The Argus Threat Model

1. **Zero Exfiltration:** Argus must NEVER send user data (screens, camera frames, files, chat history, memory dumps) to external servers. There is no telemetry, no analytics, and no crash reporting to third parties.
2. **Prompt Injection Defense:** External content (web pages, OCR'd text, imported files) is treated as **untrusted data**. It must never be executed as system instructions or tool calls without explicit user approval and sandboxing.
3. **Silent Capture Prevention:** MediaProjection and Camera access must always show a system-level indicator. A global "kill switch" must instantly sever all perception pipelines.
4. **Local Data at Rest:** All episodic/semantic memory, LoRA adapters, and training candidates must be encrypted using Android Keystore-backed encryption (e.g., SQLCipher) where applicable.
5. **Biometric Gating:** Any autonomous action that modifies state outside the app (e.g., sending a message, making a purchase, deleting a file) requires biometric confirmation or explicit user interaction.

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
