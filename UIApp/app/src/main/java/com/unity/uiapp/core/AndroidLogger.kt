package com.unity.uiapp.core

import android.util.Log
import javax.inject.Inject

/** Production [Logger] that delegates to `android.util.Log`. */
class AndroidLogger @Inject constructor() : Logger {
    override fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
    }
}
