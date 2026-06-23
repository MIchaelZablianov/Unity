# BackendApp

Data provider app that owns the article dataset. Seeds `assets/raw/articles.json` into a Room database via `kotlinx.serialization`, and exposes a read-only `ContentProvider` for cross-app IPC. A signature-protected permission restricts access to first-party callers.

## Build

```bash
cd BackendApp
./gradlew assembleDebug \
  -Dorg.gradle.java.home=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
```

APK at `app/build/outputs/apk/debug/app-debug.apk`.

## Install

**Must be installed before UIApp** — the UI app resolves the ContentProvider at startup.

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

> **Important**: Both BackendApp and UIApp must be signed with the **same key**. Debug builds from the same machine satisfy this automatically. If you install from different sources (e.g. CI vs local), you may need to align signing configs, or the UI app will get a `SecurityException` when querying the provider.

## Verify it works

### 1. ContentProvider via ADB

```bash
# Fetch all articles — should return a cursor with ~129 rows
adb shell content query --uri content://com.unity.backendapp.provider/articles

# Filter by title (case-insensitive)
adb shell content query --uri \
  "content://com.unity.backendapp.provider/articles?titleQuery=sport"

# Filter by minimum rating
adb shell content query --uri \
  "content://com.unity.backendapp.provider/articles?ratingMin=4"
```

A successful response is a tabular cursor. On a device, `content query` prints rows directly.

### 2. Dashboard screen

Launch the app. A dashboard shows the article count and the ContentProvider URI — confirms the DB is seeded and the provider is active.

### 3. UIApp loads articles

Install UIApp, launch it. If the article list populates, IPC is working.

## Architecture

```
assets/raw/articles.json
  → ArticleContentProvider.onCreate() → Room (synchronous build)
    → seedOnce() (lazy, on first query)
      → ArticlesRepository.query(ArticleFilter)
        → ArticleDao.queryArticles(SimpleSQLiteQuery)
          → Cursor via ContentProvider.query()
```

- **`kotlinx.serialization`** for type-safe JSON deserialization into `Article` / `ArticlesResponse`.
- **Room** as the persistence layer. DB is built synchronously in `onCreate()` (cheap — file open, no data I/O). Seeding happens lazily on the first `query()` to avoid blocking process start.
- **`ContentProvider.query()`** returns a `Cursor` (the standard Android idiom for tabular cross-process data). Filter parameters travel as URI query parameters (`titleQuery`, `ratingMin`).
- **`ArticleFilter`** data class owns the filter contract. `toSqlQuery()` renders parameterized SQL (injection-safe — user values are always bound, never interpolated). `fromUri()` parses the IPC contract. Adding a new filter is a localized change in this one class.
- **`ArticlesRepository`** sits between the provider and Room — the provider translates IPC ↔ `ArticleFilter`, the repository translates `ArticleFilter` ↔ SQL. This separation keeps both layers unit-testable independently.
- **Security**: the provider is `exported="true"` but requires `android:readPermission="com.unity.backendapp.permission.READ_ARTICLES"` (`protectionLevel="signature"`). Only apps signed with the same key as BackendApp can read data.
- **Hilt** provides the Room database (via `DatabaseProvider.instance`) and DAO. `ArticlesRepository` is `@Singleton @Inject`.
- **No networking**: JSON is bundled in assets; no OkHttp/Retrofit dependency.
- **No foreground service**: Room persists data to disk, so the DB survives process death. The ContentProvider is re-created by the OS on demand.

## Tests

```bash
# Unit tests — ArticleFilter SQL rendering (6), JSON parsing + entity mapping (4), example (1)
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

- `android.util.Log` throws `RuntimeException("Stub!")` in JVM unit tests — all logging uses try-catch wrappers.
- ContentProvider authority: `com.unity.backendapp.provider` (must match UIApp's `<queries>` entry).
- Signature permission requires same signing key for both APKs (see Install section).
- `ArticleFilter.toSqlQuery()` is pure JVM-testable; `fromUri()` depends on `android.net.Uri` and is tested via instrumented tests.
