# AI Tooling Use Documentation

## Overview

This project was built through iterative pair-programming with an LLM-powered coding agent on a Mac terminal. The AI had no Android emulator or IDE access — only file read/write, bash, grep/glob search, and Gradle test output.

- **Agent**: ZCode (opencode CLI), plus a later AI-assisted review pass (see "Post-review hardening")
- **Total files created/edited**: ~30 source files across 2 Gradle modules
- **Total tests**: 44 (20 unit + 24 instrumented)
- **Total iterations (build→test→fix cycles)**: ~50 across all phases

---

## Workflow

The project followed a consistent loop across each phase:

```
Read assignment spec + existing files
  → Write/update source files
    → Run `./gradlew testDebugUnitTest` (and `connectedDebugAndroidTest` when device available)
      → Parse compiler errors or test failures
        → Fix and repeat
```

Key practice: **persistent context files** (`AGENTS.md`, `progress_summary.md`) were maintained between phases. Each phase summary included "Notes for Next Session" sections — critical because the AI has no memory between sessions.

---

## Architecture choices where AI was helpful

### ContentProvider.query() + Cursor (not call() + Bundle)

The initial plan considered `ContentProvider.call()` returning a JSON string. The AI's first implementation used `call()` with a Bundle, which works but is non-standard for tabular data. During review, the architecture was shifted to `query()` returning a Room-backed `Cursor` — this is the idiomatic Android pattern for cross-process data, supports projection, and lets the UI app use standard `Cursor` iteration. The AI generated the migration cleanly, including a new `@RawQuery` DAO method that returns `Cursor` directly.

### ArticleFilter data class for extensibility

The assignment explicitly requires that *"adding a new filter type should not require a structural rewrite."* The initial implementation had fixed-arity parameters (`fetchArticles(titleQuery: String?, ratingMin: Int?)`), which would require signature changes everywhere for a new filter. The AI extracted an `ArticleFilter` data class with `toSqlQuery()` (pure JVM-testable SQL rendering) and URI param encoding (`toUri` / `fromUri`). New filters are now a localized 4-step change documented in AGENTS.md.

### ArticlesRepository extraction

Filter SQL was initially inlined in the ContentProvider's `query()` method, making it untestable without a device. The AI extracted it into `ArticlesRepository` + `ArticleFilter.toSqlQuery()`, which enabled 6 JVM unit tests for filter logic and 4 for JSON parsing — coverage that previously didn't exist.

---

## What the AI got right

- **Boilerplate generation**: ContentProvider, Room entities/DAOs, Compose screens, notification channels, manifest entries — Android has a lot of repetitive scaffolding that AI handles effortlessly.
- **Gradle dependency wiring**: Version catalogs, plugin application, BOM imports, KSP/Hilt interplay with AGP 9.x. The AI learned the project's conventions after one phase and replicated them.
- **Test-first debugging**: Running tests, reading stack traces, and fixing failures in a tight loop. The AI methodically addresses each error.
- **Cross-module consistency**: The `ArticleFilter` contract (param names, types) was mirrored correctly in both apps despite no compile-time dependency.
- **CancellationSignal wiring**: The AI identified that `withTimeout` alone doesn't cancel the underlying `contentResolver.query()` binder call, and wired `invokeOnCompletion` on the coroutine job to cancel the signal.

---

## What the AI got wrong or missed

| Issue | How it was caught | Resolution |
|---|---|---|
| `ContentProvider.call()` instead of `query()` | Design review | Migrated to `query()` + `Cursor` + Room — more idiomatic |
| Fixed-arity `fetchArticles(titleQuery, ratingMin)` | Design review | Extracted `ArticleFilter` data class for extensibility |
| No security on exported provider | Design review | Added `signature`-level custom permission |
| Async `Thread` in `onCreate()` causing null cursor race | Instrumentation test needed a 30×200ms poll loop (the code smell) | Made Room init synchronous; seed lazily on first `query()` |
| `CancellationSignal` not wired to coroutine | Code review | Added `invokeOnCompletion` to cancel signal on coroutine completion |
| Errors swallowed as `emptyList()` | Code review | Changed to propagate `IOException` so UI distinguishes error vs empty |
| mockk incompatible with Kotlin 2.2.x | Build failure | Switched to interface-abstraction + fake pattern |
| `android.util.Log` throws in unit tests | Test crash | Defensive try-catch wrappers |
| `Bundle.getInt()` returns 0 for missing keys | Discovered during early `call()` approach | Moot after switching to URI query params |
| `FOREGROUND_SERVICE_DATA_SYNC` requirement on Android 14+ | Build error | Moot after removing foreground service (Room persists, no service needed) |
| Version catalog syntax errors | Confusing build errors | 2–3 attempts per catalog change to get alias/version reference right |

