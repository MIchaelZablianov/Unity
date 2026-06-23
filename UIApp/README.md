# UIApp

News reader UI that fetches articles from BackendApp's ContentProvider via IPC. Built with Jetpack Compose (MVVM), Hilt for DI, and Coil for image loading.

## Prerequisites

**BackendApp must be installed** before running UIApp. The ContentProvider authority `com.unity.backendapp.provider` is resolved at the system level — if BackendApp isn't installed, `ContentResolver.query()` returns null and the user sees an error.

> Both APKs must be signed with the **same key** so that UIApp can hold BackendApp's signature-protected read permission. Debug builds from the same machine satisfy this automatically.

## Build

```bash
cd UIApp
./gradlew assembleDebug \
  -Dorg.gradle.java.home=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
```

APK at `app/build/outputs/apk/debug/app-debug.apk`.

## Install

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Usage

1. Launch the app.
2. Articles load automatically from BackendApp via ContentProvider IPC.
3. **Search field** — filter by title (case-insensitive substring match).
4. **Rating slider** — set minimum rating (1–5, discrete steps).
5. **Apply Filters** button — sends the filter to BackendApp.
6. If no articles match, a "No articles found" message is shown.
7. If the backend is unreachable, an error message is shown (distinct from "no matches").

## Verify it's talking to BackendApp

### Quick check — article count

The bundled dataset has **129 articles**. If the list shows 129 items on first launch with no filters, IPC is working.

### Kill BackendApp and re-verify

1. Force-stop BackendApp from Settings → Apps → BackendApp → Force Stop.
2. In UIApp, tap "Apply Filters" with no filter changes.
3. Articles should still load — Android re-creates the ContentProvider on demand (Room DB persists on disk).

### ADB verification

```bash
adb shell dumpsys package com.unity.backendapp | grep provider
```

You should see `com.unity.backendapp.provider` listed as an exported provider.

### Check for errors

If the screen stays on "Loading..." indefinitely, BackendApp is not installed or the provider is blocked. On Android 11+ devices, verify that UIApp's manifest includes:
```xml
<queries>
    <package android:name="com.unity.backendapp" />
</queries>
```

## Architecture

```
BackendApp ContentProvider (query → Cursor)
  → ContentResolverDataSource (ContentResolver.query, cursor → List<Article>)
    → ArticleViewModel (StateFlow<ArticleUiState>)
      → ArticleListScreen (Compose) → ArticleCard
```

- **`ArticleFilter` data class**: Both apps share the same filter shape. UIApp's `ArticleUiState.toFilter()` converts form fields to the immutable `ArticleFilter` that crosses the data-source boundary. Adding a new filter = add a field + one UI control — no interface or data-source signature changes.
- **`ArticlesDataSource` interface**: Abstracts the IPC mechanism — allows testing with an in-memory fake instead of mocking the (final) `ContentResolver.query()` method.
- **`ContentResolverDataSource`**: Maps a `Cursor` to `List<Article>`. Throws `IOException` on failure (null cursor, timeout, IPC error) so the VM can distinguish "no matches" from "backend unreachable". Column indices are cached once per query. `CancellationSignal` is wired to coroutine cancellation for proper timeout behavior.
- **MVVM**: `ArticleViewModel` holds a `StateFlow<ArticleUiState>`. Filter params are exposed as mutable state; `applyFilters()` launches a coroutine on an injected `@IoDispatcher` and updates state on completion.
- **Concurrency protection**: `fetchJob?.cancel()` prevents overlapping filter calls from overwriting each other out of order.
- **Hilt**: `@HiltViewModel`, `@AndroidEntryPoint`, `@Binds` for `ArticlesDataSource`, `@IoDispatcher` qualifier for testable dispatchers.
- **Coil 2.7.x**: `AsyncImage` for article images from network URLs, with a per-article placeholder color (from `placeholderColor` field) shown behind the image until it loads.
- **`INTERNET` permission**: Required by Coil for network image loading.
- **`READ_ARTICLES` permission**: Required to query BackendApp's provider (signature-level, auto-granted for same-key APKs).

## Tests

```bash
# Unit tests — ArticleViewModel (10), example (1)
./gradlew testDebugUnitTest

# Instrumentation tests — ArticleCard Compose (3), ArticleFilterUri (5)
./gradlew connectedDebugAndroidTest
```

| Suite | Location | Tests | What they cover |
|-------|----------|-------|-----------------|
| `ArticleViewModelTest` | `src/test` | 10 | Loading state, filter application, title/rating filter, empty result, error state, rating clamping, error vs empty distinction |
| `ArticleFilterUriTest` | `src/androidTest` | 5 | URI encoding: empty filter, title only, rating only, both, blank-title skipping |
| `ArticleCardTest` | `src/androidTest` | 3 | Title/description rendered, rating number, low-rating variant |

## Key gotchas

- **`<queries>` manifest entry**: Android 11+ hides BackendApp's ContentProvider from UIApp unless `<queries><package android:name="com.unity.backendapp"/>` is declared.
- **Signature permission**: Both APKs must be signed with the same key (see Prerequisites).
- **`INTERNET` permission**: Coil silently crashes without it.
- **Filter key parity**: URI param names (`titleQuery`, `ratingMin`) in `ArticleFilter` companion constants must match BackendApp's `ArticleFilter` constants exactly — no compile-time cross-app checking. Centralized constants in both apps minimize drift.
