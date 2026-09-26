# 🎙️ Sweaty AI — Voice-First Autonomous Device Assistant

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-brightgreen.svg?logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.0%2B-blue.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20(M3)-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Room Database](https://img.shields.io/badge/Storage-Room%20(SQLite%20%2B%20KSP)-FF6F00.svg)](https://developer.android.com/training/data-storage/room)
[![Tests](https://img.shields.io/badge/Tests-Robolectric%20%26%20Roborazzi-success.svg)](https://robolectric.org)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20On--Device%20AES--256-blueviolet.svg)](#-privacy--security-first)

> **"Your voice-first AI that remembers, reminds, and controls your device — completely private."**

**Sweaty AI** is an advanced, offline-first autonomous voice assistant for Android built with Kotlin and Jetpack Compose. It blends continuous speech recognition, bilingual wake-word triggers (English & Bengali), long-term semantic memory, intelligent reminder scheduling, and native device control via the Android Accessibility API.

---

## 🌟 Key Features

### 🗣️ 1. Voice-First Interaction & "Always Listening" Mode
- **Interactive Voice Orb**: Fluid, neon-lit visualizer with animated reactive states: *Idle*, *Listening*, *Thinking*, *Speaking*, and *Error*.
- **Continuous Hands-Free Pipeline**: Always-listening audio loop that automatically restarts on silence timeouts without throwing annoying error toasts.
- **TTS Echo Self-Suppression**: Automatically pauses microphone listening during Text-to-Speech playback and resumes immediately once finished, eliminating self-triggering audio loops.
- **Bilingual Wake-Word Detection**:
  - **English**: *"Hey Sweaty"*, *"Sweaty"*, *"Sweetie"*
  - **Bengali (বাংলা)**: *"সোয়েটি"*, *"হেই সোয়েটি"*, *"সুইটি"*
  - Automatically strips trigger phrases when a user speaks a direct command, or responds with a conversational vocal prompt when summoned by name alone.
- **Configurable Speech Synthesis**: Customize speech rate and pitch with localized Bengali and English TTS voices.
- **Active Follow-Up Window**: 30-second conversational window keeps the assistant receptive after answering for natural back-and-forth dialogue.

### 🧠 2. Personal Memory Engine
- **Persistent Local Knowledge Base**: Automatically extracts and catalogs names, relationships, personal preferences, and facts mentioned during conversations.
- **Categorized Storage**: Organizes memories under *Personal*, *Work*, *Preference*, and *General*.
- **Bilingual Search**: Instant, query-filtered search in both English and Bengali.
- **AES-256 Local Encryption**: All memories are encrypted on-device using AndroidKeyStore hardware-backed keys.

### ⏰ 3. Intelligent Reminders & Daily Briefing
- **Natural Language Parsing**: Automatically detects dates, exact times, and relative offsets in both English (*"in 10 minutes"*, *"tomorrow at 5 PM"*) and Bengali (*"১০ মিনিট পর"*, *"কাল সকাল ৮টায়"*).
- **Localized Digit Conversion**: Converts Bengali numerals (`০-৯`) to standard digits seamlessly.
- **High-Priority Android Notifications**: Integrated with custom notification channels, sound, and vibration patterns.
- **Morning Audio Briefing**: One-tap or voice-activated summary that aggregates upcoming reminders, current day context, and scheduled tasks into a single spoken briefing.

### 📱 4. Autonomous Device Control (Accessibility Engine)
Sweaty AI operates as an autonomous on-device co-pilot using a dedicated Android Accessibility Service (`SweatyAccessibilityService`):
- **System Navigation**: Back, Home, Recent Apps, Open Notification Shade, Open Quick Settings, Lock Screen.
- **Screen Reading**: Reads active screen text aloud upon request.
- **App Launching**: Launches any installed application by spoken name.
- **UI Automation**: Autonomous clicking by button text or resource ID, text input into focused input fields, and page scrolling.
- **Safety & Verification**:
  - **Action Audit Log**: Real-time record of all actions executed on the device with timestamps and status.
  - **Panic Stop (`PANIC STOP`)**: Instant floating kill-switch to immediately abort executing operations.
  - **Autonomous Action Confirmation**: Safeguard dialog prompt for high-risk actions before execution.

### 🌐 5. Multi-LLM Provider Support
- **Google Gemini**: Default high-speed intelligence using Gemini 2.5 Flash / Gemini 3.5 Flash / Gemini 3.1 Pro.
- **OpenAI**: Optional support for GPT-4o / GPT-4o-mini.
- **xAI Grok**: Optional integration with Grok-2.
- User-provided API keys are encrypted locally and never transmitted to any third-party telemetry servers.

---

## 🔒 Privacy & Security First

Sweaty AI is built from the ground up on the principle of **Zero-Telemetry User Privacy**:
- **No Cloud Database**: All chat history, reminders, and personal memory nodes are stored in a local SQLite database using Android Room.
- **Hardware-Backed AES-256 GCM**: Sensitive keys and preferences are encrypted using AndroidKeyStore hardware security modules.
- **No Analytics / No Tracking**: No Firebase Analytics, no Mixpanel, and no background crash loggers.
- **Strictly Local Network Requests**: Network requests are exclusively made to your chosen LLM endpoint (Google Gemini, OpenAI, or xAI) with your personal API key.

---

## 🏗️ Architecture & Tech Stack

```
com.example
├── data
│   ├── db
│   │   ├── ActionLogDao.kt
│   │   ├── ActionLogEntity.kt
│   │   ├── AppDatabase.kt
│   │   ├── MemoryDao.kt
│   │   ├── MemoryEntity.kt
│   │   ├── ReminderDao.kt
│   │   └── ReminderEntity.kt
│   └── model
│       └── ChatMessage.kt
├── domain
│   ├── SecurePreferences.kt
│   └── WakeWordDetector.kt
├── service
│   ├── DeviceActionExecutor.kt
│   ├── SpeechManager.kt
│   ├── SweatyAccessibilityService.kt
│   └── ai
│       └── LlmClient.kt
├── ui
│   ├── components
│   │   └── VoiceOrb.kt
│   ├── screens
│   │   ├── ChatScreen.kt
│   │   ├── DeviceControlScreen.kt
│   │   ├── HomeScreen.kt
│   │   ├── MemoryScreen.kt
│   │   ├── RemindersScreen.kt
│   │   └── SettingsScreen.kt
│   └── theme
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
├── viewmodel
│   └── SweatyViewModel.kt
├── MainActivity.kt
└── SweatyApp.kt
```

### Core Technologies
- **Language**: Kotlin 2.0+
- **UI Toolkit**: Jetpack Compose (Material Design 3)
- **Local Persistence**: Room Database 2.6+ with KSP (Kotlin Symbol Processing)
- **Asynchronous Flow**: Kotlin Coroutines & `StateFlow` / `collectAsStateWithLifecycle`
- **Security**: Android KeyStore API with AES-GCM 256-bit encryption
- **Hardware & System**: Android `AccessibilityService`, `SpeechRecognizer`, `TextToSpeech`, `NotificationManager`
- **Testing**: Robolectric (CUJ testing) and Roborazzi (Screenshot regression testing)

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** Hedgehog / Ladybug or newer
- **JDK 17** or newer
- **Android SDK API 34** (Minimum SDK: 26)
- A physical Android device or emulator with Google Play Services installed

### 1. Clone the Repository
```bash
git clone https://github.com/your-username/sweaty-ai.git
cd sweaty-ai
```

### 2. Configure API Keys
Copy the example environment configuration:
```bash
cp .env.example .env
```
Open `.env` and insert your Gemini API Key (or configure it inside the app under **Settings**):
```ini
GEMINI_API_KEY=your_actual_gemini_api_key_here
```

### 3. Build & Run
Compile and launch the debug build:
```bash
# Compile debug APK
gradle :app:assembleDebug

# Run unit and Robolectric tests
gradle :app:testDebugUnitTest
```
The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## ⚙️ Enabling Device Control (Accessibility Service)

To allow Sweaty AI to navigate your phone, click buttons, and read screen content:

1. Open **Sweaty AI** and navigate to the **Control** tab.
2. Tap **Enable in Settings**.
3. Locate **Sweaty AI** in your device's Accessibility list and toggle it **ON**.

> [!IMPORTANT]
> **For Android 13 and Android 14+ users**:
> If the Accessibility toggle appears greyed out (*"Restricted setting"*):
> 1. Open your phone's **Settings > Apps > Sweaty AI**.
> 2. Tap the **three dots (⋮)** in the top right corner.
> 3. Select **"Allow restricted settings"**.
> 4. Return to the Accessibility menu and turn on Sweaty AI.

---

## 🎙️ Spoken Command Examples

| Category | Spoken Command (English) | Spoken Command (বাংলা) |
|---|---|---|
| **Wake-Word** | *"Hey Sweaty, what's my schedule today?"* | *"সোয়েটি, আজকের আবহাওয়া কেমন?"* |
| **Reminders** | *"Remind me to call Sarah in 15 minutes"* | *"আমাকে ১০ মিনিট পর ওষুধ খাওয়ার কথা মনে করিয়ে দাও"* |
| **Memory** | *"Remember that my passport number is A12345"* | *"মনে রাখো আমার প্রিয় মিষ্টি রসগোল্লা"* |
| **App Launch** | *"Open WhatsApp"* | *"ইউটিউব খোলো"* |
| **Navigation** | *"Go back"*, *"Go home"*, *"Show recent apps"* | *"হোম স্ক্রিনে যাও"*, *"পেছনে যাও"* |
| **Screen Reading**| *"What is on my screen right now?"* | *"আমার স্ক্রিনে কি লেখা আছে পড়ে শোনাও"* |
| **Morning Brief**| *"Give me my morning briefing"* | *"আমার সকালের ব্রিফিং শোনাও"* |
| **Emergency** | *"Stop listening"*, *"Panic stop"* | *"থামো"*, *"শোনা বন্ধ করো"* |

---

## 🧪 Testing & Verification

The project includes JVM unit tests, Robolectric simulations, and visual screenshot tests:

```bash
# Run all unit and Robolectric tests
gradle :app:testDebugUnitTest

# Verify Roborazzi visual regression screenshots
gradle :app:verifyRoborazziDebug

# Record new baseline screenshots (if UI was modified)
gradle :app:recordRoborazziDebug
```

---

## 📄 Permissions Used

| Permission | Purpose |
|---|---|
| `RECORD_AUDIO` | Real-time speech recognition and wake-word detection. |
| `POST_NOTIFICATIONS` | Delivering scheduled reminder alerts on Android 13+. |
| `INTERNET` | Sending LLM queries to the configured AI API provider. |
| `VIBRATE` | Tactile feedback for voice activations and alerts. |
| `BIND_ACCESSIBILITY_SERVICE` | Autonomous device control, navigation gestures, and screen reading. |

---

## 📜 License

Distributed under the **MIT License**. See `LICENSE` for more information.

---

<p align="center">
  Built with ❤️ for privacy and autonomy using <b>Kotlin & Jetpack Compose</b>.
</p>
