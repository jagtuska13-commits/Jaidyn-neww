@file:OptIn(ExperimentalMaterial3Api::class)

package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.content.ContentValues
import android.provider.MediaStore
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.api.EmailInbox
import com.example.api.GmailRest
import com.example.api.VoiceRecorder
import com.example.ui.features.DisputeCase
import com.example.ui.features.DisputeStatus
import com.example.ui.features.ShowcaseViewModel
import com.example.ui.features.DocketEvent
import com.example.ui.features.VaultFile
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    private val viewModel: ShowcaseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleOAuthCallback(intent)
        setContent {
            MyApplicationTheme {
                GlassChatApp(viewModel, Modifier.fillMaxSize())
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthCallback(intent)
    }

    private fun handleOAuthCallback(intent: Intent?) {
        val data = intent?.data ?: return
        val code = data.getQueryParameter("code") ?: return
        viewModel.completeGoogleSignIn(code)
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
                    colors = listOf(color.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = radius
                ),
                radius = radius,
                center = Offset(cx, cy)
            )
        }
    }
}

fun exportReplyToPdf(context: Context, title: String, text: String) {
    val cleanTitle = title.replace(Regex("[^\\w\\s-]"), "").trim().take(60).ifBlank { "Stratagem reply" }
    Thread {
        try {
            val doc = android.graphics.pdf.PdfDocument()
            val pageW = 595
            val pageH = 842
            val margin = 46f
            val textPaint = android.text.TextPaint().apply {
                color = android.graphics.Color.BLACK
                textSize = 11f
                isAntiAlias = true
            }
            var pageNo = 1
            var page = doc.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create())
            var canvas = page.canvas
            val titlePaint = android.text.TextPaint(textPaint).apply { textSize = 14f; isFakeBoldText = true }
            canvas.drawText(cleanTitle, margin, margin + 14f, titlePaint)
            var y = margin + 44f
            val paraWidth = (pageW - 2 * margin).toInt()
            val paras = text.split("\n")
            for (para in paras) {
                val line = para.ifBlank { " " }
                val layout = android.text.StaticLayout.Builder.obtain(line, 0, line.length, textPaint, paraWidth)
                    .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(3f, 1f)
                    .build()
                if (y + layout.height > pageH - margin) {
                    doc.finishPage(page)
                    pageNo++
                    page = doc.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create())
                    canvas = page.canvas
                    y = margin + 20f
                }
                canvas.save()
                canvas.translate(margin, y)
                layout.draw(canvas)
                canvas.restore()
                y += layout.height + 10f
            }
            doc.finishPage(page)
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "$cleanTitle.pdf")
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) }
            }
            doc.close()
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                if (uri != null) {
                    android.widget.Toast.makeText(context, "PDF saved to Downloads", android.widget.Toast.LENGTH_LONG).show()
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(send, "Share reply PDF"))
                } else {
                    android.widget.Toast.makeText(context, "Couldn't save the PDF", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(context, "PDF export failed", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }.start()
}

fun queryDisplayName(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    } catch (e: Exception) {
        null
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

data class SmsThread(
    val address: String,
    val snippet: String,
    val count: Int,
    val latestDate: Long,
    val text: String
)

fun loadSmsThreads(context: Context, maxThreads: Int = 500, perThread: Int = 15): List<SmsThread> {
    data class RawSms(val date: Long, val body: String, val sent: Boolean)
    val map = LinkedHashMap<String, MutableList<RawSms>>()
    try {
        val cursor = context.contentResolver.query(
            android.net.Uri.parse("content://sms"),
            arrayOf("address", "body", "date", "type"),
            null,
            null,
            "date DESC"
        ) ?: return emptyList()
        cursor.use { c ->
            while (c.moveToNext()) {
                val addr = c.getString(0) ?: continue
                val body = c.getString(1) ?: ""
                val date = c.getLong(2)
                val type = c.getInt(3)
                map.getOrPut(addr) { mutableListOf() }.add(RawSms(date, body, type == 2 || type == 4 || type == 6))
                if (map.size > maxThreads) break
            }
        }
    } catch (e: Exception) {
        return emptyList()
    }
    val fmt = java.text.SimpleDateFormat("d MMM yyyy HH:mm", java.util.Locale.getDefault())
    return map.entries.map { (addr, msgs) ->
        val recent = msgs.take(perThread).sortedBy { it.date }
        SmsThread(
            address = addr,
            snippet = recent.lastOrNull()?.body?.take(60) ?: "",
            count = msgs.size,
            latestDate = msgs.firstOrNull()?.date ?: 0L,
            text = "SMS thread with $addr (${recent.size} recent message(s), oldest first, most recent last):\n\n" +
                recent.joinToString("\n") { sms ->
                    val who = if (sms.sent) "You" else "Them"
                    "[${fmt.format(java.util.Date(sms.date))}] $who: ${sms.body}"
                }
        )
    }.sortedByDescending { it.latestDate }.take(maxThreads)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsPickerSheet(onDismiss: () -> Unit, onPick: (SmsThread) -> Unit) {
    var threads by remember { mutableStateOf<List<SmsThread>?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        threads = loadSmsThreads(context)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F0B1A),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "SMS THREADS",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF00FFCC), Color(0xFF80DEFF), Color(0xFFFF007F))
                    )
                )
            )
            val list = threads
            if (list == null) {
                Text("Scanning your whole inbox...", color = Color(0xB3FFFFFF), fontSize = 14.sp)
            } else if (list.isEmpty()) {
                Text("No messages found. Make sure SMS permission is granted and there are SMS conversations on this phone.", color = Color(0xB3FFFFFF), fontSize = 14.sp)
            } else {
                val query = searchQuery.trim().lowercase()
                val filtered = if (query.isBlank()) list else list.filter {
                    it.address.lowercase().contains(query) || it.snippet.lowercase().contains(query)
                }
                if (query.isBlank()) {
                    Text("All ${list.size} conversations on this phone", color = Color(0x60FFFFFF), fontSize = 11.sp)
                } else if (filtered.isEmpty()) {
                    Text("No conversations match \"$searchQuery\"", color = Color(0xB3FFFFFF), fontSize = 13.sp)
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    decorationBox = { inner ->
                        Box {
                            if (searchQuery.isEmpty()) Text("Search any number, name or word...", color = Color(0x60FFFFFF), fontSize = 14.sp)
                            inner()
                        }
                    }
                )
                filtered.take(80).forEach { t ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPick(t) }
                            .background(Color(0x14FFFFFF))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(t.address, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("${t.count} msg", color = Color(0x80FFFFFF), fontSize = 11.sp)
                        }
                        Text(t.snippet, color = Color(0xB3FFFFFF), fontSize = 12.sp, maxLines = 2)
                    }
                }
            }
        }
    }
}

