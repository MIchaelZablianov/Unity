# BackendApp

Data provider app that owns the article dataset. Seeds `assets/raw/articles.json` into a Room database via `kotlinx.serialization`, and exposes a read-only `ContentProvider` for cross-app IPC. A signature-protected permission restricts access to first-party callers.

## Build

Requires **JDK 17+** and the Android SDK. Android Studio (Ladybug or newer) resolves both
automatically; from the command line, point `JAVA_HOME` at a JDK 17+ if your shell default is older.

```bash
cd BackendApp
./gradlew assembleDebug
```

APK at `app/build/outputs/apk/debug/app-debug.apk`.

## Install

**Must be installed before UIApp** — the UI app resolves the ContentProvider at startup.

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

> **Important**: Both BackendApp and UIApp must be signed with the **same key**. Debug builds from the same machine satisfy this automatically. If you install from different sources (e.g. CI vs local), you may need to align signing configs, or the UI app will get a `SecurityException` when querying the provider.

## Verify it works

### 1. ContentProvider via ADB (debug builds only)

```bash
# Fetch all articles — should return a cursor with ~129 rows
adb shell content query --uri content://com.unity.backendapp.provider/articles

# Filter by title (case-insensitive)
adb shell content query --uri \
  "content://com.unity.backendapp.provider/articles?titleQuery=sport"

# Combined filter — note the escaped \& so the device shell doesn't treat it as a separator
adb shell content query --uri \
  "content://com.unity.backendapp.provider/articles?titleQuery=biden\&ratingMin=4"
```

A successful response is a tabular cursor; on a device, `content query` prints rows directly.

> **Why "debug builds only":** the release manifest guards the provider with a signature-level
> permission, so `adb shell` (which runs as the unsigned `shell` uid) is **correctly denied** —
> that is the security model working. A debug-only manifest overlay
> (`src/debug/AndroidManifest.xml`) drops that permission so the provider can be inspected from
> the shell during development. For a release build, verify via the dashboard (step 2) and the
> UI app (step 3) instead.

### 2. Dashboard screen

Launch the app. A dashboard shows the article count and the ContentProvider URI — confirms the DB is seeded and the provider is active.

### 3. UIApp loads articles

Install UIApp, launch it. If the article list populates, IPC is working.

## Architecture

```
assets/raw/articles.json
  → ArticleSeeder.ensureSeeded()  (lazy, idempotent — triggered by the first provider
  │                                query OR the dashboard, whichever comes first)
  → ArticleContentProvider.query()
      → ArticlesRepository.query(ArticleFilter)
        → ArticleDao.queryArticles(SimpleSQLiteQuery)
          → Cursor returned across the binder
```

- **`kotlinx.serialization`** for type-safe JSON deserialization into `Article` / `ArticlesResponse`.
- **Room** as the persistence layer, provided by Hilt (`DatabaseModule`) as a `@Singleton`. The DB build itself is cheap (file open, no data I/O). Seeding is deferred to first access so process start isn't blocked by JSON I/O.
- **`ArticleSeeder`** owns one-time seeding. It is idempotent and thread-safe and is invoked by *both* the ContentProvider and the dashboard `ViewModel`, so the backend app shows its data whether it is launched standalone or hit over IPC first.
- **`ContentProvider.query()`** returns a `Cursor` (the standard Android idiom for tabular cross-process data) and `getType()` returns a vendor MIME type for the articles directory. Filter parameters travel as URI query parameters (`titleQuery`, `ratingMin`).
- **`ArticleFilter`** data class owns the filter contract. `toSqlQuery()` renders parameterized SQL (injection-safe — user values are always bound, never interpolated). `fromUri()` parses the IPC contract. Adding a new filter is a localized change in this one class.
- **`ArticlesRepository`** sits between the provider and Room — the provider translates IPC ↔ `ArticleFilter`, the repository translates `ArticleFilter` ↔ SQL and exposes seeding. This separation keeps each layer unit-testable independently.
- **Security**: the provider is `exported="true"` but requires `android:readPermission="com.unity.backendapp.permission.READ_ARTICLES"` (`protectionLevel="signature"`). Only apps signed with the same key as BackendApp can read data. A debug-only manifest overlay (`src/debug/AndroidManifest.xml`) lifts that permission so `adb shell content query` works during development; release builds keep it enforced.
- **Hilt** provides the Room database, DAO and `ArticleSeeder`. The ContentProvider — which Hilt cannot inject directly — reaches the same singletons through a Hilt `@EntryPoint` (resolved lazily on first query, once the app is initialized).
- **No networking**: JSON is bundled in assets; no OkHttp/Retrofit dependency.
- **No foreground service**: Room persists data to disk, so the DB survives process death. The ContentProvider is re-created by the OS on demand.
- **Release builds** run R8 in full mode (shrink + obfuscate + optimize); see `app/build.gradle.kts`.

## Tests

```bash
# Unit tests — ArticleFilter SQL rendering (6), JSON parsing + entity mapping (4)
./gradlew testDebugUnitTest

# Instrumentation tests — ContentProvider IPC contract (8), DAO with in-memory Room (5)
./gradlew connectedDebugAndroidTest
```

| Suite | Location | Tests | What they cover |
|-------|----------|-------|-----------------|
| `ArticleFilterTest` | `src/test` | 6 | SQL rendering, NOCASE, blank-title skipping, injection safety |
| `ArticlesResponseParsingTest` | `src/test` | 4 | JSON→DTO, snake_case mapping, `toEntities()` flattening, unknown-key tolerance |
| `ArticleContentProviderTest` | `src/androidTest` | 8 | Black-box IPC: columns, title/rating/combined filters, unknown URI |
| `ArticleDaoTest` | `src/androidTest` | 5 | `insertAll`, `count`, `@RawQuery` filter, case-insensitive LIKE, column names |

## Key gotchas

- JVM unit tests cover only pure code (`ArticleFilter`, JSON parsing); anything touching
  `android.net.Uri`, `android.util.Log` or Room is exercised by instrumented tests, so the unit
  suite needs no Android-stub workarounds.
- ContentProvider authority: `com.unity.backendapp.provider` (must match UIApp's `<queries>` entry).
- Signature permission requires same signing key for both APKs (see Install section).
- `ArticleFilter.toSqlQuery()` is pure JVM-testable; `fromUri()` depends on `android.net.Uri` and is tested via instrumented tests.
