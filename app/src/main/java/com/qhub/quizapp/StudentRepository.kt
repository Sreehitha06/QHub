
package com.qhub.quizapp

import com.google.firebase.firestore.FirebaseFirestore

class StudentRepository {

    private val db = FirebaseFirestore.getInstance()

    // Find subjects assigned to the student's email
    fun findSubjects(
        email: String,
        onSuccess: (List<String>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val normalizedEmail = email.trim().lowercase()

        db.collection("studentRoster")
            .whereEqualTo("email", normalizedEmail)
            .get()
            .addOnSuccessListener { documents ->

                val subjects = documents
                    .mapNotNull { document ->
                        document.getString("subjectName")
                    }
                    .distinct()

                onSuccess(subjects)
            }
            .addOnFailureListener { exception ->
                onFailure(
                    exception.message ?: "Unable to find subjects."
                )
            }
    }

    // Verify the student against the teacher's roster and save details
    fun verifyAndSaveStudent(
        uid: String,
        name: String,
        rollNumber: String,
        email: String,
        department: String,
        section: String,
        semester: String,
        onSuccess: (List<String>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val normalizedEmail = email.trim().lowercase()

        val studentData = hashMapOf<String, Any>(
            "uid" to uid,
            "email" to normalizedEmail,
            "name" to name.trim(),
            "rollNumber" to rollNumber.trim(),
            "department" to department.trim(),
            "section" to section.trim(),
            "semester" to semester.trim(),
            "role" to "Student"
        )

        // Check whether this email exists in the uploaded roster
        db.collection("studentRoster")
            .whereEqualTo("email", normalizedEmail)
            .get()
            .addOnSuccessListener { documents ->

                if (documents.isEmpty) {
                    onFailure(
                        "No classrooms were found for this email ID."
                    )
                    return@addOnSuccessListener
                }

                val subjects = documents
                    .mapNotNull { document ->
                        document.getString("subjectName")
                    }
                    .distinct()

                studentData["subjects"] = subjects

                // Save student details using Firebase UID
                db.collection("students")
                    .document(uid)
                    .set(studentData)
                    .addOnSuccessListener {
                        onSuccess(subjects)
                    }
                    .addOnFailureListener { exception ->
                        onFailure(
                            exception.message
                                ?: "Unable to save student details."
                        )
                    }
            }
            .addOnFailureListener { exception ->
                onFailure(
                    exception.message
                        ?: "Unable to verify student email."
                )
            }
    }
}
