<div align="center">
  <img src="assets/logo.png" width="128" height="128" alt="JamLink Logo">

  # JamLink
  *Local-Network Low-Latency Audio Synchronization for Live Musicians*

  [![React Native](https://img.shields.io/badge/React%20Native-0.86.0-61dafb?logo=react&logoColor=black&style=flat-square)](https://reactnative.dev)
  [![Platform](https://img.shields.io/badge/Platform-Android%20%28API%2024%2B%29-3ddc84?logo=android&logoColor=white&style=flat-square)](https://developer.android.com)
  [![C++](https://img.shields.io/badge/Audio%20Engine-C%2B%2B%20%2F%20Oboe-blue?logo=c%2B%2B&logoColor=white&style=flat-square)](https://github.com/google/oboe)
  [![New Architecture](https://img.shields.io/badge/Bridge-TurboModules%20%2F%20JSI-ff69b4?style=flat-square)](https://reactnative.dev/docs/the-new-architecture-introduction)
  
  ⭐ If you like this project, star it on GitHub!

  [Features](#features) • [Architecture](#architecture) • [Getting Started](#getting-started) • [Running the App](#running-the-app)

</div>

---

JamLink is a local-network audio synchronization application designed for live jamming sessions. Up to **6 Android devices** (1 Master, 5 Clients) can connect via Wi-Fi Direct or local hotspot and play the same audio file in tight, perceptual synchronization (targeting a **5–15ms sync window**).

By bypassing high-latency Android API frameworks and implementing a custom native audio scheduling and network synchronization system, JamLink enables musicians to play together side-by-side with zero distracting delay.

## Features

- ⚡ **Low-Latency Audio Engine** - Built in C++ using Google's Oboe (AAudio/OpenSL ES) to minimize output buffer delays.
- ⏱️ **NTP-like Synchronization Protocol** - Custom high-precision time sync protocol over UDP to maintain aligned clocks between Master and Client devices.
- 📐 **Microphone Click Calibration** - Automated self-calibration measuring individual physical device hardware latencies to adjust playback start times precisely.
- 🔗 **Native Bridge Discipline** - Time-critical synchronization, network scheduling, and frame rendering remain 100% on the native side (Kotlin/C++) via TurboModules to avoid JS engine garbage collection pauses.
- 🌐 **Automatic Peer Discovery** - Offline peer discovery and Wi-Fi group assembly utilizing native Wi-Fi Direct and Network Service Discovery (NSD).

## Architecture

```mermaid
graph TD
    subgraph React Native Layer (TypeScript UI)
        UI[App Screens & Playback UI]
        BridgeSpec[TurboModule Spec]
    end

    subgraph Android Native Module (Kotlin)
        TM[JamLinkBridgeModule]
        Discovery[Wi-Fi Direct / NSD Engine]
        Sync[NTP Time Sync Client/Server]
    end

    subgraph C++ Audio Engine (Oboe/AAudio)
        JNI[JNI Layer]
        Engine[Audio Stream Scheduler]
        Calibrator[Hardware Output Calibrator]
    end

    UI <-->|Taps & Events| BridgeSpec
    BridgeSpec <-->|New Architecture JSI| TM
    TM <-->|JNI| JNI
    JNI <-->|Oboe API| Engine
    JNI <-->|Mic Capture| Calibrator
```

> [!NOTE]
> All latency-sensitive operations (clock offset calculations, latency calibration, and Oboe playback scheduled frames) occur entirely within the C++ and Kotlin native environment to ensure thread priority stability. The JS thread is only informed of asynchronous state changes (e.g. connected, track ready, play tap).

## Getting Started

### Prerequisites

Ensure you have the following installed on your developer machine:
- **Node.js** (>= 22.11.0)
- **Android SDK** with Command Line Tools, NDK (recommended version `27.1.12297006`), and CMake (minimum `3.22.1`)
- **Android Studio** configured for React Native Android development

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/reav4nn/JamLink.git
   cd JamLink
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

## Running the App

1. **Start the Metro Bundler:**
   ```bash
   npm start
   ```

2. **Connect your Android device** via USB debugging (recommended) or launch an emulator.

3. **Build and install on Android:**
   ```bash
   npm run android
   ```

> [!TIP]
> Ensure your Android device supports **API Level 24** (Android 7.0 Nougat) or higher, as low-latency AAudio drivers are only accessible from API 24 onwards.
