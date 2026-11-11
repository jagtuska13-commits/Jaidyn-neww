@file:OptIn(ExperimentalMaterial3Api::class)

package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.api.VoiceRecorder
import com.example.ui.features.DisputeCase
import com.example.ui.features.DisputeStatus
import com.example.ui.features.ShowcaseViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    private val viewModel: ShowcaseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                GlassChatApp(viewModel, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun AnimatedBlobsBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "blob")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(90000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(modifier = Modifier.fillMaxSize().background(Color(0xFF07030E))) {
        val w = size.width
        val h = size.height
        val colors = listOf(Color(0xFFFF007F), Color(0xFF7B2CBF), Color(0xFF00FFCC), Color(0xFFFFB703))

        for (i in 0 until 6) {
            val color = colors[i % colors.size]
            val cx = w / 2 + kotlin.math.sin(time * 0.05 + i * 1.5).toFloat() * (w / 1.6f)
            val cy = h / 2 + kotlin.math.cos(time * 0.04 + i * 2.0).toFloat() * (h / 1.6f)
            val radius = 320f + kotlin.math.sin(time * 0.1 + i).toFloat() * 120f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.5f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = radius
                ),
                radius = radius,
                center = Offset(cx, cy)
            )
        }
    }
}

fun getResizedBase64(context: Context, uri: Uri): Pair<String, String>? {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        if (originalBitmap == null) return null

        val maxSize = 1024
        var width = originalBitmap.width
        var height = originalBitmap.height
        if (width > maxSize || height > maxSize) {
            val ratio = width.toFloat() / height.toFloat()
            if (ratio > 1) {
                width = maxSize
                height = (maxSize / ratio).toInt()
            } else {
                height = maxSize
                width = (maxSize * ratio).toInt()
            }
        }
        val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(originalBitmap, width, height, true)
        val outputStream = java.io.ByteArrayOutputStream()
        scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
        val bytes = outputStream.toByteArray()
        return Pair(Base64.encodeToString(bytes, Base64.NO_WRAP), "image/jpeg")
    } catch (e: Exception) {
        return null
    }
}

@Composable
fun Bubble3D(isUser: Boolean, content: @Composable () -> Unit) {
    val corner = RoundedCornerShape(
        topStart = if (isUser) 26.dp else 10.dp,
        topEnd = if (isUser) 10.dp else 26.dp,
        bottomStart = 26.dp,
        bottomEnd = 26.dp
    )
    val tint = if (isUser) Color(0xFF00E5C7) else Color(0xFF80DEFF)

    Box(
        modifier = Modifier
            .widthIn(max = 300.dp)
            .graphicsLayer {
                shadowElevation = 10f
                ambientShadowColor = tint.copy(alpha = 0.16f)
                spotShadowColor = tint.copy(alpha = 0.24f)
                shape = corner
                clip = false
            }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.30f),
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                corner
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.0f),
                        Color.White.copy(alpha = 0.14f)
                    ),
                    startY = 0f,
                    endY = 900f
                ),
                corner
            )
            .background(Color(0xFF0A0E1A).copy(alpha = 0.22f), corner)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.65f),
                        tint.copy(alpha = 0.28f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = corner
            )
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .height(1.dp)
                .padding(horizontal = 14.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.8f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .height(20.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)
                    ),
                    corner
                )
        )
        content()
    }
}

@Composable
fun BubbleEntrance(isUser: Boolean, content: @Composable () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (isUser) {
            progress.snapTo(0.6f)
            progress.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 900f)
            )
        } else {
            progress.animateTo(
                1f,
                tween(450, easing = FastOutSlowInEasing)
            )
        }
    }
    Box(
        modifier = Modifier.graphicsLayer {
            val p = progress.value
            alpha = if (isUser) 1f else (p * 1.4f).coerceIn(0f, 1f)
            scaleX = 0.75f + 0.25f * p
            scaleY = 0.75f + 0.25f * p
            translationX = if (isUser) (1f - p) * 40f else 0f
            translationY = if (isUser) 0f else (1f - p) * 46f
        }
    ) {
        content()
    }
}

