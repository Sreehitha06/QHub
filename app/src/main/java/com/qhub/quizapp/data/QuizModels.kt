
package com.qhub.quizapp.data

data class Quiz(
    val quizId: String,
    val title: String,
    val durationMinutes: Int,
    val questions: List<Question>,
    val shuffleOptions: Boolean = false
)

data class Question(
    val questionText: String,
    val options: List<String>,
    val correctAnswerIndex: Int
)
