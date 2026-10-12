# Mira — Bangla-first AI Assistant

Professional, lightweight AI assistant for Android — voice-driven automation of any task,
beautiful UI, human-like Bangla + Hindi TTS. Built entirely on **free tiers**
(no PC/VPS owned by the developer).

> **Not a Maya clone.** UI is original and more professional; only the *category*
> (voice assistant with on-device automation) is shared. Package `com.salman.mira`.

## Architecture: local-first, strict BYOK

- **Memory:** on the phone, `<filesDir>/memory/` (facts, prefs, history JSON).
  Backup/restore from inside the app (zip export/import). No account, no server.
- **LLM brain:** the user's **own** API key, chosen at onboarding —
  **Gemini** (Google AI Studio, free) · **OpenRouter** (free models) ·
  **Custom** OpenAI-compatible base URL. The key never leaves the phone.
  The app does NOT work without a key (no fallback key of ours).
- **No login/register.** For 2–3 users to start; global sharing later if good.

## Free-tier services (only when needed)

| Need | Service (free tier) | Status |
|---|---|---|
| Code hosting | **GitHub** free | when repo is created |
| Model/macro manifest hosting | **Cloudflare Workers** free | later |
| Landing / docs | **Cloudflare Pages** free | later |
| Auth + DB | **Supabase** free | **deferred** — only if login is ever added |
| TTS training GPU | **Kaggle** free (30h/week) | when training Bangla TTS |
| On-device voice | Sherpa-ONNX, Silero VAD, openWakeWord | no server cost |

## Project layout

- `app/` — Android app module (Kotlin + Jetpack Compose, minSdk 26)
- `docs/ARCHITECTURE.md` — full architecture
- `supabase/` — DB schema + edge function sketches (coming)
- `workers/` — Cloudflare Worker code (coming)

## Build order

1. ✅ Reverse-engineer Maya AI (done — see `../maya-re/HANDOFF.md`)
2. ✅ Project scaffold + onboarding flow + main UI shell (theme, bottom nav, Home/Scan/Memories/Chat)
3. Automation engine (AccessibilityService)
4. Voice pipeline (wake-word → STT → LLM → TTS)
5. Dynamic skills system (Cloudflare-hosted manifest + on-demand downloads)
6. TTS fine-tune (Bangla+Hindi, Kaggle GPU)
7. Polish: themes, wallpaper, widgets

## Building (no PC needed)

APKs are built free on **GitHub Actions**:

1. Push this project to a GitHub repo.
2. The workflow in `.github/workflows/build-apk.yml` compiles a debug APK on every push.
3. Download the APK artifact from the Actions tab straight to the phone and install.

Release builds: same workflow with a keystore (stored as GitHub Secrets) when ready
for distribution.

## App identity

- Name: **Mira** · Package: `com.salman.mira` · minSdk 26, targetSdk 34
