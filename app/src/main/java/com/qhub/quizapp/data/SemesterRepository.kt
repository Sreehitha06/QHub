
package com.qhub.quizapp.data

object SemesterRepository {

    private var currentSemester = 3

    private val semesterSubjects = mapOf(
        3 to listOf(
            Subject("s301", "Data Structures"),
            Subject("s302", "Database Management Systems"),
            Subject("s303", "Computer Organization"),
            Subject("s304", "Operating Systems"),
            Subject("s305", "Mathematics")
        ),

        4 to listOf(
            Subject("s401", "Design and Analysis of Algorithms"),
            Subject("s402", "Computer Networks"),
            Subject("s403", "Software Engineering"),
            Subject("s404", "Operating Systems"),
            Subject("s405", "Web Technologies")
        ),

        5 to listOf(
            Subject("s501", "Machine Learning"),
            Subject("s502", "Compiler Design"),
            Subject("s503", "Computer Graphics"),
            Subject("s504", "Artificial Intelligence")
        )
    )

    fun getCurrentSemester(): Int {
        return currentSemester
    }

    fun getActiveSemester(): Semester {
        return Semester(
            semesterNumber = currentSemester,
            subjects = semesterSubjects[currentSemester] ?: emptyList(),
            isActive = true
        )
    }

    fun getArchivedSemesters(): List<Semester> {
        return (1 until currentSemester).map { semesterNumber ->
            Semester(
                semesterNumber = semesterNumber,
                subjects = semesterSubjects[semesterNumber] ?: emptyList(),
                isActive = false
            )
        }
    }

    fun promoteToNextSemester() {
        if (currentSemester < 5) {
            currentSemester++
        }
    }
}
