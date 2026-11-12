
package com.qhub.quizapp.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.qhub.quizapp.StudentRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StudentQuizRepository {

    private val db = FirebaseFirestore.getInstance()
    private val quizRecords = mutableListOf<StudentQuizRecord>()

    fun getAttemptedQuizzes(): List<StudentQuizRecord> =
        quizRecords.filter { it.status == StudentQuizStatus.ATTEMPTED }

    fun getUnattemptedQuizzes(): List<StudentQuizRecord> =
        quizRecords.filter { it.status == StudentQuizStatus.UNATTEMPTED }

    fun getUpcomingQuizzes(): List<StudentQuizRecord> =
        quizRecords.filter { it.status == StudentQuizStatus.UPCOMING }

    fun saveAttemptedQuiz(record: StudentQuizRecord) {
        quizRecords.removeAll { it.quizId == record.quizId }
        quizRecords.add(record.copy(status = StudentQuizStatus.ATTEMPTED))
    }

    fun loadStudentQuizzes(
        onSuccess: (List<StudentQuizRecord>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val studentUid = FirebaseAuth.getInstance().currentUser?.uid

        if (studentUid.isNullOrBlank()) {
            onFailure("Please log in again.")
            return
        }

        StudentRepository().getVerifiedStudentEnrollments(
            onSuccess = { enrollments ->

                if (enrollments.isEmpty()) {
                    quizRecords.clear()
                    onSuccess(emptyList())
                    return@getVerifiedStudentEnrollments
                }

                val uniqueEnrollments = enrollments.distinctBy {
                    "${it.teacherId}|${it.classId}|${it.subjectName.trim().lowercase()}|${it.semester.trim().lowercase()}"
                }

                val loadedRecords = mutableListOf<StudentQuizRecord>()
                var completedRequests = 0
                var failed = false

                uniqueEnrollments.forEach { enrollment ->

                    db.collection("teachers")
                        .document(enrollment.teacherId)
                        .collection("classes")
                        .document(enrollment.classId)
                        .collection("quizzes")
                        .whereArrayContains("allowedStudentUids", studentUid)
                        .get()
                        .addOnSuccessListener { documents ->

                            if (failed) return@addOnSuccessListener

                            documents.documents.forEach documentLoop@{ document ->

                                val subjectName =
                                    document.getString("subjectName")?.trim().orEmpty()

                                val semester =
                                    document.getString("semester")?.trim().orEmpty()

                                if (
                                    !subjectName.equals(
                                        enrollment.subjectName.trim(),
                                        ignoreCase = true
                                    ) ||
                                    !semester.equals(
                                        enrollment.semester.trim(),
                                        ignoreCase = true
                                    )
                                ) {
                                    return@documentLoop
                                }

                                val firebaseStatus = document.getString("status")
                                    ?.trim()
                                    ?.uppercase(Locale.ROOT)
                                    .orEmpty()

                                if (
                                    firebaseStatus !in listOf(
                                        "SCHEDULED",
                                        "PUBLISHED",
                                        "ACTIVE",
                                        "LIVE"
                                    )
                                ) {
                                    return@documentLoop
                                }

                                val quizId =
                                    document.getString("quizId") ?: document.id

                                val scheduledDate =
                                    document.getString("scheduledDate").orEmpty()

                                val scheduledTime =
                                    document.getString("scheduledTime").orEmpty()

                                val questions = parseQuestions(
                                    document.get("questions")
                                )

                                val quiz = Quiz(
                                    quizId = quizId,
                                    title = document.getString("title")
                                        ?: "Untitled Quiz",
                                    durationMinutes =
                                        (document.get("durationMinutes") as? Number)
                                            ?.toInt() ?: 0,
                                    questions = questions,
                                    shuffleOptions =
                                        document.getBoolean("shuffleOptions") ?: false
                                )

                                val recordStatus =
                                    if (
                                        firebaseStatus == "SCHEDULED" &&
                                        isFutureSchedule(scheduledDate, scheduledTime)
                                    ) {
                                        StudentQuizStatus.UPCOMING
                                    } else {
                                        StudentQuizStatus.UNATTEMPTED
                                    }

                                loadedRecords.add(
                                    StudentQuizRecord(
                                        quizId = quizId,
                                        subjectName = subjectName,
                                        unitName = document.getString("unitName") ?: "",
                                        status = recordStatus,
                                        quiz = quiz,
                                        scheduledDate = scheduledDate,
                                        scheduledTime = scheduledTime,
                                        totalMarks =
                                            (document.get("totalMarks") as? Number)?.toInt()
                                    )
                                )
                            }

                            completedRequests++

                            if (completedRequests == uniqueEnrollments.size) {
                                finishLoading(loadedRecords, onSuccess)
                            }
                        }
                        .addOnFailureListener { exception ->

                            if (!failed) {
                                failed = true
                                onFailure(
                                    exception.message
                                        ?: "Unable to load quizzes from Firebase."
                                )
                            }
                        }
                }
            },
            onFailure = { error ->
                onFailure(error)
            }
        )
    }

    private fun parseQuestions(data: Any?): List<Question> {
        val questionList = data as? List<*> ?: return emptyList()

        return questionList.mapNotNull { item ->

            val questionMap = item as? Map<*, *>
                ?: return@mapNotNull null

            val questionText = questionMap["questionText"] as? String
                ?: return@mapNotNull null

            val options = (questionMap["options"] as? List<*>)
                ?.mapNotNull { it as? String }
                ?: emptyList()

            if (options.isEmpty()) return@mapNotNull null

            Question(
                questionText = questionText,
                options = options,
                correctAnswerIndex = -1
            )
        }
    }

    private fun isFutureSchedule(date: String, time: String): Boolean {
        if (date.isBlank() || time.isBlank()) return true

        return try {
            val formatter = SimpleDateFormat(
                "yyyy-MM-dd HH:mm",
                Locale.US
            )
            formatter.isLenient = false

            val scheduledDate = formatter.parse("$date $time") ?: return true
            scheduledDate.after(Date())
        } catch (e: Exception) {
            true
        }
    }

    private fun finishLoading(
        loadedRecords: List<StudentQuizRecord>,
        onSuccess: (List<StudentQuizRecord>) -> Unit
    ) {
        val attemptedRecords = quizRecords.filter {
            it.status == StudentQuizStatus.ATTEMPTED
        }

        val attemptedIds = attemptedRecords.map { it.quizId }.toSet()

        val finalRecords = loadedRecords.distinctBy { it.quizId }.map { record ->

            if (record.quizId in attemptedIds) {
                val attempted = attemptedRecords.first {
                    it.quizId == record.quizId
                }

                record.copy(
                    status = StudentQuizStatus.ATTEMPTED,
                    marksObtained = attempted.marksObtained,
                    totalMarks = attempted.totalMarks
                )
            } else {
                record
            }
        }

        quizRecords.clear()
        quizRecords.addAll(finalRecords)

        onSuccess(quizRecords.toList())
    }

    fun setQuizRecords(records: List<StudentQuizRecord>) {
        quizRecords.clear()
        quizRecords.addAll(records)
    }
}
