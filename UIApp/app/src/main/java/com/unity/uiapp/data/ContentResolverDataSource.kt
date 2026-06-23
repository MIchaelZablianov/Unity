package com.unity.uiapp.data

import android.content.Context
import android.net.Uri
import android.os.CancellationSignal
import android.util.Log
import com.unity.uiapp.data.model.Article
import com.unity.uiapp.data.model.PlaceholderColor
import com.unity.uiapp.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private fun logD(tag: String, msg: String) = try { Log.d(tag, msg) } catch (_: RuntimeException) {}
private fun logE(tag: String, msg: String, e: Throwable? = null) =
    try { if (e != null) Log.e(tag, msg, e) else Log.e(tag, msg) } catch (_: RuntimeException) {}

@Singleton
class ContentResolverDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ArticlesDataSource {

    private val tag = "UIDataSource"
    private val contentResolver = context.contentResolver
    private val baseUri = Uri.parse("content://com.unity.backendapp.provider/articles")

    override suspend fun fetchArticles(filter: com.unity.uiapp.data.model.ArticleFilter): List<Article> =
        withContext(ioDispatcher) {
            val uri = filter.toUri(baseUri)
            logD(tag, "fetchArticles: uri=$uri")

            // Cancel the binder query if the coroutine is cancelled or times out. Without this,
            // withTimeout only unwinds the coroutine while the provider keeps running the query.
            val cancellationSignal = CancellationSignal()
            val parentJob = currentCoroutineContext()[kotlinx.coroutines.Job]
            val handle = parentJob?.invokeOnCompletion { cancellationSignal.cancel() }

            val cursor = try {
                withTimeout(QUERY_TIMEOUT_MS) {
                    contentResolver.query(uri, null, null, null, null, cancellationSignal)
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                handle?.dispose()
                logE(tag, "fetchArticles: timed out", e)
                throw IOException("Timed out contacting backend", e)
            } catch (e: java.util.concurrent.CancellationException) {
                // Coroutine was cancelled; honour cancellation rather than converting to empty.
                throw e
            } catch (e: Exception) {
                handle?.dispose()
                logE(tag, "fetchArticles: query threw exception", e)
                throw IOException("Failed to query backend", e)
            } finally {
                handle?.dispose()
            }

            if (cursor == null) {
                // A null cursor means the provider refused (unknown URI, or rejected before ready).
                // Surface as an error so the UI distinguishes "backend unreachable" from "no matches".
                throw IOException("Backend provider returned no cursor")
            }

            cursor.use { c ->
                // Resolve column indices once, not per row.
                val idxId = c.getColumnIndexOrThrow("id")
                val idxTitle = c.getColumnIndexOrThrow("title")
                val idxDescription = c.getColumnIndexOrThrow("description")
                val idxImageUrl = c.getColumnIndexOrThrow("image_url")
                val idxRating = c.getColumnIndexOrThrow("rating")
                val idxRed = c.getColumnIndexOrThrow("color_red")
                val idxGreen = c.getColumnIndexOrThrow("color_green")
                val idxBlue = c.getColumnIndexOrThrow("color_blue")

                val articles = ArrayList<Article>(c.count)
                while (c.moveToNext()) {
                    // ensureActive() lets a cancelled fetch bail out mid-iteration.
                    ensureActive()
                    articles.add(
                        Article(
                            id = c.getInt(idxId),
                            title = c.getString(idxTitle),
                            description = c.getString(idxDescription),
                            imageUrl = c.getString(idxImageUrl),
                            rating = c.getInt(idxRating),
                            placeholderColor = PlaceholderColor(
                                red = c.getInt(idxRed),
                                green = c.getInt(idxGreen),
                                blue = c.getInt(idxBlue)
                            )
                        )
                    )
                }
                logD(tag, "fetchArticles: parsed ${articles.size} articles")
                articles
            }
        }

    companion object {
        private const val QUERY_TIMEOUT_MS = 10_000L
    }
}
