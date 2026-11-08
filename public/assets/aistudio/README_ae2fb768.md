# MAHI AI — Native Android Starter

This is the first native Android foundation for MAHI AI. It includes:
- Kotlin + Jetpack Compose starter UI
- Explicitly started microphone foreground service
- Persistent notification with Stop action
- Wake-word engine interface (actual wake-word model is a separate integration)
- Gemini backend client placeholder (never put a production API key in the APK)
- App launcher, camera, music, and settings integration points

## Important limitations
This starter is source code, not a compiled or tested APK. It requires Android Studio/Gradle sync and device testing.
"Always active" is not guaranteed by Android. The user must start microphone mode while the app is visible and grant microphone permission. Android may stop services due to battery policies, force-stop, reboot, or manufacturer restrictions. Android 14+ imposes while-in-use restrictions on microphone foreground services.
A genuine "Hey Mahi" detector needs an on-device wake-word engine/model (for example, a licensed Porcupine model or a custom TensorFlow Lite model). `WakeWordEngine` is intentionally an adapter, not a fake always-listening implementation.
Gemini requests should go through your own authenticated backend that holds the API key. Do not ship Gemini API keys in the APK.

## Open
1. Install Android Studio.
2. Open this folder as an Android project.
3. Use JDK 17 and Android Gradle Plugin compatible with your installed Android Studio.
4. Sync Gradle and run on a physical Android device.
5. Add your backend URL in `local.properties` or a secure runtime configuration mechanism.
6. Implement a licensed wake-word engine in `WakeWordEngine`.

## Current modules
- `app/src/main/java/com/mahi/ai/MainActivity.kt`
- `app/src/main/java/com/mahi/ai/assistant/AssistantForegroundService.kt`
- `app/src/main/java/com/mahi/ai/assistant/WakeWordEngine.kt`
- `app/src/main/java/com/mahi/ai/data/GeminiRepository.kt`
- `app/src/main/AndroidManifest.xml`

## Roadmap
1. Build and validate foreground microphone lifecycle.
2. Integrate real wake-word model and false-trigger tuning.
3. Build secure backend proxy + Gemini Live audio streaming.
4. Add bilingual conversation, tool calling, TTS and interruption.
5. Add consent-based app launching, camera capture, and media controls.
6. Add avatar UI, memory controls, privacy dashboard and APK signing.