---

## AI-driven vs human-driven decisions

### AI-led (AI suggested, human approved)
- Interface abstraction (`ArticlesDataSource`) over mocking `ContentResolver`
- `ArticleFilter` data class + `toSqlQuery()` pattern
- `CancellationSignal` wiring via `invokeOnCompletion`
- `StandardTestDispatcher` + constructor-injected dispatcher for VM tests

### Human-led (human identified, AI implemented)
- Switch from `call()`/Bundle to `query()`/Cursor architecture
- Signature-protected permission on the provider
- Extracting `ArticlesRepository` from the provider
- Lazy seeding instead of foreground service
- All documentation corrections

---

## Recommendations for AI + Android development

1. **Keep a `AGENTS.md`**: A living document of conventions, gotchas, and build commands prevents re-learning across sessions.
2. **Test-first, always**: The AI's ability to iteratively fix test failures is its strongest capability.
3. **Plan for context loss**: AI context windows are finite. Persistent summary files are essential.
4. **Accept the blind spots**: The AI cannot verify layouts, animations, or runtime behavior on a device. Reserve device testing for a human review pass.
5. **Prefer interface abstractions**: Patterns like `ArticlesDataSource` that decouple from Android framework classes make testing trivially simple. Mocking concrete/final Android classes is a dead end with current AI tooling.
6. **Don't trust AI's architecture at face value**: The initial implementation had good code but the wrong architecture (`call()` instead of `query()`, no security, no extensibility model). A human design review caught all of this. Use AI for speed, not for architecture judgment.

---

## Post-review hardening

After the first end-to-end build, I ran an AI-assisted code review over the whole repo and fixed the
findings myself:

- **Standalone seeding bug.** Seeding only ran inside the provider's `query()`, so launching
  BackendApp on its own showed *zero* articles. Extracted an idempotent `ArticleSeeder` invoked by
  *both* the provider and the dashboard `ViewModel`, so the data is present on either entry path.
- **Hilt ↔ ContentProvider bridge.** Replaced the hand-rolled `ArticleDatabaseFactory` singleton
  with a Hilt `@EntryPoint` resolved lazily in `query()`. One source of truth for the DB; the
  provider no longer duplicates instance management.
- **R8 enabled** for release in both apps (was disabled) — shrink + obfuscate + optimize.
- **Test gap closed.** Added `ContentResolverMappingTest` (the cursor → `Article` mapping was the
  riskiest code and was only covered transitively). Extracted `mapCursorToArticles` to make it
  testable with a `MatrixCursor`.
- **Simplification.** Removed a redundant `AtomicInteger` request-id guard in the ViewModel —
  structured cancellation (`fetchJob.cancel()` + `ensureActive()`) already prevents stale writes.
- **Repo hygiene & docs.** Added `getType()` MIME, fixed stale README references and machine-specific
  build commands, added a root README, and stopped tracking AI tooling / scratch docs.

The lesson reinforces point 6 below: AI is excellent for *speed*, but a deliberate review pass —
human-led, AI-assisted — is what catches the architecture- and lifecycle-level issues.

## What I would do differently next time

1. **Start with the architecture, not the code**: I let the AI jump into implementation before the IPC mechanism and data flow were settled. A short architecture doc first (provider contract, filter model, security model) would have avoided the `call()`→`query()` migration and the missing-permission oversight.
2. **Define the test strategy upfront**: I didn't plan for BackendApp unit tests until after the provider was written. If I'd specified "every layer has JVM tests" from the start, the AI would have structured the code differently (extracting `ArticleFilter.toSqlQuery()` earlier).
3. **Review the generated code before building**: Some issues (async `onCreate`, swallowed errors) shipped to the build system before being caught in code review. A 5-minute read of each generated file before running Gradle would have saved several build cycles.
