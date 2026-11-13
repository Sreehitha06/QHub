
package com.qhub.quizapp.data

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
    val totalMarks: Int? = null,
    val teacherId: String = "",
    val classId: String = ""
)
