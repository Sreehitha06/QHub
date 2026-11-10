
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
        onFailure: (String) -> Unit
    ) {
        db.collection("studentRoster")
            .whereEqualTo("teacherId", teacherId)
            .whereEqualTo("classId", classId)
            .limit(1)
            .get()
            .addOnSuccessListener { rosterSnapshot ->

                if (rosterSnapshot.isEmpty) {
                    onFailure("No CSV roster found for this class.")
                    return@addOnSuccessListener
                }

                val roster = rosterSnapshot.documents.first()

                val subjectName =
                    roster.getString("subjectName") ?: ""

                val semester =
                    roster.getString("semester") ?: ""

                if (subjectName.isBlank() || semester.isBlank()) {
                    onFailure("Subject or semester information is missing.")
                    return@addOnSuccessListener
                }

                val quizCollection = db.collection("teachers")
                    .document(teacherId)
                    .collection("classes")
                    .document(classId)
                    .collection("quizzes")

                val quizDocument = quizCollection.document()

                // Student-readable questions: NO correct answers here.
                val questionData = questions.mapIndexed { index, question ->
                    hashMapOf(
                        "questionNumber" to index + 1,
                        "questionText" to question.question.trim(),
                        "options" to question.options.map { it.trim() }
                    )
                }

                // Teacher-only answer key.
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
                    "subjectName" to subjectName,
                    "semester" to semester,
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
}
