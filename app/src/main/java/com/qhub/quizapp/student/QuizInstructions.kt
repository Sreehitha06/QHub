
package com.qhub.quizapp.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qhub.quizapp.data.Quiz

private val Cream = Color(0xFFF0EEE8)
private val DeepBlue = Color(0xFF2B4C63)
private val BlueGray = Color(0xFF6F8B9B)
private val White = Color(0xFFFFFFFF)

@Composable
fun QuizInstructions(
    quiz: Quiz,
    onStartQuiz: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(20.dp)
    ) {
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

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onStartQuiz,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DeepBlue,
                contentColor = White
            )
        ) {
            Text(
                text = "Start Quiz",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
