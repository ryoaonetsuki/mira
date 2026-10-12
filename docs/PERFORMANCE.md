# Mira Performance Plan — "very fast and smooth" is a feature

Target: voice question → voice answer starts in **< 2.5s** (stretch < 1.5s).
Every screen must respond to touch in **< 100ms**.

## Latency budget (voice turn)

| Stage | Budget | How |
|---|---|---|
| Wake-word → mic open | < 300ms | On-device wake-word (no network) |
| End of speech → STT final | < 500ms | Streaming STT partials shown live; on-device VAD cuts mic precisely — no dead air |
| LLM first token | < 800ms | Streaming API (`generateContentStream`); warm keep-alive connection; short system prompt |
| First token → first audio | < 700ms | **Streaming TTS**: synthesize per sentence boundary, play chunk N while chunk N+1 generates |
| Automation step | < 300ms/step | Cache accessibility node tree; debounce window events; label lookup before coordinates |

## Techniques

1. **Stream everything.** Never wait for a full response: STT partials → UI,
   LLM tokens → screen + TTS queue, TTS audio → speaker. Pipeline stages overlap.
2. **Sentence-chunked TTS.** Split LLM output at sentence boundaries; synthesize
   and play each chunk immediately instead of waiting for the full reply.
3. **Warm connections.** Reuse HTTP connections (keep-alive) to the LLM;
   no TLS handshake per turn.
4. **On-device first.** Wake-word, VAD, and (later) small TTS run on-device —
   zero network round-trip for the critical path.
5. **Lightweight APK.** Small download = fast cold start. Baseline Profiles +
   R8 full mode for startup < 1.5s.
6. **Compose discipline.** `remember`/`derivedStateOf`, `LazyColumn` everywhere,
   no heavy work on the main thread, no recomposition storms.
7. **Automation engine.** Cache the node tree per window; only re-dump on
   `typeWindowStateChanged`; prefer `tap_text` (one lookup) over coordinate math.
8. **Smart prefetch.** While TTS speaks chunk 1, LLM is already generating
   chunk 2's text — the pipeline never idles.

## What we will NOT do

- No full-response buffering before speaking.
- No network call on the wake-word path.
- No unoptimized debug builds shipped; release = R8 + baseline profile.
