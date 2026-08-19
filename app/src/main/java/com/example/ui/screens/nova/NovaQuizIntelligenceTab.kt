package com.example.ui.screens.nova

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Question
import com.example.ui.components.GlassCard
import com.example.ui.components.springClickable
import com.example.ui.theme.*
import com.example.viewmodel.NovaViewModel

@Composable
fun NovaQuizIntelligenceTab(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier
) {
    val quizState by viewModel.quizState.collectAsState()
    val studyContext by viewModel.studyContext.collectAsState()

    var selectedSubject by remember { mutableStateOf(studyContext.subjects.firstOrNull() ?: "Physics") }
    var selectedTopic by remember { mutableStateOf(studyContext.weakTopics.firstOrNull() ?: "All Topics") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = DarkSurfaceElevated.copy(alpha = 0.85f),
            borderColor = AmberGold.copy(alpha = 0.35f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = AmberGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("QUIZ INTELLIGENCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberGold, letterSpacing = 1.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Targeted Concept Practice", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Instant explanations & weak area remediation", fontSize = 12.sp, color = TextSecondary)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AmberGold.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Score: ${quizState.score}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberGold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (quizState.isGenerating) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AmberGold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("NOVA is preparing concept questions...", fontSize = 13.sp, color = TextSecondary)
                }
            }
        } else if (quizState.questions.isNotEmpty() && !quizState.isQuizFinished) {
            val currentQuestion = quizState.questions.getOrNull(quizState.currentIndex)
            if (currentQuestion != null) {
                ActiveQuizCard(
                    question = currentQuestion,
                    index = quizState.currentIndex,
                    total = quizState.questions.size,
                    selectedOption = quizState.selectedOptionIndex,
                    isSubmitted = quizState.isAnswerSubmitted,
                    explanation = quizState.explanation,
                    onSelectOption = { viewModel.selectQuizOption(it) },
                    onSubmitAnswer = { viewModel.submitQuizAnswer() },
                    onNextQuestion = { viewModel.nextQuizQuestion() }
                )
            }
        } else if (quizState.isQuizFinished) {
            QuizSummaryCard(
                score = quizState.score,
                total = quizState.questions.size,
                subject = quizState.subject,
                onRestart = {
                    viewModel.startQuizSession(quizState.subject, quizState.topic)
                }
            )
        } else {
            // Quiz Start Generator
            QuizGeneratorCard(
                subjects = studyContext.subjects,
                weakTopics = studyContext.weakTopics,
                selectedSubject = selectedSubject,
                selectedTopic = selectedTopic,
                onSubjectSelected = { selectedSubject = it },
                onTopicSelected = { selectedTopic = it },
                onGenerateQuiz = {
                    viewModel.startQuizSession(selectedSubject, selectedTopic)
                }
            )
        }
    }
}

@Composable
private fun ActiveQuizCard(
    question: Question,
    index: Int,
    total: Int,
    selectedOption: Int?,
    isSubmitted: Boolean,
    explanation: String,
    onSelectOption: (Int) -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextQuestion: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = DarkSurface.copy(alpha = 0.85f),
        borderColor = NeonCyan.copy(alpha = 0.25f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Question ${index + 1} of $total",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                Text(
                    text = "${question.subject} • ${question.topic}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = question.questionText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Options
            question.options.forEachIndexed { optIndex, optText ->
                val isSelected = selectedOption == optIndex
                val isCorrect = optIndex == question.correctOptionIndex
                val optionBg = when {
                    isSubmitted && isCorrect -> EmeraldGreen.copy(alpha = 0.25f)
                    isSubmitted && isSelected && !isCorrect -> CoralPink.copy(alpha = 0.25f)
                    isSelected -> ElectricIndigo.copy(alpha = 0.35f)
                    else -> DarkSurfaceElevated
                }
                val borderColor = when {
                    isSubmitted && isCorrect -> EmeraldGreen
                    isSubmitted && isSelected && !isCorrect -> CoralPink
                    isSelected -> ElectricIndigo
                    else -> Color.White.copy(alpha = 0.1f)
                }

                val optionModifier = if (!isSubmitted) {
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .springClickable {
                            onSelectOption(optIndex)
                        }
                } else {
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = optionBg,
                    border = BorderStroke(1.dp, borderColor),
                    modifier = optionModifier
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${('A' + optIndex)}.",
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) NeonCyan else Color.White,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = optText,
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Explanation card
            if (isSubmitted && explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSurfaceElevated,
                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Concept Explanation", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(explanation, fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f), lineHeight = 18.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isSubmitted) {
                Button(
                    onClick = onSubmitAnswer,
                    enabled = selectedOption != null,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Check Answer", fontWeight = FontWeight.Bold, color = DarkCanvas)
                }
            } else {
                Button(
                    onClick = onNextQuestion,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (index + 1 < total) "Next Question →" else "Finish Quiz 🎉", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun QuizSummaryCard(
    score: Int,
    total: Int,
    subject: String,
    onRestart: () -> Unit
) {
    val accuracy = if (total > 0) ((score.toFloat() / total) * 100).toInt() else 0

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = DarkSurfaceElevated.copy(alpha = 0.85f),
        borderColor = if (accuracy >= 60) EmeraldGreen.copy(alpha = 0.4f) else AmberGold.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎉", fontSize = 36.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Quiz Complete!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text("$subject • Accuracy $accuracy%", fontSize = 14.sp, color = TextSecondary)

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Score: $score / $total",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Practice Another Quiz", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun QuizGeneratorCard(
    subjects: List<String>,
    weakTopics: List<String>,
    selectedSubject: String,
    selectedTopic: String,
    onSubjectSelected: (String) -> Unit,
    onTopicSelected: (String) -> Unit,
    onGenerateQuiz: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = DarkSurface.copy(alpha = 0.8f),
        borderColor = Color.White.copy(alpha = 0.1f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Select Subject for AI Quiz", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subjects.forEach { sub ->
                    FilterChip(
                        selected = selectedSubject == sub,
                        onClick = { onSubjectSelected(sub) },
                        label = { Text(sub) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Recommended Topics", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                weakTopics.forEach { top ->
                    FilterChip(
                        selected = selectedTopic == top,
                        onClick = { onTopicSelected(top) },
                        label = { Text(top) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onGenerateQuiz,
                colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DarkCanvas, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Generate AI Concept Quiz (5 Questions)", fontWeight = FontWeight.Bold, color = DarkCanvas)
            }
        }
    }
}
