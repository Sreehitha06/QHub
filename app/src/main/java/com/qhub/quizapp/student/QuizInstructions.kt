
package com.qhub.quizapp.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qhub.quizapp.data.Quiz
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

private val Cream = Color(0xFFF0EEE8)
private val DeepBlue = Color(0xFF2B4C63)
private val BlueGray = Color(0xFF6F8B9B)
private val White = Color(0xFFFFFFFF)

private fun parseQuizStartTime(
    scheduledDate: String,
    scheduledTime: String
): Long? {
    return try {
        val formatter = SimpleDateFormat(
            "yyyy-MM-dd HH:mm",
            Locale.getDefault()
        ).apply {
            isLenient = false
        }

        formatter.parse(
            "${scheduledDate.trim()} ${scheduledTime.trim()}"
        )?.time
    } catch (e: Exception) {
        null
    }
}

@Composable
fun QuizInstructions(
    quiz: Quiz,
    scheduledDate: String = "",
    scheduledTime: String = "",
    onBack: () -> Unit = {},
    onStartQuiz: () -> Unit
) {
    var currentTime by remember {
        mutableLongStateOf(System.currentTimeMillis())
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000)
        }
    }

    val startTime = remember(scheduledDate, scheduledTime) {
        parseQuizStartTime(scheduledDate, scheduledTime)
    }

    val endTime = startTime?.plus(
        quiz.durationMinutes.coerceAtLeast(0) * 60_000L
    )

    val canStartQuiz = startTime != null &&
            endTime != null &&
            currentTime >= startTime &&
            currentTime < endTime

    val statusMessage = when {
        startTime == null ->
            "The quiz schedule is invalid. Please contact your teacher."

        currentTime < startTime ->
            "This quiz has not started yet."

        endTime != null && currentTime >= endTime ->
            "The scheduled quiz time has expired."

        else ->
            "The quiz is now available. You can start!"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(20.dp)
    ) {
        TextButton(onClick = onBack) {
            Text(
                text = "← Back",
                color = DeepBlue,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = "Quiz Instructions",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = DeepBlue
        )

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = White
                )
            ) {
                Column(
                    modifier = Modifier.padding(22.dp)
                ) {
                    Text(
                        text = quiz.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepBlue
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Quiz Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepBlue
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Total Questions: ${quiz.questions.size}",
                        color = BlueGray,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Duration: ${quiz.durationMinutes} minutes",
                        color = BlueGray,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Important Instructions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepBlue
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val instructions = listOf(
                        "Read every question carefully.",
                        "Select one option for each question.",
                        "Use Previous and Next to navigate.",
                        "You can change your answers before submitting.",
                        "The quiz will submit automatically when time runs out.",
                        "Once submitted, your answers cannot be changed."
                    )

                    instructions.forEachIndexed { index, instruction ->
                        Text(
                            text = "${index + 1}. $instruction",
                            fontSize = 15.sp,
                            color = BlueGray,
                            lineHeight = 23.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = statusMessage,
            color = if (canStartQuiz) Color(0xFF27834A) else BlueGray,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                val latestStart = parseQuizStartTime(
                    scheduledDate,
                    scheduledTime
                )

                val latestEnd = latestStart?.plus(
                    quiz.durationMinutes.coerceAtLeast(0) * 60_000L
                )

                val now = System.currentTimeMillis()

                if (
                    latestStart != null &&
                    latestEnd != null &&
                    now >= latestStart &&
                    now < latestEnd
                ) {
                    onStartQuiz()
                }
            },
            enabled = canStartQuiz,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DeepBlue,
                contentColor = White,
                disabledContainerColor = BlueGray.copy(alpha = 0.35f),
                disabledContentColor = White
            )
        ) {
            Text(
                text = if (canStartQuiz) "Start Quiz" else "Quiz Not Available",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
