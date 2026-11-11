package com.qhub.quizapp.data


import com.qhub.quizapp.data.Quiz

enum class StudentQuizStatus {
    UPCOMING,
    ATTEMPTED,
    UNATTEMPTED
}

data class StudentQuizRecord(
    val quizId: String,
    val subjectName: String,
    val unitName: String,
    val status: StudentQuizStatus,
    val quiz: Quiz? = null,
    val scheduledDate: String = "",
    val scheduledTime: String = "",
    val marksObtained: Int? = null,
    val totalMarks: Int? = null
)