@Composable
fun AttachOptionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(30.dp))
            .background(Color(0x33141414))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    Brush.linearGradient(listOf(Color(0xFF00FFCC), Color(0xFF0099FF))),
                    CircleShape
                )
                .clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.Black, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineSheet(
    events: List<DocketEvent>,
    currentMatterId: String,
    onDelete: (String) -> Unit,
    onAttach: (DocketEvent) -> Unit,
    onDismiss: () -> Unit
) {
    val now = System.currentTimeMillis()
    val thisMatterUpcoming = events.filter { it.convId == currentMatterId && it.ts >= now }.sortedBy { it.ts }
    val thisMatterPast = events.filter { it.convId == currentMatterId && it.ts < now }.sortedByDescending { it.ts }
    val other = events.filter { it.convId != currentMatterId }.sortedBy { it.ts }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F0B1A),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "TIMELINE / DOCKET",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp,
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF00FFCC), Color(0xFF80DEFF), Color(0xFFFF007F))
                    )
                )
            )
            Text("Dates lifted automatically from replies and documents. Ask about it and tap a card to drop it into the chat.", color = Color(0x80FFFFFF), fontSize = 12.sp)
            if (thisMatterUpcoming.isEmpty() && thisMatterPast.isEmpty() && other.isEmpty()) {
                Text("No dates captured yet. Send documents or ask about your matter and key dates will be filed here automatically.", color = Color(0xB3FFFFFF), fontSize = 13.sp)
            }
            if (thisMatterUpcoming.isNotEmpty()) {
                Text("UPCOMING", color = Color(0xFF00FFCC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                DocketList(thisMatterUpcoming, onDelete, onAttach)
            }
            if (thisMatterPast.isNotEmpty()) {
                Text("PAST", color = Color(0xFF80DEFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                DocketList(thisMatterPast.take(10), onDelete, onAttach)
            }
            if (other.isNotEmpty()) {
                Text("OTHER MATTERS", color = Color(0xFF80DEFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                DocketList(other.take(10), onDelete, onAttach)
            }
        }
    }
}

@Composable
fun countdownLabel(ts: Long): String {
    val days = ((ts - System.currentTimeMillis()) / 86400000L)
    return when {
        ts < System.currentTimeMillis() -> "passed"
        days == 0L -> "today"
        days == 1L -> "tomorrow"
        days <= 7L -> "in $days days"
        days <= 60L -> "in ${(days / 7)} wks"
        else -> "in ${(days / 30)} mths"
    }
}

@Composable
fun DocketList(events: List<DocketEvent>, onDelete: (String) -> Unit, onAttach: (DocketEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        events.forEach { e ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x14FFFFFF))
                    .clickable { onAttach(e) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            java.text.SimpleDateFormat("EEE d MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(e.ts)),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            countdownLabel(e.ts),
                            color = if (e.isDeadline) Color(0xFFFF4D6D) else Color(0xFF80DEFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(e.description, color = Color(0xB3FFFFFF), fontSize = 11.sp, maxLines = 3)
                }
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = Color(0x60FFFFFF),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onDelete(e.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSheet(
    files: List<VaultFile>,
    currentMatterId: String,
    onDelete: (String) -> Unit,
    onAttach: (VaultFile) -> Unit,
    onDismiss: () -> Unit
) {
    val thisMatter = files.filter { it.convId == currentMatterId }.sortedByDescending { it.savedAt }
    val other = files.filter { it.convId != currentMatterId }.sortedByDescending { it.savedAt }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F0B1A),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "CASE VAULT",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp,
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF00FFCC), Color(0xFF80DEFF), Color(0xFFFF007F))
                    )
                )
            )
            Text("Every document you upload is filed here. Attach any file to the chat instantly.", color = Color(0x80FFFFFF), fontSize = 12.sp)
            if (thisMatter.isEmpty() && other.isEmpty()) {
                Text("No documents on file yet. Upload one with the + menu and it will be filed automatically.", color = Color(0xB3FFFFFF), fontSize = 13.sp)
            }
            if (thisMatter.isNotEmpty()) {
                Text("THIS MATTER", color = Color(0xFF80DEFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                VaultFileList(thisMatter, onDelete, onAttach)
            }
            if (other.isNotEmpty()) {
                Text("OTHER MATTERS", color = Color(0xFF80DEFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                VaultFileList(other, onDelete, onAttach)
            }
        }
    }
}

@Composable
fun VaultFileList(files: List<VaultFile>, onDelete: (String) -> Unit, onAttach: (VaultFile) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        files.forEach { f ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x14FFFFFF))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF80DEFF), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(f.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    Text("${formatEmailDate(f.savedAt)}  \u00b7  ${f.text.length} chars", color = Color(0x60FFFFFF), fontSize = 10.sp)
                }
                Text(
                    "ATTACH",
                    color = Color(0xFF00FFCC),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x1400FFCC), RoundedCornerShape(10.dp))
                        .clickable { onAttach(f) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = Color(0x60FFFFFF),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onDelete(f.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailPickerSheet(
    connectedLabel: String?,
    load: suspend () -> List<EmailInbox.EmailThread>,
    onDismiss: () -> Unit,
    onPick: (EmailInbox.EmailThread) -> Unit
) {
    var threads by remember { mutableStateOf<List<EmailInbox.EmailThread>?>(null) }
    LaunchedEffect(Unit) {
        threads = try {
            withContext(Dispatchers.IO) { load() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
            Text(
                "RECENT EMAILS",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF00FFCC), Color(0xFF80DEFF), Color(0xFFFF007F))
                    )
                )
            )
            if (connectedLabel == null) {
                Text(
                    "No email connected yet. Open Settings (gear icon, top right) and either fill in your Google Client ID and press SIGN IN WITH GOOGLE, or scroll to OTHER PROVIDERS and add your email with its password.",
                    color = Color(0xB3FFFFFF),
                    fontSize = 13.sp
                )
            } else {
                Text(connectedLabel, color = Color(0xFF00FFCC), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                val list = threads
                if (list == null) {
                    Text("Loading inbox...", color = Color(0xB3FFFFFF), fontSize = 14.sp)
                } else if (list.isEmpty()) {
                    Text("Couldn't load the inbox. Double-check your sign-in or provider details in Settings.", color = Color(0xB3FFFFFF), fontSize = 13.sp)
                } else {
                    list.take(25).forEach { t ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onPick(t) }
                                .background(Color(0x14FFFFFF))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(t.from, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(formatEmailDate(t.sentDate), color = Color(0x80FFFFFF), fontSize = 10.sp)
                            }
                            Text(t.subject, color = Color(0xFF80DEFF), fontSize = 12.sp, maxLines = 1)
                            Text(t.snippet, color = Color(0xB3FFFFFF), fontSize = 11.sp, maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}

fun formatEmailDate(ts: Long): String {
    if (ts == 0L) return ""
    return java.text.SimpleDateFormat("d MMM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ts))
}

@Composable
fun MarkdownLiteText(raw: String, color: Color = Color.White, fontSize: TextUnit = 15.sp, lineHeight: TextUnit = 22.sp) {
    val annotated = remember(raw) { buildMarkdownLite(raw) }
    Text(annotated, color = color, fontSize = fontSize, lineHeight = lineHeight)
}

fun buildMarkdownLite(raw: String): AnnotatedString = buildAnnotatedString {
    raw.lines().forEachIndexed { idx, line ->
        if (idx > 0) append("\n")
        var l = line.trimEnd()
        if (l.isBlank()) return@forEachIndexed
        var isBullet = false
        val bulletMatch = Regex("^\\s*[-\u2022*+]\\s+").find(l)
        if (bulletMatch != null) {
            isBullet = true
            l = l.removeRange(bulletMatch.range)
        }
        val headerMatch = Regex("^#{1,6}\\s+").find(l)
        if (headerMatch != null) {
            l = l.removeRange(headerMatch.range).removePrefix("**").removeSuffix("**")
        }
        if (isBullet) append("\u2022  ")
        var pos = 0
        val statuteRegex = Regex("(?i)\\b(?:s|sec|section|cl|clause|sch|schedule|reg|rule|art|para)\\.? ?\\d+[A-Z]?(?:\\([0-9A-Za-z]+\\))*\\b|\\b[A-Z][a-z]+(?:\\s+[A-Z][a-z]+){0,4}\\s+(?:Act|Code|Regulation|Rules)s?\\b")
        fun appendSegment(seg: String) {
            var sp = 0
            for (mm in statuteRegex.findAll(seg)) {
                append(seg.substring(sp, mm.range.first))
                withStyle(SpanStyle(color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold)) {
                    append(seg.substring(mm.range))
                }
                sp = mm.range.last + 1
            }
            append(seg.substring(sp))
        }
        for (m in Regex("\\*\\*(.+?)\\*\\*|__(.+?)__").findAll(l)) {
            appendSegment(l.substring(pos, m.range.first))
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = if (statuteRegex.containsMatchIn(l.substring(m.range))) Color(0xFF00FFCC) else androidx.compose.ui.graphics.Color.Unspecified
                )
            ) {
                append(m.groupValues.drop(1).firstOrNull { it.isNotBlank() } ?: m.value)
            }
            pos = m.range.last + 1
        }
        appendSegment(l.substring(pos))
    }
}

fun stripMarkdown(raw: String): String {
    var t = raw
    t = t.replace("!", "!")
    t = t.replace("```", " ")
    t = Regex("^#{1,6} ", RegexOption.MULTILINE).replace(t, "")
    t = Regex("[*]{2}(.+?)[*]{2}").replace(t, "$1")
    t = Regex("__(.+?)__").replace(t, "$1")
    t = Regex("`([^`]+)`").replace(t, "$1")
    t = Regex("^> ?", RegexOption.MULTILINE).replace(t, "")
    t = Regex("^\\s*[-*+] ", RegexOption.MULTILINE).replace(t, "")
    t = Regex("^\\s*1\\. ", RegexOption.MULTILINE).replace(t, "")
    t = Regex("[*](.+?)[*]").replace(t, "$1")
    t = Regex("\\n{3,}").replace(t, "\\n\\n")
    return t.trim()
}

@Composable
fun GlassChatApp(viewModel: ShowcaseViewModel, modifier: Modifier = Modifier) {
    val chatHistory by viewModel.chatHistory.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val vaultFiles by viewModel.vault.collectAsState()
    val docketEvents by viewModel.docket.collectAsState()
    var openConversationId by remember { mutableStateOf<String?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    val isLoading by viewModel.isLoading.collectAsState()
    val isTranscribing by viewModel.isTranscribing.collectAsState()
    val isReadingDocState by viewModel.isReadingDoc.collectAsState()
    val docsReadingCount by viewModel.docsReading.collectAsState()
    val personaName by viewModel.personaName.collectAsState()
    val personaText by viewModel.personaText.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userAbout by viewModel.userAbout.collectAsState()
    val jurisdiction by viewModel.jurisdiction.collectAsState()
    val legalDetail by viewModel.legalDetail.collectAsState()
    val aiEffort by viewModel.aiEffort.collectAsState()
    val citeSources by viewModel.citeSources.collectAsState()
    val reportedAnswers by viewModel.reported.collectAsState()
    val speakReplies by viewModel.speakReplies.collectAsState()
    val useClonedVoice by viewModel.useClonedVoice.collectAsState()
    val useLiveSearch by viewModel.useLiveSearch.collectAsState()
    val emailAddress by viewModel.emailAddress.collectAsState()
    val emailPassword by viewModel.emailPassword.collectAsState()
    val emailHost by viewModel.emailHost.collectAsState()
    val googleClientId by viewModel.googleClientId.collectAsState()
    val googleClientSecret by viewModel.googleClientSecret.collectAsState()
    val gmailAccountEmail by viewModel.gmailAccountEmail.collectAsState()
    val clonedVoiceBase64 by viewModel.clonedVoiceBase64.collectAsState()
    val isSavingVoice by viewModel.isSavingVoice.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showSettingsPanel by remember { mutableStateOf(false) }
    val pendingImages = remember { mutableStateListOf<Pair<String, String>>() }
    var isRecording by remember { mutableStateOf(false) }
    var voiceRecorder by remember { mutableStateOf<VoiceRecorder?>(null) }
    var isCloningRecording by remember { mutableStateOf(false) }
    var cloneRecorder by remember { mutableStateOf<VoiceRecorder?>(null) }
    var micAmplitude by remember { mutableStateOf(0f) }
    var playingAudioId by remember { mutableStateOf<String?>(null) }
    val pendingDocs = remember { mutableStateListOf<Pair<String, String>>() }
    var docError by remember { mutableStateOf(false) }
    var showSmsPicker by remember { mutableStateOf(false) }
    var showEmailPicker by remember { mutableStateOf(false) }
    var showVault by remember { mutableStateOf(false) }
    var showSaver by remember { mutableStateOf(false) }
    var lastInteraction by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDocket by remember { mutableStateOf(false) }
    var showAttachMenu by remember { mutableStateOf(false) }
    var attachMenuShown by remember { mutableStateOf(0) }
    LaunchedEffect(showAttachMenu) {
        if (showAttachMenu) {
            for (i in 1..4) {
                attachMenuShown = i
                delay(50L)
            }
        } else {
            attachMenuShown = 0
        }
    }
    var showDisputes by remember { mutableStateOf(false) }
    var openDisputeId by remember { mutableStateOf<String?>(null) }
    var disputeError by remember { mutableStateOf(false) }
    var splashGone by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                    uris.forEach { uri ->
                        try {
                            val resized = getResizedBase64(context, uri)
                            if (resized != null) {
                                kotlinx.coroutines.withContext(Dispatchers.Main) {
                                    pendingImages.add(resized.first to resized.second)
                                }
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
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

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showSmsPicker = true
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
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            docError = false
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                uris.forEach { uri ->
                    try {
                        val name = queryDisplayName(context, uri) ?: "document.pdf"
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        if (bytes != null && bytes.size <= 8 * 1024 * 1024) {
                            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                            viewModel.processDocument(b64, "application/pdf", name) { text ->
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    if (text != null) {
                                        pendingDocs.add(name to text)
                                        viewModel.saveToVault(name, text)
                                    } else {
                                        docError = true
                                    }
                                }
                            }
                        } else {
                            android.os.Handler(android.os.Looper.getMainLooper()).post { docError = true }
                        }
                    } catch (e: Exception) {
                        android.os.Handler(android.os.Looper.getMainLooper()).post { docError = true }
                    }
                }
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

    val streamingTextLength = chatHistory.lastOrNull()?.let { if (it.isUser) -1 else it.text.length } ?: -1
    LaunchedEffect(streamingTextLength) {
        if (streamingTextLength > 0) {
            listState.animateScrollToItem(chatHistory.lastIndex)
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000L)
            if (!showSaver && System.currentTimeMillis() - lastInteraction > 45000L) {
                showSaver = true
            }
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xF20F0B1A),
                windowInsets = WindowInsets(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 48.dp, bottom = 32.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "HISTORY",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 3.sp,
                        style = TextStyle(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF00FFCC), Color(0xFF80DEFF), Color(0xFFFF007F))
                            )
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF00FFCC), Color(0xFF0099FF))), RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.newConversation()
                                drawerScope.launch { drawerState.close() }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("NEW CHAT", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    if (conversations.isEmpty()) {
                        Text("No conversations yet.", color = Color(0x80FFFFFF), fontSize = 13.sp)
                    }
                    conversations.forEach { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (c.id == openConversationId) Color(0x26FFFFFF) else Color.Transparent)
                                .clickable {
                                    openConversationId = c.id
                                    viewModel.openConversation(c.id)
                                    drawerScope.launch { drawerState.close() }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(c.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                Text(formatEmailDate(c.updatedAt), color = Color(0x60FFFFFF), fontSize = 10.sp)
                            }
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Delete",
                                tint = Color(0x60FFFFFF),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { viewModel.deleteConversation(c.id) }
                            )
                        }
                    }
                }
            }
        }
    ) {
    Box(modifier = modifier.fillMaxSize().pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent()
                lastInteraction = System.currentTimeMillis()
            }
        }
    }) {
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
        val imePaddingDp = WindowInsets.ime.getBottom(LocalDensity.current).let { px ->
            with(LocalDensity.current) { px.toDp() }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 150.dp,
                    bottom = 168.dp + imePaddingDp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(chatHistory, key = { message -> message.id }) { message ->
                    if (message.isUser) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.End
                        ) {
                            if (message.imageBase64 != null) {
                                val bitmap = remember(message.imageBase64) {
                                    val bytes = Base64.decode(message.imageBase64, Base64.DEFAULT)
                                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                }
                                bitmap?.let { Image(it, "Attached image", Modifier.fillMaxWidth(0.55f).height(150.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop) }
                            }
                            if (message.documentName != null) {
                                Row(
                                    modifier = Modifier
                                        .background(Color(0x26FFFFFF), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF80DEFF), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(message.documentName!!, color = Color(0xFF80DEFF), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                            if (message.text.isNotBlank()) {
                                Text(
                                    message.text,
                                    color = Color(0xFF9BE8FF),
                                    fontSize = 15.sp,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (message.text.isNotBlank()) {
                                MarkdownLiteText(raw = message.text)
                            } else {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                    StrataLoader()
                                }
                            }
                            if (message.text.isNotBlank()) {
                                val clipboard = LocalClipboardManager.current
                                val isReported = reportedAnswers.any { it.first == message.id }
                                Row(
                                    modifier = Modifier.padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(22.dp)
                                ) {
                                    if (playingAudioId == message.id) {
                                        Icon(
                                            Icons.Default.Stop,
                                            contentDescription = "Stop audio",
                                            tint = Color(0xFFFF4D6D),
                                            modifier = Modifier.size(20.dp).clickable {
                                                viewModel.stopSpeaking()
                                                playingAudioId = null
                                            }
                                        )
                                        AudioBars()
                                    } else {
                                        Icon(
                                            Icons.Default.VolumeUp,
                                            contentDescription = "Read aloud",
                                            tint = Color(0xB3FFFFFF),
                                            modifier = Modifier.size(18.dp).clickable {
                                                playingAudioId = message.id
                                                viewModel.speakMessage(message.text, message.audioBase64, context.cacheDir)
                                            }
                                        )
                                    }
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = Color(0xB3FFFFFF),
                                        modifier = Modifier.size(16.dp).clickable {
                                            clipboard.setText(AnnotatedString(message.text))
                                        }
                                    )
                                    Icon(
                                        Icons.Default.PictureAsPdf,
                                        contentDescription = "Export PDF",
                                        tint = Color(0xB3FFFFFF),
                                        modifier = Modifier.size(16.dp).clickable {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            exportReplyToPdf(context, chatHistory.getOrNull(0)?.text?.take(40) ?: "Stratagem reply", message.text)
                                        }
                                    )
                                    Icon(
                                        if (isReported) Icons.Default.Flag else Icons.Outlined.Flag,
                                        contentDescription = "Report answer",
                                        tint = if (isReported) Color(0xFFFF4D6D) else Color(0x80FFFFFF),
                                        modifier = Modifier.size(16.dp).clickable {
                                            viewModel.reportAnswer(message.id, message.text)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(158.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFF07030E), Color(0xFF07030E), Color(0xB307030E), Color(0x0007030E))))
            ) {}
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(86.dp)
                    .background(Brush.verticalGradient(listOf(Color(0x0007030E), Color(0xFF07030E))))
            ) {}

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "STRATAGEM",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp,
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF00FFCC), Color(0xFF80DEFF), Color(0xFFFF007F))
                        )
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (playingAudioId != null) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0x2EFFFFFF), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                                .clickable {
                                    viewModel.stopSpeaking()
                                    playingAudioId = null
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = Color(0xFFFF4D6D), modifier = Modifier.size(14.dp))
                            Text("Playing", color = Color(0xB3FFFFFF), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    IconButton(onClick = { showSettingsPanel = !showSettingsPanel }, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                AnimatedVisibility(
                    visible = attachMenuShown > 0 || showAttachMenu,
                    enter = fadeIn(tween(120)),
                    exit = fadeOut(tween(150))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xE60F0B1A), RoundedCornerShape(24.dp))
                            .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(24.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnimatedVisibility(visible = attachMenuShown >= 4, enter = scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 380f)) + fadeIn(), exit = shrinkOut() + fadeOut()) {
                            AttachOptionButton(
                                icon = Icons.Default.Email,
                                label = "Email",
                                onClick = { showAttachMenu = false; showEmailPicker = true }
                            )
                        }
                        AnimatedVisibility(visible = attachMenuShown >= 3, enter = scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 380f)) + fadeIn(), exit = shrinkOut() + fadeOut()) {
                            AttachOptionButton(
                                icon = Icons.Default.Sms,
                                label = "SMS thread",
                                onClick = {
                                    showAttachMenu = false
                                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                                    if (granted) {
                                        showSmsPicker = true
                                    } else {
                                        smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                                    }
                                }
                            )
                        }
                        AnimatedVisibility(visible = attachMenuShown >= 2, enter = scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 380f)) + fadeIn(), exit = shrinkOut() + fadeOut()) {
                            AttachOptionButton(
                                icon = Icons.Default.Description,
                                label = "Document",
                                onClick = { showAttachMenu = false; docPickerLauncher.launch(arrayOf("application/pdf")) }
                            )
                        }
                        AnimatedVisibility(visible = attachMenuShown >= 1, enter = scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 380f)) + fadeIn(), exit = shrinkOut() + fadeOut()) {
                            AttachOptionButton(
                                icon = Icons.Default.Image,
                                label = "Image",
                                onClick = { showAttachMenu = false; imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                            )
                        }
                    }
                }
                val inputInteraction = remember { MutableInteractionSource() }
                val inputFocused by inputInteraction.collectIsFocusedAsState()
                val borderC1 by animateColorAsState(
                    if (inputFocused) Color(0xCCFF007F) else Color.White.copy(alpha = 0.5f), tween(350), label = "focus-b1"
                )
                val borderC2 by animateColorAsState(
                    if (inputFocused) Color(0xCC00E5C7) else Color(0xFF00E5C7).copy(alpha = 0.25f), tween(350), label = "focus-b2"
                )
                val borderC3 by animateColorAsState(
                    if (inputFocused) Color(0xCC80DEFF) else Color.White.copy(alpha = 0.06f), tween(350), label = "focus-b3"
                )
                val effortLabel = when (aiEffort) { "high" -> "MAXIMUM"; "medium" -> "BALANCED"; else -> "FAST" }
                val detailLabel = when (legalDetail) { "detailed" -> "DETAILED"; "simple" -> "PLAIN"; else -> "STANDARD" }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .shadow(if (inputFocused) 18.dp else 10.dp, RoundedCornerShape(30.dp))
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color(0x0F0A0E1A), RoundedCornerShape(30.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.10f),
                                    Color.White.copy(alpha = 0.03f)
                                )
                            ),
                            RoundedCornerShape(30.dp)
                        )
                        .border(
                            1.dp,
                            Brush.linearGradient(colors = listOf(borderC1, borderC2, borderC3)),
                            RoundedCornerShape(30.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(start = 14.dp, top = 10.dp, end = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ContextChip(Icons.Default.Bolt, "AI POWER: $effortLabel") { showSettingsPanel = true }
                        ContextChip(Icons.Default.Person, "AI: ${if (personaName.isBlank()) "STRATAGEM" else personaName}") { showSettingsPanel = true }
                        ContextChip(Icons.Default.Gavel, "DETAIL: $detailLabel") { showSettingsPanel = true }
                        if (jurisdiction.isNotBlank()) {
                            ContextChip(Icons.Default.LocationOn, jurisdiction) { showSettingsPanel = true }
                        }
                    }

                    if (pendingImages.isNotEmpty() || pendingDocs.isNotEmpty() || docError || docsReadingCount > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (docError) {
                                Row(
                                    modifier = Modifier
                                        .background(Color(0x26FF4D6D), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Couldn't read that document. Try a smaller PDF.", color = Color(0xFFFF8FA3), fontSize = 12.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0x80FFFFFF), modifier = Modifier.size(14.dp).clickable { docError = false })
                                }
                            }
                            pendingImages.forEachIndexed { i, (img, _) ->
                                Box(modifier = Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x26FFFFFF))) {
                                    val bitmap = remember(img) {
                                        val bytes = Base64.decode(img, Base64.DEFAULT)
                                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                    }
                                    bitmap?.let { Image(it, "Attached image", Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove image",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(3.dp)
                                            .size(16.dp)
                                            .background(Color(0x99000000), CircleShape)
                                            .clip(CircleShape)
                                            .clickable { pendingImages.removeAt(i) }
                                    )
                                }
                            }
                            pendingDocs.forEachIndexed { i, (docName, _) ->
                                Row(
                                    modifier = Modifier
                                        .background(Color(0x26FFFFFF), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF80DEFF), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(docName, color = Color(0xFF80DEFF), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.Default.Close, contentDescription = "Remove document", tint = Color(0x80FFFFFF), modifier = Modifier.size(14.dp).clickable { pendingDocs.removeAt(i) })
                                }
                            }
                            if (docsReadingCount > 0) {
                                Row(
                                    modifier = Modifier.background(Color(0x26FFFFFF), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF80DEFF), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (docsReadingCount > 1) "Reading $docsReadingCount documents..." else "Reading document...", color = Color(0xFF80DEFF), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp),
                            interactionSource = inputInteraction,
                            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                            decorationBox = { innerTextField ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                                    val plusRotation by animateFloatAsState(
                                        targetValue = if (showAttachMenu) 45f else 0f,
                                        animationSpec = tween(250, easing = FastOutSlowInEasing),
                                        label = "plus-rotation"
                                    )
                                    Box(modifier = Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Add attachment",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .graphicsLayer { rotationZ = plusRotation }
                                                .clickable {
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    showAttachMenu = !showAttachMenu
                                                }
                                        )
                                    }
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
                                        enabled = inputText.isNotBlank() || pendingImages.isNotEmpty() || pendingDocs.isNotEmpty(),
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            splashGone = true
                                            pendingImages.forEach { (img, mime) ->
                                                viewModel.sendMessage("", context.cacheDir, img, mime)
                                            }
                                            pendingDocs.forEach { (docName, docText) ->
                                                viewModel.sendMessage("", context.cacheDir, null, null, docName, docText)
                                            }
                                            if (inputText.isNotBlank()) {
                                                viewModel.sendMessage(inputText, context.cacheDir)
                                            }
                                            inputText = ""
                                            pendingImages.clear()
                                            pendingDocs.clear()
                                            docError = false
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
        }
        }

        if (showSmsPicker) {
            SmsPickerSheet(
                onDismiss = { showSmsPicker = false },
                onPick = { thread ->
                    pendingDocs.add("SMS thread — ${thread.address}" to thread.text)
                    showSmsPicker = false
                }
            )
        }

        if (showDocket) {
            TimelineSheet(
                events = docketEvents,
                currentMatterId = viewModel.currentMatterId(),
                onDelete = { viewModel.deleteDocketEvent(it) },
                onAttach = { e ->
                    inputText = (inputText + " " + e.description).trim()
                    showDocket = false
                },
                onDismiss = { showDocket = false }
            )
        }

        if (showVault) {
            VaultSheet(
                files = vaultFiles,
                currentMatterId = viewModel.currentMatterId(),
                onDelete = { viewModel.deleteVaultFile(it) },
                onAttach = { f ->
                    pendingDocs.add(f.name to f.text)
                    showVault = false
                },
                onDismiss = { showVault = false }
            )
        }

        if (showEmailPicker) {
            val gmailConnected = gmailAccountEmail.isNotBlank()
            val imapConfigured = emailAddress.isNotBlank() && emailPassword.isNotBlank()
            EmailPickerSheet(
                connectedLabel = when {
                    gmailConnected -> "Gmail — $gmailAccountEmail"
                    imapConfigured -> "IMAP — $emailAddress"
                    else -> null
                },
                load = {
                    if (gmailConnected) {
                        viewModel.loadGmailInbox()
                    } else {
                        EmailInbox.load(emailAddress, emailPassword, emailHost)
                    }
                },
                onDismiss = { showEmailPicker = false },
                onPick = { email ->
                    pendingDocs.add("Email — ${email.subject}" to email.text)
                    showEmailPicker = false
                }
            )
        }

        if (showSaver) {
            StratagemScreensaver {
                showSaver = false
                lastInteraction = System.currentTimeMillis()
            }
        }

        if (showSettingsPanel) {
            val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showSettingsPanel = false },
                sheetState = settingsSheetState,
                containerColor = Color(0xFF0F0B1A),
                dragHandle = {
                    Box(
                        Modifier
                            .padding(top = 10.dp, bottom = 4.dp)
                            .width(44.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0x33FFFFFF))
                    )
                },
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "SETTINGS",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 3.sp,
                        style = TextStyle(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF00FFCC), Color(0xFF80DEFF), Color(0xFFFF007F))
                            )
                        )
                    )
                    SettingsHint("Everything saves instantly - there is no save button. Pick a section below, or open START HERE if you are unsure. Swipe down or tap outside to close.")

                    SettingsHelpCard()

                    SettingsSection(0) {
                        SettingsSectionTitle("TOOLS")
                        SettingsToolRow(Icons.Default.History, "HISTORY & CONVERSATIONS") { showSettingsPanel = false; drawerScope.launch { drawerState.open() } }
                        SettingsToolRow(Icons.Default.EventNote, "TIMELINE / DOCKET") { showSettingsPanel = false; showDocket = true }
                        SettingsToolRow(Icons.Default.Archive, "CASE VAULT") { showSettingsPanel = false; showVault = true }
                        SettingsToolRow(Icons.Default.Balance, "DISPUTE LETTERS") { showSettingsPanel = false; showDisputes = true }
                        SettingsHint("Jump straight to your past chats, your matter timeline, saved case files, or drafted letters.")
                    }

                    SettingsSection(1) {
                        SettingsSectionTitle("PERSONA & PROFILE")
                        SettingsCard {
                            DrawerTextField(
                                label = "AI name",
                                value = personaName,
                                onValueChange = { viewModel.updatePersonaName(it) },
                                placeholder = "e.g. Gem",
                                hint = "What the assistant calls itself. Try 'Gem' or your firm's name."
                            )
                            DrawerTextField(
                                label = "CUSTOM INSTRUCTIONS (how you want it to act)",
                                value = personaText,
                                onValueChange = { viewModel.updatePersonaText(it) },
                                placeholder = "Inject personality: witty, caring, sarcastic...",
                                minHeight = 72.dp,
                                hint = "Examples: 'be witty but precise', 'no legalese', 'always end with my next step'. Leave blank for a balanced default."
                            )
                            DrawerTextField(
                                label = "Your name",
                                value = userName,
                                onValueChange = { viewModel.updateUserName(it) },
                                placeholder = "e.g. Alex",
                                hint = "So replies address you personally instead of generically."
                            )
                            DrawerTextField(
                                label = "About you",
                                value = userAbout,
                                onValueChange = { viewModel.updateUserAbout(it) },
                                placeholder = "Tell the AI who you are: interests, job, style...",
                                minHeight = 72.dp,
                                hint = "Context makes replies sharper: your job, situation, what you are working on."
                            )
                            DrawerTextField(
                                label = "YOUR JURISDICTION",
                                value = jurisdiction,
                                onValueChange = { viewModel.updateJurisdiction(it) },
                                placeholder = "e.g. New South Wales, Australia / California, USA",
                                minHeight = 52.dp,
                                hint = "Replies cite the law that applies where you live. Update it if you move."
                            )
                        }
                    }

                    SettingsSection(2) {
                        SettingsSectionTitle("EMAIL INTEGRATION")
                        if (gmailAccountEmail.isNotBlank()) {
                            SettingsCard {
                                Text(
                                    "Signed in to Gmail as $gmailAccountEmail",
                                    color = Color(0xFF00FFCC),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                SettingsHint("Your inbox button now reads this account. Sign out below to switch accounts.")
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x1FFF4D6D), RoundedCornerShape(12.dp))
                                        .clickable { viewModel.signOutGmail() }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFFF4D6D), modifier = Modifier.size(14.dp))
                                    Text("SIGN OUT OF GMAIL", color = Color(0xFFFF8FA3), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            SettingsCard {
                                DrawerTextField(
                                    label = "GOOGLE CLIENT ID",
                                    value = googleClientId,
                                    onValueChange = { viewModel.updateGoogleClientId(it) },
                                    placeholder = "From Google Cloud Console (one-time)",
                                    hint = "Step 1: console.cloud.google.com > APIs & Services > Credentials > Create OAuth Client ID. Copy the ID here."
                                )
                                DrawerTextField(
                                    label = "GOOGLE CLIENT SECRET",
                                    value = googleClientSecret,
                                    onValueChange = { viewModel.updateGoogleClientSecret(it) },
                                    placeholder = "From Google Cloud Console (one-time)",
                                    mask = true,
                                    hint = "Step 2: copy the client secret from the same Credentials page."
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            Brush.linearGradient(listOf(Color(0xFF00FFCC), Color(0xFF0099FF))),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            viewModel.prepareGoogleSignIn()?.let { url ->
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "SIGN IN WITH GOOGLE",
                                        color = Color.Black,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                }
                                SettingsHint("Step 3: tap the button, approve in the browser, done - one time only. No passwords needed. Afterwards the email button reads your Gmail.")
                            }
                        }
                        SettingsSectionTitle("OTHER PROVIDERS (IMAP)")
                        SettingsCard {
                            DrawerTextField(
                                label = "EMAIL ADDRESS",
                                value = emailAddress,
                                onValueChange = { viewModel.updateEmailAddress(it) },
                                placeholder = "you@outlook.com, yahoo, icloud..."
                            )
                            DrawerTextField(
                                label = "IMAP PASSWORD / APP PASSWORD",
                                value = emailPassword,
                                onValueChange = { viewModel.updateEmailPassword(it) },
                                placeholder = "Your email or app password",
                                mask = true,
                                hint = "Most providers need an App Password, not your normal one. Gmail: Google Account > Security > 2-Step Verification on > App Passwords. Outlook/Yahoo: your account security page."
                            )
                            DrawerTextField(
                                label = "IMAP HOST (OPTIONAL)",
                                value = emailHost,
                                onValueChange = { viewModel.updateEmailHost(it) },
                                placeholder = "Auto: Gmail, Outlook, Yahoo, iCloud",
                                hint = "Leave blank - it is detected from your address. Only fill it for unusual providers."
                            )
                            SettingsHint("Email setup is optional. The app works fully without it.")
                        }
                    }

                    SettingsSection(3) {
                        SettingsSectionTitle("AI POWER")
                        SettingsCard {
                            SettingsChipRow(
                                listOf("low" to "Fast", "medium" to "Balanced", "high" to "Maximum"),
                                aiEffort
                            ) { viewModel.updateAiEffort(it) }
                            SettingsHint("Fast replies instantly. Maximum thinks like a senior advocate - slower, deeper, sharper. Working an important case? Use Maximum.")
                            HorizontalDivider(color = Color(0x1FFFFFFF))
                            SettingsSectionTitle("LEGAL DETAIL LEVEL")
                            SettingsChipRow(
                                listOf("simple" to "Plain", "standard" to "Standard", "detailed" to "Detailed"),
                                legalDetail
                            ) { viewModel.updateLegalDetail(it) }
                            SettingsHint("Plain = everyday language. Detailed = full statute names, sections and case citations.")
                        }
                    }

                    SettingsSection(4) {
                        SettingsSectionTitle("BEHAVIOUR")
                        SettingsCard {
                            SettingsToggleRow(
                                "Cite current law",
                                "Name the statute, regulation, or case for every answer",
                                citeSources
                            ) { viewModel.setCiteSources(it) }
                            HorizontalDivider(color = Color(0x1FFFFFFF))
                            SettingsToggleRow(
                                "Speak replies aloud",
                                "Every response is spoken automatically",
                                speakReplies
                            ) { viewModel.setSpeakReplies(it) }
                            HorizontalDivider(color = Color(0x1FFFFFFF))
                            SettingsToggleRow(
                                "Live web search",
                                "Searches the web for current facts before replying",
                                useLiveSearch
                            ) { viewModel.setUseLiveSearch(it) }
                        }
                    }

                    SettingsSection(5) {
                        SettingsSectionTitle("MY VOICE")
                        SettingsCard {
                            SettingsToggleRow(
                                "Use MY voice",
                                if (clonedVoiceBase64.isNotBlank()) "Voice sample saved - replies in your voice" else "Record a sample below to unlock this switch",
                                useClonedVoice,
                                enabled = clonedVoiceBase64.isNotBlank()
                            ) { viewModel.setUseClonedVoice(it) }
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
                            SettingsHint("How: 1) Tap record and speak naturally for 5-15 seconds. 2) Tap stop. 3) Turn on 'Use MY voice'. Your sample stays on this device and is sent only to generate your replies. Not working? Allow microphone access when prompted, and record a bit longer.")
                        }
                    }

                    Text(
                        "STRATAGEM v" + BuildConfig.VERSION_NAME,
                        color = Color(0x60FFFFFF),
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    }
}


