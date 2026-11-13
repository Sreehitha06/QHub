
package com.qhub.quizapp.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qhub.quizapp.data.StudentQuizRecord

private val QuizCream = Color(0xFFF0EEE8)
private val QuizBlue = Color(0xFF2B4C63)
private val QuizBlueGray = Color(0xFF6F8B9B)
private val QuizWhite = Color(0xFFFFFFFF)

@Composable
fun StudentQuizListScreen(
    title: String,
    quizzes: List<StudentQuizRecord>,
    showMarks: Boolean = false,
    showSchedule: Boolean = false,
    onBack: () -> Unit,
    onAttemptQuiz: (StudentQuizRecord) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QuizCream)
            .padding(20.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Back", color = QuizBlue)
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = QuizBlue,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(18.dp))

        if (quizzes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No quizzes available yet.",
                    color = QuizBlueGray
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    quizzes,
                    key = { "${it.teacherId}_${it.classId}_${it.quizId}" }
                ) { record ->

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = QuizWhite
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Quiz name
                            Text(
                                text = record.quiz?.title
                                    ?.takeIf { it.isNotBlank() }
                                    ?: "Untitled Quiz",
                                color = QuizBlue,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(modifier = Modifier.height(5.dp))

                            // Subject name
                            Text(
                                text = "Subject: ${record.subjectName}",
                                color = QuizBlueGray,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (record.unitName.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Unit: ${record.unitName}",
                                    color = QuizBlueGray
                                )
                            }

                            if (showMarks) {
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    "Marks: ${record.marksObtained ?: 0}/${record.totalMarks ?: 0}",
                                    color = QuizBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (showSchedule) {
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    "Date: ${record.scheduledDate}",
                                    color = QuizBlueGray
                                )

                                Text(
                                    "Time: ${record.scheduledTime}",
                                    color = QuizBlueGray
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        onAttemptQuiz(record)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = QuizBlue
                                    )
                                ) {
                                    Text(
                                        "Attempt Quiz",
                                        color = QuizWhite
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
