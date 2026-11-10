
package com.qhub.quizapp.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.times
import com.qhub.quizapp.data.Quiz
import kotlinx.coroutines.delay

private val QuizCream = Color(0xFFF0EEE8)
private val QuizBlue = Color(0xFF2B4C63)
private val QuizLightBlue = Color(0xFFDCE7ED)
private val QuizMuted = Color(0xFF5C6266)

@Composable
fun QuizScreen(
    quiz: Quiz,
    onSubmit: (List<Int>) -> Unit
) {
    val answers = remember(quiz.quizId) {
        mutableStateListOf<Int>().apply {
            repeat(quiz.questions.size) {
                add(-1)
            }
        }
    }

    var currentQuestion by remember(quiz.quizId) {
        mutableIntStateOf(0)
    }

    var remainingSeconds by remember(quiz.quizId) {
        mutableIntStateOf(quiz.durationMinutes * 60)
    }

    var showSubmitDialog by remember {
        mutableStateOf(false)
    }

    var submitted by remember(quiz.quizId) {
        mutableStateOf(false)
    }

    LaunchedEffect(quiz.quizId) {
        while (remainingSeconds > 0 && !submitted) {
            delay(1000)
            remainingSeconds--
        }

        if (remainingSeconds <= 0 && !submitted) {
            submitted = true
            onSubmit(answers.toList())
        }
    }

    val question = quiz.questions.getOrNull(currentQuestion)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QuizCream)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        // Quiz title
        Text(
            text = quiz.title,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            color = QuizBlue
        )

        Spacer(Modifier.height(12.dp))

        // Question number and timer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Question ${currentQuestion + 1}/${quiz.questions.size}",
                color = QuizBlue,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Time: %02d:%02d".format(
                    remainingSeconds / 60,
                    remainingSeconds % 60
                ),
                color = if (remainingSeconds <= 60) Color.Red else QuizBlue,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(16.dp))

        // Question navigation grid
        Text(
            "Question Navigation",
            color = QuizBlue,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(10.dp))

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val columns = when {
                maxWidth >= 600.dp -> 8
                maxWidth >= 400.dp -> 7
                maxWidth >= 350.dp -> 6
                else -> 5
            }

            val itemSize =
                (maxWidth - (columns - 1) * 8.dp) / columns

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quiz.questions.indices.chunked(columns).forEach { chunk ->

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chunk.forEach { index ->

                            val boxColor = when {
                                index == currentQuestion -> QuizBlue
                                answers[index] != -1 -> QuizLightBlue
                                else -> Color.White
                            }

                            Box(
                                modifier = Modifier
                                    .size(itemSize)
                                    .background(
                                        boxColor,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        currentQuestion = index
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = if (index == currentQuestion) {
                                        Color.White
                                    } else {
                                        QuizBlue
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Question and options
        if (question != null) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Text(
                        question.questionText,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = QuizBlue
                    )

                    Spacer(Modifier.height(18.dp))

                    val optionOrder = remember(
                        quiz.quizId,
                        currentQuestion
                    ) {
                        val indices = question.options.indices.toList()

                        if (quiz.shuffleOptions) {
                            indices.shuffled()
                        } else {
                            indices
                        }
                    }

                    optionOrder.forEach { originalIndex ->

                        val selected =
                            answers[currentQuestion] == originalIndex

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .background(
                                    if (selected) {
                                        QuizLightBlue
                                    } else {
                                        QuizCream
                                    },
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    answers[currentQuestion] = originalIndex
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            RadioButton(
                                selected = selected,
                                onClick = {
                                    answers[currentQuestion] = originalIndex
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = QuizBlue
                                )
                            )

                            Text(
                                question.options[originalIndex],
                                color = QuizBlue,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Answer count
        Text(
            "Answered: ${answers.count { it != -1 }} / ${quiz.questions.size}",
            color = QuizMuted,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(16.dp))

        // Previous and Next buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            OutlinedButton(
                onClick = {
                    if (currentQuestion > 0) {
                        currentQuestion--
                    }
                },
                enabled = currentQuestion > 0,
                modifier = Modifier.weight(1f)
            ) {
                Text("Previous")
            }

            Button(
                onClick = {
                    if (currentQuestion < quiz.questions.lastIndex) {
                        currentQuestion++
                    }
                },
                enabled = currentQuestion < quiz.questions.lastIndex,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = QuizBlue
                )
            ) {
                Text("Next")
            }
        }

        Spacer(Modifier.height(12.dp))

        // Submit button
        Button(
            onClick = {
                showSubmitDialog = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = QuizBlue
            )
        ) {
            Text("Submit Quiz")
        }
    }

    // Submit confirmation dialog
    if (showSubmitDialog) {

        AlertDialog(
            onDismissRequest = {
                showSubmitDialog = false
            },
            title = {
                Text("Submit Quiz?")
            },
            text = {
                Text(
                    "Answered: ${answers.count { it != -1 }}\n" +
                            "Unanswered: ${answers.count { it == -1 }}\n\n" +
                            "Are you sure you want to submit?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSubmitDialog = false

                        if (!submitted) {
                            submitted = true
                            onSubmit(answers.toList())
                        }
                    }
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSubmitDialog = false
                    }
                ) {
                    Text("Go Back")
                }
            }
        )
    }
}
