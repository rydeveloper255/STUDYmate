package com.example.ui.screens.intelligence

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.content.WeeklyCurrentAffairsPdf
import com.example.service.content.CurrentAffairsPdfManager
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Native In-App Reader for Weekly Current Affairs Hindi PDFs.
 * Renders PDF natively using Android PdfRenderer on cached/downloaded files.
 * Supports zoom, vertical scrolling, page counters, in-app download, and sharing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyCurrentAffairsPdfViewerDialog(
    pdf: WeeklyCurrentAffairsPdf,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var scale by remember { mutableFloatStateOf(1f) }
    var isLoading by remember { mutableStateOf(true) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var localPdfFile by remember { mutableStateOf<File?>(null) }
    var pageBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isSavingToDownloads by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()

    val transformState = rememberTransformableState { zoomChange, _, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 3.5f)
    }

    // Load or download PDF file locally, then render pages via PdfRenderer
    LaunchedEffect(pdf.id) {
        isLoading = true
        errorMessage = null
        downloadProgress = 0f

        val result = CurrentAffairsPdfManager.getOrDownloadPdfFile(
            context = context,
            pdf = pdf,
            onProgress = { progress -> downloadProgress = progress }
        )

        result.fold(
            onSuccess = { file ->
                localPdfFile = file
                withContext(Dispatchers.IO) {
                    try {
                        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                        val renderer = PdfRenderer(pfd)
                        val total = renderer.pageCount
                        val bitmaps = mutableListOf<Bitmap>()

                        for (i in 0 until total) {
                            val page = renderer.openPage(i)
                            // Render at 2x density for crisp Hindi text reading
                            val bmp = Bitmap.createBitmap(
                                (page.width * 1.8f).toInt(),
                                (page.height * 1.8f).toInt(),
                                Bitmap.Config.ARGB_8888
                            )
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            page.close()
                            bitmaps.add(bmp)
                        }

                        renderer.close()
                        pfd.close()

                        withContext(Dispatchers.Main) {
                            pageBitmaps = bitmaps
                            isLoading = false
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            errorMessage = "Failed to render PDF pages: ${e.localizedMessage}"
                            isLoading = false
                        }
                    }
                }
            },
            onFailure = { err ->
                errorMessage = err.message ?: "Unable to download Current Affairs PDF."
                isLoading = false
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070B19))
                .testTag("ca_pdf_viewer_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // Header Bar
                Surface(
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0x26FFFFFF), CircleShape)
                                    .testTag("ca_pdf_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Close PDF",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "WEEKLY CURRENT AFFAIRS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF38BDF8),
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = pdf.language,
                                            color = Color(0xFF34D399),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = pdf.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Header Actions (Zoom Out, Zoom In, Save to Device, Share)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { scale = (scale - 0.25f).coerceAtLeast(1f) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ZoomOut,
                                    contentDescription = "Zoom Out",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { scale = (scale + 0.25f).coerceAtMost(3.5f) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ZoomIn,
                                    contentDescription = "Zoom In",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Download / Save to Device Button
                            IconButton(
                                onClick = {
                                    val file = localPdfFile
                                    if (file != null) {
                                        isSavingToDownloads = true
                                        scope.launch {
                                            val res = CurrentAffairsPdfManager.savePdfToDownloads(context, pdf, file)
                                            isSavingToDownloads = false
                                            res.fold(
                                                onSuccess = { path ->
                                                    Toast.makeText(context, "✓ $path", Toast.LENGTH_LONG).show()
                                                },
                                                onFailure = { err ->
                                                    Toast.makeText(context, "Failed to save: ${err.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    } else {
                                        Toast.makeText(context, "PDF file is still loading...", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = !isSavingToDownloads && localPdfFile != null,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("ca_pdf_download_button")
                            ) {
                                if (isSavingToDownloads) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF38BDF8),
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Download,
                                        contentDescription = "Download PDF",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Share Button
                            IconButton(
                                onClick = {
                                    val file = localPdfFile
                                    if (file != null) {
                                        CurrentAffairsPdfManager.sharePdfFile(context, pdf, file)
                                    }
                                },
                                enabled = localPdfFile != null,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "Share PDF",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Main Viewer Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF020617))
                ) {
                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF38BDF8),
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Text(
                                        text = "Loading Current Affairs PDF...",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (downloadProgress > 0f) {
                                        LinearProgressIndicator(
                                            progress = { downloadProgress },
                                            modifier = Modifier
                                                .width(180.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = Color(0xFF38BDF8),
                                            trackColor = Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = "${(downloadProgress * 100).toInt()}% downloaded",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                    Text(
                                        text = "Preparing offline high-definition view inside StudyMate",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF64748B),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        errorMessage != null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier
                                        .padding(24.dp)
                                        .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                                        .padding(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "Could Not Open PDF",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = errorMessage ?: "Unknown error occurred.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8),
                                        textAlign = TextAlign.Center
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                isLoading = true
                                                errorMessage = null
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                        ) {
                                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Retry Loading")
                                        }
                                    }
                                }
                            }
                        }

                        pageBitmaps.isNotEmpty() -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .transformable(state = transformState)
                            ) {
                                LazyColumn(
                                    state = lazyListState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale
                                        ),
                                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    items(pageBitmaps.size) { index ->
                                        Surface(
                                            color = Color.White,
                                            shape = RoundedCornerShape(8.dp),
                                            shadowElevation = 6.dp,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column {
                                                Image(
                                                    bitmap = pageBitmaps[index].asImageBitmap(),
                                                    contentDescription = "PDF Page ${index + 1}",
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color(0xFFF1F5F9))
                                                        .padding(vertical = 4.dp, horizontal = 12.dp),
                                                    contentAlignment = Alignment.CenterEnd
                                                ) {
                                                    Text(
                                                        text = "Page ${index + 1} of ${pageBitmaps.size}",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFF64748B),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer Bar with In-App Download and Page Counter
                Surface(
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Source: ${pdf.sourceName} • Native StudyMate Viewer",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                            Text(
                                text = "Date: ${pdf.dateRange.ifBlank { pdf.publishedAt }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B),
                                fontSize = 10.sp
                            )
                        }

                        Button(
                            onClick = {
                                val file = localPdfFile
                                if (file != null) {
                                    isSavingToDownloads = true
                                    scope.launch {
                                        val res = CurrentAffairsPdfManager.savePdfToDownloads(context, pdf, file)
                                        isSavingToDownloads = false
                                        res.fold(
                                            onSuccess = { path ->
                                                Toast.makeText(context, "✓ $path", Toast.LENGTH_LONG).show()
                                            },
                                            onFailure = { err ->
                                                Toast.makeText(context, "Failed to save: ${err.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                } else {
                                    Toast.makeText(context, "PDF is still loading...", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isSavingToDownloads && localPdfFile != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0284C7)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isSavingToDownloads) "Saving..." else "Save PDF to Device",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
