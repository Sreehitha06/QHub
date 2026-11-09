
package com.qhub.quizapp.data

object StudentQuizRepository {

    // Empty until real quiz records are received from Firebase.
    private val quizRecords = mutableListOf<StudentQuizRecord>()

    fun getAttemptedQuizzes(): List<StudentQuizRecord> {
        return quizRecords.filter {
            it.status == StudentQuizStatus.ATTEMPTED
        }
    }

    fun getUnattemptedQuizzes(): List<StudentQuizRecord> {
        return quizRecords.filter {
            it.status == StudentQuizStatus.UNATTEMPTED
        }
    }

    fun getUpcomingQuizzes(): List<StudentQuizRecord> {
        return quizRecords.filter {
            it.status == StudentQuizStatus.UPCOMING
        }
    }

    fun saveAttemptedQuiz(record: StudentQuizRecord) {
        quizRecords.removeAll {
            it.quizId == record.quizId
        }

        quizRecords.add(
            record.copy(status = StudentQuizStatus.ATTEMPTED)
        )
    }

    // Used later when real quiz records are loaded from Firebase.
    fun setQuizRecords(records: List<StudentQuizRecord>) {
        quizRecords.clear()
        quizRecords.addAll(records)
    }
}
