# JamLink

Local-network audio synchronization app for live musicians. Up to 6 Android devices (1 Master, 5 Clients) connect via Wi-Fi Direct or local hotspot and play the same audio track in tight sync (~5–15ms perceptual window), so musicians can jam together listening to the same moment of the same track.

Built with React Native (TypeScript) for the UI, with all timing-critical logic (NTP clock sync, audio scheduling, device discovery) running natively in Kotlin + C++/JNI via TurboModules. Audio playback uses Google Oboe/AAudio for low-latency output.

## Current Phase

See [CLAUDE.md](CLAUDE.md) for the full phase checklist and architecture rules.

- **Phase 1** — Project setup, RN↔Native bridge (TurboModules/JSI), JNI/C++ config ← *in progress*
- Phase 2 — Networking & device discovery
- Phase 3 — Time synchronization protocol
- Phase 4 — Low-latency audio engine + hardware calibration
- Phase 5 — State sync & RN UI integration

## Setup

### Prerequisites

- Node.js ≥ 22.11.0
- Android SDK (API 24+), NDK, and CMake (installed via Android Studio SDK Manager)
- A physical Android device or emulator (API 24+)

### Install & Run

```bash
# Install JS dependencies
npm install

# Start Metro bundler
npx react-native start

# In another terminal — build and run on Android
npx react-native run-android
```

### Project Structure

```
app/                          # React Native source (screens, specs, state)
android/app/                  # RN-generated host app (thin)
android/jamlink-native/       # Native engine: Kotlin TurboModules + C++/JNI
docs/                         # Design docs, one per phase
```

## License

TBD
