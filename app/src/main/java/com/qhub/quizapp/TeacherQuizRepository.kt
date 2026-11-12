
package com.qhub.quizapp

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object TeacherQuizRepository {

    private val db = FirebaseFirestore.getInstance()

    fun saveQuiz(
        teacherId: String,
        classId: String,
        className: String,
        subjectName: String,
        semester: String,
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
            .whereEqualTo("subjectName", subjectName)
            .whereEqualTo("semester", semester)
            .get()
            .addOnSuccessListener { rosterSnapshot ->

                if (rosterSnapshot.isEmpty) {
                    onFailure(
                        "No student roster found for $subjectName, Semester $semester."
                    )
                    return@addOnSuccessListener
                }

                // Collect Firebase UIDs of students linked to this roster.
                val allowedStudentUids = rosterSnapshot.documents
                    .mapNotNull { document ->
                        document.getString("studentUid")
                            ?.takeIf { it.isNotBlank() }
                    }
                    .distinct()

                if (allowedStudentUids.isEmpty()) {
                    onFailure(
                        "No students from this subject and semester have linked " +
                                "their QHub accounts yet. Ask at least one student to log in first."
                    )
                    return@addOnSuccessListener
                }

                val quizCollection = db.collection("teachers")
                    .document(teacherId)
                    .collection("classes")
                    .document(classId)
                    .collection("quizzes")

                val quizDocument = quizCollection.document()

                // Public quiz questions: correct answers are NOT included.
                val questionData = questions.mapIndexed { index, question ->
                    hashMapOf(
                        "questionNumber" to index + 1,
                        "questionText" to question.question.trim(),
                        "options" to question.options.map { it.trim() }
                    )
                }

                // Private answer key, readable only by the teacher.
                val answerKey = hashMapOf(
                    "correctAnswerIndexes" to questions.map {
                        it.correctAnswerIndex
                    }
                )

                val quizData = hashMapOf(
                    "quizId" to quizDocument.id,
                    "title" to title.trim(),

                    "classId" to classId,
                    "className" to className,

                    "teacherId" to teacherId,
                    "subjectName" to subjectName,
                    "semester" to semester,

                    "allowedStudentUids" to allowedStudentUids,

                    "questions" to questionData,
                    "totalQuestions" to questions.size,
                    "totalMarks" to questions.size,

                    "durationMinutes" to durationMinutes,

                    "scheduledDate" to scheduledDate,
                    "scheduledTime" to scheduledTime,

                    "shuffleQuestions" to shuffleQuestions,
                    "shuffleOptions" to shuffleOptions,

                    // Keep the existing Firestore status.
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
                        ?: "Could not retrieve student roster."
                )
            }
    }
}
