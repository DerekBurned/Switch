---
name: documenting-tech-stack
description: Use when asked to document, write, or update this Messenger project's tech stack, architecture, or technology choices - including fine-grained choices like Flow vs StateFlow, SharedFlow, channelFlow vs callbackFlow, cold vs hot streams, ObjectBox, Hilt, Firebase (Firestore/Realtime DB/Auth/Storage), Jetpack Compose, Navigation 3, Coil, or Agora; or after introducing, swapping, or removing a major library or pattern that the docs must reflect.
---

# Documenting the Tech Stack

## Overview

Produce and maintain a dedicated knowledge base of every technology decision in this
project: **what** was chosen, **how** it is actually used in this codebase, and **why** it
was chosen over the alternatives. The granularity goes all the way down to async primitives
(e.g. `StateFlow` vs `SharedFlow` vs `channelFlow`), not just top-level libraries.

**Core principle: document the code that exists, not the code you assume exists.** The
project's tech spec is aspirational — verify every claim against the actual source before
writing it down. A wrong doc is worse than a missing one.

## Where the docs live

A separate Obsidian vault, sibling to the `Everything` vault:

```
D:/Obsidian/Messenger/            <- vault root (create if missing)
  00 - Overview/
    Home.md                       <- index / map of content, links every note
  01 - Async & Reactivity/
  02 - Architecture & Layers/
  03 - Dependency Injection/
  04 - Persistence/
  05 - Backend & Realtime/
  06 - UI & Compose/
  07 - Navigation/
  08 - Media & Calling/
  09 - Build & Tooling/
  10 - Testing/
```

One **topic file per concern** (folder-of-topic-files). Split a folder into multiple notes
when one note exceeds ~400 lines. This is a normal Obsidian vault — leave `.obsidian/` for
Obsidian to create on first open; do not hand-write it.

## What every note must contain

For each technology / pattern, write four labelled parts:

1. **What** — the concrete choice (library + version from `gradle/libs.versions.toml`, or the
   specific primitive).
2. **How we use it** — real usage in *this* repo, with clickable refs as `path:line`. Quote
   the actual call site, not a generic example.
3. **Why this choice** — the reasoning and trade-offs that made it win here.
4. **Alternatives considered / rejected** — what else was on the table and why it lost. If
   unknown, write "Rationale not yet captured — confirm with maintainer" rather than inventing.

## Async granularity is mandatory

The `01 - Async & Reactivity` note must distinguish, with a real call site for each that
exists in the code:

- `Flow` (cold) vs `StateFlow` (hot, conflated, always-has-value) vs `SharedFlow`
- `channelFlow` / `callbackFlow` and where each bridges a listener (e.g. Firebase
  `addSnapshotListener`, `ValueEventListener`, ObjectBox `query(...).asFlow()`)
- `MutableStateFlow` + `asStateFlow()` exposure, and the MVI `setState { copy(...) }` pattern
- operators that encode a decision: `combine`, `launchIn`, `update`, `onEach`, `withTimeoutOrNull`,
  `collectAsStateWithLifecycle`
- dispatcher / scope choices (`viewModelScope`, any `withContext`)

When a primitive is chosen, state the property that forced it (e.g. "`StateFlow` because the UI
needs a current value on first frame" — this is exactly why a seeded value beats a bare `Flow`).

## Workflow

1. **Verify first.** Before documenting an area, `Grep`/`Read` the relevant code. Never
   describe a feature you have not seen in source.
2. **Create the vault + folder** for the area if missing.
3. **Write or update the topic note** using the four-part structure above.
4. **Update `00 - Overview/Home.md`** — one bullet linking the note with `[[wikilinks]]`.
5. **Cross-link** related notes with `[[...]]` (e.g. the avatar-flash note links DI, Persistence,
   and Async).

## Conventions

- No emojis anywhere (project rule).
- Obsidian `[[wikilinks]]` between notes; `path:line` for code references.
- Convert relative dates to absolute (e.g. "today" -> the actual date).
- Keep prose dense and factual; this is reference material, not narrative.
- Documentation comments are fine here; the repo's "no code comments" rule applies to source, not docs.

## Common mistakes

| Mistake | Fix |
|---|---|
| Copying the tech spec doc as fact | Verify against source; spec is aspirational |
| Generic example instead of real call site | Quote the actual `path:line` from this repo |
| "What" without "Why" / "Alternatives" | All four parts are required per note |
| Documenting only top-level libs | Async primitives and operator choices count as tech |
| Inventing a rationale | Write "Rationale not yet captured" instead |
| Forgetting the index | Every new note gets a `[[link]]` in `Home.md` |
