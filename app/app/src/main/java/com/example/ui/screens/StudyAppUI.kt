package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.viewmodel.StudyViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

// --- Color Constants for the "Warm Beige" Material 3 Theme ---
val BeigeBackground = Color(0xFFF5F5DC)
val BeigeSurface = Color(0xFFF5F5DC)
val BeigeSurfaceVariant = Color(0xFFEFEBE9)
val BlackText = Color(0xFF000000)
val BrownPrimary = Color(0xFF5D4037)
val BrownSecondary = Color(0xFF8D6E63)
val GoldAccent = Color(0xFFFFD700)
val WarningRed = Color(0xFFD32F2F)
val SuccessGreen = Color(0xFF388E3C)

// --- Compile-Safe Icon Aliases mapped to basic standard library icons ---
val IconChat: ImageVector = Icons.Default.Home
val IconDeviceHub: ImageVector = Icons.Default.Menu
val IconQuiz: ImageVector = Icons.Default.Search
val IconAlarm: ImageVector = Icons.Default.Add
val IconQueryStats: ImageVector = Icons.Default.Settings
val IconSchool: ImageVector = Icons.Default.Star
val IconDeleteSweep: ImageVector = Icons.Default.Delete
val IconStars: ImageVector = Icons.Default.Star
val IconAccountTree: ImageVector = Icons.Default.Menu
val IconAdjust: ImageVector = Icons.Default.Check
val IconDynamicFeed: ImageVector = Icons.Default.Menu
val IconCancel: ImageVector = Icons.Default.Close
val IconLightbulb: ImageVector = Icons.Default.Info
val IconPictureAsPdf: ImageVector = Icons.Default.Menu
val IconDownload: ImageVector = Icons.Default.ArrowBack
val IconRadioButtonChecked: ImageVector = Icons.Default.Check
val IconPhotoCamera: ImageVector = Icons.Default.Person
val IconVideoCameraBack: ImageVector = Icons.Default.PlayArrow
val IconImage: ImageVector = Icons.Default.Star
val IconPlayCircleFilled: ImageVector = Icons.Default.PlayArrow
val IconNetworkCheck: ImageVector = Icons.Default.Check
val IconAnalytics: ImageVector = Icons.Default.Info

class NodeData(val topic: String, val depth: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyAppUI(viewModel: StudyViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(0) } // 0: Chat, 1: MindMaps, 2: Quizzes, 3: Alarms & PDFs, 4: Platform Sync

    // Flows from ViewModel
    val chatMessages by viewModel.chatMessages.collectAsState()
    val mindMaps by viewModel.mindMaps.collectAsState()
    val quizzes by viewModel.quizzes.collectAsState()
    val platformLogs by viewModel.platformLogs.collectAsState()
    
    val totalBytesSpent by viewModel.totalBytesSpent.collectAsState()
    val dataLimitMB by viewModel.dataLimitMB.collectAsState()
    val requestsCount by viewModel.requestsCount.collectAsState()
    val averageLatencyMs by viewModel.averageLatencyMs.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    val currentDataMB = totalBytesSpent.toFloat() / (1024f * 1024f)
    val isDataGuardWarning = currentDataMB >= (dataLimitMB * 0.9f)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Brush.sweepGradient(listOf(GoldAccent, BrownSecondary, GoldAccent))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ب",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Column {
                            Text(
                                text = "بَرعي المساعد الدراسي",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    IconButton(
                        onClick = {
                            try {
                                uriHandler.openUri(viewModel.getDashboardUrl())
                            } catch (e: Exception) {
                                Toast.makeText(context, "فشل فتح لوحة التحكم: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("btn_external_dashboard")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "لوحة التحكم الخارجية للمالك",
                            tint = GoldAccent
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDataGuardWarning) WarningRed.copy(alpha = 0.2f) else BrownSecondary)
                            .border(
                                width = 1.dp,
                                color = if (isDataGuardWarning) WarningRed else BrownPrimary,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Data Limit",
                                tint = if (isDataGuardWarning) WarningRed else GoldAccent,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${String.format(Locale.ENGLISH, "%.2f", currentDataMB)} / ${dataLimitMB} MB",
                                fontSize = 11.sp,
                                color = if (isDataGuardWarning) WarningRed else BlackText,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrownPrimary,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = BrownPrimary,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(IconChat, contentDescription = "المساعد") },
                    label = { Text("المساعد برعي", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GoldAccent,
                        selectedTextColor = GoldAccent,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = BrownSecondary
                    ),
                    modifier = Modifier.testTag("tab_chat")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(IconDeviceHub, contentDescription = "الخرائط") },
                    label = { Text("خرائط ذهنية", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GoldAccent,
                        selectedTextColor = GoldAccent,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = BrownSecondary
                    ),
                    modifier = Modifier.testTag("tab_mindmap")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(IconQuiz, contentDescription = "اختبارات") },
                    label = { Text("اختبارك اليوم", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GoldAccent,
                        selectedTextColor = GoldAccent,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = BrownSecondary
                    ),
                    modifier = Modifier.testTag("tab_quizzes")
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.Share, contentDescription = "تحليل الصور") },
                    label = { Text("تحليل المستندات", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GoldAccent,
                        selectedTextColor = GoldAccent,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = BrownSecondary
                    ),
                    modifier = Modifier.testTag("tab_analysis")
                )
            }
        },
        containerColor = BrownPrimary
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> StudyChatTab(viewModel, chatMessages, isProcessing)
                1 -> MindMapTab(viewModel, mindMaps, isProcessing, onSwitchTab = { currentTab = it })
                2 -> QuizTab(viewModel, quizzes, isProcessing)
                3 -> FileAnalysisTab(viewModel, isProcessing)
            }
            
            // Global Processing Overlay
            if (isProcessing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = GoldAccent,
                    trackColor = BrownSecondary
                )
            }
        }
    }
}

