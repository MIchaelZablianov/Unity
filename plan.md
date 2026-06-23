# Android News App — Implementation Plan

## Architecture

- **Two APKs**: BackendApp (`com.unity.backendapp`) and UIApp (`com.unity.uiapp`)
- **IPC**: ContentProvider `query()` → Room-backed `Cursor`
- **Security**: Signature-protected custom permission (`com.unity.backendapp.permission.READ_ARTICLES`)
- **Data**: JSON bundled in `assets/raw/articles.json`, parsed via `kotlinx.serialization`, seeded lazily into Room on first query
- **Filtering**: `ArticleFilter` data class owns the contract. `toSqlQuery()` renders parameterized SQL (JVM-testable). `fromUri()` / `toUri()` handle URI encoding. Adding a new filter = one field + one SQL clause + one URI param.
- **Layering**: ContentProvider (IPC translation) → `ArticlesRepository` (filter → SQL) → `ArticleDao` (Room `@RawQuery`)
- **Image loading**: Coil in UIApp
- **UI**: Jetpack Compose, MVVM (ViewModel + StateFlow), Hilt DI

## IPC contract

```
ContentResolver.query(
    Uri.parse("content://com.unity.backendapp.provider/articles?titleQuery=X&ratingMin=Y"),
    null, null, null, null
) → Cursor

Columns: id, title, description, image_url, rating, color_red, color_green, color_blue
Filter params: titleQuery (String, case-insensitive LIKE), ratingMin (Int, >=)
Security: android:readPermission="com.unity.backendapp.permission.READ_ARTICLES" (signature)
```