@Composable
fun SettingsSection(delayMillis: Int, content: @Composable ColumnScope.() -> Unit) {
    val state = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        state.targetState = true
    }
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(tween(320, easing = FastOutSlowInEasing)) + slideInVertically(tween(360, easing = FastOutSlowInEasing)) { it / 6 },
        exit = fadeOut()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x14FFFFFF), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
fun SettingsSectionTitle(text: String) {
    Text(text, color = Color(0xFF80DEFF), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
}

@Composable
fun SettingsHint(text: String) {
    Text(text, color = Color(0x80FFFFFF), fontSize = 11.sp, lineHeight = 15.sp)
}

@Composable
fun SettingsChipRow(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    val haptics = LocalHapticFeedback.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (key, label) ->
            val isSelected = selected == key
            Box(
                modifier = Modifier
                    .background(
                        if (isSelected) Brush.linearGradient(listOf(Color(0xFF00E5C7), Color(0xFF0077B6))) else Brush.linearGradient(listOf(Color(0x1AFFFFFF), Color(0x1AFFFFFF))),
                        RoundedCornerShape(14.dp)
                    )
                    .border(1.dp, if (isSelected) Color.Transparent else Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(key)
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(label, color = if (isSelected) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SettingsToggleRow(title: String, subtitle: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = if (enabled) Color.White else Color(0x80FFFFFF), fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = Color(0x80FFFFFF), fontSize = 12.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onChange(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00FFCC),
                checkedTrackColor = Color(0x3300FFCC)
            )
        )
    }
}

@Composable
fun SettingsHelpCard() {
    var open by remember { mutableStateOf(true) }
    val chevron by animateFloatAsState(if (open) 180f else 0f, tween(250), label = "help-chevron")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(listOf(Color(0x2600FFCC), Color(0x1A80DEFF), Color(0x1FFF007F))),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = !open },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("START HERE - HOW SETTINGS WORK", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("▾", color = Color(0xFF80DEFF), fontSize = 13.sp, modifier = Modifier.graphicsLayer { rotationZ = chevron })
        }
        AnimatedVisibility(open, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Spacer(Modifier.height(4.dp))
                SettingsHelpStep("1", "PERSONA & PROFILE", "Tell the AI who it is and who you are. Everything here shapes every reply - leave blank for defaults.")
                SettingsHelpStep("2", "AI POWER & LEGAL DETAIL", "Fast for quick chats, Maximum for deep legal strategy. Detailed names statutes and cases.")
                SettingsHelpStep("3", "EMAIL (OPTIONAL)", "Gmail users: paste a Google Client ID/Secret from Google Cloud Console and tap SIGN IN. Outlook, Yahoo, iCloud: use your address plus an app password. You can skip this - the app works without it.")
                SettingsHelpStep("4", "VOICE", "Record a 5-15 second sample to have replies read aloud in your own voice.")
                HorizontalDivider(color = Color(0x26FFFFFF))
                Text("IF YOU GET STUCK", color = Color(0xFF80DEFF), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                SettingsHint("- No replies? Check you are online - the AI needs internet. Also check AI POWER is set to Fast, Balanced or Maximum.")
                SettingsHint("- Gmail sign-in failing? The Client ID and secret must come from the same Google Cloud Console project, and its consent screen must be published (External > Publish app).")
                SettingsHint("- IMAP login refused? That almost always means an app password is required, or 2-Step Verification is not enabled yet.")
                SettingsHint("- Voice not working? Allow microphone permission when asked and record at least 5 seconds of plain natural speech.")
                SettingsHint("- Nothing to press when done - every change saves itself the moment you type or tap.")
            }
        }
    }
}

