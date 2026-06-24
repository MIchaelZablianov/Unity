package com.unity.uiapp.core

/**
 * Thin logging abstraction so production code never touches `android.util.Log` directly.
 *
 * `android.util.Log` is stubbed in JVM unit tests and throws `RuntimeException("Stub!")`, which
 * previously forced defensive try/catch around every log call. Depending on this interface instead
 * lets tests inject a no-op implementation and keeps the call sites clean.
 */
interface Logger {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}
