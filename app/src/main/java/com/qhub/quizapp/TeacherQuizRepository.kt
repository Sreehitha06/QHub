
package com.qhub.quizapp

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object TeacherQuizRepository {

    private val db = FirebaseFirestore.getInstance()

    fun saveQuiz(
        teacherId: String,
        classId: String,
        className: String,
        title: String,
        questions: List<QHubQuizQuestionDraft>,
        durationMinutes: Int,
        scheduledDate: String,
        scheduledTime: String,
        shuffleQuestions: Boolean,
        shuffleOptions: Boolean,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit,
        subjectName: String = "",
        semester: String = ""
    ) {
        if (questions.isEmpty()) {
            onFailure("Please add at least one question.")
            return
        }

        db.collection("studentRoster")
            .whereEqualTo("teacherId", teacherId)
            .whereEqualTo("classId", classId)
            .get()
            .addOnSuccessListener { rosterSnapshot ->

                if (rosterSnapshot.isEmpty) {
                    onFailure("No CSV roster found for this class.")
                    return@addOnSuccessListener
                }

                // Get all distinct subject and semester combinations.
                val availableEnrollments = rosterSnapshot.documents
                    .mapNotNull { document ->
                        val subject = document.getString("subjectName")
                            ?.trim()
                            .orEmpty()

                        val semesterValue = document.getString("semester")
                            ?.trim()
                            .orEmpty()

                        if (subject.isBlank() || semesterValue.isBlank()) {
                            null
                        } else {
                            subject to semesterValue
                        }
                    }
                    .distinct()

                if (availableEnrollments.isEmpty()) {
                    onFailure(
                        "Subject or semester information is missing in the CSV roster."
                    )
                    return@addOnSuccessListener
                }

                // Use the explicitly selected subject and semester when supplied.
                // Otherwise, proceed only if the class has exactly one combination.
                val selectedEnrollment =
                    if (subjectName.isNotBlank() && semester.isNotBlank()) {
                        availableEnrollments.firstOrNull {
                            it.first.equals(subjectName.trim(), ignoreCase = true) &&
                                    it.second.equals(semester.trim(), ignoreCase = true)
                        }
                    } else if (availableEnrollments.size == 1) {
                        availableEnrollments.first()
                    } else {
                        null
                    }

                if (selectedEnrollment == null) {
                    onFailure(
                        "This class has multiple subjects or semesters. " +
                                "Please select the correct subject and semester before creating the quiz."
                    )
                    return@addOnSuccessListener
                }

                val selectedSubject = selectedEnrollment.first
                val selectedSemester = selectedEnrollment.second

                val quizCollection = db.collection("teachers")
                    .document(teacherId)
                    .collection("classes")
                    .document(classId)
                    .collection("quizzes")

                val quizDocument = quizCollection.document()

                // Student-readable questions contain no correct answers.
                val questionData = questions.mapIndexed { index, question ->
                    hashMapOf(
                        "questionNumber" to index + 1,
                        "questionText" to question.question.trim(),
                        "options" to question.options.map { it.trim() }
                    )
                }

                // Correct answers are stored separately.
                val answerKey = hashMapOf(
                    "correctAnswerIndexes" to
                            questions.map { it.correctAnswerIndex }
                )

                val quizData = hashMapOf(
                    "quizId" to quizDocument.id,
                    "title" to title.trim(),
                    "classId" to classId,
                    "className" to className,
                    "teacherId" to teacherId,
                    "subjectName" to selectedSubject,
                    "semester" to selectedSemester,
                    "questions" to questionData,
                    "totalQuestions" to questions.size,
                    "totalMarks" to questions.size,
                    "durationMinutes" to durationMinutes,
                    "scheduledDate" to scheduledDate,
                    "scheduledTime" to scheduledTime,
                    "shuffleQuestions" to shuffleQuestions,
                    "shuffleOptions" to shuffleOptions,
                    "status" to "SCHEDULED",
                    "createdAt" to FieldValue.serverTimestamp()
                )

                val batch = db.batch()

                batch.set(quizDocument, quizData)

                batch.set(
                    quizDocument.collection("answerKeys")
                        .document("private"),
                    answerKey
                )

                batch.commit()
                    .addOnSuccessListener {
                        onSuccess(quizDocument.id)
                    }
                    .addOnFailureListener { exception ->
                        onFailure(
                            exception.localizedMessage
                                ?: "Could not save quiz."
                        )
                    }
            }
            .addOnFailureListener { exception ->
                onFailure(
                    exception.localizedMessage
                        ?: "Could not retrieve class roster."
                )
            }
    }

    fun updateQuiz(
        teacherId: String,
        classId: String,
        quizId: String,
        title: String,
        questions: List<QHubQuizQuestionDraft>,
        durationMinutes: Int,
        scheduledDate: String,
        scheduledTime: String,
        shuffleQuestions: Boolean,
        shuffleOptions: Boolean,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (questions.isEmpty()) {
            onFailure("Please add at least one question.")
            return
        }

        val quizDocument = db.collection("teachers")
            .document(teacherId)
            .collection("classes")
            .document(classId)
            .collection("quizzes")
            .document(quizId)

        val questionData = questions.mapIndexed { index, question ->
            hashMapOf(
                "questionNumber" to index + 1,
                "questionText" to question.question.trim(),
                "options" to question.options.map { it.trim() }
            )
        }

        val answerKey = hashMapOf(
            "correctAnswerIndexes" to
                    questions.map { it.correctAnswerIndex }
        )

        val updatedQuizData = hashMapOf<String, Any>(
            "title" to title.trim(),
            "questions" to questionData,
            "totalQuestions" to questions.size,
            "totalMarks" to questions.size,
            "durationMinutes" to durationMinutes,
            "scheduledDate" to scheduledDate,
            "scheduledTime" to scheduledTime,
            "shuffleQuestions" to shuffleQuestions,
            "shuffleOptions" to shuffleOptions,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val batch = db.batch()

        // Preserve the existing class, subject, semester and teacher metadata.
        batch.update(quizDocument, updatedQuizData)

        // Update only the private answer key.
        batch.set(
            quizDocument.collection("answerKeys")
                .document("private"),
            answerKey
        )

        batch.commit()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(
                    exception.localizedMessage
                        ?: "Could not update quiz."
                )
            }
    }
}
