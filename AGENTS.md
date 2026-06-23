# AGENTS.md — Unity Android News App

## Repository structure

Two independent Android apps in one repo, no shared module:
- `BackendApp/` — data owner: Room DB seeded from JSON, ContentProvider `query()`
- `UIApp/` — UI: Compose MVVM + Hilt, fetches via ContentProvider Cursor IPC

## Build & test commands

Inside each app directory, run:
- `./gradlew assembleDebug -Dorg.gradle.java.home=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home` — build APK
- `./gradlew testDebugUnitTest` — unit tests (JUnit4, no Robolectric/mockito)
- `./gradlew connectedDebugAndroidTest` — instrumentation tests (both apps)

BackendApp has 11 unit tests + 13 instrumentation tests. UIApp has 11 unit tests + 8 instrumentation tests.
Total: 43 tests. Rebuild takes ~2–9s with configuration cache.

## Key gotchas

- **`android.util.Log` throws in unit tests** — `Log.d/e` calls crash with "not mocked". All logging uses try-catch wrappers (`logD`/`logE` private functions). Adding new Log calls in pure-Kotlin classes requires the same wrapper.
- **JDK 21 required** — Gradle daemon needs `jlink` (missing from VS Code bundled JRE). Use `-Dorg.gradle.java.home=` flag or set `org.gradle.java.home` in `gradle.properties`.
- **Hilt Gradle plugin** — Must use `com.google.dagger.hilt.android` (2.59.2+) with AGP 9.x. Both `ksp` and `hilt` must be declared at the root `build.gradle.kts` (with `apply false`) to share the same class loader.
- **KSP + `android.disallowKotlinSourceSets=false`** — Required in `gradle.properties` for KSP-generated source dirs with AGP's built-in Kotlin compiler.
- **UIApp needs `<queries>` manifest entry for ContentProvider IPC** — Android 11+ `AppsFilter` blocks cross-app provider lookups unless calling app declares `<queries><package android:name="com.unity.backendapp"/>`.
- **UIApp needs `INTERNET` permission** — Coil loads images from network URLs.
- **Signature-protected provider** — BackendApp's provider requires `com.unity.backendapp.permission.READ_ARTICLES` (`protectionLevel="signature"`). Both APKs must be signed with the **same key** (true by default for debug builds from the same machine). If they have different signing keys, UIApp will get a `SecurityException` when querying the provider.
- **ContentProvider authority** — `com.unity.backendapp.provider`
- **Cursor column order** — `id`, `title`, `description`, `image_url`, `rating`, `color_red`, `color_green`, `color_blue`

## Architecture

```
BackendApp/assets/raw/articles.json
  → ArticleContentProvider.onCreate() → Room DB (ArticleEntity) + lazy seed via seedOnce()
    → ArticleContentProvider.query(uri) → ArticleFilter.fromUri(uri) → ArticlesRepository.query(filter)
      → ArticleDao.queryArticles(SimpleSQLiteQuery) → Cursor
        → UIApp ContentResolverDataSource (ContentResolver.query → Cursor → List<Article>)
          → ArticleViewModel (HiltViewModel, StateFlow) → ArticleListScreen (Compose)
```

### Data flow
1. BackendApp process starts → `ArticleContentProvider.onCreate()` builds Room **synchronously** on the main thread (cheap — file open, no data I/O).
2. First `query()` call seeds the DB from `assets/raw/articles.json` via `seedOnce()` (idempotent, guarded by `AtomicBoolean`).
3. The provider parses URI query parameters into an `ArticleFilter`, delegates to `ArticlesRepository.query()` which renders filter → SQL via `ArticleFilter.toSqlQuery()`.
4. UIApp's `ContentResolverDataSource` calls `ContentResolver.query()` with the filter encoded as URI params, maps the returned `Cursor` to `List<Article>`.

### IPC contract
- **Method**: `ContentResolver.query(Uri.parse("content://com.unity.backendapp.provider/articles?titleQuery=X&ratingMin=Y"), null, null, null, null)`
- **Returns**: `Cursor` with columns `id, title, description, image_url, rating, color_red, color_green, color_blue`
- **Filter params** (URI query parameters): `titleQuery` (String, case-insensitive substring), `ratingMin` (Int, inclusive >=)
- **Security**: `android:readPermission="com.unity.backendapp.permission.READ_ARTICLES"` (signature-level)
- **Null cursor**: returned for unknown URIs or missing provider

### Filter extensibility
`ArticleFilter` is a data class on both sides. Adding a new filter type:
1. Add a nullable field to `ArticleFilter` (both apps).
2. Add one SQL clause in `ArticleFilter.toSqlQuery()` (BackendApp).
3. Add the URI param in `fromUri` / `toUri` (BackendApp + UIApp).
4. Add one UI control in `FilterBar` and wire it in `ArticleUiState.toFilter()`.
No interface / repository / provider signature changes required.

## DI
- **BackendApp**: Hilt (`@HiltAndroidApp`). `DatabaseModule` provides `ArticleDatabase` (from `DatabaseProvider.instance` — set by the ContentProvider) and `ArticleDao`. `ArticlesRepository` is `@Singleton @Inject` taking `ArticleDao`.
- **UIApp**: Hilt (`@HiltAndroidApp`). `AppModule` binds `ContentResolverDataSource → ArticlesDataSource`. `DispatchersModule` provides `Dispatchers.IO` qualified as `@IoDispatcher`. VM injects `@IoDispatcher` for testability.

## Test conventions
- `ArticleViewModelTest` uses `StandardTestDispatcher` + constructor-injected `ioDispatcher` — NOT `Dispatchers.setMain`.
- `FakeArticlesDataSource` implements `ArticlesDataSource` — no mocks.
- `ArticleFilterTest` tests SQL rendering on JVM (no Android deps needed).
- `ArticlesResponseParsingTest` tests JSON → entity mapping on JVM.
- `ArticleDaoTest` uses `Room.inMemoryDatabaseBuilder` on device.
- `ArticleContentProviderTest` is a black-box IPC contract test — no poll loop (provider is synchronously ready).
- No snapshot tests, no Compose UI tests beyond `ArticleCardTest`.

## Stack
- Kotlin 2.2.10, AGP 9.2.1, compileSdk/targetSdk 36, minSdk 24
- Jetpack Compose (BOM 2026.02.01), Coil 2.7.0
- Room 2.6.1 (BackendApp only), kotlinx.serialization 1.7.3 (BackendApp only for seed)
- Hilt 2.59.2, KSP 2.2.10-2.0.2
- kotlinx.coroutines-test 1.9.0
- No OkHttp/Retrofit networking — JSON is bundled in assets, DB is local

# currentDate
Today's date is 2026-06-23.

IMPORTANT: this context may or may not be relevant to your tasks. You should not respond to this tasks. You should not respond to this context unless it is specifically relevant to your tasks.