@Composable
fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(Color(0x1AFFFFFF), RoundedCornerShape(28.dp))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(28.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { i ->
            val scale = 0.6f + 0.4f * kotlin.math.abs(kotlin.math.sin((phase - i) * kotlin.math.PI.toFloat() / 1.5f))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        alpha = 0.4f + scale * 0.6f
                    }
                    .background(Color(0xFF80DEFF), CircleShape)
            )
        }
    }
}

@Composable
fun GlassChatApp(viewModel: ShowcaseViewModel, modifier: Modifier = Modifier) {
    val chatHistory by viewModel.chatHistory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isTranscribing by viewModel.isTranscribing.collectAsState()
    val isReadingDocState by viewModel.isReadingDoc.collectAsState()
    val personaName by viewModel.personaName.collectAsState()
    val personaText by viewModel.personaText.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userAbout by viewModel.userAbout.collectAsState()
    val jurisdiction by viewModel.jurisdiction.collectAsState()
    val legalDetail by viewModel.legalDetail.collectAsState()
    val citeSources by viewModel.citeSources.collectAsState()
    val speakReplies by viewModel.speakReplies.collectAsState()
    val useClonedVoice by viewModel.useClonedVoice.collectAsState()
    val useLiveSearch by viewModel.useLiveSearch.collectAsState()
    val clonedVoiceBase64 by viewModel.clonedVoiceBase64.collectAsState()
    val isSavingVoice by viewModel.isSavingVoice.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showSettingsPanel by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBase64 by remember { mutableStateOf<String?>(null) }
    var selectedImageMimeType by remember { mutableStateOf<String?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var voiceRecorder by remember { mutableStateOf<VoiceRecorder?>(null) }
    var isCloningRecording by remember { mutableStateOf(false) }
    var cloneRecorder by remember { mutableStateOf<VoiceRecorder?>(null) }
    var micAmplitude by remember { mutableStateOf(0f) }
    var selectedDocName by remember { mutableStateOf<String?>(null) }
    var selectedDocText by remember { mutableStateOf<String?>(null) }
    var docError by remember { mutableStateOf(false) }
    var showDisputes by remember { mutableStateOf(false) }
    var openDisputeId by remember { mutableStateOf<String?>(null) }
    var disputeError by remember { mutableStateOf(false) }
    var splashGone by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedImageUri = uri
                val base64Data = getResizedBase64(context, uri)
                selectedImageBase64 = base64Data?.first
                selectedImageMimeType = base64Data?.second
            }
        }
    )

    fun startRecording() {
        val audioFile = File(context.cacheDir, "voice_input_${System.currentTimeMillis()}.wav")
        val recorder = VoiceRecorder(audioFile)
        if (recorder.start()) {
            voiceRecorder = recorder
            isRecording = true
        }
    }

    fun startCloneRecording() {
        val audioFile = File(context.cacheDir, "voice_sample_${System.currentTimeMillis()}.wav")
        val recorder = VoiceRecorder(audioFile)
        if (recorder.start()) {
            cloneRecorder = recorder
            isCloningRecording = true
        }
    }

    fun stopRecording() {
        voiceRecorder?.stop { audioFile ->
            viewModel.transcribeAudio(audioFile) { text ->
                if (text.isNotBlank()) {
                    inputText += if (inputText.isNotEmpty()) " $text" else text
                }
            }
        }
        voiceRecorder = null
        isRecording = false
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startRecording()
        }
    }

    val clonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCloneRecording()
        }
    }

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            docError = false
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "document.pdf"
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null && bytes.size <= 8 * 1024 * 1024) {
                    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    viewModel.processDocument(b64, "application/pdf", name) { text ->
                        if (text != null) {
                            selectedDocName = name
                            selectedDocText = text
                        } else {
                            docError = true
                        }
                    }
                } else {
                    docError = true
                }
            } catch (e: Exception) {
                docError = true
            }
        }
    }

    val disputeImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                getResizedBase64(context, uri)?.let { (base64, mime) ->
                    disputeError = false
                    viewModel.createDispute(base64, mime) { ok ->
                        if (ok) showDisputes = true else disputeError = true
                    }
                } ?: run { disputeError = true }
            }
        }
    )

    fun toggleCloneRecording() {
        if (isCloningRecording) {
            cloneRecorder?.stop { audioFile ->
                viewModel.saveVoiceSample(audioFile)
            }
            cloneRecorder = null
            isCloningRecording = false
        } else {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                startCloneRecording()
            } else {
                clonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.lastIndex)
        }
    }

    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    LaunchedEffect(imeVisible) {
        if (imeVisible && chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.lastIndex)
        }
    }

    LaunchedEffect(isRecording) {
        while (isRecording) {
            micAmplitude = voiceRecorder?.amplitude ?: 0f
            kotlinx.coroutines.delay(50)
        }
        micAmplitude = 0f
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedBlobsBackground()

        AnimatedVisibility(
            visible = !splashGone,
            exit = scaleOut(tween(450, easing = FastOutSlowInEasing)) + fadeOut(tween(350))
        ) {
            Box(modifier = Modifier.fillMaxSize().clickable { splashGone = true }) {
                StratagemSplash()
            }
        }

        Crossfade(targetState = showDisputes, label = "screen") { full ->
        if (full) {
            DisputesScreen(
                viewModel = viewModel,
                onBack = { showDisputes = false },
                onNewDispute = { disputeImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                openDisputeId = openDisputeId,
                onOpenDispute = { openDisputeId = it },
                onCloseDispute = { openDisputeId = null },
                isError = disputeError,
                onDismissError = { disputeError = false }
            )
        } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "STRATAGEM",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp,
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF00FFCC), Color(0xFF80DEFF), Color(0xFFFF007F))
                        )
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showDisputes = true }) {
                        Text("⚖️", fontSize = 20.sp)
                    }
                    IconButton(onClick = { showSettingsPanel = !showSettingsPanel }) {
                        Text("⚙️", fontSize = 20.sp)
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(chatHistory, key = { message -> message.id }) { message ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
                    ) {
                        BubbleEntrance(message.isUser) {
                        Bubble3D(message.isUser) {
                            Column {
                                if (message.imageBase64 != null) {
                                    val bitmap = remember(message.imageBase64) {
                                        val bytes = Base64.decode(message.imageBase64, Base64.DEFAULT)
                                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                    }
                                    bitmap?.let { Image(it, "Attached image", Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)).padding(bottom = 8.dp), contentScale = ContentScale.Crop) }
                                }
                                if (message.documentName != null) {
                                    Row(
                                        modifier = Modifier
                                            .background(Color(0x26FFFFFF), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("📄", fontSize = 12.sp)
                                        Spacer(Modifier.width(6.dp))
                                        Text(message.documentName!!, color = Color(0xFF80DEFF), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                    if (message.text.isNotBlank()) Spacer(Modifier.height(8.dp))
                                }
                                if (message.text.isNotBlank()) Text(message.text, color = Color.White, fontSize = 15.sp)
                                if (message.audioBase64 != null) {
                                    Button(onClick = { viewModel.playAudioFromBase64(message.audioBase64, context.cacheDir) }, colors = ButtonDefaults.buttonColors(Color(0x33FFFFFF))) { Text("🔊 Play") }
                                }
                            }
                        }
                        }
                    }
                }
                if (isLoading) {
                    item(key = "typing-indicator") {
                        Row(modifier = Modifier.fillMaxWidth().animateItem(), horizontalArrangement = Arrangement.Start) {
                            StrataLoader()
                        }
                    }
                }
            }

            if (selectedDocName != null || isReadingDocState || docError) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (docError) {
                        Text("Couldn't read that document. Try a smaller PDF.", color = Color(0xFFFF8FA3), fontSize = 12.sp)
                    } else {
                        Text(
                            if (isReadingDocState) "📄 Reading document..." else "📄 $selectedDocName",
                            color = Color(0xFF80DEFF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (!isReadingDocState) {
                            Text("✕", color = Color(0x80FFFFFF), fontSize = 12.sp, modifier = Modifier.clickable {
                                selectedDocName = null
                                selectedDocText = null
                            })
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .background(Color(0x26FFFFFF), RoundedCornerShape(28.dp))
                        .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(28.dp)),
                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                    decorationBox = { innerTextField ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                            IconButton(onClick = { imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.size(38.dp)) { Text("+", color = Color.White, fontSize = 20.sp) }
                            IconButton(onClick = { docPickerLauncher.launch("application/pdf") }, modifier = Modifier.size(38.dp)) { Text("📄", color = Color.White, fontSize = 15.sp) }
                            PulsingMicButton(
                                isRecording = isRecording,
                                isTranscribing = isTranscribing,
                                amplitude = micAmplitude,
                                onToggle = {
                                    if (isRecording) {
                                        stopRecording()
                                    } else {
                                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            startRecording()
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (inputText.isEmpty()) Text("Message STRATAGEM...", color = Color(0x80FFFFFF), fontSize = 15.sp)
                                innerTextField()
                            }
                            AnimatedSendButton(
                                enabled = (inputText.isNotBlank() || selectedImageBase64 != null || selectedDocText != null) && !isLoading,
                                onClick = {
                                    splashGone = true
                                    viewModel.sendMessage(
                                        inputText,
                                        context.cacheDir,
                                        selectedImageBase64,
                                        selectedImageMimeType,
                                        selectedDocName,
                                        selectedDocText
                                    )
                                    inputText = ""
                                    selectedImageUri = null
                                    selectedImageBase64 = null
                                    selectedImageMimeType = null
                                    selectedDocName = null
                                    selectedDocText = null
                                }
                            )
                        }
                    }
                )
            }
        }
        }
        }

        if (showSettingsPanel) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsPanel = false },
                containerColor = Color(0xFF0F0B1A),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "PERSONA & PROFILE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                        style = TextStyle(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF00FFCC), Color(0xFF80DEFF), Color(0xFFFF007F))
                            )
                        )
                    )

                    DrawerTextField(
                        label = "AI name",
                        value = personaName,
                        onValueChange = { viewModel.updatePersonaName(it) },
                        placeholder = "e.g. Gem"
                    )

                    DrawerTextField(
                        label = "AI personality",
                        value = personaText,
                        onValueChange = { viewModel.updatePersonaText(it) },
                        placeholder = "Inject personality: witty, caring, sarcastic...",
                        minHeight = 72.dp
                    )

                    DrawerTextField(
                        label = "Your name",
                        value = userName,
                        onValueChange = { viewModel.updateUserName(it) },
                        placeholder = "e.g. Alex"
                    )

                    DrawerTextField(
                        label = "About you",
                        value = userAbout,
                        onValueChange = { viewModel.updateUserAbout(it) },
                        placeholder = "Tell the AI who you are: interests, job, style...",
                        minHeight = 72.dp
                    )

                    DrawerTextField(
                        label = "YOUR JURISDICTION",
                        value = jurisdiction,
                        onValueChange = { viewModel.updateJurisdiction(it) },
                        placeholder = "e.g. New South Wales, Australia / California, USA",
                        minHeight = 52.dp
                    )

                    Text("LEGAL DETAIL LEVEL", color = Color(0xFF80DEFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("simple" to "Plain", "standard" to "Standard", "detailed" to "Detailed").forEach { (key, label) ->
                            val selected = legalDetail == key
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (selected) Brush.linearGradient(listOf(Color(0xFF00E5C7), Color(0xFF0077B6))) else Brush.linearGradient(listOf(Color(0x1AFFFFFF), Color(0x1AFFFFFF))),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .border(1.dp, if (selected) Color.Transparent else Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                                    .clickable { viewModel.updateLegalDetail(key) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(label, color = if (selected) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Cite current law", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("Name the statute, regulation, or case for every answer", color = Color(0x80FFFFFF), fontSize = 12.sp)
                        }
                        Switch(
                            checked = citeSources,
                            onCheckedChange = { viewModel.setCiteSources(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF00FFCC),
                                checkedTrackColor = Color(0x3300FFCC)
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Speak replies aloud", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("Every response is spoken automatically", color = Color(0x80FFFFFF), fontSize = 12.sp)
                        }
                        Switch(
                            checked = speakReplies,
                            onCheckedChange = { viewModel.setSpeakReplies(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF00FFCC),
                                checkedTrackColor = Color(0x3300FFCC)
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Live web search", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("Searches the web for current facts before replying", color = Color(0x80FFFFFF), fontSize = 12.sp)
                        }
                        Switch(
                            checked = useLiveSearch,
                            onCheckedChange = { viewModel.setUseLiveSearch(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF00FFCC),
                                checkedTrackColor = Color(0x3300FFCC)
                            )
                        )
                    }

                    HorizontalDivider(color = Color(0x1FFFFFFF))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Use MY voice", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text(
                                if (clonedVoiceBase64.isNotBlank()) "Voice sample saved — replies in your voice" else "Record a sample to unlock voice cloning",
                                color = Color(0x80FFFFFF),
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = useClonedVoice,
                            enabled = clonedVoiceBase64.isNotBlank(),
                            onCheckedChange = { viewModel.setUseClonedVoice(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF00FFCC),
                                checkedTrackColor = Color(0x3300FFCC)
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { toggleCloneRecording() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                if (isCloningRecording) Color(0xFFFF4D6D) else Color(0x33FFFFFF)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                when {
                                    isCloningRecording -> "⏹ Stop recording"
                                    isSavingVoice -> "Saving..."
                                    else -> "🎙 Record voice sample"
                                },
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                        if (clonedVoiceBase64.isNotBlank()) {
                            OutlinedButton(
                                onClick = { viewModel.clearVoiceSample() },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Delete", color = Color(0xFFFF8FA3), fontSize = 14.sp)
                            }
                        }
                    }

                    Text(
                        "Speak naturally for 5–15 seconds. Your sample stays on this device and is sent only to generate your replies.",
                        color = Color(0x60FFFFFF),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minHeight: Dp = 52.dp
) {
    Column {
        Text(label, color = Color(0xFF80DEFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .background(Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, color = Color(0x60FFFFFF), fontSize = 14.sp)
                    inner()
                }
            }
        )
    }
}
@Composable
fun PulsingMicButton(isRecording: Boolean, isTranscribing: Boolean, amplitude: Float, onToggle: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "mic-pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val ampScale by animateFloatAsState(
        targetValue = 1f + amplitude * 0.6f,
        animationSpec = tween(80),
        label = "amp-scale"
    )
    val scale = if (isRecording) pulse * ampScale else 1f

    Box(
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(
                if (isRecording) Color(0xFFFF4D6D).copy(alpha = 0.25f + amplitude * 0.35f) else Color.Transparent,
                CircleShape
            )
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        if (isRecording) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { i ->
                    val barPhase by rememberInfiniteTransition(label = "bar$i").animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(280 + i * 90, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "bar-phase"
                    )
                    val barHeight = (6 + 16 * amplitude + 8 * barPhase * amplitude.coerceAtLeast(0.15f)).dp
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(barHeight.coerceAtMost(26.dp))
                            .background(Color(0xFFFF4D6D), RoundedCornerShape(2.dp))
                    )
                }
            }
        } else {
            Text(
                when {
                    isTranscribing -> "⏳"
                    else -> "🎙️"
                },
                fontSize = 18.sp,
                color = Color.White
            )
        }
    }
}

@Composable
fun AnimatedSendButton(enabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "send-scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.4f,
        animationSpec = tween(200),
        label = "send-alpha"
    )

    IconButton(
        onClick = { if (enabled) onClick() },
        interactionSource = interactionSource,
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF00FFCC), Color(0xFF0099FF))
                ),
                CircleShape
            )
    ) {
        Icon(Icons.Default.Send, "Send", tint = Color.Black)
    }
}

