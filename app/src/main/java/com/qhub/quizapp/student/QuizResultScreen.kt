
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

private val ResultCream = Color(0xFFF0EEE8)
private val ResultBlue = Color(0xFF2B4C63)
private val ResultBlueGray = Color(0xFF6F8B9B)
private val ResultWhite = Color(0xFFFFFFFF)

@Composable
fun QuizResultScreen(
    quiz: Quiz,
    answers: List<Int>,
    onBackToDashboard: () -> Unit
) {
    val correct = quiz.questions.indices.count { index ->
        answers.getOrNull(index) == quiz.questions[index].correctAnswerIndex
    }

    val answered = answers.take(quiz.questions.size).count { it != -1 }
    val wrong = answered - correct
    val unanswered = quiz.questions.size - answered

    val percentage = if (quiz.questions.isNotEmpty()) {
        (correct * 100) / quiz.questions.size
    } else {
        0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ResultCream)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Quiz Completed!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = ResultBlue
        )

        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ResultWhite
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = quiz.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ResultBlue
                    )

                    Spacer(Modifier.height(22.dp))

                    Text(
                        text = "$percentage%",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = ResultBlue
                    )

                    Text(
                        text = "Your Score",
                        color = ResultBlueGray,
                        fontSize = 16.sp
                    )

                    Spacer(Modifier.height(24.dp))

                    HorizontalDivider(color = ResultCream)

                    ResultRow("Total Questions", quiz.questions.size)
                    ResultRow("Correct Answers", correct)
                    ResultRow("Wrong Answers", wrong)
                    ResultRow("Unanswered", unanswered)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onBackToDashboard,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ResultBlue,
                contentColor = ResultWhite
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Back to Dashboard",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ResultRow(label: String, value: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = ResultBlueGray,
            fontSize = 15.sp
        )

        Text(
            text = value.toString(),
            color = ResultBlue,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}
