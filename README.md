# JaggedGem

An independent, Google-free Android AI assistant with a glassmorphic neon UI.

- **Chat** powered by Mistral AI (`mistral-small-latest`)
- **Voice input** via on-device microphone recording + Voxtral transcription (no Google speech services)
- **Natural text-to-speech** via Mistral's Voxtral TTS (voice: `amelia` — configurable)
- **Image understanding** — attach photos and ask about them

No Google accounts, no Google APIs, no Gemini. One free Mistral API key is all you need.

## Setup

1. Install [Android Studio](https://developer.android.com/studio)
2. Open this project directory in Android Studio and let Gradle sync finish
3. Get a free API key at [console.mistral.ai](https://console.mistral.ai/) (no credit card required)
4. Create a file named `.env` in the project root:
   ```
   MISTRAL_API_KEY=your-key-here
   ```
   (see `.env.example` for the full list of options, including chat model and TTS voice)
5. Run the app on an emulator or a phone (Build > Build APK(s) for a shareable APK)

## Usage

- Type a message and hit send to chat
- Tap 🎙️ to start recording, ⏹️ to stop — your speech is transcribed into the input box
- Open ⚙️ Settings to give JaggedGem a personality and switch between **Live Chat** and **Text to Speech** modes
- Tap + to attach an image to your message

## Notes

- The API key is embedded in the APK at build time. Keep your built APK private.
- Chat, voice input, and TTS require an internet connection (they call the Mistral API); no data goes to Google.
