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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.api.VoiceRecorder
import com.example.ui.features.ChatMode
import com.example.ui.features.ShowcaseViewModel
import com.example.ui.theme.MyApplicationTheme
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
    val bgColor = if (isUser) Color(0xFF111827) else Color(0xFF0F172A)
    val neonColor = if (isUser) Color(0xFF00FFCC) else Color(0xFF80DEFF)
    val corner = RoundedCornerShape(
        topStart = if (isUser) 28.dp else 6.dp,
        topEnd = if (isUser) 6.dp else 28.dp,
        bottomStart = 28.dp,
        bottomEnd = 28.dp
    )

    Box(
        modifier = Modifier
            .widthIn(max = 300.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                corner
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f),
                        neonColor.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.1f)
                    )
                ),
                shape = corner
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(bgColor.copy(alpha = 0.3f), corner)
        )
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
    val systemInstruction by viewModel.systemInstruction.collectAsState()
    val currentMode by viewModel.currentMode.collectAsState()
    val isTranscribing by viewModel.isTranscribing.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showSettingsPanel by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBase64 by remember { mutableStateOf<String?>(null) }
    var selectedImageMimeType by remember { mutableStateOf<String?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var voiceRecorder by remember { mutableStateOf<VoiceRecorder?>(null) }

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

    LaunchedEffect(chatHistory.size, isLoading) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size + if (isLoading) 0 else -1)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedBlobsBackground()

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
                    "JAGGEDGEM",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp,
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF00FFCC), Color(0xFF80DEFF), Color(0xFFFF007F))
                        )
                    )
                )
                IconButton(onClick = { showSettingsPanel = !showSettingsPanel }) {
                    Text("⚙️", fontSize = 20.sp)
                }
            }

            AnimatedVisibility(
                visible = showSettingsPanel,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(Color(0x33000000), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Text("SETTINGS & MEMORY", color = Color(0xFF80DEFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    BasicTextField(
                        value = systemInstruction,
                        onValueChange = { viewModel.updateSystemInstruction(it) },
                        modifier = Modifier.fillMaxWidth().height(80.dp).background(Color(0x26000000), RoundedCornerShape(12.dp)).padding(12.dp),
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        decorationBox = { inner -> if (systemInstruction.isEmpty()) Text("Inject personality...", color = Color.Gray) else inner() }
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (currentMode == ChatMode.LIVE) "Mode: Live Chat" else "Mode: Text to Speech",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = {
                                viewModel.setMode(if (currentMode == ChatMode.LIVE) ChatMode.TTS else ChatMode.LIVE)
                            },
                            colors = ButtonDefaults.buttonColors(Color(0x33FFFFFF))
                        ) {
                            Text(if (currentMode == ChatMode.LIVE) "Switch to Speech" else "Switch to Chat", fontSize = 12.sp, color = Color.White)
                        }
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
                        Bubble3D(message.isUser) {
                            Column {
                                if (message.imageBase64 != null) {
                                    val bitmap = remember(message.imageBase64) {
                                        val bytes = Base64.decode(message.imageBase64, Base64.DEFAULT)
                                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                    }
                                    bitmap?.let { Image(it, "Attached image", Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)).padding(bottom = 8.dp), contentScale = ContentScale.Crop) }
                                }
                                if (message.text.isNotBlank()) Text(message.text, color = Color.White, fontSize = 15.sp)
                                if (message.audioBase64 != null) {
                                    Button(onClick = { viewModel.playAudioFromBase64(message.audioBase64, context.cacheDir) }, colors = ButtonDefaults.buttonColors(Color(0x33FFFFFF))) { Text("🔊 Play") }
                                }
                            }
                        }
                    }
                }
                if (isLoading) {
                    item(key = "typing-indicator") {
                        Row(modifier = Modifier.fillMaxWidth().animateItem(), horizontalArrangement = Arrangement.Start) {
                            TypingIndicator()
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
                            IconButton(onClick = { imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.size(40.dp)) { Text("+", color = Color.White, fontSize = 22.sp) }
                            PulsingMicButton(
                                isRecording = isRecording,
                                isTranscribing = isTranscribing,
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
                                if (inputText.isEmpty()) Text("Message JAGGEDGEM...", color = Color(0x80FFFFFF), fontSize = 15.sp)
                                innerTextField()
                            }
                            AnimatedSendButton(
                                enabled = (inputText.isNotBlank() || selectedImageBase64 != null) && !isLoading,
                                onClick = {
                                    viewModel.sendMessage(inputText, context.cacheDir, selectedImageBase64, selectedImageMimeType)
                                    inputText = ""
                                    selectedImageUri = null
                                    selectedImageBase64 = null
                                    selectedImageMimeType = null
                                }
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PulsingMicButton(isRecording: Boolean, isTranscribing: Boolean, onToggle: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "mic-pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val scale = if (isRecording) pulse else 1f

    Box(
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(
                if (isRecording) Color(0xFFFF4D6D).copy(alpha = 0.25f) else Color.Transparent,
                CircleShape
            )
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        Text(
            when {
                isRecording -> "⏹️"
                isTranscribing -> "⏳"
                else -> "🎙️"
            },
            fontSize = 18.sp,
            color = if (isRecording) Color(0xFFFF4D6D) else Color.White
        )
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