@Composable
fun DisputesScreen(
    viewModel: ShowcaseViewModel,
    onBack: () -> Unit,
    onNewDispute: () -> Unit,
    openDisputeId: String?,
    onOpenDispute: (String) -> Unit,
    onCloseDispute: () -> Unit,
    isError: Boolean,
    onDismissError: () -> Unit
) {
    val disputes by viewModel.disputes.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val clipboard = LocalClipboardManager.current
    val openDispute = disputes.firstOrNull { it.id == openDisputeId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Text("←", fontSize = 22.sp, color = Color.White)
                }
                Text(
                    "DISPUTES",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp,
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFFFB703), Color(0xFFFF007F))
                        )
                    )
                )
            }
            Button(
                onClick = onNewDispute,
                enabled = !isAnalyzing,
                colors = ButtonDefaults.buttonColors(Color(0x33FFFFFF)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (isAnalyzing) "Analyzing..." else "+ New dispute", color = Color.White, fontSize = 13.sp)
            }
        }

        if (isError) {
            Text(
                "Couldn't analyze that document. Try a clearer photo.",
                color = Color(0xFFFF8FA3),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp).clickable(onClick = onDismissError)
            )
        }

        if (disputes.isEmpty() && !isAnalyzing) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("⚖️", fontSize = 48.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Snap a bill, fine, or rejection letter\nand Stratagem fights it for you.",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "It reads the document, drafts the dispute letter,\nand tracks the case until it's resolved.",
                    color = Color(0x80FFFFFF),
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onNewDispute,
                    colors = ButtonDefaults.buttonColors(Color(0x33FFFFFF)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Start your first dispute", color = Color.White, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isAnalyzing) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x1AFFFFFF), RoundedCornerShape(20.dp))
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StrataLoader()
                        }
                    }
                }
                items(disputes, key = { it.id }) { dispute ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .background(Color(0x1AFFFFFF), RoundedCornerShape(20.dp))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                            .clickable { onOpenDispute(dispute.id) }
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(dispute.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            StatusChip(dispute.status)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(dispute.issuer, color = Color(0x80FFFFFF), fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        Row {
                            Text("Amount: ${dispute.amount}", color = Color(0xFF00FFCC), fontSize = 12.sp)
                            Spacer(Modifier.width(16.dp))
                            Text("Deadline: ${dispute.deadline}", color = Color(0xFFFFB703), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (openDispute != null) {
        ModalBottomSheet(
            onDismissRequest = onCloseDispute,
            containerColor = Color(0xFF0F0B1A),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(openDispute.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    StatusChip(openDispute.status)
                }
                Text(openDispute.summary, color = Color(0xB3FFFFFF), fontSize = 13.sp)
                HorizontalDivider(color = Color(0x1FFFFFFF))
                Text("DISPUTE LETTER", color = Color(0xFF80DEFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(openDispute.letter, color = Color.White, fontSize = 13.sp)
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(openDispute.letter)) },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Copy letter", color = Color(0xFF00FFCC), fontSize = 13.sp)
                }
                if (openDispute.followUpLetter != null) {
                    HorizontalDivider(color = Color(0x1FFFFFFF))
                    Text("FOLLOW-UP LETTER", color = Color(0xFFFF8FA3), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(openDispute.followUpLetter!!, color = Color.White, fontSize = 13.sp)
                    OutlinedButton(
                        onClick = { clipboard.setText(AnnotatedString(openDispute.followUpLetter!!)) },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Copy follow-up", color = Color(0xFF00FFCC), fontSize = 13.sp)
                    }
                }
                HorizontalDivider(color = Color(0x1FFFFFFF))
                Text("Move case forward:", color = Color(0x80FFFFFF), fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (openDispute.status == DisputeStatus.DRAFTED) {
                        Button(
                            onClick = { viewModel.setDisputeStatus(openDispute.id, DisputeStatus.SENT) },
                            colors = ButtonDefaults.buttonColors(Color(0x3300FFCC)),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Mark sent", color = Color.White, fontSize = 12.sp) }
                    }
                    if (openDispute.status == DisputeStatus.SENT) {
                        Button(
                            onClick = { viewModel.setDisputeStatus(openDispute.id, DisputeStatus.AWAITING) },
                            colors = ButtonDefaults.buttonColors(Color(0x33FFB703)),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Awaiting reply", color = Color.White, fontSize = 12.sp) }
                    }
                    if (openDispute.followUpLetter == null) {
                        OutlinedButton(
                            onClick = { viewModel.draftFollowUp(openDispute.id) { } },
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Draft follow-up", color = Color(0xFFFF8FA3), fontSize = 12.sp) }
                    }
                    if (openDispute.status != DisputeStatus.RESOLVED) {
                        OutlinedButton(
                            onClick = { viewModel.setDisputeStatus(openDispute.id, DisputeStatus.RESOLVED) },
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Resolved ✓", color = Color(0xFF00FFCC), fontSize = 12.sp) }
                    }
                }
                TextButton(onClick = { viewModel.deleteDispute(openDispute.id); onCloseDispute() }) {
                    Text("Delete case", color = Color(0xFFFF4D6D), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: DisputeStatus) {
    val (label, color) = when (status) {
        DisputeStatus.DRAFTED -> "Drafted" to Color(0xFF80DEFF)
        DisputeStatus.SENT -> "Sent" to Color(0xFFFFB703)
        DisputeStatus.AWAITING -> "Awaiting" to Color(0xFFFF4D6D)
        DisputeStatus.RESOLVED -> "Resolved" to Color(0xFF00FFCC)
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StratagemSplash() {
    val logoScale = remember { Animatable(0.4f) }
    val glowAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch {
            logoScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
            )
        }
        launch {
            glowAlpha.animateTo(1f, tween(700))
        }
    }
    val glowPulse by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow-pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE607030E)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                    alpha = glowAlpha.value
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .graphicsLayer { alpha = glowPulse }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF00FFCC).copy(alpha = 0.35f), Color.Transparent)
                        ),
                        CircleShape
                    )
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "S",
                    fontSize = 96.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp,
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF00FFCC), Color(0xFF80DEFF), Color(0xFFFF007F))
                        )
                    )
                )
                Text(
                    "STRATAGEM",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp,
                    color = Color(0x99FFFFFF)
                )
            }
        }
    }
}

@Composable
fun StrataLoader() {
    val transition = rememberInfiniteTransition(label = "strata")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "rotation"
    )
    val morph by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "morph"
    )

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(Color(0x14FFFFFF), RoundedCornerShape(28.dp))
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(28.dp))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(36.dp)) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(Color(0xFF00FFCC), Color(0xFF0077B6), Color(0xFF7B2CBF), Color(0xFF00FFCC))
                    ),
                    startAngle = rotation,
                    sweepAngle = 300f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
            Text(
                "S",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.graphicsLayer {
                    val pulse = 0.75f + 0.25f * morph
                    scaleX = pulse
                    scaleY = pulse
                    alpha = 0.35f + 0.65f * morph
                }
            )
        }
        Text(
            when ((morph * 3).toInt()) {
                0 -> "Researching the law"
                1 -> "Checking jurisdiction"
                else -> "Preparing your answer"
            },
            color = Color(0x99FFFFFF),
            fontSize = 12.sp
        )
    }
}
