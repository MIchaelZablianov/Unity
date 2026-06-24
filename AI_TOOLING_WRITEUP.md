# AI Tooling Write-up

## Tools used, and roughly how often

Built through pair-programming with several AI agents on a Mac terminal, used
roughly equally and deliberately cross-checked against each other:

- opencode / ZCode + GLM 5.2
- Codex + GPT-5.5
- Claude (Sonnet + Opus)

I also used curated **Android / Kotlin / Compose AI skills** to nudge output toward idiomatic patterns instead of each model's defaults.

## Where AI clearly accelerated the work

- **Boilerplate and wiring.** ContentProvider, Room entities/DAOs, Compose screens, Hilt modules, and
  Gradle/version-catalog setup are repetitive; the agents produced these quickly and matched the
  project's conventions after the first pass.
- **Test-first debugging.** Running tests, reading stack traces, and fixing failures in a tight loop
  is something the agents do well and fast — it kept the build green through repeated refactors.

## Where AI was wrong or off-architecture, and how I corrected it

- **IPC surface: returned a JSON string instead of a Cursor.** The first version sent articles across
  apps as a JSON blob via `ContentProvider.call()`. I switched it to the standard `query()` + `Cursor`
  approach, which is the idiomatic Android way to share tabular data between apps.
- **Old-style database management instead of Room.** The AI reached for hand-rolled
  `SQLiteOpenHelper`-style database code. I moved it to Room, which gives compile-time-checked
  queries, less boilerplate, and a cleaner DAO layer to test against.
- **Exported provider with no access control.** The generated provider was exported with no
  permission, so any app could read it. I added a `signature`-level custom permission so only
  first-party apps (same signing key) can query it, with a debug-only manifest overlay so `adb` and
  local testing still work.

## What I'd do differently next time

- **State the bar up front.** The AI often pushed for the simplest solution on the grounds that this
  is a tiny demo project, and kept steering away from abstractions it judged as overkill. I should
  have made clear from the start that the goal is to demonstrate architecture, scalability, security,
  and testability — even where that looks like over-engineering for a project this size.
