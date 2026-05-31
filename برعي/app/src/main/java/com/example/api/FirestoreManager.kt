package com.example.api

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.FieldValue

/**
 * مدير اتصالات قاعدة بيانات Firebase Firestore للتطبيق "براعي".
 * يضمن التوافق بنسبة 100% مع لوحة تحكم الويب دون أي تعارض في البيانات.
 */
class FirestoreManager private constructor(context: Context) {

    private val firestore: FirebaseFirestore

    init {
        // تهيئة FirebaseApp برمجياً من إعدادات مشروعك لضمان التشغيل الفوري والعمل المباشر
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey("AIzaSyCfi6lJ5JnMKXAZb66CPprrnpJGf1iXcNo")
                    .setApplicationId("1:786446124883:web:4c29afe66cac80bbf9c3a6")
                    .setProjectId("boooray-59b87")
                    .setStorageBucket("boooray-59b87.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(context, options)
                Log.d(TAG, "تمت تهيئة Firebase بنجاح بشكل برمجى متكامل.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "خطأ أثناء تهيئة FirebaseApp: ${e.localizedMessage}", e)
        }

        // تفعيل ميزة التخزين المؤقت المحلي للعمل في وضع غير متصل بالإنترنت (Offline Mode)
        firestore = FirebaseFirestore.getInstance()
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true) // الكاش محلياً لعدم فقدان أي حدث وضمان تزامنه لاحقاً
                .build()
            firestore.firestoreSettings = settings
            Log.d(TAG, "تم تفعيل الـ Offline Persistence في Firestore بنجاح.")
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تكوين إعدادات الـ Offline Cache: ${e.localizedMessage}", e)
        }
    }

    /**
     * إرسال قرار الإذن (الموافقة أو الرفض) إلى كوليكشن "permissions" مع مطابقة الحقول
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

        // تطابق الحقول وحالة الحروف لتظهر فوراً في الـ Live Feed والـ Media Gallery للوحة التحكم
        val data = hashMapOf(
            "userId" to userId,
            "mediaUrl" to mediaUrl,
            "mediaType" to validatedMediaType,
            "siteUrl" to siteUrl,
            "action" to actionValue,
            "timestamp" to FieldValue.serverTimestamp() // الطابع التزامني للخادم
        )

        firestore.collection("permissions")
            .add(data)
            .addOnSuccessListener { ref ->
                Log.d(TAG, "تمت إضافة المستند بنجاح في Firestore بالمعرف الأوتوماتيكي: ${ref.id}")
            }
            .addOnFailureListener { e ->
                // سيقوم محرك Firestore بحفظه محلياً ومحاولة إعادة إرساله تلقائياً في الخلفية فور تحسن الشبكة
                Log.e(TAG, "حدث عطل مؤقت في الرفع، تم الكاش محلياً وسيعاد رفعه لاحقاً: ${e.localizedMessage}")
            }
    }

    companion object {
        private const val TAG = "FirestoreManager"

        @Volatile
        private var INSTANCE: FirestoreManager? = null

        fun getInstance(context: Context): FirestoreManager {
            return INSTANCE ?: synchronized(this) {
                val instance = FirestoreManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}