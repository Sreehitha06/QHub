
package com.qhub.quizapp.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.qhub.quizapp.StudentRepository
import java.text.SimpleDateFormat
import java.util.Locale

object StudentQuizRepository {

    private val db = FirebaseFirestore.getInstance()
    private val quizRecords = mutableListOf<StudentQuizRecord>()

    fun getAttemptedQuizzes(): List<StudentQuizRecord> =
        quizRecords.filter {
            it.status == StudentQuizStatus.ATTEMPTED
        }

    fun getUnattemptedQuizzes(): List<StudentQuizRecord> =
        quizRecords.filter {
            it.status == StudentQuizStatus.UNATTEMPTED
        }

    fun getUpcomingQuizzes(): List<StudentQuizRecord> =
        quizRecords.filter {
            it.status == StudentQuizStatus.UPCOMING
        }

    private fun recordKey(record: StudentQuizRecord): String {
        return "${record.teacherId}|${record.classId}|${record.quizId}"
    }

    private fun quizReference(record: StudentQuizRecord) =
        db.collection("teachers")
            .document(record.teacherId)
            .collection("classes")
            .document(record.classId)
            .collection("quizzes")
            .document(record.quizId)

    // SUBMIT QUIZ AND SAVE ATTEMPT PERMANENTLY
    fun submitQuizAttempt(
        record: StudentQuizRecord,
        answers: List<Int>,
        onSuccess: (
            StudentQuizRecord,
            Int,
            Int,
            Int
        ) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        val studentUid = user?.uid

        if (studentUid.isNullOrBlank()) {
            onFailure("Please log in again.")
            return
        }

        val quiz = record.quiz

        if (quiz == null || quiz.questions.isEmpty()) {
            onFailure("Quiz questions are unavailable.")
            return
        }

        val quizRef = quizReference(record)

        val attemptRef = quizRef
            .collection("attempts")
            .document(studentUid)

        val answerKeyRef = quizRef
            .collection("answerKeys")
            .document("private")

        attemptRef.get()
            .addOnSuccessListener { existingAttempt ->

                if (existingAttempt.exists()) {
                    onFailure("You have already submitted this quiz.")
                    return@addOnSuccessListener
                }

                answerKeyRef.get()
                    .addOnSuccessListener { answerKeySnapshot ->

                        val rawIndexes =
                            answerKeySnapshot.get("correctAnswerIndexes")
                                    as? List<*>

                        if (
                            rawIndexes == null ||
                            rawIndexes.size != quiz.questions.size
                        ) {
                            onFailure(
                                "The answer key is missing or invalid."
                            )
                            return@addOnSuccessListener
                        }

                        val correctIndexes = rawIndexes.mapNotNull {
                            (it as? Number)?.toInt()
                        }

                        if (
                            correctIndexes.size != quiz.questions.size
                        ) {
                            onFailure(
                                "The answer key contains invalid values."
                            )
                            return@addOnSuccessListener
                        }

                        for (index in quiz.questions.indices) {
                            if (
                                correctIndexes[index] !in
                                quiz.questions[index].options.indices
                            ) {
                                onFailure(
                                    "The answer key contains an invalid answer."
                                )
                                return@addOnSuccessListener
                            }
                        }

                        val finalAnswers = quiz.questions.indices.map {
                            answers.getOrElse(it) { -1 }
                        }

                        var correctCount = 0
                        var wrongCount = 0
                        var unansweredCount = 0

                        for (index in quiz.questions.indices) {
                            when {
                                finalAnswers[index] == -1 -> {
                                    unansweredCount++
                                }

                                finalAnswers[index] == correctIndexes[index] -> {
                                    correctCount++
                                }

                                else -> {
                                    wrongCount++
                                }
                            }
                        }

                        val totalMarks =
                            record.totalMarks ?: quiz.questions.size

                        val marksObtained = correctCount

                        val attemptData = hashMapOf(
                            "studentUid" to studentUid,
                            "studentEmail" to (user.email ?: ""),
                            "quizId" to record.quizId,
                            "teacherId" to record.teacherId,
                            "classId" to record.classId,
                            "subjectName" to record.subjectName,
                            "answers" to finalAnswers,
                            "marksObtained" to marksObtained,
                            "totalMarks" to totalMarks,
                            "correctAnswers" to correctCount,
                            "wrongAnswers" to wrongCount,
                            "unanswered" to unansweredCount,
                            "submittedAt" to FieldValue.serverTimestamp()
                        )

                        attemptRef.set(attemptData)
                            .addOnSuccessListener {

                                val attemptedRecord = record.copy(
                                    status = StudentQuizStatus.ATTEMPTED,
                                    marksObtained = marksObtained,
                                    totalMarks = totalMarks
                                )

                                updateLocalRecord(attemptedRecord)

                                onSuccess(
                                    attemptedRecord,
                                    correctCount,
                                    wrongCount,
                                    unansweredCount
                                )
                            }
                            .addOnFailureListener { exception ->
                                onFailure(
                                    exception.message
                                        ?: "Unable to save your quiz attempt."
                                )
                            }
                    }
                    .addOnFailureListener { exception ->
                        onFailure(
                            exception.message
                                ?: "Unable to retrieve the answer key."
                        )
                    }
            }
            .addOnFailureListener { exception ->
                onFailure(
                    exception.message
                        ?: "Unable to check your previous submission."
                )
            }
    }

