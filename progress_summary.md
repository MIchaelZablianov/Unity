# Progress Summary

## Phase 1 — Backend: Data Model, Room DB, ContentProvider

**Status:** ✅ Complete

### Done
- Added `kotlinx-serialization-json` 1.7.3 to version catalog
- Created data model: `Article.kt` (with `@SerialName("image_url")`), `ArticlesResponse.kt`, `ArticleEntity.kt` (Room)
- Created `assets/raw/articles.json` with a 129-article dataset
- Created `ArticleDao.kt` with `@RawQuery` returning `Cursor`, `insertAll`, `count`
- Created `ArticleDatabase.kt` and `DatabaseProvider.kt` (bridge for ContentProvider → Hilt)
- Created `ArticleContentProvider.kt` using `query()` (not `call()`) — returns a Room Cursor
- Registered provider in `AndroidManifest.xml` with authority `com.unity.backendapp.provider`, `exported=true`
- Created `ArticleContentProviderTest.kt` (instrumentation test): 8 tests covering provider availability, full list, title filter, rating filter, combined filter, column names, unknown URI

### Architecture decisions
- Chose `ContentProvider.query()` → `Cursor` over `call()` → Bundle: Cursor is the standard Android idiom for tabular cross-process data, supports projection, and avoids forcing CRUD semantics.
- Chose Room as the persistence/cache layer (SQLite file survives process death, no foreground service needed).
- Room DB is built synchronously in `onCreate()` — cheap (file open only, no data I/O). Seeding happens lazily on first `query()`.

### Bugs & Issues
- Initial approach used an async `Thread` for DB init, which caused `query()` to return `null` until the thread completed — fixed by moving to synchronous init.

---

## Phase 2 — Backend: Architecture Cleanup (Repository + Filter Extract)

**Status:** ✅ Complete

### Done
- Created `ArticleFilter.kt` — data class owning the filter contract with `toSqlQuery()` (pure, JVM-testable), `fromUri()`, and `toUri()`
- Created `ArticlesRepository.kt` — domain layer between ContentProvider and Room, takes `ArticleDao`, exposes `query(filter): Cursor`
- Extracted `Article.toEntity()` mapping into `ArticlesResponse.kt`
- Thinned `ArticleContentProvider` to IPC translation only: `query()` parses URI → `ArticleFilter` → delegates to `ArticlesRepository`
- Added `provideArticleDao()` to Hilt `DatabaseModule`
- Created `ArticleFilterTest.kt` (6 JVM unit tests): SQL rendering, NOCASE, blank-title, injection safety
- Created `ArticlesResponseParsingTest.kt` (4 JVM unit tests): JSON parsing, snake_case mapping, entity flattening, unknown keys

### Decisions
- Filter logic lives in `ArticleFilter.toSqlQuery()` — pure Kotlin, no Android deps, unit-testable without a device. Provider only translates IPC ↔ filter.
- `ArticleFilter` is the extensibility point: adding a new filter type = one field + one SQL clause + one URI param. No interface/repository/provider signature changes.

---

## Phase 3 — UI App: Data Layer, DI, and UI

**Status:** ✅ Complete

### Done
- Created `ArticlesDataSource` interface — clean abstraction over IPC
- Created `ContentResolverDataSource` — real impl: `ContentResolver.query()` → Cursor → `List<Article>`, with `CancellationSignal` wired to coroutine cancellation, `@IoDispatcher` injection, cached column indices
- Created `ArticleFilter.kt` (UIApp mirror) with `toUri()` for URI param encoding
- Set up Hilt: `AppModule` (`@Binds`), `DispatchersModule` (`@IoDispatcher`), `@HiltViewModel`, `@AndroidEntryPoint`
- Created `ArticleViewModel.kt`: `StateFlow<ArticleUiState>`, `fetchJob?.cancel()` concurrency protection, `toFilter()` converts form state to data contract
- Created `ArticleCard.kt`: Coil `AsyncImage` with per-article placeholder color, `RatingBadge` with color tiers
- Created `ArticleListScreen.kt`: `FilterBar` (title text field + rating slider + Apply button), loading/error/empty/list states, `collectAsStateWithLifecycle()`
- Created `ArticleViewModelTest.kt` (10 unit tests): loading state, filter application, empty result, error state, error-vs-empty distinction, rating clamping
- Created `ArticleCardTest.kt` (3 Compose instrumented tests): title/description/rating rendering
- Created `ArticleFilterUriTest.kt` (5 instrumented tests): URI param encoding
- Added `INTERNET` permission, `<queries>` package declaration

### Decisions
- `ArticlesDataSource.fetchArticles(filter: ArticleFilter)` — filter is a value object, not fixed-arity params. New filter types don't change the interface.
- Errors propagate as `IOException` from `ContentResolverDataSource` (null cursor, timeout) so VM can show error vs empty.
- `CancellationSignal` is cancelled via `invokeOnCompletion` on the coroutine job, so timeout/cancellation actually stops the binder query.

### Bugs & Issues
- mockk was incompatible with Kotlin 2.2.10 — switched to interface-abstraction + fake pattern.
- `CancellationSignal` was initially not wired to coroutine cancellation — timeout was decorative. Fixed with `invokeOnCompletion`.

---

## Phase 4 — Security Hardening

**Status:** ✅ Complete

### Done
- Defined custom permission `com.unity.backendapp.permission.READ_ARTICLES` (`protectionLevel="signature"`) in BackendApp manifest
- Set `android:readPermission` on the ContentProvider
- UIApp declares `<uses-permission android:name="com.unity.backendapp.permission.READ_ARTICLES" />`
- Documented same-key signing requirement in both READMEs

### Decisions
- `signature` level: zero-friction (no user prompt), auto-granted for same-key APKs, fully blocks arbitrary third-party apps. This is the standard pattern for same-developer cross-app IPC.

---

## Phase 5 — Documentation

**Status:** ✅ Complete

### Done
- `AGENTS.md` — architecture, IPC contract, gotchas, test conventions, DI, stack, build commands
- `BackendApp/README.md` — build, install (same-key caveat), ADB verification, architecture, test table
- `UIApp/README.md` — build, install, usage, ADB verification, architecture, test table
- `AI_TOOLING_WRITEUP.md` — honest AI tooling use documentation
- `progress_summary.md` — this file

---

## Current state summary

- **BackendApp**: Room DB, lazy seed, `ArticleFilter`-based filtering, signature-protected ContentProvider, Hilt DI, 11 unit + 13 instrumented tests.
- **UIApp**: Compose MVVM + Hilt, `ArticleFilter` data object, error-propagating data source with CancellationSignal, 11 unit + 8 instrumented tests.
- **Total tests**: 43 (24 unit + 21 instrumented).
- **Both apps build and all unit tests pass.**
