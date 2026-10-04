# ⛔ DO NOT MERGE — nexum-backed PoC, private for now

This branch (`poc/agent-live-game`) is the **live agent-play proof of concept**: an
external agent live-edits a running KMPMedia game (UFO Dodge) by streaming JSON
patches, and the game streams a ~1 Hz state snapshot back.

**It depends on nexum**, which is a **private** project. The current transport
(commit `63693f1`) uses nexum's own Realtime Database: anonymous sign-in plus SSE on
`games/kmpmedia/poc/patches`, and PUTs the game state to `/state`.

## The boundary

- **Do NOT merge this branch into `main`** (or any public branch) while nexum is private.
- The public demo (`main`) and the KMPMedia library stay **nexum-free**. The
  library's AI features (`OGAiVector`) are network-free and provider-agnostic, and
  the public demo's AI screen uses canned / paste-your-own replies. Nothing public
  references nexum.
- This PoC is the **only** artifact in the KMPMedia world that touches nexum, and it
  lives here, off `main`, on purpose.

## When could this merge?

Only if/when nexum is released publicly, or the nexum transport is swapped for a
public one (for example a user-supplied Firebase project). Until then, keep it
isolated on this branch.

Design: `KMPMedia-internal-docs/LIVE_AGENT_GAMES_SPEC.md`.