@Composable
fun SettingsHelpStep(num: String, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(0x4080DEFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(num, color = Color(0xFF80DEFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(body, color = Color(0x99FFFFFF), fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
fun SettingsToolRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = Color(0xFF80DEFF), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun DrawerTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minHeight: Dp = 52.dp,
    mask: Boolean = false,
    hint: String = ""
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
            visualTransformation = if (mask) PasswordVisualTransformation() else VisualTransformation.None,
            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, color = Color(0x60FFFFFF), fontSize = 14.sp)
                    inner()
                }
            }
        )
        if (hint.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(hint, color = Color(0x60FFFFFF), fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
fun ContextChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x14FFFFFF), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF80DEFF), modifier = Modifier.size(13.dp))
        Text(text, color = Color(0xFF80DEFF), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
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
            if (isTranscribing) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = "Transcribing",
                    tint = Color(0xFF80DEFF),
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Hold to talk",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
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
                Icon(Icons.Default.Balance, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
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
fun StratagemScreensaver(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07030E))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = 4f
                    scaleY = 4f
                }
            ) {
                StrataLoader(showStatus = false)
            }
            Text("Touch to return to your conversation", color = Color(0x60FFFFFF), fontSize = 12.sp, letterSpacing = 2.sp)
        }
    }
}

