package com.example.viewmodel

import android.content.Context
import android.util.Log
import com.example.data.PlatformRecord
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DashboardServer(private val context: Context, private val viewModel: StudyViewModel) {

    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private var executor: ExecutorService? = null
    private val prefs = context.getSharedPreferences("borei_dashboard_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val PORT = 8082
        private const val TAG = "DashboardServer"
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        executor = Executors.newFixedThreadPool(2)
        executor?.execute {
            try {
                serverSocket = ServerSocket(PORT)
                Log.d(TAG, "Socket-based Web Dashboard successfully started on port $PORT")
                while (isRunning) {
                    val socket = serverSocket?.accept() ?: break
                    executor?.execute {
                        handleClient(socket)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Dashboard web server socket took an exception: ${e.localizedMessage}", e)
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        executor?.shutdownNow()
    }

    private fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
            val firstLine = reader.readLine() ?: return
            
            // Parse HTTP Request line: e.g. "POST /setup HTTP/1.1"
            val parts = firstLine.split(" ")
            if (parts.size < 2) {
                socket.close()
                return
            }
            val method = parts[0].uppercase(Locale.ENGLISH)
            val fullPath = parts[1]
            // split query strings
            val path = fullPath.split("?")[0]

            // Parse headers
            var contentLength = 0
            var cookieHeader: String? = null
            
            while (true) {
                val headerLine = reader.readLine() ?: break
                if (headerLine.trim().isEmpty()) {
                    break // end of headers
                }
                val colonIndex = headerLine.indexOf(':')
                if (colonIndex != -1) {
                    val name = headerLine.substring(0, colonIndex).trim().lowercase(Locale.ENGLISH)
                    val value = headerLine.substring(colonIndex + 1).trim()
                    if (name == "content-length") {
                        contentLength = value.toIntOrNull() ?: 0
                    } else if (name == "cookie") {
                        cookieHeader = value
                    }
                }
            }

            // Read POST body if present
            var body = ""
            if (contentLength > 0) {
                val bodyChars = CharArray(contentLength)
                var totalRead = 0
                while (totalRead < contentLength) {
                    val read = reader.read(bodyChars, totalRead, contentLength - totalRead)
                    if (read == -1) break
                    totalRead += read
                }
                body = String(bodyChars, 0, totalRead)
            }

            // Evaluate Cookies & Auth Status
            val cookies = parseCookies(cookieHeader)
            val isSetup = prefs.contains("dashboard_password")
            val sessionToken = cookies["session_token"]
            val savedSession = prefs.getString("session_id", null)

            val isAuthenticated = isSetup && sessionToken != null && sessionToken == savedSession

            val out = socket.getOutputStream()

            // Root routing
            if (!isSetup) {
                if (path == "/setup" && method == "POST") {
                    handleSetupPost(out, body)
                } else {
                    serveSetupPage(out, null)
                }
            } else if (!isAuthenticated) {
                if (path == "/login" && method == "POST") {
                    handleLoginPost(out, body)
                } else {
                    serveLoginPage(out, null)
                }
            } else {
                // Authenticated routes
                when (path) {
                    "/", "/dashboard" -> {
                        serveDashboard(out)
                    }
                    "/update_limit" -> {
                        if (method == "POST") handleUpdateLimit(out, body) else redirect(out, "/")
                    }
                    "/clear_chat" -> {
                        if (method == "POST") handleClearChat(out) else redirect(out, "/")
                    }
                    "/clear_quizzes" -> {
                        if (method == "POST") handleClearQuizzes(out) else redirect(out, "/")
                    }
                    "/clear_logs" -> {
                        if (method == "POST") handleClearLogs(out) else redirect(out, "/")
                    }
                    "/logout" -> {
                        handleLogout(out)
                    }
                    else -> {
                        serveDashboard(out)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception handling client socket", e)
        } finally {
            try {
                socket.close()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun handleSetupPost(out: OutputStream, body: String) {
        val params = parseForm(body)
        val password = params["password"] ?: ""

        if (password.trim().length >= 4) {
            val sessionId = UUID.randomUUID().toString()
            prefs.edit()
                .putString("dashboard_password", password)
                .putString("session_id", sessionId)
                .apply()

            val headers = listOf(
                "Set-Cookie: session_token=$sessionId; Path=/; HttpOnly; SameSite=Lax",
                "Location: /dashboard"
            )
            sendResponse(out, 302, "Found", "text/plain", "Redirecting...", headers)
        } else {
            serveSetupPage(out, "رمز المرور قصير جداً! يجب أن يحتوي على 4 أحرف أو أرقام على الأقل.")
        }
    }

    private fun handleLoginPost(out: OutputStream, body: String) {
        val params = parseForm(body)
        val password = params["password"] ?: ""
        val savedPassword = prefs.getString("dashboard_password", "")

        if (password == savedPassword && password.isNotEmpty()) {
            val sessionId = UUID.randomUUID().toString()
            prefs.edit().putString("session_id", sessionId).apply()

            val headers = listOf(
                "Set-Cookie: session_token=$sessionId; Path=/; HttpOnly; SameSite=Lax",
                "Location: /dashboard"
            )
            sendResponse(out, 302, "Found", "text/plain", "Redirecting...", headers)
        } else {
            serveLoginPage(out, "رمز المرور غير صحيح! يرجى المحاولة مجدداً.")
        }
    }

    private fun handleUpdateLimit(out: OutputStream, body: String) {
        val params = parseForm(body)
        val limitStr = params["limit"] ?: ""
        try {
            val limitFloat = limitStr.toFloat()
            if (limitFloat > 0) {
                viewModel.updateDataLimit(limitFloat)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Invalid limit update string", e)
        }
        redirect(out, "/dashboard")
    }

    private fun handleClearChat(out: OutputStream) {
        viewModel.clearChatHistory()
        redirect(out, "/dashboard")
    }

    private fun handleClearQuizzes(out: OutputStream) {
        viewModel.clearQuizzes()
        redirect(out, "/dashboard")
    }

    private fun handleClearLogs(out: OutputStream) {
        viewModel.clearPlatformLogs()
        redirect(out, "/dashboard")
    }

    private fun handleLogout(out: OutputStream) {
        val headers = listOf(
            "Set-Cookie: session_token=; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT; HttpOnly",
            "Location: /login"
        )
        sendResponse(out, 302, "Found", "text/plain", "Redirecting...", headers)
    }

    // --- HTML Renders ---

    private fun serveSetupPage(out: OutputStream, error: String? = null) {
        val errorHtml = if (error != null) {
            "<div class='bg-red-500/10 border border-red-500 text-red-200 p-3 rounded-lg text-sm mb-4 text-right'>$error</div>"
        } else ""

        val html = """
            <!DOCTYPE html>
            <html lang="ar" dir="rtl">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>إنشاء رمز المرور للوحة برعي</title>
                <script src="https://cdn.tailwindcss.com"></script>
                <link href="https://fonts.googleapis.com/css2?family=Cairo:wght@400;600;700;800&display=swap" rel="stylesheet">
                <style>
                    body { font-family: 'Cairo', sans-serif; }
                </style>
            </head>
            <body class="bg-slate-950 text-slate-100 flex items-center justify-center min-h-screen p-4">
                <div class="w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-2xl relative overflow-hidden">
                    <div class="absolute top-0 right-0 left-0 h-1 bg-gradient-to-r from-sky-400 via-purple-500 to-emerald-400"></div>
                    <div class="text-center mb-6">
                        <div class="w-16 h-16 bg-sky-500/10 rounded-full flex items-center justify-center mx-auto mb-4 border border-sky-400/30">
                            <span class="text-sky-400 text-3xl font-extrabold">ب</span>
                        </div>
                        <h1 class="text-2xl font-extrabold text-white">إعداد لوحة التحكم الخارجية</h1>
                        <p class="text-sm text-slate-400 mt-2">إنشاء رمز مرور آمن للمالك فقط للوصول لنظام برعي</p>
                    </div>
                    
                    $errorHtml

                    <form action="/setup" method="POST" class="space-y-4">
                        <div>
                            <label class="block text-sm font-semibold text-slate-300 mb-1.5 text-right">رمز المرور الجديد للمدير</label>
                            <input type="password" name="password" required placeholder="أدخل رمز مرور قوي لا يقل عن ٤ رموز" class="w-full bg-slate-950 border border-slate-800 focus:border-sky-400 focus:ring-1 focus:ring-sky-400 rounded-xl px-4 py-3 text-white text-right outline-none transition">
                        </div>
                        <button type="submit" class="w-full bg-sky-500 hover:bg-sky-600 text-slate-950 font-bold py-3 rounded-xl transition shadow-lg shadow-sky-500/20">
                            إنشاء وحفظ رمز المرور
                        </button>
                    </form>
                </div>
            </body>
            </html>
        """.trimIndent()
        sendHtml(out, html)
    }

    private fun serveLoginPage(out: OutputStream, error: String? = null) {
        val errorHtml = if (error != null) {
            "<div class='bg-red-500/10 border border-red-500 text-red-200 p-3 rounded-lg text-sm mb-4 text-right'>$error</div>"
        } else ""

        val html = """
            <!DOCTYPE html>
            <html lang="ar" dir="rtl">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>تسجيل دخول المالك للوحة برعي</title>
                <script src="https://cdn.tailwindcss.com"></script>
                <link href="https://fonts.googleapis.com/css2?family=Cairo:wght@400;600;700;800&display=swap" rel="stylesheet">
                <style>
                    body { font-family: 'Cairo', sans-serif; }
                </style>
            </head>
            <body class="bg-slate-950 text-slate-100 flex items-center justify-center min-h-screen p-4">
                <div class="w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-2xl relative overflow-hidden">
                    <div class="absolute top-0 right-0 left-0 h-1 bg-gradient-to-r from-sky-400 via-purple-500 to-emerald-400"></div>
                    <div class="text-center mb-6">
                        <div class="w-16 h-16 bg-sky-500/10 rounded-full flex items-center justify-center mx-auto mb-4 border border-sky-400/30">
                            <span class="text-sky-400 text-3xl font-extrabold">ب</span>
                        </div>
                        <h1 class="text-2xl font-extrabold text-white">تسجيل دخول المالك والمدير</h1>
                        <p class="text-sm text-slate-400 mt-2">يرجى تأكيد هويتك برمز المرور السري للدخول</p>
                    </div>
                    
                    $errorHtml

                    <form action="/login" method="POST" class="space-y-4">
                        <div>
                            <label class="block text-sm font-semibold text-slate-300 mb-1.5 text-right">أدخل رمز المرور السري</label>
                            <input type="password" name="password" required placeholder="••••••••" class="w-full bg-slate-950 border border-slate-800 focus:border-sky-400 focus:ring-1 focus:ring-sky-400 rounded-xl px-4 py-3 text-white text-center outline-none transition">
                        </div>
                        <button type="submit" class="w-full bg-gradient-to-r from-sky-400 to-purple-500 text-slate-950 hover:opacity-90 font-bold py-3 rounded-xl transition shadow-lg">
                            تسجيل الدخول للمنصة
                        </button>
                    </form>
                </div>
            </body>
            </html>
        """.trimIndent()
        sendHtml(out, html)
    }

    private fun serveDashboard(out: OutputStream) {
        val totalBytes = viewModel.totalBytesSpent.value
        val currentDataMB = totalBytes.toFloat() / (1024f * 1024f)
        val dataLimit = viewModel.dataLimitMB.value
        val requests = viewModel.requestsCount.value
        val latency = viewModel.averageLatencyMs.value
        val logs = viewModel.platformLogs.value

        val progressPercent = Math.min(100, ((currentDataMB / dataLimit) * 100).toInt())
        val progressColor = if (progressPercent >= 85) "bg-red-500" else "bg-sky-400"

        // Build list of logs
        val logsBuilder = StringBuilder()
        if (logs.isEmpty()) {
            logsBuilder.append("""
                <tr>
                    <td colspan="4" class="px-6 py-8 text-center text-slate-500 text-sm">
                        لا توجد سجلات منشورة حتى الآن.
                    </td>
                </tr>
            """.trimIndent())
        } else {
            for (log in logs) {
                val badgeColor = when (log.type) {
                    "PERMISSION_GRANTED" -> "bg-emerald-500/10 text-emerald-400 border-emerald-500/20"
                    "PERMISSION_DENIED" -> "bg-red-500/10 text-red-400 border-red-500/20"
                    "MEDIA_IMAGE", "MEDIA_VIDEO" -> "bg-amber-500/10 text-amber-400 border-amber-500/20"
                    else -> "bg-sky-500/10 text-sky-400 border-sky-500/20"
                }
                val typeArabic = when (log.type) {
                    "PERMISSION_GRANTED" -> "صلاحية مقبولة"
                    "PERMISSION_DENIED" -> "صلاحية مرفوضة"
                    "MEDIA_IMAGE" -> "صورة مضافة"
                    "MEDIA_VIDEO" -> "فيديو مضاف"
                    else -> "أداء النظام"
                }

                logsBuilder.append("""
                    <tr class="border-b border-slate-800 hover:bg-slate-800/20 transition">
                        <td class="px-6 py-4 whitespace-nowrap text-sm text-slate-400">
                            ${formatDate(log.timestamp)}
                        </td>
                        <td class="px-6 py-4 whitespace-nowrap">
                            <span class="px-2.5 py-1 text-xs font-bold rounded-full border $badgeColor">$typeArabic</span>
                        </td>
                        <td class="px-6 py-4 text-sm font-semibold text-slate-200">
                            ${log.title}
                        </td>
                        <td class="px-6 py-4 text-xs text-slate-400 max-w-sm overflow-hidden text-ellipsis">
                            ${log.details}
                        </td>
                    </tr>
                """.trimIndent())
            }
        }

        val html = """
            <!DOCTYPE html>
            <html lang="ar" dir="rtl">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>لوحة برعي المطور المركزية</title>
                <script src="https://cdn.tailwindcss.com"></script>
                <link href="https://fonts.googleapis.com/css2?family=Cairo:wght@400;600;700;800&display=swap" rel="stylesheet">
                <style>
                    body { font-family: 'Cairo', sans-serif; }
                </style>
            </head>
            <body class="bg-slate-950 text-slate-100 min-h-screen flex flex-col">
                <!-- Navigation -->
                <nav class="bg-slate-900 border-b border-slate-800 sticky top-0 z-50 px-4 py-3 shadow-md">
                    <div class="max-w-7xl mx-auto flex justify-between items-center">
                        <div class="flex items-center space-x-3 space-x-reverse">
                            <div class="w-10 h-10 bg-sky-500/10 rounded-full flex items-center justify-center border border-sky-400/20">
                                <span class="text-sky-400 font-extrabold text-xl">ب</span>
                            </div>
                            <div>
                                <span class="font-extrabold text-lg text-white block">لوحة تحكم برعي</span>
                                <span class="text-xs text-emerald-400 block flex items-center">
                                    <span class="w-2 h-2 bg-emerald-500 rounded-full inline-block ml-1.5 animate-pulse"></span>
                                    منظومة المالك الخارجية النشطة
                                </span>
                            </div>
                        </div>
                        <a href="/logout" class="bg-slate-800 hover:bg-red-500/10 hover:text-red-400 border border-slate-700 hover:border-red-500/20 text-slate-400 px-4 py-2 rounded-xl text-sm font-bold transition">
                            خروج المالك
                        </a>
                    </div>
                </nav>

                <!-- Dashboard Content -->
                <main class="max-w-7xl mx-auto w-full p-4 lg:p-8 flex-1 space-y-6">
                    <!-- Stats Section -->
                    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
                        <!-- Data Spend Card -->
                        <div class="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-4">
                            <div class="flex justify-between items-start">
                                <span class="p-2 bg-sky-500/10 text-sky-400 rounded-xl border border-sky-500/10">
                                    <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path></svg>
                                </span>
                                <span class="text-xs text-slate-400 font-bold">باقة البيانات</span>
                            </div>
                            <div class="space-y-1">
                                <span class="text-2xl font-black text-white">${String.format(Locale.ENGLISH, "%.2f", currentDataMB)}</span>
                                <span class="text-slate-400 text-sm">/ $dataLimit ميجابايت</span>
                            </div>
                            <div class="space-y-1.5">
                                <div class="w-full bg-slate-950 rounded-full h-2 overflow-hidden">
                                    <div class="h-full $progressColor" style="width: $progressPercent%"></div>
                                </div>
                                <div class="flex justify-between text-xs text-slate-400">
                                    <span>المستهلك: $progressPercent%</span>
                                    <span>الحد الأقصى: $dataLimit ميجابايت</span>
                                </div>
                            </div>
                        </div>

                        <!-- Requests Count -->
                        <div class="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl flex flex-col justify-between space-y-4">
                            <div class="flex justify-between items-start">
                                <span class="p-2 bg-purple-500/10 text-purple-400 rounded-xl border border-purple-500/10">
                                    <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"></path></svg>
                                </span>
                                <span class="text-xs text-slate-400 font-bold">الطلبات والمحادثات</span>
                            </div>
                            <div>
                                <div class="text-3xl font-black text-white">$requests</div>
                                <p class="text-sm text-slate-400 mt-1">إجمالي التفاعلات بذكاء اصطناعي</p>
                            </div>
                        </div>

                        <!-- Average Latency -->
                        <div class="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl flex flex-col justify-between space-y-4">
                            <div class="flex justify-between items-start">
                                <span class="p-2 bg-emerald-500/10 text-emerald-400 rounded-xl border border-emerald-500/10">
                                    <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                                </span>
                                <span class="text-xs text-slate-400 font-bold">زمن الاستجابة</span>
                            </div>
                            <div>
                                <div class="text-3xl font-black text-white">${latency}ms</div>
                                <p class="text-sm text-slate-400 mt-1">السرعة المتوسطة للاستجابات والموجات</p>
                            </div>
                        </div>
                    </div>

                    <!-- Config & Danger Area -->
                    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
                        <!-- Config Form -->
                        <div class="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-4 lg:col-span-1">
                            <h2 class="text-base font-black text-white flex items-center">
                                <span class="w-2.5 h-2.5 bg-sky-400 rounded-full inline-block ml-2"></span>
                                تعديل سقف البيانات الاستهلاكية
                            </h2>
                            <p class="text-xs text-slate-400">حدد سقفاً مخصصاً بالـ (MB) لتوقف برعي المطور فجأة فور تخطيه ومكافحة استنزاف رصيد الباقة المالية الفعلي.</p>
                            
                            <form action="/update_limit" method="POST" class="space-y-4">
                                <div>
                                    <input type="number" step="0.5" min="1" name="limit" value="$dataLimit" required class="w-full bg-slate-950 border border-slate-800 focus:border-sky-400 focus:ring-1 focus:ring-sky-400 rounded-xl px-4 py-2.5 text-white text-center outline-none transition">
                                </div>
                                <button type="submit" class="w-full bg-sky-500 hover:bg-sky-600 text-slate-950 font-bold py-2 rounded-xl transition text-sm">
                                    حفظ وتعديل الحد
                                </button>
                            </form>
                        </div>

                        <!-- Management & Danger Zone -->
                        <div class="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-4 lg:col-span-2">
                            <h2 class="text-base font-black text-white flex items-center">
                                <span class="w-2.5 h-2.5 bg-red-500 rounded-full inline-block ml-2"></span>
                                المنطقة الحساسة (Danger Zone)
                            </h2>
                            <p class="text-xs text-slate-400">تحكم بتهيئة بيانات التطبيق وإفراغ الذاكرة المحلية والدروس لتحسين سرعة البناء وصيانة الأداء العام للمستودعات.</p>
                            
                            <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                                <form action="/clear_chat" method="POST" onsubmit="return confirm('هل أنت متأكد من مسح جميع محادثات المذاكرة نهائياً؟');">
                                    <button type="submit" class="w-full bg-slate-950 hover:bg-red-500/10 border border-slate-800 hover:border-red-500/20 text-slate-300 hover:text-red-400 font-bold py-3 px-4 rounded-xl transition text-xs">
                                        مسح سجل المحادثات
                                    </button>
                                </form>

                                <form action="/clear_quizzes" method="POST" onsubmit="return confirm('هل تريد إفراغ وحذف جميع الامتحانات المعدة؟');">
                                    <button type="submit" class="w-full bg-slate-950 hover:bg-amber-500/10 border border-slate-800 hover:border-amber-500/20 text-slate-300 hover:text-amber-400 font-bold py-3 px-4 rounded-xl transition text-xs">
                                        تصفير بنك الأسئلة
                                    </button>
                                </form>

                                <form action="/clear_logs" method="POST" onsubmit="return confirm('هل تريد تفريغ وحذف سجل لوحة التحكم بالكامل؟');">
                                    <button type="submit" class="w-full bg-slate-950 hover:bg-red-500/10 border border-slate-800 hover:border-red-500/20 text-slate-300 hover:text-red-400 font-bold py-3 px-4 rounded-xl transition text-xs">
                                        تصفير سجلات لوحة التحكم
                                    </button>
                                </form>
                            </div>
                        </div>
                    </div>

                    <!-- Logs Table -->
                    <div class="bg-slate-900 border border-slate-800 rounded-2xl shadow-xl overflow-hidden">
                        <div class="px-6 py-4 border-b border-slate-800">
                            <h2 class="text-base font-black text-white">سجلات مراقبة الأداء التزامنية</h2>
                            <p class="text-xs text-slate-400 mt-1">مراقبة لحظية لنشاط صلاحيات ومخرجات التطبيق في الوقت الحقيقي</p>
                        </div>
                        
                        <div class="overflow-x-auto">
                            <table class="w-full text-right">
                                <thead class="bg-slate-950 text-xs font-bold text-slate-400 uppercase tracking-wider border-b border-slate-800">
                                    <tr>
                                        <th class="px-6 py-3">تاريخ ووقت الحدث</th>
                                        <th class="px-6 py-3">النوع</th>
                                        <th class="px-6 py-3">العنوان</th>
                                        <th class="px-6 py-3 font-semibold">تفاصيل الحدث الفني</th>
                                    </tr>
                                </thead>
                                <tbody class="divide-y divide-slate-800">
                                    $logsBuilder
                                </tbody>
                            </table>
                        </div>
                    </div>
                </main>

                <footer class="bg-slate-900/40 border-t border-slate-800/80 py-4 px-4 text-center text-xs text-slate-500">
                    حقوق الإدارة محفوظة © 2026 برعي الوفي الذكائي • طُور بكل وفاء وإتقان للمالك.
                </footer>
            </body>
            </html>
        """.trimIndent()
        sendHtml(out, html)
    }

    // --- Helpers ---

    private fun parseForm(bodyString: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        val pairs = bodyString.split("&")
        for (pair in pairs) {
            val parts = pair.split("=", limit = 2)
            if (parts.size == 2) {
                try {
                    val key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8.name())
                    val value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name())
                    params[key] = value
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return params
    }

    private fun parseCookies(cookieHeader: String?): Map<String, String> {
        val cookies = mutableMapOf<String, String>()
        if (cookieHeader != null) {
            val pairs = cookieHeader.split(";")
            for (pair in pairs) {
                val parts = pair.split("=", limit = 2)
                if (parts.size == 2) {
                    cookies[parts[0].trim()] = parts[1].trim()
                }
            }
        }
        return cookies
    }

    private fun sendHtml(out: OutputStream, html: String) {
        val bytes = html.toByteArray(StandardCharsets.UTF_8)
        val response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        out.write(response.toByteArray(StandardCharsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    private fun redirect(out: OutputStream, targetLocation: String) {
        val response = "HTTP/1.1 302 Found\r\n" +
                "Location: $targetLocation\r\n" +
                "Connection: close\r\n\r\n"
        out.write(response.toByteArray(StandardCharsets.UTF_8))
        out.flush()
    }

    private fun sendResponse(out: OutputStream, code: Int, status: String, type: String, body: String, extraHeaders: List<String> = emptyList()) {
        val bodyBytes = body.toByteArray(StandardCharsets.UTF_8)
        val headersStr = extraHeaders.joinToString("\r\n") { it }
        val responseHeaders = "HTTP/1.1 $code $status\r\n" +
                "Content-Type: $type; charset=utf-8\r\n" +
                "Content-Length: ${bodyBytes.size}\r\n" +
                if (headersStr.isNotEmpty()) "$headersStr\r\n" else "" +
                "Connection: close\r\n\r\n"
        out.write(responseHeaders.toByteArray(StandardCharsets.UTF_8))
        out.write(bodyBytes)
        out.flush()
    }

    private fun formatDate(timestamp: Long): String {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        } catch (e: Exception) {
            timestamp.toString()
        }
    }
}
