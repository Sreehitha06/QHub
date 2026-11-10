
package com.qhub.quizapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalTime

data class QHubQuizQuestionDraft(
    val question: String = "",
    val options: List<String> = listOf("", "", "", ""),
    val correctAnswerIndex: Int = -1
)

@Composable
fun CreateQuizScreen(
    className: String,
    onBack: () -> Unit,
    onSaveQuiz: (
        title: String,
        questions: List<QHubQuizQuestionDraft>,
        durationMinutes: Int,
        scheduledDate: String,
        scheduledTime: String,
        shuffleQuestions: Boolean,
        shuffleOptions: Boolean,
        onResult: (Boolean, String?) -> Unit
    ) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    var quizTitle by remember { mutableStateOf("") }

    var questions by remember {
        mutableStateOf(listOf(QHubQuizQuestionDraft()))
    }

    var durationHours by remember { mutableStateOf("1") }
    var durationExtraMinutes by remember { mutableStateOf("0") }

    var scheduledDate by remember {
        mutableStateOf(LocalDate.now().toString())
    }

    var scheduledTime by remember {
        mutableStateOf(
            LocalTime.now().withSecond(0).withNano(0).toString()
        )
    }

    var shuffleQuestions by remember { mutableStateOf(false) }
    var shuffleOptions by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        TextButton(
            onClick = onBack,
            enabled = !isSaving
        ) {
            Text("← Back")
        }

        Text(
            "Create a Quiz",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text("Class: $className", color = colors.onSurfaceVariant)

        OutlinedTextField(
            value = quizTitle,
            onValueChange = { quizTitle = it },
            label = { Text("Quiz Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isSaving
        )

        HorizontalDivider()

        Text(
            "Questions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        questions.forEachIndexed { questionIndex, question ->

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Question ${questionIndex + 1}",
                            fontWeight = FontWeight.Bold
                        )

                        if (questions.size > 1) {
                            TextButton(
                                onClick = {
                                    questions = questions.filterIndexed { index, _ ->
                                        index != questionIndex
                                    }
                                },
                                enabled = !isSaving
                            ) {
                                Text("Remove", color = colors.error)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = question.question,
                        onValueChange = { newValue ->
                            questions = questions.toMutableList().also {
                                it[questionIndex] =
                                    question.copy(question = newValue)
                            }
                        },
                        label = { Text("Enter Question") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        enabled = !isSaving
                    )

                    Text("Options (select the correct answer)")

                    question.options.forEachIndexed { optionIndex, option ->

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            RadioButton(
                                selected = question.correctAnswerIndex == optionIndex,
                                onClick = {
                                    questions = questions.toMutableList().also {
                                        it[questionIndex] = question.copy(
                                            correctAnswerIndex = optionIndex
                                        )
                                    }
                                },
                                enabled = !isSaving
                            )

                            OutlinedTextField(
                                value = option,
                                onValueChange = { newValue ->
                                    questions = questions.toMutableList().also {
                                        it[questionIndex] = question.copy(
                                            options = question.options.toMutableList().also {
                                                it[optionIndex] = newValue
                                            }
                                        )
                                    }
                                },
                                label = {
                                    Text("Option ${optionIndex + 1}")
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                enabled = !isSaving
                            )
                        }
                    }

                    Text(
                        if (question.correctAnswerIndex == -1)
                            "No correct answer selected"
                        else
                            "Correct answer: Option ${question.correctAnswerIndex + 1}",
                        color = if (question.correctAnswerIndex == -1)
                            colors.error else colors.primary
                    )
                }
            }
        }

        OutlinedButton(
            onClick = {
                questions = questions + QHubQuizQuestionDraft()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        ) {
            Text("+ Add Another Question")
        }

        HorizontalDivider()

        Text(
            "Quiz Duration",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

            OutlinedTextField(
                value = durationHours,
                onValueChange = {
                    durationHours = it.filter(Char::isDigit)
                },
                label = { Text("Hours") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                enabled = !isSaving
            )

            OutlinedTextField(
                value = durationExtraMinutes,
                onValueChange = {
                    durationExtraMinutes = it.filter(Char::isDigit)
                },
                label = { Text("Extra Minutes") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                enabled = !isSaving
            )
        }

        Text("Example: 1 hour 30 minutes")

        HorizontalDivider()

        Text(
            "Schedule Quiz",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = scheduledDate,
            onValueChange = { scheduledDate = it },
            label = { Text("Date (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isSaving
        )

        OutlinedTextField(
            value = scheduledTime,
            onValueChange = { scheduledTime = it },
            label = { Text("Time (HH:MM)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isSaving
        )

        HorizontalDivider()

        Text(
            "Quiz Settings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Shuffle Questions")

            Switch(
                checked = shuffleQuestions,
                onCheckedChange = { shuffleQuestions = it },
                enabled = !isSaving
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Shuffle Options")

            Switch(
                checked = shuffleOptions,
                onCheckedChange = { shuffleOptions = it },
                enabled = !isSaving
            )
        }

        errorMessage?.let {
            Text(it, color = colors.error)
        }

        Button(
            onClick = {

                if (isSaving) return@Button

                val hours = durationHours.toIntOrNull()
                val minutes = durationExtraMinutes.toIntOrNull()

                val validDate = runCatching {
                    LocalDate.parse(scheduledDate)
                }.isSuccess

                val validTime = runCatching {
                    LocalTime.parse(scheduledTime)
                }.isSuccess

                val duration =
                    if (hours != null && minutes != null) {
                        hours * 60 + minutes
                    } else {
                        0
                    }

                when {

                    quizTitle.isBlank() -> {
                        errorMessage = "Enter a quiz title."
                    }

                    questions.any { it.question.isBlank() } -> {
                        errorMessage = "Enter all question texts."
                    }

                    questions.any {
                        it.options.size != 4 ||
                                it.options.any { option -> option.isBlank() }
                    } -> {
                        errorMessage = "Fill all four options."
                    }

                    questions.any {
                        it.correctAnswerIndex !in 0..3
                    } -> {
                        errorMessage =
                            "Select the correct answer for every question."
                    }

                    hours == null || minutes == null ||
                            hours < 0 || minutes !in 0..59 ||
                            duration <= 0 -> {
                        errorMessage = "Enter a valid quiz duration."
                    }

                    !validDate -> {
                        errorMessage = "Enter a valid date."
                    }

                    !validTime -> {
                        errorMessage = "Enter a valid time."
                    }

                    else -> {
                        errorMessage = null
                        isSaving = true

                        onSaveQuiz(
                            quizTitle.trim(),
                            questions.toList(),
                            duration,
                            scheduledDate,
                            scheduledTime,
                            shuffleQuestions,
                            shuffleOptions
                        ) { success, message ->

                            isSaving = false

                            if (success) {
                                onBack()
                            } else {
                                errorMessage =
                                    message ?: "Could not save quiz."
                            }
                        }
                    }
                }
            },
            enabled = !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                if (isSaving) "Saving Quiz..." else "Create Quiz"
            )
        }
    }
}