@Composable
fun StrataLoader(showStatus: Boolean = true) {
    val transition = rememberInfiniteTransition(label = "strata")
    val morph by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "morph"
    )
    val spinY by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2700, easing = LinearEasing)),
        label = "spinY"
    )
    val orbit by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "orbit"
    )
    val wordPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wordPhase"
    )

    val status = when ((morph * 4f).toInt()) {
        0 -> "Researching the law"
        1 -> "Checking jurisdiction"
        2 -> "Preparing your answer"
        else -> "Verifying sources"
    }
    val words = if (showStatus) status.split(" ") else emptyList()

    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                rotationY = spinY
                cameraDistance = 12f * density
            }
        ) {
            Canvas(modifier = Modifier.size(34.dp)) {
                val scale = size.width / 100f
                val sPath = listOf(
                    70f to 28f,
                    65f to 18f, 35f to 18f, 35f to 34f,
                    35f to 48f, 65f to 52f, 65f to 66f,
                    65f to 82f, 35f to 82f, 30f to 72f
                )
                val circlePath = listOf(
                    50f to 15f,
                    70f to 15f, 85f to 30f, 85f to 50f,
                    85f to 70f, 70f to 85f, 50f to 85f,
                    30f to 85f, 15f to 70f, 15f to 50f
                )
                val path = androidx.compose.ui.graphics.Path()
                fun pt(i: Int): Offset {
                    val s = sPath[i]
                    val c = circlePath[i]
                    val spread = 0.5f
                    val delay = (i / 9f) * spread
                    val local = ((morph - delay) / (1f - spread)).coerceIn(0f, 1f)
                    val t = local * local * (3f - 2f * local)
                    return Offset(
                        ((s.first + (c.first - s.first) * t) * scale),
                        ((s.second + (c.second - s.second) * t) * scale)
                    )
                }
                path.moveTo(pt(0).x, pt(0).y)
                for (seg in 0 until 3) {
                    val a = pt(seg * 3 + 1)
                    val b = pt(seg * 3 + 2)
                    val c = pt(seg * 3 + 3)
                    path.cubicTo(a.x, a.y, b.x, b.y, c.x, c.y)
                }

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x2600E5C7), Color.Transparent),
                        center = Offset(size.width / 2, size.height / 2),
                        radius = size.width
                    ),
                    radius = size.width * 0.9f,
                    center = Offset(size.width / 2, size.height / 2)
                )

                drawArc(
                    color = Color(0x4F06B6D4),
                    startAngle = orbit,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.04f, size.height * 0.04f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.92f, size.height * 0.92f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 1.2.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )

                drawPath(
                    path = path,
                    brush = Brush.sweepGradient(
                        colors = listOf(Color(0xFF06B6D4), Color(0xFF8B5CF6), Color(0xFFF59E0B), Color(0xFF06B6D4)),
                        center = Offset(size.width / 2, size.height / 2)
                    ),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 9f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    ),
                    alpha = 0.25f
                )
                drawPath(
                    path = path,
                    brush = Brush.sweepGradient(
                        colors = listOf(Color(0xFF06B6D4), Color(0xFF8B5CF6), Color(0xFFF59E0B), Color(0xFF06B6D4)),
                        center = Offset(size.width / 2, size.height / 2)
                    ),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 2.6.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
            }
        }
        key(status) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val total = 1f + (words.size - 1) * 0.12f
                words.forEachIndexed { i, word ->
                    val a = ((wordPhase * total - i * 0.12f).coerceIn(0f, 1f))
                    val eased = a * a * (3f - 2f * a)
                    Text(
                        word,
                        color = Color(0x99FFFFFF),
                        fontSize = 12.sp,
                        modifier = Modifier.graphicsLayer { alpha = eased }
                    )
                }
            }
        }
    }
}

@Composable
fun AudioBars() {
    val transition = rememberInfiniteTransition(label = "eq")
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(4) { i ->
            val h by transition.animateFloat(
                initialValue = 4f,
                targetValue = 16f,
                animationSpec = infiniteRepeatable(
                    animation = tween(240 + i * 90, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$i"
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(h.dp)
                    .background(Color(0xFF00E5C7), RoundedCornerShape(2.dp))
            )
        }
    }
}
