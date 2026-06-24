# Android News — Two-App Architecture Exercise

A news experience split across **two independently installable Android apps** that communicate
over IPC:

| App | Role | Key tech |
|-----|------|----------|
| [`BackendApp`](BackendApp/README.md) | Owns the data. Bundles the article dataset as JSON, seeds it into Room, and serves filtered results over a `ContentProvider`. Filtering runs here. | Room, kotlinx.serialization, Hilt, `ContentProvider` |
| [`UIApp`](UIApp/README.md) | Owns the experience. Renders the news list and filter controls, and requests filtered articles from BackendApp via `ContentResolver`. | Jetpack Compose, MVVM + `StateFlow`, Hilt, Coil |

The two apps have different `applicationId`s and **no compile-time or runtime dependency** on each
other. Their only contract is the `ContentProvider` URI + query-parameter shape, mirrored in each
app's `ArticleFilter`.

## Architecture at a glance

```
UIApp                                   BackendApp
─────                                   ──────────
ArticleListScreen (Compose)
   ↕  StateFlow<ArticleUiState>
ArticleViewModel
   ↓  ArticleFilter
ContentResolverDataSource ──query()──▶  ArticleContentProvider
   ▲                                       ↓  ArticleFilter.fromUri()
   └──────────── Cursor ◀───────────────  ArticlesRepository ─▶ Room (seeded from assets JSON)
```

- **IPC mechanism:** a `ContentProvider` returning a `Cursor` — the idiomatic Android surface for
  tabular cross-process data. Filters travel as URI query parameters.
- **Security:** the provider is exported but guarded by a **signature-level** custom permission, so
  only apps signed with the same key (our UI app, and any future first-party caller) can read it.
- **Extensibility:** filters are a single `ArticleFilter` value object on each side. Adding a new
  filter type is a localized change — no interface, data-source, or IPC-shape rewrite.

See each app's README for the deeper rationale.

## Build & run (out of the box)

Requires **JDK 17+** and the Android SDK; open either app in **Android Studio (Ladybug or newer)**,
or build from the command line. The two apps are separate Gradle builds.

```bash
# 1. Build both APKs
(cd BackendApp && ./gradlew assembleDebug)
(cd UIApp      && ./gradlew assembleDebug)

# 2. Install BackendApp FIRST (UIApp resolves its provider at startup)
adb install BackendApp/app/build/outputs/apk/debug/app-debug.apk
adb install UIApp/app/build/outputs/apk/debug/app-debug.apk
```

> **Same signing key required.** UIApp can only hold BackendApp's signature-level permission if both
> APKs are signed with the same key. Debug builds from the same machine satisfy this automatically.

## Verify the apps are talking

```bash
# Query the provider directly — should print ~129 rows
adb shell content query --uri content://com.unity.backendapp.provider/articles

# With filters
adb shell content query --uri "content://com.unity.backendapp.provider/articles?titleQuery=sport&ratingMin=4"
```

Then launch **UIApp**: the list populates from BackendApp, and the search field + rating slider +
**Apply Filters** button round-trip through IPC. Launch **BackendApp** on its own: its dashboard
reports the stored article count, confirming the dataset seeded independently of the UI app.

## Tests

```bash
(cd BackendApp && ./gradlew testDebugUnitTest)          # filter SQL, JSON parsing
(cd BackendApp && ./gradlew connectedDebugAndroidTest)  # provider IPC, Room DAO   (device/emulator)
(cd UIApp      && ./gradlew testDebugUnitTest)          # ViewModel state machine
(cd UIApp      && ./gradlew connectedDebugAndroidTest)  # cursor mapping, URI encoding, Compose card
```

## AI tooling

See [`AI_TOOLING_WRITEUP.md`](AI_TOOLING_WRITEUP.md) for an honest account of how AI tools were used,
where they accelerated the work, and where they were wrong.
