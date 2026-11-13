
package com.qhub.quizapp.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qhub.quizapp.data.Quiz

private val ResultCream = Color(0xFFF0EEE8)
private val ResultBlue = Color(0xFF2B4C63)
private val ResultBlueGray = Color(0xFF6F8B9B)
private val ResultWhite = Color.White

@Composable
fun QuizResultScreen(
    quiz: Quiz,
    correct: Int,
    wrong: Int,
    unanswered: Int,
    marksObtained: Int,
    totalMarks: Int,
    onBackToDashboard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ResultCream)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Quiz Completed!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = ResultBlue,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = quiz.title,
            fontSize = 17.sp,
            color = ResultBlueGray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(35.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = ResultWhite
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 45.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Your Marks",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ResultBlueGray
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "$marksObtained / $totalMarks",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ResultBlue
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Marks Obtained",
                    fontSize = 14.sp,
                    color = ResultBlueGray
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onBackToDashboard,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ResultBlue
            )
        ) {
            Text(
                text = "Back to Dashboard",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = ResultWhite
            )
        }
    }
}
