package com.example.ui.screens.profile

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExamEntity
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamSwitcherSubScreen(
    currentExamName: String,
    catalogExams: List<ExamEntity>,
    onSelectExam: (examId: String, examName: String) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var pendingExamToSwitch by remember { mutableStateOf<ExamEntity?>(null) }

    val defaultExams = remember {
        listOf(
            ExamEntity(id = "upsc_cse", name = "UPSC Civil Services (IAS/IPS)", category = "Civil Services", shortCode = "UPSC", description = "National civil services examination", examPattern = "Prelims + Mains", totalMarks = 200, durationMinutes = 120, conductsConductingBody = "UPSC", isPopular = true),
            ExamEntity(id = "ssc_cgl", name = "SSC CGL (Tier 1 & 2)", category = "Staff Selection", shortCode = "SSC", description = "Staff Selection Commission Combined Graduate Level", examPattern = "Tier 1 & 2 CBT", totalMarks = 200, durationMinutes = 60, conductsConductingBody = "SSC", isPopular = true),
            ExamEntity(id = "jee_main", name = "JEE Main (Engineering)", category = "Engineering", shortCode = "JEE", description = "Joint Entrance Examination for IIT/NIT admissions", examPattern = "Physics, Chem, Math CBT", totalMarks = 300, durationMinutes = 180, conductsConductingBody = "NTA", isPopular = true),
            ExamEntity(id = "neet_ug", name = "NEET UG (Medical)", category = "Medical", shortCode = "NEET", description = "National Eligibility cum Entrance Test for MBBS", examPattern = "Pen & Paper MCQ", totalMarks = 720, durationMinutes = 200, conductsConductingBody = "NTA", isPopular = true),
            ExamEntity(id = "ibps_po", name = "Banking IBPS / SBI PO", category = "Banking", shortCode = "IBPS", description = "Probationary Officer exam for public sector banks", examPattern = "Prelims + Mains", totalMarks = 100, durationMinutes = 60, conductsConductingBody = "IBPS", isPopular = true),
            ExamEntity(id = "gate_cs", name = "GATE CS & IT", category = "Engineering", shortCode = "GATE", description = "Graduate Aptitude Test in Engineering", examPattern = "CBT Multiple Choice & NAT", totalMarks = 100, durationMinutes = 180, conductsConductingBody = "IIT", isPopular = false),
            ExamEntity(id = "nda_na", name = "NDA & Naval Academy", category = "Defense", shortCode = "NDA", description = "National Defence Academy & Naval Academy Examination", examPattern = "Maths + GAT", totalMarks = 300, durationMinutes = 150, conductsConductingBody = "UPSC", isPopular = false),
            ExamEntity(id = "cat_mba", name = "CAT (IIM / MBA)", category = "Management", shortCode = "CAT", description = "Common Admission Test for management programs", examPattern = "VARC + DILR + QA", totalMarks = 198, durationMinutes = 120, conductsConductingBody = "IIM", isPopular = false),
            ExamEntity(id = "cuet_ug", name = "CUET UG (Central Universities)", category = "University", shortCode = "CUET", description = "Common University Entrance Test", examPattern = "Domain Specific CBT", totalMarks = 200, durationMinutes = 45, conductsConductingBody = "NTA", isPopular = false),
            ExamEntity(id = "state_pcs", name = "State PCS / Civil Services", category = "Civil Services", shortCode = "PCS", description = "State Public Service Commission general studies exam", examPattern = "General Studies + CSAT", totalMarks = 200, durationMinutes = 120, conductsConductingBody = "State PSC", isPopular = false)
        )
    }

    val examsToDisplay = remember(catalogExams, searchQuery) {
        val pool = if (catalogExams.isNotEmpty()) catalogExams else defaultExams
        if (searchQuery.isBlank()) pool
        else pool.filter { it.name.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Switch Target Exam",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("exam_switcher_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF090D16))
            )
        },
        containerColor = Color(0xFF090D16)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Adaptive Explanation Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ElectricViolet.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Changing your target exam automatically updates your AI study plan, syllabus hierarchy, and daily quiz topics. Your past mock scores and notes will be preserved.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 16.sp
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by exam name or category (e.g. UPSC, JEE, SSC)...") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = NeonCyan)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = Color.White.copy(alpha = 0.6f))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("exam_search_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.5f),
                    unfocusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Exams List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(examsToDisplay, key = { it.id }) { exam ->
                    val isCurrent = currentExamName.contains(exam.name, ignoreCase = true) || exam.name.contains(currentExamName, ignoreCase = true)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                if (!isCurrent) {
                                    pendingExamToSwitch = exam
                                }
                            }
                            .testTag("exam_option_${exam.id}"),
                        color = if (isCurrent) ElectricViolet.copy(alpha = 0.2f) else Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isCurrent) 1.5.dp else 1.dp,
                            color = if (isCurrent) ElectricViolet else Color.White.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = exam.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = NeonCyan.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = exam.category,
                                            color = NeonCyan,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = exam.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f),
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("⏳ ${exam.durationMinutes} mins", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                    Text("🎯 ${exam.totalMarks} Marks", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                    Text("🏛 ${exam.conductsConductingBody}", color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            if (isCurrent) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = EmeraldSuccess.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess)
                                ) {
                                    Text(
                                        text = "Active",
                                        color = EmeraldSuccess,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }
        }
    }

    // Switch Confirmation Dialog
    pendingExamToSwitch?.let { targetExam ->
        AlertDialog(
            onDismissRequest = { pendingExamToSwitch = null },
            title = { Text("Switch Exam to ${targetExam.name}?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Your daily schedule, AI tutor syllabus recommendations, and question topics will be adapted for ${targetExam.name}. Your past study history and notes will remain safe.",
                    color = Color.White.copy(alpha = 0.85f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val chosen = targetExam
                        pendingExamToSwitch = null
                        onSelectExam(chosen.id, chosen.name)
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                ) {
                    Text("Switch & Adapt", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingExamToSwitch = null }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(16.dp)
        )
    }
}
