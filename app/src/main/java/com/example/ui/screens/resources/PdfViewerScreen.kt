package com.example.ui.screens.resources

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ResourceBookmarkEntity
import com.example.data.model.StudyResourceEntity
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    resource: StudyResourceEntity,
    bookmarks: List<ResourceBookmarkEntity> = emptyList(),
    onUpdatePageProgress: (resourceId: String, page: Int, totalPages: Int) -> Unit = { _, _, _ -> },
    onBookmarkPage: (resourceId: String, page: Int, noteSnippet: String) -> Unit = { _, _, _ -> },
    onAskDocumentQA: (resourceId: String, question: String, onResult: (String) -> Unit) -> Unit = { _, _, _ -> },
    onStartFocus: (subject: String, topic: String, resourceId: String) -> Unit = { _, _, _ -> },
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var currentPage by remember { mutableIntStateOf(resource.lastViewedPage.coerceAtLeast(1)) }
    var totalPages by remember { mutableIntStateOf(resource.totalPages.coerceAtLeast(1)) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Transform / Zoom
    var scale by remember { mutableFloatStateOf(1f) }
    val transformState = rememberTransformableState { zoomChange, _, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 3f)
    }

    // AI Q&A Drawer State
    var showQaDrawer by remember { mutableStateOf(false) }
    var userQuestion by remember { mutableStateOf("") }
    var qaAnswer by remember { mutableStateOf<String?>(null) }
    var isQaLoading by remember { mutableStateOf(false) }

    // Render page logic
    LaunchedEffect(resource.resourceId, currentPage) {
        isLoading = true
        errorMessage = null
        try {
            val file = File(resource.fileUrl)
            if (file.exists() && file.length() > 0) {
                withContext(Dispatchers.IO) {
                    val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(pfd)
                    totalPages = renderer.pageCount.coerceAtLeast(1)
                    val targetIdx = (currentPage - 1).coerceIn(0, totalPages - 1)
                    val pdfPage = renderer.openPage(targetIdx)

                    val bmp = Bitmap.createBitmap(pdfPage.width * 2, pdfPage.height * 2, Bitmap.Config.ARGB_8888)
                    pdfPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    pdfPage.close()
                    renderer.close()
                    pfd.close()

                    pageBitmap = bmp
                }
            } else {
                // Asset / Text Fallback rendering
                pageBitmap = null
            }
            onUpdatePageProgress(resource.resourceId, currentPage, totalPages)
        } catch (e: Exception) {
            errorMessage = "Unable to render PDF page. ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pdf_viewer_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = resource.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            color = Color.White
                        )
                        Text(
                            text = "${resource.subjectName} • ${resource.topicName} (${resource.language})",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("pdf_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { scale = if (scale > 1f) 1f else 1.5f }) {
                        Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom", tint = NeonCyan)
                    }
                    IconButton(onClick = {
                        onBookmarkPage(resource.resourceId, currentPage, "Page $currentPage bookmark")
                        Toast.makeText(context, "Page $currentPage Bookmarked!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Filled.Bookmark, contentDescription = "Bookmark Page", tint = GoldenSpark)
                    }
                    IconButton(onClick = { showQaDrawer = !showQaDrawer }) {
                        Icon(Icons.Filled.Psychology, contentDescription = "Ask AI Document QA", tint = NebulaPurple)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        bottomBar = {
            Surface(
                color = DarkSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Page Navigation Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (currentPage > 1) currentPage-- },
                            enabled = currentPage > 1
                        ) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "Prev Page", tint = if (currentPage > 1) Color.White else Color.Gray)
                        }

                        Text(
                            text = "Page $currentPage of $totalPages",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )

                        IconButton(
                            onClick = { if (currentPage < totalPages) currentPage++ },
                            enabled = currentPage < totalPages
                        ) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Next Page", tint = if (currentPage < totalPages) Color.White else Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Progress Slider
                    Slider(
                        value = currentPage.toFloat(),
                        onValueChange = { currentPage = it.toInt().coerceIn(1, totalPages) },
                        valueRange = 1f..totalPages.toFloat().coerceAtLeast(1f),
                        steps = (totalPages - 2).coerceAtLeast(0),
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = Color.DarkGray
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { onStartFocus(resource.subjectName, resource.topicName, resource.resourceId) },
                            colors = ButtonDefaults.buttonColors(containerColor = NebulaPurple),
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        ) {
                            Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start Focus", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                onUpdatePageProgress(resource.resourceId, totalPages, totalPages)
                                Toast.makeText(context, "Marked as Completed!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            modifier = Modifier.weight(1f).padding(start = 6.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark Complete", fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        containerColor = DarkCanvas
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // PDF Page Container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E293B))
                        .transformable(state = transformState),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = NeonCyan)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Loading PDF Page $currentPage...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.LightGray
                                )
                            }
                        }

                        errorMessage != null -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = CoralRose, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = errorMessage ?: "Error reading document",
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(onClick = { currentPage = currentPage }) {
                                    Text("Retry Reading")
                                }
                            }
                        }

                        pageBitmap != null -> {
                            Image(
                                bitmap = pageBitmap!!.asImageBitmap(),
                                contentDescription = "PDF Page $currentPage",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale
                                    )
                            )
                        }

                        else -> {
                            // Text Fallback Reader Card
                            Card(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                ) {
                                    item {
                                        Surface(
                                            color = NeonCyan.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        ) {
                                            Text(
                                                text = "📖 Page $currentPage of $totalPages • Text Reader View",
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = NeonCyan
                                            )
                                        }

                                        Text(
                                            text = resource.title,
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = resource.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.LightGray
                                        )

                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            color = Color.White.copy(alpha = 0.1f)
                                        )

                                        val contentText = resource.contentText.ifBlank {
                                            "Section $currentPage Content:\nKey concepts, formulas, and verified study material for ${resource.topicName}."
                                        }

                                        Text(
                                            text = contentText,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.White,
                                            lineHeight = 24.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // AI Document Q&A Drawer Overlay
            AnimatedVisibility(
                visible = showQaDrawer,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = NebulaPurple)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ask Nova about this PDF", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            }
                            IconButton(onClick = { showQaDrawer = false }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = userQuestion,
                                onValueChange = { userQuestion = it },
                                placeholder = { Text("e.g. Is PDF me formulas kya hain?") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (userQuestion.isNotBlank()) {
                                        isQaLoading = true
                                        onAskDocumentQA(resource.resourceId, userQuestion) { result ->
                                            qaAnswer = result
                                            isQaLoading = false
                                        }
                                    }
                                },
                                modifier = Modifier.background(NeonCyan, CircleShape)
                            ) {
                                Icon(Icons.Filled.Send, contentDescription = "Ask", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isQaLoading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = NebulaPurple)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Searching document with Nova...", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                            }
                        } else if (qaAnswer != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                LazyColumn(modifier = Modifier.padding(12.dp)) {
                                    item {
                                        Text(qaAnswer!!, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
