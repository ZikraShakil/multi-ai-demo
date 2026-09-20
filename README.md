# Multi-AI Chat with Context Handoff

An interactive chat where you can **hand off an ongoing conversation to a
different AI mid-way through** — including a different provider entirely
— and the new AI receives the full conversation history automatically.
This is the practical test of the original proposal's core claim: that
context can transfer between AIs so a conversation feels continuous.

Runs on **Google Gemini's free tier by default** (no billing required).
You can hand off to OpenAI or Claude once you have paid keys for them.

## How it works

```
You type a message
        │
        ▼
┌───────────────────┐        writes every turn        ┌──────────────────────┐
│   Active AI        │ ───────────────────────────────▶│  SharedMemory         │
│  (e.g. Gemini)      │                                  │  context_store.json  │
└───────────────────┘                                  └──────────────────────┘
        ▲                                                        │
        │                     you type: /switch <provider> <model>
        │                                                        │
┌───────────────────┐        reads FULL history back              │
│   New Active AI     │◀────────────────────────────────────────┘
│  (e.g. Claude)       │
└───────────────────┘
```

Every message you type, and every AI reply, is written to
`context_store.json` tagged `"conversation"`. When you run `/switch`, the
new AI's very first call is given the *entire* conversation history
pulled from that file — not just the last message — so it can respond as
if it had been part of the conversation from the beginning.

## Files

- **`ChatSession.java`** — sends your message to the currently active AI
  along with full conversation history, and records both sides of the
  exchange in shared memory. `switchAdapter()` swaps which AI answers
  next messages.
- **`adapters/AIAdapter.java`**, **`GeminiAdapter.java`**,
  **`OpenAIAdapter.java`**, **`ClaudeAdapter.java`**,
  **`AdapterFactory.java`** — provider adapters and the factory that
  creates them by name (`"gemini"`, `"openai"`, `"claude"`).
- **`SharedMemory.java`** — the context bus, backed by a JSON file.
- **`SafetyCheck.java`** — screens AI replies before they're written back
  to shared memory.
- **`Main.java`** — the interactive REPL.

## Getting a free Gemini API key

1. Go to **aistudio.google.com/api-keys**.
2. Sign in with a Google account.
3. Click **"Create API key"** — issued instantly, no billing required.
4. Copy it somewhere safe — treat it like a password.

## Setting your API key

This project reads keys from **`config.properties`** in the project root
first, falling back to environment variables if that file doesn't exist
or doesn't have the key. `config.properties` already exists in this repo
with a Gemini key filled in — replace it with your own if needed:

```properties
GEMINI_API_KEY=your-key-here
# OPENAI_API_KEY=
# ANTHROPIC_API_KEY=
```

> **Security note:** `config.properties` is committed to this repository
> because it's private and shared only with trusted people. If this repo
> is ever made public, remove the real keys from this file first — anyone
> who can read a public repo can read this file and use your key on your
> account.

## Setup in IntelliJ

1. `File > Open`, select this `multi-ai-demo-java` folder.
2. Click **"Load Maven Project"** when IntelliJ prompts (or refresh the
   Maven tool window if it doesn't).
3. Make sure `config.properties` (see above) has a valid key — no
   environment variable setup is needed.
4. Open `Main.java`, click the green ▶ Run button.
5. The **Run panel at the bottom is where you type** — click into it and
   type your messages there, since this program reads from the console.

## Using it

```
=== Multi-AI Chat ===
Active AI: gemini-chat
Type a message to chat.
Type /switch <provider> <model> to hand off the conversation to a different AI.
Type /exit to quit.

You: Here's an email I got: "Hi, can we move Thursday's meeting to Friday 3pm?"
gemini-chat: Got it — that's a reschedule request from Thursday to Friday at 3pm...

You: /switch claude claude-sonnet-4-6
Switched to claude-chat. It has been given the full conversation so far.

You: Draft a reply confirming the new time
claude-chat: Here's a draft reply confirming Friday at 3pm...
```

Claude in that example never saw your original email message directly —
it only received what `SharedMemory` handed it. If its reply correctly
references the Friday 3pm reschedule, that confirms context genuinely
transferred between the two AIs, not just within one AI's own memory.

**To test with your own email data**: paste the email text as your first
message, ask a few questions about it, then `/switch` to a different
model or provider and ask something that requires remembering details
from the email you pasted earlier. If the new AI answers correctly, the
handoff worked.

## Switching providers

- `/switch openai gpt-4o` — requires `OPENAI_API_KEY` set (paid)
- `/switch claude claude-sonnet-4-6` — requires `ANTHROPIC_API_KEY` set (paid)
- `/switch gemini gemini-3.6-flash` — switch back to Gemini (free)

You can add these environment variables in the same Run Configuration
dialog as `GEMINI_API_KEY`, even if you're not using them yet — the
adapter only checks for the key when you actually `/switch` to it.

## Honest limitations

- Conversation history grows unbounded within a single run — every past
  turn is re-sent as context on every message. For long conversations,
  a real system would summarize or truncate older turns.
- `SafetyCheck` is a demo-level check only, not real prompt-injection
  protection.
- The context store is a plain JSON file; a real system would use a
  vector database for large-scale context retrieval.
- Gemini's free-tier model names change over time. If you get a `404`
  error saying a model is "no longer available," the error message tells
  you the current model name — use that in `/switch` or in `Main.java`.
