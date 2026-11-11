
package com.qhub.quizapp.data

data class Semester(
    val semesterNumber: Int,
    val subjects: List<Subject>,
    val isActive: Boolean
)

data class Subject(
    val subjectId: String,
    val subjectName: String
)
