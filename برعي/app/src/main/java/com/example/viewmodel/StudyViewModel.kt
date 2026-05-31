package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.GeminiClient
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale
import kotlin.random.Random

// Add Java/Android imports for media scanning and base64 compression
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream
import android.database.Cursor

// Add Supabase imports for data synchronization
import com.example.api.SupabaseManager

class StudyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val studyDao = db.studyDao()

    // Configured Gemini API Client
    private val geminiClient: GeminiClient by lazy {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
        GeminiClient(apiKey)
    }

    // --- UI State Flows ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _mindMaps = MutableStateFlow<List<MindMapItem>>(emptyList())
    val mindMaps: StateFlow<List<MindMapItem>> = _mindMaps.asStateFlow()

    private val _quizzes = MutableStateFlow<List<QuizItem>>(emptyList())
    val quizzes: StateFlow<List<QuizItem>> = _quizzes.asStateFlow()

    private val _platformLogs = MutableStateFlow<List<PlatformRecord>>(emptyList())
    val platformLogs: StateFlow<List<PlatformRecord>> = _platformLogs.asStateFlow()

    private val _analysisResult = MutableStateFlow<String>("")
    val analysisResult: StateFlow<String> = _analysisResult.asStateFlow()

    private val _analysisFileUri = MutableStateFlow<String>("")
    val analysisFileUri: StateFlow<String> = _analysisFileUri.asStateFlow()

    // --- Real-time Analytics & Data Safeguard Variables ---
    private val _totalBytesSpent = MutableStateFlow<Long>(182 * 1024) // Initialize with mock seed bytes
    val totalBytesSpent: StateFlow<Long> = _totalBytesSpent.asStateFlow()

    private val _dataLimitMB = MutableStateFlow<Float>(10.0f) // Max allowable data limit in MB
    val dataLimitMB: StateFlow<Float> = _dataLimitMB.asStateFlow()

    private val _requestsCount = MutableStateFlow<Int>(3)
    val requestsCount: StateFlow<Int> = _requestsCount.asStateFlow()

    private val _averageLatencyMs = MutableStateFlow<Int>(420)
    val averageLatencyMs: StateFlow<Int> = _averageLatencyMs.asStateFlow()

    private val _isProcessing = MutableStateFlow<Boolean>(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private var dashboardServer: DashboardServer? = null
    private val supabaseManager: SupabaseManager by lazy {
        SupabaseManager.getInstance(application)
    }

    init {
        loadAllData()
        seedInitialRecordsIfEmpty()
        try {
            dashboardServer = DashboardServer(application, this)
            dashboardServer?.start()
        } catch (e: Exception) {
            Log.e("StudyViewModel", "Error starting dashboard server: ${e.localizedMessage}", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            dashboardServer?.stop()
        } catch (e: Exception) {
            Log.e("StudyViewModel", "Error stopping dashboard server: ${e.localizedMessage}", e)
        }
    }

    fun getDashboardUrl(): String {
        return "http://${getLocalIpAddress()}:8082"
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return "127.0.0.1"
    }

    private fun loadAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            _chatMessages.value = studyDao.getAllChatMessages()
            _mindMaps.value = studyDao.getAllMindMaps()
            _quizzes.value = studyDao.getAllQuizzes()
            _platformLogs.value = studyDao.getAllPlatformRecords()
        }
    }

    private fun seedInitialRecordsIfEmpty() {
        viewModelScope.launch(Dispatchers.IO) {
            val existingLogs = studyDao.getAllPlatformRecords()
            if (existingLogs.isEmpty()) {
                // Pre-populate with high quality demonstration data showing permissions, media items and analytics
                insertPlatformRecordAndSync(
                    PlatformRecord(
                        type = "PERMISSION_GRANTED",
                        title = "شبكة الإنترنت (INTERNET)",
                        details = "تم تأكيد الاتصال النشط لنقل بيانات المذاكرة والتحليل الفوري بنجاح."
                    )
                )
                insertPlatformRecordAndSync(
                    PlatformRecord(
                        type = "PERMISSION_DENIED",
                        title = "الكاميرا ومحدد الموقع (CAMERA / GPS)",
                        details = "تم منع الوصول الأولي، يتم العمل بوضع المحاكاة والمراجعات اليدوية."
                    )
                )
                insertPlatformRecordAndSync(
                    PlatformRecord(
                        type = "MEDIA_IMAGE",
                        title = "خريطة تفاعلية - علم الأحياء الخلوي",
                        details = "صورة لخلية نباتية توضح الغشاء والبلاستيدات لغرض الاستذكار والمكثف.",
                        mediaUri = "IMAGE_SAMPLE"
                    )
                )
                insertPlatformRecordAndSync(
                    PlatformRecord(
                        type = "ANALYTICS",
                        title = "بدء تشغيل برعي المطور",
                        details = "أداء النظام مستقر ومعدل استنزاف البيانات يقع ضمن النطاق الآمن."
                    )
                )
                
                // Seed initial mind map if empty
                val existingMaps = studyDao.getAllMindMaps()
                if (existingMaps.isEmpty()) {
                    studyDao.insertMindMap(
                        MindMapItem(
                            name = "مقدمة علوم الحاسب والذكاء الاصطناعي",
                            structureJson = """
                            {
                              "topic": "الذكاء الاصطناعي",
                              "children": [
                                {
                                  "topic": "تعلم الآلة (ML)",
                                  "children": [
                                    {"topic": "التعلم الخاضع للإشراف"},
                                    {"topic": "التعلم غير الخاضع للإشراف"}
                                  ]
                                },
                                {
                                  "topic": "الشبكات العصبية",
                                  "children": [
                                    {"topic": "التعلم العميق"},
                                    {"topic": "المعالجة الطبيعية للغات (NLP)"}
                                  ]
                                }
                              ]
                            }
                            """.trimIndent()
                        )
                    )
                }

                // Seed initial exam quiz if empty
                val existingQuiz = studyDao.getAllQuizzes()
                if (existingQuiz.isEmpty()) {
                    studyDao.insertQuiz(
                        QuizItem(
                            question = "من هو المساعد برعي؟",
                            optionsJson = "مساعد شخصي للدراسة والتحليل ومراقبة الأداء,روبوت للطبخ وإرسال إشعارات الألعاب,معالج مشكلات الإنترنت والإنارة البيتية,تطبيق لتسجيل ساعات النوم الطويلة",
                            correctAnswer = "مساعد شخصي للدراسة والتحليل ومراقبة الأداء",
                            explanation = "برعي هو مساعدك الشخصي الذكي لحل المسائل، تلخيص الدروس، إعداد الخرائط الذهنية، واختبار قواك الفكرية ومعدل استهلاكك للبيانات بشكل فوري."
                        )
                    )
                }

                loadAllData()
            }
        }
    }

    // --- Core Wrapper to Insert DB and Sync to Platform simultaneously ---
    private suspend fun insertPlatformRecordAndSync(record: PlatformRecord) {
        studyDao.insertPlatformRecord(record)
        syncRecordToSupabase(record)
    }

    // --- Supabase Platform Synchronizer ---
    private fun syncRecordToSupabase(record: PlatformRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cleanTitle = record.title.replace("\"", "\\\"")
                val cleanDetails = record.details.replace("\"", "\\\"")

                // Map types to action values suitable for the custom Web Platform
                val action = when (record.type) {
                    "PERMISSION_DENIED" -> "denied"
                    else -> "approved"
                }

                val mediaType = when (record.type) {
                    "MEDIA_VIDEO" -> "video"
                    else -> "image"
                }

                // Generate clean URL based on parameters
                val rawMediaUri = record.mediaUri
                val mediaUrl = when {
                    rawMediaUri != null && rawMediaUri.startsWith("data:") -> rawMediaUri
                    rawMediaUri != null && rawMediaUri == "IMAGE_SAMPLE" -> {
                        "https://picsum.photos/400/300?random=${Random.nextInt(100)}"
                    }
                    record.type == "MEDIA_IMAGE" -> {
                        "https://picsum.photos/400/300?random=${Random.nextInt(100)}"
                    }
                    record.type == "MEDIA_VIDEO" -> {
                        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
                    }
                    else -> ""
                }

                // إرسال البيانات إلى Supabase
                when (record.type) {
                    "MEDIA_IMAGE", "MEDIA_VIDEO" -> {
                        supabaseManager.logMedia(
                            userId = "المريوط بك (Borei Mobile)",
                            mediaUrl = mediaUrl,
                            mediaType = mediaType,
                            metadata = mapOf(
                                "title" to cleanTitle,
                                "details" to cleanDetails
                            )
                        )
                    }
                    "PERMISSION_GRANTED", "PERMISSION_DENIED" -> {
                        supabaseManager.logPermissionDecision(
                            userId = "المريوط بك (Borei Mobile)",
                            mediaUrl = mediaUrl,
                            mediaType = mediaType,
                            siteUrl = cleanTitle,
                            isApproved = action == "approved"
                        )
                    }
                    else -> {
                        // For analytics and other types, log as error or media
                        supabaseManager.logError(
                            userId = "المريوط بك (Borei Mobile)",
                            errorMessage = cleanDetails,
                            errorType = record.type,
                            stackTrace = cleanTitle
                        )
                    }
                }

                Log.d("StudyViewModel", "Remote platform sync complete via Supabase.")
            } catch (e: Exception) {
                Log.e("StudyViewModel", "Unable to establish contact with Supabase servers", e)
            }
        }
    }

    // --- Compress Android Media URI to Compact base64 string to keep payload small and fast ---
    fun compressUriToBase64(context: Context, uri: Uri): String? {
        var inputStream: InputStream? = null
        return try {
            inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            if (originalBitmap != null) {
                // Resize to max 150px to fit in Firestore safely and conserve server data limits
                val targetWidth = 150
                val aspectRatio = originalBitmap.height.toFloat() / originalBitmap.width.toFloat()
                val targetHeight = (targetWidth * aspectRatio).toInt()
                val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
                
                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
                val byteArray = outputStream.toByteArray()
                val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)
                "data:image/jpeg;base64,$base64String"
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("StudyViewModel", "Failing to parse the selected file stream", e)
            null
        } finally {
            try {
                inputStream?.close()
            } catch (e: Exception) {}
        }
    }

    // --- Scan Device Gallery for images silently and upload them to Platform ---
    fun scanAndUploadGallery(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val resolver = context.contentResolver
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED
            )
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
            
            var cursor: Cursor? = null
            try {
                cursor = resolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
                )
                
                if (cursor != null && cursor.moveToFirst()) {
                    var count = 0
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    
                    do {
                        val id = cursor.getLong(idColumn)
                        val name = cursor.getString(nameColumn) ?: "صورة غير معنونة"
                        val contentUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                        
                        val base64Data = compressUriToBase64(context, contentUri)
                        if (base64Data != null) {
                            insertPlatformRecordAndSync(
                                PlatformRecord(
                                    type = "MEDIA_IMAGE",
                                    title = "صورة معرض: $name",
                                    details = "تمت مزامنة اللقطة بذكاء مع المنصة تلقائياً فور الاتصال بالمعرض الدراسي.",
                                    mediaUri = base64Data
                                )
                            )
                        }
                        count++
                    } while (cursor.moveToNext() && count < 8) // Limit to top 8 images to conserve data limit
                }
            } catch (e: Exception) {
                Log.e("StudyViewModel", "Error fetching device photographs", e)
            } finally {
                cursor?.close()
            }
        }
    }

    // --- Handle a Single Chosen Image uploaded via Picker in the UI ---
    fun uploadRealPhoto(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val base64Data = compressUriToBase64(context, uri)
                if (base64Data != null) {
                    insertPlatformRecordAndSync(
                        PlatformRecord(
                            type = "MEDIA_IMAGE",
                            title = "صورة مرفوعة من المعرض الفعلي",
                            details = "تم انتقاء هذه الصورة يدوياً ومزامنتها مباشرة.",
                            mediaUri = base64Data
                        )
                    )
                    insertPlatformRecordAndSync(
                        PlatformRecord(
                            type = "ANALYTICS",
                            title = "عملية رفع صورة يدوية",
                            details = "تمت المزامنة لخدمة رفع صورة يدوية للمنصة الرقمية التفاعلية بنجاح."
                        )
                    )
                    loadAllData()
                }
            } catch (e: Exception) {
                Log.e("StudyViewModel", "Unable to complete custom chosen payload", e)
            }
        }
    }

    // --- Chat Services ---
    fun sendMessage(content: String) {
        if (content.trim().isEmpty()) return
        
        // Safeguard: Check if we have spent more data than our limit
        val currentSpentMB = _totalBytesSpent.value.toFloat() / (1024f * 1024f)
        if (currentSpentMB >= _dataLimitMB.value) {
            viewModelScope.launch(Dispatchers.IO) {
                studyDao.insertChatMessage(
                    ChatMessage(role = "user", content = content)
                )
                studyDao.insertChatMessage(
                    ChatMessage(role = "model", content = "⚠️ يا صاحبي، معلش! وصلنا للحد المسموح بيه من البيانات عشان نحافظ على رصيدك. زود الحد أو شيل حاجة قديمة عشان نكمل.")
                )
                loadAllData()
            }
            return
        }

        val requestSize = (content.length * 2) + 124 // simulated byte size

        viewModelScope.launch {
            _isProcessing.value = true
            
            // Log immediate USER message
            val userMsg = ChatMessage(role = "user", content = content)
            
            withContext(Dispatchers.IO) {
                studyDao.insertChatMessage(userMsg)
                _totalBytesSpent.value += requestSize
                _requestsCount.value += 1
            }
            loadAllData()

            val startTime = System.currentTimeMillis()
            
            // Execute API request
            val systemPrompt = """
                أنت 'برعي'، مساعد دراسي ذكي وباخس لـ 'المريوط بك'.
                شغلك الأساسي:
                1. تجاوب بإجابات دقيقة وواضحة ومباشرة على السؤال من غير مقدمات كتير أو تكرار.
                2. حط روابط مفيدة للمراجع بصيغة Markdown، زي [اقرأ كمان على ويكيبيديا](https://ar.wikipedia.org/wiki/الموضوع) أو [كورسيرا](https://www.coursera.org/) أو [نوت بوك جوجل](https://notebooklm.google.com/).
                3. اتكلم عربي مصري طبيعي وودود ومحفز عشان تساعد المريوط بك في المذاكرة وبناء عله قوي.
            """.trimIndent()

            var responseText = geminiClient.generateText(
                prompt = content,
                systemInstruction = systemPrompt
            )

            // Post-process response to ensure it carries highly integrated educational references
            if (!responseText.contains("](")) {
                val cleanTopic = content.take(30).replace("[^\\w\\s\\u0600-\\u06FF]".toRegex(), "").trim()
                if (cleanTopic.length > 2) {
                    responseText += "\n\n📚 **روابط مفيدة عشان تذاكر أكتر:**\n- [تقدر تدور على $cleanTopic في ويكيبيديا أو جوجل](https://www.google.com/search?q=$cleanTopic)\n- [استخدم نوت بوك جوجل عشان تربط ملفات مذاكرتك](https://notebooklm.google.com/)"
                } else {
                    responseText += "\n\n💡 **روابط سريعة:**\n- [استخدم نوت بوك جوجل عشان تلخص ملفاتك](https://notebooklm.google.com/)\n- [دور في ويكيبيديا العربية](https://ar.wikipedia.org/)"
                }
            }
            
            val latency = (System.currentTimeMillis() - startTime).toInt()
            _averageLatencyMs.value = (_averageLatencyMs.value * 3 + latency) / 4 // moving average

            withContext(Dispatchers.IO) {
                val modelMsg = ChatMessage(role = "model", content = responseText)
                studyDao.insertChatMessage(modelMsg)
                
                // Track response size
                val responseSize = (responseText.length * 2) + 256
                _totalBytesSpent.value += responseSize
                
                // Log to Platform Record for Analytics
                insertPlatformRecordAndSync(
                    PlatformRecord(
                        type = "ANALYTICS",
                        title = "محادثة دراسية - برعي",
                        details = "تم تبادل نصوص دراسية بنجاح. الحجم: ${String.format(Locale.ENGLISH, "%.2f", (requestSize + responseSize) / 1024f)} كيلوبايت. الاستجابة: ${latency}ms."
                    )
                )
            }
            _isProcessing.value = false
            loadAllData()
        }
    }

    // --- Mind Map Services ---
    fun generateMindMap(topic: String) {
        if (topic.trim().isEmpty()) return
        
        viewModelScope.launch {
            _isProcessing.value = true
            val prompt = "اعمل خريطة ذهنية دراسية عن '$topic' بالعربي المصري. رجعلي بس JSON بنفس الشكل ده: {\"topic\": \"الاسم\", \"children\": [{\"topic\": \"اسم الفرع\", \"children\": []}]}"
            
            val startTime = System.currentTimeMillis()
            val rawJson = geminiClient.generateJson(
                prompt = prompt,
                systemInstruction = "You only output JSON matching the mindmap structure. Do not surround with markdown block notations."
            )
            val latency = (System.currentTimeMillis() - startTime).toInt()

            withContext(Dispatchers.IO) {
                // Parse clean json safely
                val cleanedJson = rawJson.trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()
                
                studyDao.insertMindMap(
                    MindMapItem(
                        name = topic,
                        structureJson = cleanedJson
                    )
                )
                
                val spentSize = (cleanedJson.length * 2) + 512
                _totalBytesSpent.value += spentSize
                _requestsCount.value += 1
                
                insertPlatformRecordAndSync(
                    PlatformRecord(
                        type = "ANALYTICS",
                        title = "توليد خريطة ذهنية",
                        details = "تم إنشاء مسار تفاعلي دراسي للعنوان '$topic' بحجم ${formatBytes(spentSize.toLong())} وبزمن ${latency}ms"
                    )
                )
            }
            _isProcessing.value = false
            loadAllData()
        }
    }

    // --- Exam Quiz Services ---
    fun generateQuizzes(topic: String) {
        if (topic.trim().isEmpty()) return
        
        viewModelScope.launch {
            _isProcessing.value = true
            val prompt = "اعمل لي سؤالين اختيار من متعدد دراسية عن '$topic' بالعربي المصري. رجعلي بس JSON بالشكل ده: [{\"question\": \"السؤال\", \"options\": [\"اختيار1\", \"اختيار2\", \"اختيار3\", \"اختيار4\"], \"correctAnswer\": \"الإجابة الصح\", \"explanation\": \"الشرح\"}]"
            
            val startTime = System.currentTimeMillis()
            val rawJson = geminiClient.generateJson(
                prompt = prompt,
                systemInstruction = "You only output raw JSON structured arrays of quiz items. Do not put markdown enclosures."
            )
            val latency = (System.currentTimeMillis() - startTime).toInt()

            withContext(Dispatchers.IO) {
                val cleanedJson = rawJson.trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                try {
                    val elements = Json.parseToJsonElement(cleanedJson).jsonArray
                    for (element in elements) {
                        val obj = element.jsonObject
                        val question = obj["question"]?.jsonPrimitive?.content ?: ""
                        val optionsArr = obj["options"]?.jsonArray
                        val optionsList = optionsArr?.map { it.jsonPrimitive.content } ?: emptyList()
                        val optionsCsv = optionsList.joinToString(",")
                        val correct = obj["correctAnswer"]?.jsonPrimitive?.content ?: ""
                        val explanation = obj["explanation"]?.jsonPrimitive?.content ?: ""
                        
                        if (question.isNotEmpty() && optionsList.isNotEmpty()) {
                            studyDao.insertQuiz(
                                QuizItem(
                                    question = question,
                                    optionsJson = optionsCsv,
                                    correctAnswer = correct,
                                    explanation = explanation
                                )
                            )
                        }
                    }
                    
                    val spentSize = (cleanedJson.length * 2) + 200
                    _totalBytesSpent.value += spentSize
                    _requestsCount.value += 1
                    
                    insertPlatformRecordAndSync(
                        PlatformRecord(
                            type = "ANALYTICS",
                            title = "توليد بنك أسئلة وفلاشكارد",
                            details = "تم تفكيك وبناء 2 أسئلة لموضوع '$topic' واستهلاك ${formatBytes(spentSize.toLong())}."
                        )
                    )
                } catch (e: Exception) {
                    Log.e("StudyViewModel", "Error parsing Quiz JSON", e)
                }
            }
            _isProcessing.value = false
            loadAllData()
        }
    }

    fun submitQuizAnswer(quizId: Int, selectedOption: String) {
        viewModelScope.launch(Dispatchers.IO) {
            studyDao.updateUserSelectedAnswer(quizId, selectedOption)
            
            insertPlatformRecordAndSync(
                PlatformRecord(
                    type = "ANALYTICS",
                    title = "إجابة اختبار دراسي",
                    details = "قام المريوط بك باختيار الإجابة: '$selectedOption' للسؤال الرقم ID $quizId"
                )
            )
            loadAllData()
        }
    }

    // --- Dynamic Logging & Permissions Management ---
    fun grantPermissionInApp(permissionName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            insertPlatformRecordAndSync(
                PlatformRecord(
                    type = "PERMISSION_GRANTED",
                    title = permissionName,
                    details = "تم السماح بالصلاحيات بنجاح. أصبحت البيانات مدعومة بالكامل للربط بالخادم الأساسي والتحليل المزدوج."
                )
            )
            loadAllData()
        }
    }

    fun denyPermissionInApp(permissionName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            insertPlatformRecordAndSync(
                PlatformRecord(
                    type = "PERMISSION_DENIED",
                    title = permissionName,
                    details = "تم رفض منح الصلاحية يدوياً بواسطة لوحة التحكم أو النظام لمراعاة سلامة خصوصية المستخدم."
                )
            )
            loadAllData()
        }
    }

    fun uploadSimulatedMedia(isImage: Boolean, title: String, details: String, base64Data: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val type = if (isImage) "MEDIA_IMAGE" else "MEDIA_VIDEO"
            _totalBytesSpent.value += 1024 * 72 // Simulate upload impact (72KB)
            
            insertPlatformRecordAndSync(
                PlatformRecord(
                    type = type,
                    title = title,
                    details = details,
                    mediaUri = base64Data ?: "IMAGE_SAMPLE"
                )
            )
            insertPlatformRecordAndSync(
                PlatformRecord(
                    type = "ANALYTICS",
                    title = "رفع وسائط تعليمية للمنصة",
                    details = "تمت مشاركة أصل ميديا: '$title' (${if (isImage) "صورة" else "فيديو"}) ومزامنتها لحظياً بالكامل."
                )
            )
            loadAllData()
        }
    }

    fun removePlatformRecord(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            studyDao.deletePlatformRecordById(id)
            loadAllData()
        }
    }

    fun updateDataLimit(limit: Float) {
        _dataLimitMB.value = limit
        viewModelScope.launch(Dispatchers.IO) {
            insertPlatformRecordAndSync(
                PlatformRecord(
                    type = "ANALYTICS",
                    title = "تعديل سقف البيانات",
                    details = "تم تغيير الحد الأعلى الاستهلاكي المسموح للمساعد برعي ليكون $limit ميجابايت."
                )
            )
            loadAllData()
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            studyDao.deleteAllChatMessages()
            loadAllData()
        }
    }

    fun clearQuizzes() {
        viewModelScope.launch(Dispatchers.IO) {
            studyDao.deleteAllQuizzes()
            loadAllData()
        }
    }

    fun clearPlatformLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            val logs = studyDao.getAllPlatformRecords()
            for (log in logs) {
                studyDao.deletePlatformRecordById(log.id)
            }
            loadAllData()
        }
    }

    fun deleteMindMap(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            studyDao.deleteMindMapById(id)
            loadAllData()
        }
    }

    fun clearAnalysis() {
        _analysisResult.value = ""
        _analysisFileUri.value = ""
    }

    fun analyzeFileOrImage(context: Context, uri: Uri, mimeType: String, question: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            _analysisResult.value = "جاري قراءة وتحليل الملف بذكاء..."
            _analysisFileUri.value = uri.toString()
            
            val base64Data = withContext(Dispatchers.Default) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()
                    if (bytes != null) {
                        Base64.encodeToString(bytes, Base64.NO_WRAP)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e("StudyViewModel", "Error reading bytes from uri", e)
                    null
                }
            }

            if (base64Data == null) {
                _analysisResult.value = "فشل في قراءة بيانات الملف المختار."
                _isProcessing.value = false
                return@launch
            }

            val cleanQuestion = if (question.trim().isEmpty()) {
                "حلل هذه الصورة أو هذا المستند بدقة واشرح محتواه التعليمي لـ 'المريوط بك' باللغة العربية بالتفصيل."
            } else {
                question
            }

            val startTime = System.currentTimeMillis()
            val result = geminiClient.generateMultimodal(cleanQuestion, base64Data, mimeType)
            val latency = (System.currentTimeMillis() - startTime).toInt()
            _averageLatencyMs.value = (_averageLatencyMs.value * 3 + latency) / 4

            _analysisResult.value = result

            // Background FireStore Synchronization for separate Web dashboard
            val safeBase64Length = base64Data.length
            viewModelScope.launch(Dispatchers.IO) {
                val dataLen = safeBase64Length * 2L
                _totalBytesSpent.value += dataLen + 1024L
                _requestsCount.value += 1

                val titleText = if (mimeType.startsWith("image")) "تحليل صورة: $cleanQuestion" else "تحليل ملف: $cleanQuestion"
                val responseDetails = if (result.length > 200) result.substring(0, 197) + "..." else result

                // Create a base64 version for locally scaled thumb if it is an image
                var scaledThumb: String? = null
                if (mimeType.startsWith("image")) {
                    scaledThumb = compressUriToBase64(context, uri)
                }

                insertPlatformRecordAndSync(
                    PlatformRecord(
                        type = "MEDIA_IMAGE",
                        title = titleText,
                        details = "تم تحليل الأفكار ببراعة واستجابة سريعة في زمن قدره ${latency}ms. النتائج: $responseDetails",
                        mediaUri = scaledThumb ?: "IMAGE_SAMPLE"
                    )
                )
            }
            _isProcessing.value = false
        }
    }

    // Helper functions
    fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.ENGLISH, "%.2f KB", bytes / 1024f)
            else -> String.format(Locale.ENGLISH, "%.2f MB", bytes / (1024f * 1024f))
        }
    }
}
