package com.qhub.quizapp

import com.google.firebase.auth.FirebaseAuth
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

// One enrollment represents one teacher's subject/class assignment.
data class StudentEnrollment(
    val rosterId: String = "",
    val name: String = "",
    val email: String = "",
    val rollNumber: String = "",
    val department: String = "",
    val section: String = "",
    val subjectName: String = "",
    val semester: String = "",
    val classId: String = "",
    val className: String = "",
    val teacherId: String = "",
    val teacherEmail: String = ""
)

class StudentRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Fetch every subject/class enrollment belonging to the
     * currently logged-in student using their verified Firebase email.
     */
    fun getVerifiedStudentEnrollments(
        onSuccess: (List<StudentEnrollment>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            onFailure("Please log in again.")
            return
        }

        if (!currentUser.isEmailVerified) {
            onFailure("Please verify your email before viewing your subjects.")
            return
        }

        val verifiedEmail = currentUser.email
            ?.trim()
            ?.lowercase()
            .orEmpty()

        if (verifiedEmail.isBlank()) {
            onFailure("Your verified email ID could not be found.")
            return
        }

        db.collection("studentRoster")
            .whereEqualTo("email", verifiedEmail)
            .get()
            .addOnSuccessListener { documents ->

                val enrollments = documents.mapNotNull { document ->

                    val subjectName =
                        document.getString("subjectName")?.trim().orEmpty()

                    val teacherId =
                        document.getString("teacherId")?.trim().orEmpty()

                    val classId =
                        document.getString("classId")?.trim().orEmpty()

                    // Ignore incomplete roster records.
                    if (
                        subjectName.isBlank() ||
                        teacherId.isBlank() ||
                        classId.isBlank()
                    ) {
                        return@mapNotNull null
                    }

                    StudentEnrollment(
                        rosterId = document.id,
                        name = document.getString("name") ?: "",
                        email = document.getString("email") ?: "",
                        rollNumber = document.getString("rollNumber") ?: "",
                        department = document.getString("department") ?: "",
                        section = document.getString("section") ?: "",
                        subjectName = subjectName,
                        semester = document.getString("semester") ?: "",
                        classId = classId,
                        className = document.getString("className") ?: "",
                        teacherId = teacherId,
                        teacherEmail = document.getString("teacherEmail") ?: ""
                    )
                }.distinctBy { enrollment ->
                    "${enrollment.teacherId}|" +
                            "${enrollment.classId}|" +
                            "${enrollment.subjectName.lowercase()}|" +
                            enrollment.semester.lowercase()
                }.sortedWith(
                    compareBy<StudentEnrollment>(
                        { it.semester },
                        { it.subjectName }
                    )
                )

                if (enrollments.isEmpty()) {
                    onSuccess(emptyList())
                } else {
                    onSuccess(enrollments)
                }
            }
            .addOnFailureListener { exception ->
                onFailure(
                    exception.message
                        ?: "Unable to load your assigned subjects."
                )
            }
    }

    // Find subjects assigned to a particular email.
    // Retained for compatibility with existing callers.
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

                val subjects = documents.mapNotNull { document ->
                    document.getString("subjectName")
                }.distinct()

                onSuccess(subjects)
            }
            .addOnFailureListener { exception ->
                onFailure(
                    exception.message ?: "Unable to find subjects."
                )
            }
    }

    // Verify student against teacher's roster and save details.
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

                val subjects = documents.mapNotNull { document ->
                    document.getString("subjectName")
                }.distinct()

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

    // Get saved student details.
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

    // Update student details without changing email or subjects.
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