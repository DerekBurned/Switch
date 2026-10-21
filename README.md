# Switch

A real-time, end-to-end encrypted messenger for Android — built with Kotlin, Jetpack Compose, and Firebase.

Switch is a full-featured chat client: encrypted 1:1 messaging, voice messages, audio and video calling, media sharing, presence and typing indicators, and an offline-first local store that keeps the app usable with no connection.

![Kotlin](https://img.shields.io/badge/Kotlin-2.3.10-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Min SDK](https://img.shields.io/badge/minSdk-29-3DDC84?logo=android&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Auth%20%7C%20Firestore%20%7C%20RTDB%20%7C%20Remote%20Config%20%7C%20Crashlytics-FFCA28?logo=firebase&logoColor=black)
![E2EE](https://img.shields.io/badge/E2EE-X25519%20%2B%20AES--256--GCM-2F855A)
![License](https://img.shields.io/badge/license-AGPL--3.0--only-A42E2B)

---

## Features

### Messaging
- Real-time 1:1 conversations backed by Firestore listeners
- Per-message delivery state: sending, sent, read
- Typing indicators and online/last-seen presence over Firebase Realtime Database
- Reply, forward and delete (for me / for everyone)
- Paginated history loading, and recent history restored on sign-in
- Search from the floating tab bar, with recently opened contacts as suggestions
- Mark conversations read or unread, clear history, delete conversations
- Deleted accounts appear as an unknown user while their conversation stays readable

### Voice messages
- Hold the mic to record, slide left to cancel, slide up to lock and record hands-free
- A live waveform while recording; a 50-bar waveform travels with every message
- Recordings cut short by leaving the app, a call or the one-hour limit are kept as a per-chat draft to send or discard, even after the app restarts
- Voice bubbles with seek, 1x / 1.5x / 2x playback, an unplayed dot and reply quotes
- A mini player keeps playback going across screens and continues into the next voice message
- Upload progress with cancel, tap-to-retry on failure, and recovery of clips interrupted by the app being killed
- Playback pauses for calls, other apps' audio and unplugged headphones, and hands audio back when it ends

### Calling
- One-to-one audio and video calls powered by the Agora RTC SDK
- System-level integration through Telecom `ConnectionService`, so calls behave like native ones
- Full-screen incoming call UI, a foreground service for ongoing calls, and a floating picture-in-picture bubble when you leave the call screen
- The screen stays on during calls, and the proximity sensor blanks it while the phone is at your ear
- Call history with missed-call recording

### Account
- Phone-number sign-in with a validated session gate before any screen loads
- Account deletion from Privacy settings, with server-side cleanup of the user's data

### Media
- In-app camera capture (CameraX) for photos and video
- Device gallery picker built on MediaStore
- Image and video rendering with Coil 3, video playback with Media3 ExoPlayer
- BlurHash placeholders, so media previews render instantly before the download finishes
- Cancellable transfers with a local media cache

### Offline-first
- ObjectBox as the local source of truth; the UI always reads from disk
- Writes are queued and replayed by a WorkManager job when connectivity returns
- Network state is observed continuously and drives sync automatically

### Notifications
- FCM push, delivered per conversation with messaging-style notifications
- Direct reply straight from the notification shade via `RemoteInput`
- Conversation shortcuts and Android bubbles

### Interface
- Single-activity Jetpack Compose app on Material 3
- Light, dark and system theme modes, persisted with DataStore
- A centralised design token layer for color, type, shape, spacing and motion
- Colour pairings covered by automated WCAG contrast tests

---

## Security

Messages are encrypted on the sending device and decrypted on the receiving one. The server stores ciphertext.

| Concern | Implementation |
| --- | --- |
| Key agreement | X25519 ECDH between long-term identity keys |
| Key derivation | HKDF-SHA256, salted with a hash of the sorted participant UIDs |
| Message encryption | AES-256-GCM, 96-bit random nonce, 128-bit tag |
| Media encryption | Per-file AES-256-GCM key, streamed in chunks |
| Voice messages | Same per-file scheme; the file key and the waveform travel inside the encrypted message payload |
| Key storage | Private identity key wrapped by an AES-GCM key held in the Android Keystore |
| Key rotation | Per-participant epochs feed the derivation, so re-keying invalidates old material |
| Verification | Signal-style safety numbers, derived from iterated SHA-512 over both public keys |

Crypto primitives come from Google Tink and the platform JCE. `ConversationCrypto`, `E2eeMessageCodec`, `MediaCipher`, `IdentityKeyStore` and `SafetyNumber` each carry unit-test coverage.

> Note: this is a static-static Diffie-Hellman scheme with epoch-based rotation. It does not implement a double ratchet, so it does not provide per-message forward secrecy.

---

## Architecture

Clean Architecture across three layers, with MVI in the presentation layer.

```
app/src/main/java/com/example/messenger/
├── data/
│   ├── crypto/          E2EE: key agreement, message and media ciphers, safety numbers
│   ├── local/obx/       ObjectBox entities and converters; mappers/ holds the entity mappers
│   ├── media/           Camera, gallery, BlurHash, transfers, cache, voice recorder, clip playback, drafts
│   ├── presence/        Firebase presence service and app-lifecycle tracking
│   ├── remote/          Firebase auth, Firestore, DTOs, Agora call services, Telecom
│   ├── repository/      Repository implementations
│   ├── service/         Typing, session-cleanup and auth-flow services
│   └── sync/            Sync queue, coordinator, network observer, WorkManager worker
├── domain/
│   ├── featureflag/     Feature flag keys and state for the remote kill switches
│   ├── model/           Pure Kotlin domain models
│   ├── repository/      Repository interfaces
│   ├── service/         Service interfaces
│   └── usecase/         Single-responsibility use cases
├── di/                  Hilt modules
├── presentation/
│   ├── base/mvi/        UiState, UiIntent, UiEffect, base ViewModels
│   ├── components/      Reusable composables, grouped by domain; search and the clip player keep their ViewModels here
│   ├── featureflag/     featureEnabled() and LocalFeatureFlags for Compose
│   ├── navigation/      Type-safe Navigation 3 graphs
│   ├── notification/    Notification channels, bubbles, direct reply
│   ├── screens/         One package per feature: Screen, Content with its previews, ViewModel, UiState, Intent, Effect
│   └── theme/           Colors, typography, shapes, design tokens and theme settings
└── util/
```

Key conventions:

- **Unidirectional data flow.** Each screen exposes a single immutable `UiState`, receives `UiIntent`s, and emits one-shot `UiEffect`s.
- **Screen / Content split.** `XScreen` wires up the ViewModel and navigation; `XScreenContent` is a stateless composable, which keeps previews and UI tests cheap.
- **Repositories return Flows.** The UI observes ObjectBox; remote listeners write into it.
- **Cloud Functions** handle the work a client should not: `getRtcToken` mints Agora tokens, `onCallSignalCreated` and `onMessageCreated` fan out push notifications, and `deleteAccount` removes a user's data server-side.

---

## Tech stack

| Area | Choice |
| --- | --- |
| Language | Kotlin 2.3.10, coroutines and Flow |
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation 3 with kotlinx.serialization routes |
| DI | Hilt |
| Local storage | ObjectBox, DataStore Preferences |
| Backend | Firebase Auth, Firestore, Realtime Database, Storage, Cloud Messaging, Cloud Functions, Remote Config, Crashlytics |
| Real-time media | Agora RTC SDK, Android Telecom |
| Media | CameraX, Coil 3, Media3 ExoPlayer, MediaRecorder |
| Background work | WorkManager |
| Crypto | Google Tink, Android Keystore |
| Build | AGP 9.1.1, Gradle version catalogs, KSP, JDK 21 |
| Testing | JUnit 5, MockK, Turbine, AssertK, Compose UI Test |

---

## Getting started

### Prerequisites

- Android Studio with AGP 9.1 support
- JDK 21
- A Firebase project
- An Agora account for calling

### 1. Firebase

Create a Firebase project, add an Android app with the package name `com.example.messenger`, and place the downloaded `google-services.json` in `app/`.

Enable in the Firebase console:

- Authentication, with the Phone provider
- Cloud Firestore
- Realtime Database
- Storage
- Cloud Messaging
- Remote Config. It needs no parameters: every feature switch has a built-in default. Debug builds turn every switch on; release builds keep ten unfinished features off (`featuresOffInRelease` in `app/build.gradle.kts`). To see the switches in the console, publish `remoteconfig.template.json` with **Publish from a file**.
- Crashlytics. Open its dashboard once to enable it; debug builds don't send reports.

Phone sign-in only works for builds whose signing certificate Firebase knows. Add the SHA-1 and SHA-256 fingerprints of your debug and release keys to the Android app in the project settings. `./gradlew signingReport` prints them.

Deploy the rules and indexes checked into this repository:

```bash
firebase deploy --only firestore:rules,firestore:indexes,database,storage
```

The Cloud Functions need the Blaze (pay-as-you-go) plan, and they read the Agora credentials from Secret Manager. Store both secrets before the first deploy; each command asks for the value:

```bash
firebase functions:secrets:set AGORA_APP_ID
```

```bash
firebase functions:secrets:set AGORA_APP_CERTIFICATE
```

Then deploy the functions:

```bash
cd functions && npm install && npm run deploy
```

### 2. Agora

Create a project in the Agora console and add the App ID to `local.properties`, which is gitignored:

```properties
AGORA_APP_ID=your_agora_app_id
```

When this is missing, the build uses an empty App ID. The project still compiles, but calls will have no audio or video until a real App ID is supplied.

### 3. Build

```bash
./gradlew assembleDebug
```

Install on a connected device:

```bash
./gradlew installDebug
```

---

## Testing

Unit tests run on the JVM with JUnit 5:

```bash
./gradlew testDebugUnitTest
```

Instrumented and Compose UI tests need a connected device or emulator. Build both APKs, install them, and run the tests with `adb`:

```bash
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
```

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

```bash
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
```

```bash
adb shell am instrument -w -r com.example.messenger.test/androidx.test.runner.AndroidJUnitRunner
```

> Warning: don't use `./gradlew connectedDebugAndroidTest` on a device you use for real chats. It uninstalls the app when the tests finish, which deletes the end-to-end encryption keys stored on the device, so old messages can no longer be decrypted there.

Coverage focuses on the places where bugs are expensive: the crypto layer, DTO and entity mappers, message status transitions, conversation reconciliation, use cases, ViewModel state machines, and the decisions behind the voice pipeline (playback toggles, audio focus, upload timeouts, recovery after process death, drafts). Instrumented Compose tests render each leaf component, voice UI included, in both themes and assert WCAG contrast ratios against what is actually behind the text.

Continuous integration runs the unit-test suite on every push and pull request to `main` and `develop`.

---

## License

Copyright (C) 2025-2026 Derek Burned and contributors

Switch is free software: you can redistribute it and/or modify it under the terms of version 3 of the GNU Affero General Public License as published by the Free Software Foundation (`AGPL-3.0-only`). The full text is in [LICENSE](LICENSE).

Switch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

In plain terms, and not as a replacement for the license text: anyone may use, study, change and share this code. Anyone who distributes the app, or lets people use a modified version over a network, must publish the complete source of their version under this same license and keep the existing copyright notices, including the author's name. A closed-source copy is not allowed.

### Additional permission under GNU AGPL version 3 section 7

If you modify this Program, or any covered work, by linking or combining it with the Agora RTC SDK, Google Play services or the Firebase SDKs for Android (or a modified version of those libraries), containing parts covered by the terms of their own licenses, the licensors of this Program grant you additional permission to convey the resulting work.

### Third-party components

- The Inter typeface in `app/src/main/res/font/` is Copyright 2016 The Inter Project Authors and is licensed under the SIL Open Font License, Version 1.1. See [licenses/Inter-OFL.txt](licenses/Inter-OFL.txt).
- `app/src/main/java/com/example/messenger/data/media/BlurHash.kt` is adapted from the BlurHash reference implementation, Copyright (c) 2018 Wolt Enterprises, licensed under the MIT License. See [licenses/BlurHash-MIT.txt](licenses/BlurHash-MIT.txt).
- Libraries that Gradle and npm download at build time keep their own licenses.
