package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.example.ui.features.ChatMode
import com.example.ui.features.ShowcaseViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: ShowcaseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GlassChatApp(viewModel, Modifier.padding(innerPadding))
                }
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
            animation = tween(20000, easing = LinearEasing),
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
            val cx = w / 2 + kotlin.math.sin(time * 0.05 + i * 1.5).toFloat() * (w / 1.5f)
            val cy = h / 2 + kotlin.math.cos(time * 0.04 + i * 2.0).toFloat() * (h / 1.5f)
            val radius = 300f + kotlin.math.sin(time * 0.1 + i).toFloat() * 100f

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
    
    Box(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .graphicsLayer {
                shadowElevation = 40f
                ambientShadowColor = neonColor.copy(alpha = 0.5f)
                spotShadowColor = neonColor.copy(alpha = 0.8f)
                shape = RoundedCornerShape(28.dp)
                clip = false
            }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                RoundedCornerShape(28.dp)
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.6f),
                        neonColor.copy(alpha = 0.6f),
                        Color.White.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Add a secondary internal "glow" effect for more 3D depth
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(bgColor.copy(alpha = 0.3f), RoundedCornerShape(28.dp))
        )
        content()
    }
}

@Composable
fun GlassChatApp(viewModel: ShowcaseViewModel, modifier: Modifier = Modifier) {
    val chatHistory by viewModel.chatHistory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val systemInstruction by viewModel.systemInstruction.collectAsState()
    
    var inputText by remember { mutableStateOf("") }
    var showSettingsPanel by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBase64 by remember { mutableStateOf<String?>(null) }
    var selectedImageMimeType by remember { mutableStateOf<String?>(null) }
    
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

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val recognizedText = results?.get(0) ?: ""
                inputText += if (inputText.isNotEmpty()) " $recognizedText" else recognizedText
            }
        }
    )

    LaunchedEffect(chatHistory.size, isLoading) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size + if (isLoading) 0 else -1)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedBlobsBackground()

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Main Chat Area - 3D Glass Look
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        shadowElevation = 20f
                        shape = RoundedCornerShape(32.dp)
                        clip = true
                    }
                    .background(Color(0x0DFFFFFF), RoundedCornerShape(32.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(32.dp))
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "JAGGEDGEM",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    IconButton(onClick = { showSettingsPanel = !showSettingsPanel }) {
                        Text("⚙️", fontSize = 20.sp)
                    }
                }

                // Chat Feed
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(chatHistory) { message ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start) {
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
                }

                // Settings Panel
                if (showSettingsPanel) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).background(Color(0x1A000000), RoundedCornerShape(16.dp)).padding(16.dp)) {
                        Text("SETTINGS & MEMORY", color = Color(0xFF80DEFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        BasicTextField(
                            value = systemInstruction,
                            onValueChange = { viewModel.updateSystemInstruction(it) },
                            modifier = Modifier.fillMaxWidth().height(80.dp).background(Color(0x26000000), RoundedCornerShape(8.dp)).padding(12.dp),
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            decorationBox = { inner -> if (systemInstruction.isEmpty()) Text("Inject personality...", color = Color.Gray) else inner() }
                        )
                    }
                }

                // Integrated Input Area
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .background(Color(0x1AFFFFFF), RoundedCornerShape(28.dp))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(28.dp)),
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                        decorationBox = { innerTextField ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                                IconButton(onClick = { imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.size(32.dp)) { Text("+", color = Color.White) }
                                IconButton(onClick = { speechRecognizerLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM) }) }, modifier = Modifier.size(32.dp)) { Text("🎙️") }
                                Spacer(Modifier.width(8.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (inputText.isEmpty()) Text("Message JAGGEDGEM...", color = Color.Gray, fontSize = 15.sp)
                                    innerTextField()
                                }
                                IconButton(
                                    onClick = {
                                        if ((inputText.isNotBlank() || selectedImageBase64 != null) && !isLoading) {
                                            viewModel.sendMessage(inputText, context.cacheDir, selectedImageBase64, selectedImageMimeType)
                                            inputText = ""
                                            selectedImageUri = null
                                            selectedImageBase64 = null
                                            selectedImageMimeType = null
                                        }
                                    },
                                    modifier = Modifier.size(40.dp).background(Color(0xFF00FFCC), RoundedCornerShape(20.dp))
                                ) {
                                    Icon(Icons.Default.Send, "Send", tint = Color.Black)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}


