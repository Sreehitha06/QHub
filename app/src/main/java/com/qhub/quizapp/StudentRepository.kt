
package com.qhub.quizapp

import com.google.firebase.firestore.FirebaseFirestore

data class StudentDetails(
    val uid: String = "",
    val name: String = "",
    val rollNumber: String = "",
    val email: String = "",
    val department: String = "",
    val section: String = "",
    val semester: String = "",
    val subjects: List<String> = emptyList()
)

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

    // Verify student against teacher's roster and save details
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

    // Get saved student details
    fun getStudentDetails(
        uid: String,
        onSuccess: (StudentDetails) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("students")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    onFailure("Student details not found.")
                    return@addOnSuccessListener
                }

                val subjects = (document.get("subjects") as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?: emptyList()

                val details = StudentDetails(
                    uid = document.getString("uid") ?: uid,
                    name = document.getString("name") ?: "",
                    rollNumber = document.getString("rollNumber") ?: "",
                    email = document.getString("email") ?: "",
                    department = document.getString("department") ?: "",
                    section = document.getString("section") ?: "",
                    semester = document.getString("semester") ?: "",
                    subjects = subjects
                )

                onSuccess(details)
            }
            .addOnFailureListener { exception ->
                onFailure(
                    exception.message
                        ?: "Unable to load student details."
                )
            }
    }

    // Update student details without changing email or subjects
    fun updateStudentDetails(
        uid: String,
        name: String,
        rollNumber: String,
        department: String,
        section: String,
        semester: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val updatedData = mapOf(
            "name" to name.trim(),
            "rollNumber" to rollNumber.trim(),
            "department" to department.trim(),
            "section" to section.trim(),
            "semester" to semester.trim()
        )

        db.collection("students")
            .document(uid)
            .update(updatedData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(
                    exception.message
                        ?: "Unable to update student details."
                )
            }
    }
}