    private fun updateLocalRecord(record: StudentQuizRecord) {
        quizRecords.removeAll {
            recordKey(it) == recordKey(record)
        }

        quizRecords.add(record)
    }

    // LOAD QUIZZES AND RESTORE ATTEMPTS FROM FIRESTORE
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
                                    document.getString("subjectName")
                                        ?.trim()
                                        .orEmpty()

                                val semester =
                                    document.getString("semester")
                                        ?.trim()
                                        .orEmpty()

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

                                val firebaseStatus =
                                    document.getString("status")
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
                                    document.getString("scheduledDate")
                                        .orEmpty()

                                val scheduledTime =
                                    document.getString("scheduledTime")
                                        .orEmpty()

                                val durationMinutes =
                                    (document.get("durationMinutes") as? Number)
                                        ?.toInt() ?: 0

                                val questions =
                                    parseQuestions(document.get("questions"))

                                val quiz = Quiz(
                                    quizId = quizId,
                                    title = document.getString("title")
                                        ?: "Untitled Quiz",
                                    durationMinutes = durationMinutes,
                                    questions = questions,
                                    shuffleOptions =
                                        document.getBoolean("shuffleOptions")
                                            ?: false
                                )

                                val recordStatus =
                                    if (
                                        isScheduleNotExpired(
                                            scheduledDate,
                                            scheduledTime,
                                            durationMinutes
                                        )
                                    ) {
                                        StudentQuizStatus.UPCOMING
                                    } else {
                                        StudentQuizStatus.UNATTEMPTED
                                    }

                                loadedRecords.add(
                                    StudentQuizRecord(
                                        quizId = quizId,
                                        subjectName = subjectName,
                                        unitName = document.getString("unitName")
                                            ?: "",
                                        status = recordStatus,
                                        quiz = quiz,
                                        scheduledDate = scheduledDate,
                                        scheduledTime = scheduledTime,
                                        totalMarks =
                                            (document.get("totalMarks") as? Number)
                                                ?.toInt(),
                                        teacherId = enrollment.teacherId,
                                        classId = enrollment.classId
                                    )
                                )
                            }

                            completedRequests++

                            if (
                                completedRequests == uniqueEnrollments.size
                            ) {
                                loadSavedAttempts(
                                    loadedRecords,
                                    studentUid,
                                    onSuccess,
                                    onFailure
                                )
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

    // CHECK EACH QUIZ FOR A PREVIOUSLY SAVED ATTEMPT
    private fun loadSavedAttempts(
        records: List<StudentQuizRecord>,
        studentUid: String,
        onSuccess: (List<StudentQuizRecord>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (records.isEmpty()) {
            quizRecords.clear()
            onSuccess(emptyList())
            return
        }

        val restoredRecords =
            mutableListOf<StudentQuizRecord>()

        var completedReads = 0
        var failed = false

        records.distinctBy { recordKey(it) }.forEach { record ->

            quizReference(record)
                .collection("attempts")
                .document(studentUid)
                .get()
                .addOnSuccessListener { attemptSnapshot ->

                    if (failed) return@addOnSuccessListener

                    val restoredRecord =
                        if (attemptSnapshot.exists()) {

                            record.copy(
                                status = StudentQuizStatus.ATTEMPTED,
                                marksObtained =
                                    (attemptSnapshot.get("marksObtained")
                                            as? Number)?.toInt(),
                                totalMarks =
                                    (attemptSnapshot.get("totalMarks")
                                            as? Number)?.toInt()
                                        ?: record.totalMarks
                            )

                        } else {
                            record
                        }

                    restoredRecords.add(restoredRecord)

                    completedReads++

                    if (completedReads == records.distinctBy {
                            recordKey(it)
                        }.size
                    ) {
                        quizRecords.clear()
                        quizRecords.addAll(restoredRecords)

                        onSuccess(quizRecords.toList())
                    }
                }
                .addOnFailureListener { exception ->

                    if (!failed) {
                        failed = true

                        onFailure(
                            exception.message
                                ?: "Unable to restore saved quiz attempts."
                        )
                    }
                }
        }
    }

    private fun parseQuestions(data: Any?): List<Question> {

        val questionList = data as? List<*> ?: return emptyList()

        return questionList.mapNotNull { item ->

            val questionMap = item as? Map<*, *>
                ?: return@mapNotNull null

            val questionText =
                questionMap["questionText"] as? String
                    ?: return@mapNotNull null

            val options =
                (questionMap["options"] as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?: emptyList()

            if (options.isEmpty()) {
                return@mapNotNull null
            }

            Question(
                questionText = questionText,
                options = options,
                correctAnswerIndex = -1
            )
        }
    }

    private fun isScheduleNotExpired(
        date: String,
        time: String,
        durationMinutes: Int
    ): Boolean {

        if (date.isBlank() || time.isBlank()) {
            return true
        }

        return try {
            val formatter =
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

            formatter.isLenient = false

            val scheduledStart =
                formatter.parse("$date $time") ?: return true

            val scheduledEndMillis =
                scheduledStart.time +
                        durationMinutes.coerceAtLeast(0) * 60_000L

            System.currentTimeMillis() <= scheduledEndMillis

        } catch (exception: Exception) {
            true
        }
    }

    fun setQuizRecords(records: List<StudentQuizRecord>) {
        quizRecords.clear()
        quizRecords.addAll(records)
    }
}
