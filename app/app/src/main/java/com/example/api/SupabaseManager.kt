package com.example.api

import android.content.Context
import android.util.Log

class SupabaseManager private constructor() {

    companion object {
        @Volatile private var instance: SupabaseManager? = null

        fun getInstance(context: Context): SupabaseManager {
            return instance ?: synchronized(this) {
                instance ?: SupabaseManager().also { instance = it }
            }
        }
    }

    fun logPermissionDecision(
        userId: String,
        mediaUrl: String,
        mediaType: String,
        siteUrl: String,
        isApproved: Boolean
    ) {
        Log.d("SupabaseMock", "permission logged: $userId / $mediaType")
    }

    fun logMedia(
        userId: String,
        mediaUrl: String,
        mediaType: String,
        metadata: Map<String, String>
    ) {
        Log.d("SupabaseMock", "media logged: $mediaUrl")
    }

    fun logError(
        userId: String,
        errorMessage: String,
        errorType: String,
        stackTrace: String
    ) {
        Log.d("SupabaseMock", "error logged: $errorType - $errorMessage")
    }
}