// ================= HELPER: PARSE & DISPLAY CLICKABLE LINK BADGES =================
@Composable
fun MessageLinksRow(content: String) {
    val context = LocalContext.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    // Find Markdown links: [text](url)
    val linkPattern = "\\[([^\\]]+)\\]\\((https?://[^\\s\\)]+)\\)".toRegex()
    val matches = remember(content) { linkPattern.findAll(content).toList() }

    if (matches.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(BrownPrimary)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "🔗 مصادر دراسية وبحثية متصلة:",
            color = GoldAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        // Draw links horizontally scrollable
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            matches.forEach { match ->
                val title = match.groupValues[1]
                val url = match.groupValues[2]
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrownPrimary)
                        .border(1.dp, GoldAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable {
                            try {
                                uriHandler.openUri(url)
                            } catch (e: Exception) {
                                Toast.makeText(context, "فشل فتح الرابط: $url", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = GoldAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ================= TAB 0: STUDY CHAT (المساعد برعي) =================
@Composable
fun StudyChatTab(
    viewModel: StudyViewModel,
    messages: List<ChatMessage>,
    isProcessing: Boolean
) {
    var txtInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Message List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg ->
                val isUser = msg.role == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .wrapContentWidth(if (isUser) Alignment.End else Alignment.Start)
                    ) {
                        Column(
                            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isUser) 16.dp else 2.dp,
                                            bottomEnd = if (isUser) 2.dp else 16.dp
                                        )
                                    )
                                    .background(if (isUser) BrownSecondary else BrownPrimary)
                                    .border(
                                        width = 1.dp,
                                        color = if (isUser) GoldAccent.copy(alpha = 0.5f) else BrownPrimary,
                                        shape = RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isUser) 16.dp else 2.dp,
                                            bottomEnd = if (isUser) 2.dp else 16.dp
                                        )
                                    )
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = msg.content,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        textAlign = TextAlign.Right
                                    )
                                    MessageLinksRow(content = msg.content)
                                }
                            }
                            
                            Text(
                                text = if (isUser) "المريوط بك" else "برعي الوفي",
                                color = if (isUser) Color.Gray else GoldAccent,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Input Field Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .background(BrownSecondary, RoundedCornerShape(24.dp))
                .border(1.dp, BrownPrimary, RoundedCornerShape(24.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    viewModel.clearChatHistory()
                },
                modifier = Modifier.testTag("clear_chat_button")
            ) {
                Icon(IconDeleteSweep, contentDescription = "مسح المحادثة", tint = WarningRed)
            }

            Spacer(modifier = Modifier.width(4.dp))

            TextField(
                value = txtInput,
                onValueChange = { txtInput = it },
                placeholder = { Text("اسأل برعي هنا يا فندم...", color = Color.Gray, fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (txtInput.isNotEmpty()) {
                        viewModel.sendMessage(txtInput)
                        txtInput = ""
                    }
                })
            )

            IconButton(
                onClick = {
                    if (txtInput.isNotEmpty()) {
                        viewModel.sendMessage(txtInput)
                        txtInput = ""
                    }
                },
                enabled = txtInput.isNotEmpty() && !isProcessing,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (txtInput.isNotEmpty()) GoldAccent else BrownPrimary)
                    .testTag("send_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (txtInput.isNotEmpty()) BrownPrimary else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ================= TAB 1: SAVED & AI MIND MAPS (خرائط تفاعلية) =================
@Composable
fun MindMapTab(
    viewModel: StudyViewModel,
    mindMaps: List<MindMapItem>,
    isProcessing: Boolean,
    onSwitchTab: (Int) -> Unit
) {
    var selectedMap by remember { mutableStateOf<MindMapItem?>(null) }
    var inputTopic by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = BrownSecondary),
            border = BorderStroke(1.dp, BrownPrimary)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(IconDeviceHub, contentDescription = "MindMap", tint = GoldAccent)
                    Text("مولد الخرائط الذهنية الفوري", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                
                Text(
                    text = "اكتب عنواناً دراسياً لتشييد ورسم خريطة معرفية متشعبة ومتوافقة مع العقل بطريقة بصرية ممتازة.",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = inputTopic,
                        onValueChange = { inputTopic = it },
                        placeholder = { Text("مثال: فيزياء الحركة، النحو العربي...", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mindmap_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = BrownPrimary,
                            unfocusedContainerColor = BrownPrimary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = GoldAccent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = {
                            if (inputTopic.isNotEmpty()) {
                                viewModel.generateMindMap(inputTopic)
                                inputTopic = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        enabled = inputTopic.isNotEmpty() && !isProcessing,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("generate_mindmap_btn")
                    ) {
                        Text("شيد الخريطة", color = BrownPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left sidebar: list of saved maps
            Column(
                modifier = Modifier
                    .weight(0.40f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "الخرائط المشيدة (${mindMaps.size})",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                if (mindMaps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(BrownSecondary, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا توجد خرائط بعد", color = Color.DarkGray, fontSize = 11.sp)
                    }
                } else {
                    mindMaps.forEach { map ->
                        val isSelected = selectedMap?.id == map.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) GoldAccent.copy(alpha = 0.15f) else BrownSecondary)
                                .border(
                                    1.dp,
                                    if (isSelected) GoldAccent else BrownPrimary,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedMap = map }
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = map.name,
                                    color = if (isSelected) GoldAccent else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "مسار متكامل",
                                        color = Color.Gray,
                                        fontSize = 9.sp
                                    )
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Map",
                                        tint = WarningRed,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable {
                                                viewModel.deleteMindMap(map.id)
                                                if (selectedMap?.id == map.id) {
                                                    selectedMap = null
                                                }
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Right Viewport: Render the selected map
            Card(
                modifier = Modifier
                    .weight(0.60f)
                    .fillMaxHeight()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = BrownSecondary),
                border = BorderStroke(1.dp, BrownPrimary)
            ) {
                if (selectedMap == null && mindMaps.isNotEmpty()) {
                    selectedMap = mindMaps.firstOrNull()
                }

                val currMap = selectedMap
                if (currMap != null) {
                    RenderMindMapNodeView(currMap) { query ->
                        viewModel.sendMessage("حدثني واشرح لي بالتفصيل النقطة المعرفية التالية في خريطة الذهن: $query")
                        onSwitchTab(0)
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(IconDeviceHub, contentDescription = "Empty", tint = BrownPrimary, modifier = Modifier.size(48.dp))
                            Text(
                                "يرجى تشييد خريطة ذهنية جديدة أو اختيار إحدى الخرائط لفتح المعاينة الفورية تفاعلياً.",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RenderMindMapNodeView(map: MindMapItem, onAskBorei: (String) -> Unit) {
    val parsedNodes = remember(map.structureJson) {
        val list = mutableListOf<NodeData>()
        try {
            val jsonObject = JSONObject(map.structureJson)
            val coreTopic = jsonObject.optString("topic", map.name)
            list.add(NodeData(coreTopic, 0))

            val childrenArray = jsonObject.optJSONArray("children")
            if (childrenArray != null) {
                for (i in 0 until childrenArray.length()) {
                    val childObj = childrenArray.getJSONObject(i)
                    val firstChildTopic = childObj.optString("topic", "")
                    if (firstChildTopic.isNotEmpty()) {
                        list.add(NodeData(firstChildTopic, 1))

                        val leafArray = childObj.optJSONArray("children")
                        if (leafArray != null) {
                            for (j in 0 until leafArray.length()) {
                                val leafObj = leafArray.getJSONObject(j)
                                val leafTopic = leafObj.optString("topic", "")
                                if (leafTopic.isNotEmpty()) {
                                    list.add(NodeData(leafTopic, 2))
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            list.add(NodeData(map.name, 0))
            list.add(NodeData("الخلوية وعلم تصنيف المعارف", 1))
            list.add(NodeData("الخلايا النباتية وحجيرات النواة", 2))
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(BrownSecondary.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("تفاعلية ثلاثية الأبعاد", color = BrownSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Text("معاينة الخريطة", color = Color.Gray, fontSize = 10.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "العنوان: " + map.name,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        parsedNodes.forEach { node ->
            if (node.depth > 0) {
                Row(modifier = Modifier.padding(start = (node.depth * 20).dp)) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(14.dp)
                            .background(if (node.depth == 1) GoldAccent.copy(alpha = 0.4f) else SuccessGreen.copy(alpha = 0.4f))
                    )
                }
            }
            MindMapNodeCard(topic = node.topic, depth = node.depth, onAskBorei = onAskBorei)
        }
    }
}

@Composable
fun MindMapNodeCard(topic: String, depth: Int, onAskBorei: (String) -> Unit) {
    val containerBg = when (depth) {
        0 -> Brush.horizontalGradient(listOf(GoldAccent.copy(alpha = 0.3f), BrownPrimary))
        1 -> Brush.horizontalGradient(listOf(BrownSecondary.copy(alpha = 0.2f), BrownPrimary))
        else -> Brush.horizontalGradient(listOf(SuccessGreen.copy(alpha = 0.15f), BrownPrimary))
    }
    
    val borderColor = when (depth) {
        0 -> GoldAccent
        1 -> BrownSecondary
        else -> SuccessGreen.copy(alpha = 0.6f)
    }

    val paddingStart = when (depth) {
        0 -> 0.dp
        1 -> 20.dp
        else -> 40.dp
    }

    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = paddingStart)
            .clip(RoundedCornerShape(8.dp))
            .background(containerBg)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (depth == 0) IconStars else if (depth == 1) IconAccountTree else IconAdjust,
                    contentDescription = "Node Type",
                    tint = borderColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = topic,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            // Interactive Actions Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Google Notebook LM button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(BrownPrimary)
                        .clickable {
                            uriHandler.openUri("https://notebooklm.google.com/")
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "NotebookLM",
                        color = GoldAccent,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Ask Borei button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(borderColor.copy(alpha = 0.2f))
                        .clickable {
                            onAskBorei(topic)
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "اسأل برعي",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


// ================= TAB 2: EXAMS & FLASHCARDS (الاختبار الفوري) =================
@Composable
fun QuizTab(
    viewModel: StudyViewModel,
    quizzes: List<QuizItem>,
    isProcessing: Boolean
) {
    var quizTopicInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BrownSecondary),
            border = BorderStroke(1.dp, BrownPrimary)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(IconQuiz, contentDescription = "Quiz Hub", tint = GoldAccent)
                    Text("صانع الامتحانات والفلاشكارد الذكي", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                
                Text(
                    text = "اكتب اسم الملف أو المبحث الذي تذاكره، وسيقوم برعي على الفور ببناء اختبار تفاعلي فوري لتقييم مهاراتك.",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = quizTopicInput,
                        onValueChange = { quizTopicInput = it },
                        placeholder = { Text("مثال: الحرب العالمية الأولى، الهيكل الخلوي...", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quiz_topic_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = BrownPrimary,
                            unfocusedContainerColor = BrownPrimary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = GoldAccent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = {
                            if (quizTopicInput.isNotEmpty()) {
                                viewModel.generateQuizzes(quizTopicInput)
                                quizTopicInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        enabled = quizTopicInput.isNotEmpty() && !isProcessing,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("generate_quiz_btn")
                    ) {
                        Text("ابتكر أسئلة", color = BrownPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                
                if (quizzes.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { viewModel.clearQuizzes() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningRed),
                        border = BorderStroke(1.dp, WarningRed.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_quizzes_btn")
                    ) {
                        Text("تصفير بنك الأسئلة الحالي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Active Quiz Count banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("الأسئلة المتوفرة (${quizzes.size})", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            
            val solvedCount = quizzes.count { it.userSelectedAnswer != null }
            val correctCount = quizzes.count { it.userSelectedAnswer == it.correctAnswer }
            Text("تم الإجابة: $solvedCount / $correctCount صحيحة", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        if (quizzes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(BrownSecondary, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(IconDynamicFeed, contentDescription = "Quiz empty", tint = BrownPrimary, modifier = Modifier.size(40.dp))
                    Text("لا توجد أسئلة نشطة للمذاكرة. ابتكر بعضاً منها أعلاه!", color = Color.Gray, fontSize = 11.sp)
                }
            }
        } else {
            quizzes.forEach { quiz ->
                RenderQuizCard(quiz, onOptionSelected = { op ->
                    viewModel.submitQuizAnswer(quiz.id, op)
                })
            }
        }
    }
}

@Composable
fun RenderQuizCard(quiz: QuizItem, onOptionSelected: (String) -> Unit) {
    val options = remember(quiz.optionsJson) {
        quiz.optionsJson.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
    
    val isAnswered = quiz.userSelectedAnswer != null
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BrownSecondary),
        border = BorderStroke(1.dp, if (isAnswered) GoldAccent.copy(alpha = 0.4f) else BrownPrimary)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(GoldAccent.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("المستوى المعرفي • المريوط", color = GoldAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = quiz.question,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp
            )

            options.forEachIndexed { index, option ->
                val isThisOptionSelected = quiz.userSelectedAnswer == option
                val isCorrectOption = option == quiz.correctAnswer
                
                val rowBgColor = when {
                    !isAnswered -> BrownPrimary
                    isThisOptionSelected && isCorrectOption -> SuccessGreen.copy(alpha = 0.2f)
                    isThisOptionSelected && !isCorrectOption -> WarningRed.copy(alpha = 0.2f)
                    isCorrectOption -> SuccessGreen.copy(alpha = 0.15f)
                    else -> BrownPrimary.copy(alpha = 0.5f)
                }
                
                val rowBorderColor = when {
                    !isAnswered && isThisOptionSelected -> GoldAccent
                    !isAnswered -> BrownPrimary
                    isThisOptionSelected && isCorrectOption -> SuccessGreen
                    isThisOptionSelected && !isCorrectOption -> WarningRed
                    isCorrectOption -> SuccessGreen.copy(alpha = 0.5f)
                    else -> Color.Transparent
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(rowBgColor)
                        .border(1.dp, rowBorderColor, RoundedCornerShape(8.dp))
                        .clickable(enabled = !isAnswered) { onOptionSelected(option) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(if (isThisOptionSelected) GoldAccent else BrownPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ('A' + index).toString(),
                                color = if (isThisOptionSelected) BrownPrimary else Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Text(
                            text = option,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }

                    if (isAnswered) {
                        if (isCorrectOption) {
                            Icon(Icons.Default.Check, contentDescription = "Correct", tint = SuccessGreen, modifier = Modifier.size(18.dp))
                        } else if (isThisOptionSelected) {
                            Icon(IconCancel, contentDescription = "Wrong", tint = WarningRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            if (isAnswered) {
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BrownPrimary, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(IconLightbulb, contentDescription = "Explain", tint = GoldAccent, modifier = Modifier.size(14.dp))
                        Text("لماذا هذه هي الإجابة الصحيحة؟", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = quiz.explanation,
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}


// ================= TAB 3: FILE & IMAGE SCANNER / ANALYZER (تحليل الصور والمستندات) =================
@Composable
fun FileAnalysisTab(viewModel: StudyViewModel, isProcessing: Boolean) {
    val context = LocalContext.current
    val analysisResult by viewModel.analysisResult.collectAsState()
    val analysisFileUri by viewModel.analysisFileUri.collectAsState()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMimeType by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var questionInput by remember { mutableStateOf("") }

    // Restore selected URI if already analyzed in VM
    LaunchedEffect(analysisFileUri) {
        if (analysisFileUri.isNotEmpty() && selectedUri == null) {
            selectedUri = Uri.parse(analysisFileUri)
            selectedMimeType = if (analysisFileUri.contains(".jpg") || analysisFileUri.contains(".png") || analysisFileUri.contains("image")) "image/jpeg" else "application/pdf"
            fileName = if (selectedMimeType.startsWith("image")) "الصورة المحللة" else "المستند المحلل"
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            selectedMimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            fileName = "صورة مضافة من معرض الصور"
            viewModel.clearAnalysis()
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            selectedMimeType = context.contentResolver.getType(uri) ?: "application/pdf"
            fileName = "ملف تعليمي مضاف"
            viewModel.clearAnalysis()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            try {
                val cacheDir = context.cacheDir
                val file = java.io.File.createTempFile("borei_cam_", ".jpg", cacheDir)
                val out = java.io.FileOutputStream(file)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
                out.close()
                val uri = Uri.fromFile(file)
                selectedUri = uri
                selectedMimeType = "image/jpeg"
                fileName = "لقطة مأخوذة فوراً بالكاميرا"
                viewModel.clearAnalysis()
            } catch (e: Exception) {
                Toast.makeText(context, "فشل حفظ صورة الكاميرا: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Welcoming card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BrownSecondary),
            border = BorderStroke(1.dp, BrownPrimary)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(IconVideoCameraBack, contentDescription = "Cam", tint = GoldAccent, modifier = Modifier.size(24.dp))
                    Text("المحرر المتعدد للصور والمستندات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Text(
                    text = "التقط صورة لدفتر دراستك بالكاميرا، أو ارفع ملفاً دراسياً وسيقوم المساعد برعي بتحليله فورياً وحل المسائل المذكورة أو إعطائك الملخص الشامل.",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // Action Buttons Row (Camera / Gallery / Files)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Camera Button
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { cameraLauncher.launch(null) },
                colors = CardDefaults.cardColors(containerColor = BrownSecondary),
                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(IconPhotoCamera, contentDescription = "Camera", tint = GoldAccent, modifier = Modifier.size(28.dp))
                    Text("الكاميرا الفورية", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Gallery Button
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { galleryLauncher.launch("image/*") },
                colors = CardDefaults.cardColors(containerColor = BrownSecondary),
                border = BorderStroke(1.dp, BrownSecondary.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(IconImage, contentDescription = "Gallery", tint = BrownSecondary, modifier = Modifier.size(28.dp))
                    Text("معرض الصور", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // File Upload Button
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { fileLauncher.launch("*/*") },
                colors = CardDefaults.cardColors(containerColor = BrownSecondary),
                border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(IconPictureAsPdf, contentDescription = "Upload Document", tint = SuccessGreen, modifier = Modifier.size(28.dp))
                    Text("رفع المستندات", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Selected File Display Box
        val currentUri = selectedUri
        if (currentUri != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate830()),
                border = BorderStroke(1.dp, BrownPrimary)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(IconAdjust, contentDescription = "Verified", tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            Text(
                                text = "الملف النشط ومستقر للتحليل:",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Selected",
                            tint = WarningRed,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    selectedUri = null
                                    fileName = ""
                                    viewModel.clearAnalysis()
                                }
                        )
                    }

                    Text(
                        text = "الاسم: $fileName • نوع: $selectedMimeType",
                        color = GoldAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Instruction Prompt input
                    Text(
                        text = "ما الذي تود من المساعد برعي القيام به تجاه هذا الملف؟",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )

                    TextField(
                        value = questionInput,
                        onValueChange = { questionInput = it },
                        placeholder = { Text("مثال: لخص محاور الصورة، حل مسألة الرياضيات الواردة...", color = Color.Gray, fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("analysis_question_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = BrownPrimary,
                            unfocusedContainerColor = BrownPrimary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = GoldAccent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = {
                            viewModel.analyzeFileOrImage(context, currentUri, selectedMimeType, questionInput)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("trigger_analysis_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isProcessing
                    ) {
                        Text(
                            text = if (isProcessing) "جاري دراسة وتحليل الملف..." else "حلل الملف الآن بذكاء الاصطناعي",
                            color = BrownPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            // Placeholder when no file is selected
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(BrownSecondary, RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, BrownPrimary), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(IconCancel, contentDescription = "No file", tint = BrownPrimary, modifier = Modifier.size(32.dp))
                    Text(
                        text = "لم يتم تحديد أو التقاط مستند للتحليل بعد.",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "الرجاء النقر على الكاميرا أو المعرض بالأعلى للبدء.",
                        color = Color.DarkGray,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Analysis Result Card
        if (analysisResult.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BrownSecondary),
                border = BorderStroke(1.dp, BrownPrimary)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(IconStars, contentDescription = "Output", tint = GoldAccent, modifier = Modifier.size(18.dp))
                            Text(
                                "نتائج الفحص والتحليل الفوري ببرعي الذكي:",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(BrownPrimary)
                    )

                    Text(
                        text = analysisResult,
                        color = Color.White,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}


// ================= TAB 4: REAL-TIME ANALYTICS, PERMISSIONS & PLATFORM DASHBOARD =================
@Composable
fun PlatformConfigTab(
    viewModel: StudyViewModel,
    logs: List<PlatformRecord>,
    currentMBSpent: Float,
    dataLimitMB: Float,
    requestsCount: Int,
    averageLatencyMs: Int
) {
    var selectedMediaTab by remember { mutableStateOf(0) } // 0: Images & Videos, 1: Permissions Logs, 2: Analytics

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BrownSecondary),
            border = BorderStroke(1.dp, GoldAccent)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen)
                        )
                        Text("حالة الربط بالخادم مستقرة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text("مزامنة فورية للمريوط بك", color = GoldAccent, fontSize = 10.sp)
                }
                
                Text(
                    text = "تعمل هذه المحطة كمنظومة تحكم مركزية متصلة بالمنصة الأساسية لإرسال وتحويل النشاط والميديا وسلامة صلاحيات الجهاز لمنع نفاد الباقة المالية.",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        TabRow(
            selectedTabIndex = selectedMediaTab,
            containerColor = Slate830(),
            contentColor = GoldAccent
        ) {
            Tab(selected = selectedMediaTab == 0, onClick = { selectedMediaTab = 0 }) {
                Text("صور وفيديوهات", color = if (selectedMediaTab == 0) GoldAccent else Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            }
            Tab(selected = selectedMediaTab == 1, onClick = { selectedMediaTab = 1 }) {
                Text("الصلاحيات المطلوبة", color = if (selectedMediaTab == 1) GoldAccent else Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            }
            Tab(selected = selectedMediaTab == 2, onClick = { selectedMediaTab = 2 }) {
                Text("تحليلات الأداء", color = if (selectedMediaTab == 2) GoldAccent else Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            }
        }

        when (selectedMediaTab) {
            0 -> MediaStreamSection(viewModel, logs)
            1 -> SystemPermissionsSection(viewModel, logs)
            2 -> SystemAnalyticsDashboardSection(viewModel, currentMBSpent, dataLimitMB, requestsCount, averageLatencyMs, logs)
        }
    }
}

// Sub Tab Segment 0: Images & Videos Gallery (Displays user images and videos)
@Composable
fun MediaStreamSection(viewModel: StudyViewModel, logs: List<PlatformRecord>) {
    var titleInput by remember { mutableStateOf("") }
    var detailInput by remember { mutableStateOf("") }
    var isImageSelection by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val mediaItems = remember(logs) {
        logs.filter { it.type == "MEDIA_IMAGE" || it.type == "MEDIA_VIDEO" }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BrownSecondary),
            border = BorderStroke(1.dp, BrownPrimary)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("مشاركة ملف وسيط جديد للمنصة (Simulated Photo/Video)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = isImageSelection, onClick = { isImageSelection = true })
                        Text("صورة استذكار", color = Color.White, fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = !isImageSelection, onClick = { isImageSelection = false })
                        Text("فيديو تسجيلي", color = Color.White, fontSize = 11.sp)
                    }
                }

                TextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    placeholder = { Text("مثال: رسم كروكي لغشاء البكتيريا", color = Color.Gray, fontSize = 11.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("media_title_input"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = BrownPrimary,
                        unfocusedContainerColor = BrownPrimary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = GoldAccent
                    ),
                    singleLine = true
                )

                TextField(
                    value = detailInput,
                    onValueChange = { detailInput = it },
                    placeholder = { Text("ملاحظات إضافية للمدرس والأولياء...", color = Color.Gray, fontSize = 11.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("media_desc_input"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = BrownPrimary,
                        unfocusedContainerColor = BrownPrimary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = GoldAccent
                    ),
                    singleLine = true
                )

                val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    if (uri != null) {
                        viewModel.uploadRealPhoto(context, uri)
                        Toast.makeText(context, "تم رفع ومزامنة الصورة الحقيقية للمنصة.", Toast.LENGTH_SHORT).show()
                    }
                }

                Button(
                    onClick = {
                        if (titleInput.isNotEmpty() && detailInput.isNotEmpty()) {
                            viewModel.uploadSimulatedMedia(
                                isImage = isImageSelection,
                                title = titleInput,
                                details = detailInput
                            )
                            titleInput = ""
                            detailInput = ""
                            Toast.makeText(context, "تم رفع ومزامنة الميديا الجديدة للمنصة.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                    enabled = titleInput.isNotEmpty() && detailInput.isNotEmpty(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_media_btn")
                ) {
                    Text("رفع ومزامنة الميديا الآن", color = BrownPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        singlePhotoPickerLauncher.launch("image/*")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrownSecondary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .testTag("upload_real_photo_btn")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Pick Image", tint = BrownPrimary, modifier = Modifier.size(16.dp))
                        Text("اختر صورة حقيقية من معرض جهازك والمزامنة", color = BrownPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Text("تدفق الوسائط الحالي على المنصة (${mediaItems.size})", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

        if (mediaItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(BrownSecondary, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("لم يتم رفع أي وسائط أو صور من الطالب حتى اللحظة.", color = Color.Gray, fontSize = 11.sp)
            }
        } else {
            mediaItems.forEach { item ->
                val isPhoto = item.type == "MEDIA_IMAGE"
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BrownSecondary),
                    border = BorderStroke(1.dp, if (isPhoto) BrownSecondary.copy(alpha = 0.5f) else GoldAccent.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = if (isPhoto) IconPhotoCamera else IconVideoCameraBack,
                                    contentDescription = "MediaType",
                                    tint = if (isPhoto) BrownSecondary else GoldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isPhoto) "صورة مرفوعة" else "فيديو مسجل",
                                    color = if (isPhoto) BrownSecondary else GoldAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            IconButton(
                                onClick = { viewModel.removePlatformRecord(item.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = WarningRed, modifier = Modifier.size(16.dp))
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrownPrimary)
                                .drawBehind {
                                    val canvasWidth = size.width
                                    val canvasHeight = size.height
                                    val strokeWidth = 3f
                                    val colorPattern = if (isPhoto) BrownSecondary.copy(alpha = 0.15f) else GoldAccent.copy(alpha = 0.15f)
                                    
                                    for (i in 0..canvasWidth.toInt() step 50) {
                                        drawLine(
                                            color = colorPattern,
                                            start = Offset(i.toFloat(), 0f),
                                            end = Offset(i.toFloat(), canvasHeight),
                                            strokeWidth = strokeWidth,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPhoto) {
                                Icon(IconImage, contentDescription = "Simulated Photo", tint = BrownSecondary.copy(alpha = 0.5f), modifier = Modifier.size(36.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(IconPlayCircleFilled, contentDescription = "Play Video", tint = GoldAccent, modifier = Modifier.size(40.dp))
                                    Text("اضغط لتشغيل فيديو المراجعة", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(item.details, color = Color.LightGray, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }
            }
        }
    }
}

// Sub Tab Segment 1: System Permissions logs (requested and denied permissions logs)
@Composable
fun SystemPermissionsSection(viewModel: StudyViewModel, logs: List<PlatformRecord>) {
    val permissionLogs = remember(logs) {
        logs.filter { it.type == "PERMISSION_GRANTED" || it.type == "PERMISSION_DENIED" }
    }

    val declaredPermissions = listOf(
        Triple("android.permission.INTERNET", "صلاحية الوصول للإنترنت", "تمكن التطبيق من إجراء اتصالات سريعة بذكاء برعي والتحليل الفوري."),
        Triple("android.permission.CAMERA", "صلاحية تشغيل الكاميرا", "تسمح للطالب بالتقاط صور للمسائل مباشرة وحلها بالذكاء الاصطناعي."),
        Triple("android.permission.READ_MEDIA_IMAGES", "الوصول لمعرض الصور الدراسي", "تمكن برعي من استيراد ميديا علمية وصور مفرغة للمراجعة السلسة."),
        Triple("android.permission.READ_MEDIA_VIDEO", "الوصول لمستندات الفيديو التعليمي", "لإدراج الشروحات وتسجيلات الصوت داخل غرف التدقيق والمراجعة المعتمدة.")
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BrownSecondary),
            border = BorderStroke(1.dp, BrownPrimary)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("إدارة صلاحيات حماية النظام ومراجعة الأمان", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    "حدد أي من المتطلبات الفنية ترغب في محاكاتها أو تسجيلها في الخادم لإصدار تقرير شامل يوضح الصلاحيات الممنوحة (Granted) والمرفوضة (Denied).",
                    color = Color.LightGray, fontSize = 11.sp, lineHeight = 16.sp
                )
            }
        }

        Text("قائمة متطلبات النظام وإجراءات المحاكاة", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

        declaredPermissions.forEach { perm ->
            val newestLogOfThisPerm = permissionLogs.firstOrNull { it.title.contains(perm.first) || it.title.contains(perm.second) }
            val isCurrentGranted = newestLogOfThisPerm?.type == "PERMISSION_GRANTED"

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BrownSecondary)
                    .border(
                        1.dp,
                        if (isCurrentGranted) SuccessGreen.copy(alpha = 0.5f) else WarningRed.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isCurrentGranted) SuccessGreen else WarningRed)
                        )
                        Text(perm.second, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Text(perm.third, color = Color.LightGray, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                    Text(perm.first, color = Color.DarkGray, fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { viewModel.grantPermissionInApp(perm.second) },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("grant_" + perm.first.replace(".", "_"))
                    ) {
                        Text("موافق", color = BrownPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.denyPermissionInApp(perm.second) },
                        colors = ButtonDefaults.buttonColors(containerColor = WarningRed),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("deny_" + perm.first.replace(".", "_"))
                    ) {
                        Text("رفض", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Text("سجل الصلاحيات المودع لحظياً في السحابة (${permissionLogs.size})", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

        if (permissionLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(BrownSecondary, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("لا توجد تقارير صلاحيات في السجل بعد.", color = Color.Gray, fontSize = 11.sp)
            }
        } else {
            permissionLogs.forEach { logItem ->
                val isGrant = logItem.type == "PERMISSION_GRANTED"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isGrant) SuccessGreen.copy(alpha = 0.05f) else WarningRed.copy(alpha = 0.05f))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(logItem.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(logItem.details, color = Color.LightGray, fontSize = 10.sp)
                    }
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isGrant) SuccessGreen.copy(alpha = 0.2f) else WarningRed.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isGrant) "GRANTED" else "DENIED",
                            color = if (isGrant) SuccessGreen else WarningRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Sub Tab Segment 2: App Performance & Data Balance Analytics Dashboard
@Composable
fun SystemAnalyticsDashboardSection(
    viewModel: StudyViewModel,
    currentMBSpent: Float,
    dataLimitMB: Float,
    requestsCount: Int,
    averageLatencyMs: Int,
    logs: List<PlatformRecord>
) {
    var limitInputText by remember { mutableStateOf("") }
    val analyticsLogs = remember(logs) {
        logs.filter { it.type == "ANALYTICS" }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = BrownSecondary),
                border = BorderStroke(1.dp, BrownPrimary)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("إجمالي الطلبات الشبكية", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    Text("$requestsCount نداء خادم", color = GoldAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("حالة الخادم: ممتازة", color = SuccessGreen, fontSize = 9.sp)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = BrownSecondary),
                border = BorderStroke(1.dp, BrownPrimary)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("متوسط زمن الاستجابة", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    Text("$averageLatencyMs ms", color = BrownSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("سرعة المزامنة: فورية", color = SuccessGreen, fontSize = 9.sp)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BrownSecondary),
            border = BorderStroke(1.dp, BrownPrimary)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(IconNetworkCheck, contentDescription = "Data Guard", tint = GoldAccent)
                    Text("منظم ومكافئ رصيد استهلاك البيانات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                
                Text(
                    "حدد حداً أقصى لاستهلاك البيانات (ميجابايت) للتحذير ومنع المساعد برعي من استنزاف الرصيد قبل نفاده.",
                    color = Color.LightGray, fontSize = 11.sp, lineHeight = 16.sp
                )

                val safetyPercent = (currentMBSpent / dataLimitMB).coerceIn(0f, 1f)
                val sliderColor = if (safetyPercent > 0.85f) WarningRed else GoldAccent
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الاستهلاك الحالي: ${String.format(Locale.ENGLISH, "%.2f", currentMBSpent)} MB", color = Color.White, fontSize = 11.sp)
                        Text("الحد الأقصى: ${String.format(Locale.ENGLISH, "%.1f", dataLimitMB)} MB", color = GoldAccent, fontSize = 11.sp)
                    }
                    
                    LinearProgressIndicator(
                        progress = safetyPercent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = sliderColor,
                        trackColor = BrownPrimary
                    )
                    
                    Text(
                        text = if (safetyPercent > 0.85f) "⚠️ متأهب للخطر: استهلكت أكثر من 85% من الباقة!" else "✓ الوضع آمن للاستذكار والمذاكرة مع برعي.",
                        color = if (safetyPercent > 0.85f) WarningRed else SuccessGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = limitInputText,
                        onValueChange = { limitInputText = it },
                        placeholder = { Text("أدخل سقف الباقة (ميجابايت) لتجنب النفاد", color = Color.Gray, fontSize = 10.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("data_limit_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = BrownPrimary,
                            unfocusedContainerColor = BrownPrimary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = GoldAccent
                        ),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val valInput = limitInputText.toLongOrNull()?.toFloat() ?: limitInputText.toFloatOrNull()
                            if (valInput != null && valInput > 0f) {
                                viewModel.updateDataLimit(valInput)
                                limitInputText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        enabled = limitInputText.isNotEmpty(),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("update_limit_btn")
                    ) {
                        Text("تعديل السقف", color = BrownPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        Text("سجل العمليات وتحليلات الأداء المحدثة لحظياً (${analyticsLogs.size})", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

        if (analyticsLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(BrownSecondary, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("لا توجد عمليات مسجلة في المحلل الإحصائي.", color = Color.Gray, fontSize = 11.sp)
            }
        } else {
            analyticsLogs.forEach { log ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BrownSecondary, RoundedCornerShape(6.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(log.title, color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(log.details, color = Color.LightGray, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                    
                    Icon(IconAnalytics, contentDescription = "Stat", tint = BrownPrimary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun Slate830(): Color {
    return BrownPrimary
}
