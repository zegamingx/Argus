# Contributing to Argus

First off, thanks for taking the time to contribute to the future of on-device AGI! 🧠⚡

Argus is not a standard Android app. It is a privacy-first, local-only intelligence framework. Because of this, we have strict architectural rules that supersede standard Android development practices.

## 🚫 The "Golden Rules" of Argus

Before writing code, ensure your contribution respects these core tenets:

1. **NO CLOUD AI / APIS:** Never introduce a dependency on OpenAI, Anthropic, Google Gemini, or any external LLM API. All inference must happen locally via NPU/GPU/CPU.
2. **NO TELEMETRY:** No Firebase Analytics, no Crashlytics, no Mixpanel, no Sentry. If the app needs to log an error, it logs it to a local, user-exportable file.
3. **NO SILENT CAPTURE:** Never bypass system consent dialogs. Screen and Camera agents must always respect the global kill-switch and system indicators.
4. **EXTERNAL DATA IS UNTRUSTED:** Web fetchers and file parsers must sanitize inputs to prevent prompt injection. Never pass raw external text directly into the system prompt without wrapping it in untrusted data tags.
5. **NO MOCKS IN PRODUCTION:** Fallback mechanisms must be transparent. If the NPU fails, route to CPU and log it. Do not fake responses.

## 🛠️ Development Setup

- **Language:** Kotlin 2.1+ / C++20 (Native)
- **JDK:** 21 (LTS)
- **Android Studio:** Ladybug or newer
- **Build:** Gradle 8.13+, AGP 8.7.x
- **NDK:** r27+ (Required for native inference engines like llama.cpp / ONNX)
- **Architecture:** MVVM + Clean Architecture + Multi-Agent Orchestrator

## 📥 Pull Request Process

1. Fork the repo and create your branch from `main`.
2. If you've added code that requires native compilation (JNI/C++), ensure it builds for `arm64-v8a`. Test on both Snapdragon and MediaTek devices if possible.
3. Ensure your code passes the Privacy & Security lint rules (no hardcoded URLs, no background network calls without explicit user trigger).
4. Update the `README.md` or architecture diagrams if your PR changes the core Agent flow or Memory schema.
5. Ensure your code does not introduce catastrophic forgetting in the continual learning pipeline.

## 🧪 Testing Requirements

- **Privacy Suite:** Any PR touching perception (Screen/Camera) must include tests verifying that `FLAG_SECURE` apps are ignored and the kill-switch halts capture within <1 second.
- **Injection Tests:** Any PR touching web fetching or file parsing must include adversarial prompt injection tests.
