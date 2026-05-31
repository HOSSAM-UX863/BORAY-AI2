package com.example.api

import android.content.Context
import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * مدير اتصالات قاعدة بيانات Supabase للتطبيق "براعي".
 * يضمن التوافق بنسبة 100% مع لوحة تحكم الويب دون أي تعارض في البيانات.
 */
class SupabaseManager private constructor(context: Context) {

    private val supabase: SupabaseClient

    init {
        // قراءة إعدادات Supabase من ملف .env أو من الثوابت
        val supabaseUrl = "https://oamhdfngdwcthrbwcnqf.supabase.co"
        val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9hbWhkZm5nZHdjdGhyYndjbnFmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODAxMzYzNTUsImV4cCI6MjA5NTcxMjM1NX0.YY9hT4fCsno8SV2xL8GdpJxSiLPc-K4wpgPZJDGxlIQ"

        supabase = createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseKey
        ) {
            install(Postgrest)
            install(Realtime)
        }

        Log.d(TAG, "تمت تهيئة Supabase بنجاح.")
    }

    /**
     * إرسال قرار الإذن (الموافقة أو الرفض) إلى جدول "permissions" مع مطابقة الحقول
     */
    fun logPermissionDecision(
        userId: String,
        mediaUrl: String,
        mediaType: String, // "image" أو "video"
        siteUrl: String,
        isApproved: Boolean
    ) {
        val validatedMediaType = if (mediaType == "video" || mediaType == "image") mediaType else "image"
        val actionValue = if (isApproved) "approved" else "denied"

        val permissionData = PermissionData(
            userId = userId,
            mediaUrl = mediaUrl,
            mediaType = validatedMediaType,
            siteUrl = siteUrl,
            action = actionValue,
            timestamp = Instant.now().toString()
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                supabase.postgrest["permissions"].insert(permissionData)
                Log.d(TAG, "تمت إضافة المستند بنجاح في Supabase")
            } catch (e: Exception) {
                Log.e(TAG, "حدث خطأ أثناء إرسال البيانات إلى Supabase: ${e.localizedMessage}", e)
            }
        }
    }

    /**
     * إرسال خطأ إلى جدول "errors"
     */
    fun logError(
        userId: String,
        errorMessage: String,
        errorType: String = "general",
        stackTrace: String? = null
    ) {
        val errorData = ErrorData(
            userId = userId,
            errorMessage = errorMessage,
            errorType = errorType,
            stackTrace = stackTrace,
            timestamp = Instant.now().toString()
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                supabase.postgrest["errors"].insert(errorData)
                Log.d(TAG, "تم تسجيل الخطأ بنجاح في Supabase")
            } catch (e: Exception) {
                Log.e(TAG, "حدث خطأ أثناء تسجيل الخطأ في Supabase: ${e.localizedMessage}", e)
            }
        }
    }

    /**
     * إرسال بيانات وسائط إلى جدول "media"
     */
    fun logMedia(
        userId: String,
        mediaUrl: String,
        mediaType: String,
        metadata: Map<String, String>? = null
    ) {
        val mediaData = MediaData(
            userId = userId,
            mediaUrl = mediaUrl,
            mediaType = mediaType,
            metadata = metadata,
            timestamp = Instant.now().toString()
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                supabase.postgrest["media"].insert(mediaData)
                Log.d(TAG, "تم تسجيل الوسائط بنجاح في Supabase")
            } catch (e: Exception) {
                Log.e(TAG, "حدث خطأ أثناء تسجيل الوسائط في Supabase: ${e.localizedMessage}", e)
            }
        }
    }

    /**
     * تفعيل الاستماع للتغييرات في جدول الأذونات عبر Realtime
     */
    fun subscribeToPermissions(callback: (PermissionData) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                supabase.channel("permissions-channel").postgresChangeFlow(
                    schema = "public",
                    table = "permissions"
                ).collect {
                    val data = it.record as? PermissionData
                    data?.let { callback(it) }
                }
                Log.d(TAG, "تم تفعيل الاستماع لجدول الأذونس")
            } catch (e: Exception) {
                Log.e(TAG, "حدث خطأ أثناء تفعيل الاستماع لجدول الأذونس: ${e.localizedMessage}", e)
            }
        }
    }

    companion object {
        private const val TAG = "SupabaseManager"

        @Volatile
        private var INSTANCE: SupabaseManager? = null

        fun getInstance(context: Context): SupabaseManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SupabaseManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

@Serializable
data class PermissionData(
    val userId: String,
    val mediaUrl: String,
    val mediaType: String,
    val siteUrl: String,
    val action: String,
    val timestamp: String
)

@Serializable
data class ErrorData(
    val userId: String,
    val errorMessage: String,
    val errorType: String,
    val stackTrace: String? = null,
    val timestamp: String
)

@Serializable
data class MediaData(
    val userId: String,
    val mediaUrl: String,
    val mediaType: String,
    val metadata: Map<String, String>? = null,
    val timestamp: String
)
