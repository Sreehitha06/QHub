
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

    // Link the logged-in Firebase UID to every matching roster entry.
    private fun linkStudentToRoster(
        documents: List<com.google.firebase.firestore.DocumentSnapshot>,
        uid: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val batch = db.batch()

        for (document in documents) {
            val existingUid = document.getString("studentUid")

            if (!existingUid.isNullOrBlank() && existingUid != uid) {
                onFailure(
                    "A roster entry is already linked to another student account. Please contact your teacher."
                )
                return
            }

            if (existingUid.isNullOrBlank()) {
                batch.update(document.reference, "studentUid", uid)
            }
        }

        batch.commit()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(
                    exception.localizedMessage
                        ?: "Unable to link your account to the student roster."
                )
            }
    }

    fun getVerifiedStudentEnrollments(
        onSuccess: (List<StudentEnrollment>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            onFailure("Please log in again.")
            return
        }

        val uid = currentUser.uid
        val studentEmail = currentUser.email
            ?.trim()
            ?.lowercase()
            .orEmpty()

        if (studentEmail.isBlank()) {
            onFailure("Student email ID could not be found.")
            return
        }

        db.collection("studentRoster")
            .whereEqualTo("email", studentEmail)
            .get()
            .addOnSuccessListener { documents ->

                if (documents.isEmpty) {
                    onSuccess(emptyList())
                    return@addOnSuccessListener
                }

                linkStudentToRoster(
                    documents = documents.documents,
                    uid = uid,
                    onSuccess = {

                        val enrollments = documents.mapNotNull { document ->

                            val subjectName =
                                document.getString("subjectName")
                                    ?.trim()
                                    .orEmpty()

                            val teacherId =
                                document.getString("teacherId")
                                    ?.trim()
                                    .orEmpty()

                            val classId =
                                document.getString("classId")
                                    ?.trim()
                                    .orEmpty()

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
                        }.distinctBy {
                            "${it.teacherId}|${it.classId}|${it.subjectName.lowercase()}|${it.semester.lowercase()}"
                        }.sortedWith(
                            compareBy<StudentEnrollment>(
                                { it.semester },
                                { it.subjectName }
                            )
                        )

                        onSuccess(enrollments)
                    },
                    onFailure = onFailure
                )
            }
            .addOnFailureListener { exception ->
                onFailure(
                    exception.localizedMessage
                        ?: "Unable to load your assigned subjects."
                )
            }
    }

    fun findSubjects(
        email: String,
        onSuccess: (List<String>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val normalizedEmail = email.trim().lowercase()

        if (normalizedEmail.isBlank()) {
            onFailure("Student email ID is missing.")
            return
        }

        db.collection("studentRoster")
            .whereEqualTo("email", normalizedEmail)
            .get()
            .addOnSuccessListener { documents ->

                val subjects = documents.mapNotNull { document ->
                    document.getString("subjectName")
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                }.distinct()

                onSuccess(subjects)
            }
            .addOnFailureListener { exception ->
                onFailure(
                    exception.localizedMessage ?: "Unable to find subjects."
                )
            }
    }

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

        if (normalizedEmail.isBlank()) {
            onFailure("Student email ID is missing.")
            return
        }

        db.collection("studentRoster")
            .whereEqualTo("email", normalizedEmail)
            .get()
            .addOnSuccessListener { documents ->

                if (documents.isEmpty) {
                    onFailure("No classrooms were found for this email ID.")
                    return@addOnSuccessListener
                }

                val studentData = hashMapOf<String, Any>(
                    "uid" to uid,
                    "email" to normalizedEmail,
                    "name" to name.trim(),
                    "rollNumber" to rollNumber.trim(),
                    "department" to department.trim(),
                    "section" to section.trim(),
                    "semester" to semester.trim(),
                    "subjects" to documents.mapNotNull {
                        it.getString("subjectName")?.trim()
                            ?.takeIf { subject -> subject.isNotBlank() }
                    }.distinct(),
                    "role" to "Student"
                )

                val studentDocument = db.collection("students")
                    .document(uid)

                // Link roster records and save the profile together.
                val batch = db.batch()

                for (document in documents.documents) {
                    val existingUid = document.getString("studentUid")

                    if (!existingUid.isNullOrBlank() && existingUid != uid) {
                        onFailure(
                            "A roster entry is already linked to another student account."
                        )
                        return@addOnSuccessListener
                    }

                    if (existingUid.isNullOrBlank()) {
                        batch.update(document.reference, "studentUid", uid)
                    }
                }

                batch.set(studentDocument, studentData)

                batch.commit()
                    .addOnSuccessListener {
                        onSuccess(studentData["subjects"] as List<String>)
                    }
                    .addOnFailureListener { exception ->
                        onFailure(
                            exception.localizedMessage
                                ?: "Unable to save student details."
                        )
                    }
            }
            .addOnFailureListener { exception ->
                onFailure(
                    exception.localizedMessage
                        ?: "Unable to check the teacher's student roster."
                )
            }
    }

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
                    exception.localizedMessage
                        ?: "Unable to load student details."
                )
            }
    }

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
                    exception.localizedMessage
                        ?: "Unable to update student details."
                )
            }
    }
}
