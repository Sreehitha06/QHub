
package com.qhub.quizapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

private data class ManagedQuizItem(
    val id: String,
    val title: String,
    val durationMinutes: Long,
    val scheduledDate: String,
    val scheduledTime: String,
    val totalQuestions: Long,
    val totalMarks: Long,
    val status: String,
    val subjectName: String,
    val semester: String
)

private data class ManagedStudent(
    val id: String,
    val name: String,
    val rollNumber: String,
    val email: String,
    val studentUid: String
)

private data class ManagedAttempt(
    val studentUid: String,
    val studentEmail: String,
    val marksObtained: Long,
    val totalMarks: Long,
    val correctAnswers: Long,
    val wrongAnswers: Long,
    val unanswered: Long
)

@Composable
fun ManageQuizzesScreen(
    teacherId: String,
    classId: String,
    className: String,
    onBack: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    var quizzes by remember {
        mutableStateOf<List<ManagedQuizItem>>(emptyList())
    }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var expandedQuizId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(teacherId, classId) {
        var registration: ListenerRegistration? = null

        if (teacherId.isBlank() || classId.isBlank()) {
            isLoading = false
            errorMessage = "Invalid teacher or class."
        } else {
            registration = db.collection("teachers")
                .document(teacherId)
                .collection("classes")
                .document(classId)
                .collection("quizzes")
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        isLoading = false
                        errorMessage = error.localizedMessage
                            ?: "Could not load quizzes."
                    } else if (snapshot != null) {

                        quizzes = snapshot.documents.map { document ->
                            ManagedQuizItem(
                                id = document.id,
                                title = document.getString("title")
                                    ?: "Untitled Quiz",
                                durationMinutes =
                                    document.getLong("durationMinutes") ?: 0L,
                                scheduledDate =
                                    document.getString("scheduledDate") ?: "",
                                scheduledTime =
                                    document.getString("scheduledTime") ?: "",
                                totalQuestions =
                                    document.getLong("totalQuestions") ?: 0L,
                                totalMarks =
                                    document.getLong("totalMarks") ?: 0L,
                                status =
                                    document.getString("status") ?: "UNKNOWN",
                                subjectName =
                                    document.getString("subjectName") ?: "",
                                semester =
                                    document.getString("semester") ?: ""
                            )
                        }

                        isLoading = false
                        errorMessage = null
                    }
                }
        }

        onDispose {
            registration?.remove()
        }
    }

    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Column {
                Text(
                    "Manage Quizzes",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    className,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage != null -> {
                Text(
                    errorMessage!!,
                    color = colors.error
                )
            }

            quizzes.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Assessment,
                            contentDescription = null,
                            modifier = Modifier.size(55.dp),
                            tint = colors.onSurfaceVariant
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "No quizzes yet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            "Quizzes you create for this class will appear here.",
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                Text(
                    "Created Quizzes (${quizzes.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = quizzes,
                        key = { it.id }
                    ) { quiz ->

                        val isExpanded = expandedQuizId == quiz.id

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(3.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp)
                            ) {
                                Text(
                                    quiz.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(Modifier.height(10.dp))

                                Text("Subject: ${quiz.subjectName}")
                                Text("Semester: ${quiz.semester}")
                                Text("Date: ${quiz.scheduledDate}")
                                Text("Time: ${quiz.scheduledTime}")
                                Text("Duration: ${quiz.durationMinutes} minutes")
                                Text("Questions: ${quiz.totalQuestions}")
                                Text("Total Marks: ${quiz.totalMarks}")

                                Spacer(Modifier.height(12.dp))

                                Surface(
                                    color = colors.secondaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        quiz.status,
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 6.dp
                                        ),
                                        color = colors.onSecondaryContainer,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }

                                Spacer(Modifier.height(14.dp))

                                HorizontalDivider()

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedQuizId =
                                                if (isExpanded) null else quiz.id
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement =
                                        Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "View Student Results",
                                        color = colors.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Icon(
                                        imageVector = if (isExpanded)
                                            Icons.Default.ExpandLess
                                        else
                                            Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = colors.primary
                                    )
                                }

                                if (isExpanded) {
                                    HorizontalDivider()

                                    QuizStudentResults(
                                        teacherId = teacherId,
                                        classId = classId,
                                        quiz = quiz
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizStudentResults(
    teacherId: String,
    classId: String,
    quiz: ManagedQuizItem
) {
    val db = FirebaseFirestore.getInstance()
    val colors = MaterialTheme.colorScheme

    var students by remember(quiz.id) {
        mutableStateOf<List<ManagedStudent>>(emptyList())
    }

    var attempts by remember(quiz.id) {
        mutableStateOf<List<ManagedAttempt>>(emptyList())
    }

    var isLoading by remember(quiz.id) {
        mutableStateOf(true)
    }

    var errorMessage by remember(quiz.id) {
        mutableStateOf<String?>(null)
    }

    var expandedStudentId by remember(quiz.id) {
        mutableStateOf<String?>(null)
    }

    DisposableEffect(teacherId, classId, quiz.id) {
        var rosterRegistration: ListenerRegistration? = null
        var attemptsRegistration: ListenerRegistration? = null

        val rosterQuery = db.collection("studentRoster")
            .whereEqualTo("teacherId", teacherId)
            .whereEqualTo("classId", classId)

        rosterRegistration = rosterQuery.addSnapshotListener { snapshot, error ->

            if (error != null) {
                errorMessage = error.localizedMessage
                    ?: "Could not load students."
                isLoading = false
            } else if (snapshot != null) {

                students = snapshot.documents
                    .filter { document ->
                        val subjectMatches =
                            document.getString("subjectName")
                                ?.equals(quiz.subjectName, ignoreCase = true)
                                ?: false

                        val semesterMatches =
                            quiz.semester.isBlank() ||
                                    document.getString("semester")
                                        ?.equals(quiz.semester, ignoreCase = true)
                                    ?: false

                        subjectMatches && semesterMatches
                    }
                    .map { document ->
                        ManagedStudent(
                            id = document.id,
                            name = document.getString("name")
                                ?: "Unknown Student",
                            rollNumber = document.getString("rollNumber")
                                ?: "",
                            email = document.getString("email")
                                ?: "",
                            studentUid = document.getString("studentUid")
                                ?: ""
                        )
                    }
                    .distinctBy { it.id }

                isLoading = false
                errorMessage = null
            }
        }

        attemptsRegistration = db.collection("teachers")
            .document(teacherId)
            .collection("classes")
            .document(classId)
            .collection("quizzes")
            .document(quiz.id)
            .collection("attempts")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    errorMessage = error.localizedMessage
                        ?: "Could not load student marks."
                } else if (snapshot != null) {

                    attempts = snapshot.documents.map { document ->
                        ManagedAttempt(
                            studentUid = document.getString("studentUid")
                                ?: document.id,
                            studentEmail = document.getString("studentEmail")
                                ?: "",
                            marksObtained =
                                document.getLong("marksObtained") ?: 0L,
                            totalMarks =
                                document.getLong("totalMarks") ?: quiz.totalMarks,
                            correctAnswers =
                                document.getLong("correctAnswers") ?: 0L,
                            wrongAnswers =
                                document.getLong("wrongAnswers") ?: 0L,
                            unanswered =
                                document.getLong("unanswered") ?: 0L
                        )
                    }
                }
            }

        onDispose {
            rosterRegistration?.remove()
            attemptsRegistration?.remove()
        }
    }

    Spacer(Modifier.height(8.dp))

    when {
        isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        errorMessage != null -> {
            Text(
                errorMessage!!,
                color = colors.error,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }

        students.isEmpty() -> {
            Text(
                "No students found for this subject and semester.",
                modifier = Modifier.padding(vertical = 12.dp),
                color = colors.onSurfaceVariant
            )
        }

        else -> {
            val attemptedCount = students.count { student ->
                attempts.any { attempt ->
                    (
                            student.studentUid.isNotBlank() &&
                                    attempt.studentUid == student.studentUid
                            ) || (
                            student.email.isNotBlank() &&
                                    attempt.studentEmail.equals(
                                        student.email,
                                        ignoreCase = true
                                    )
                            )
                }
            }

            Text(
                "Student Performance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Text(
                "Attempted: $attemptedCount / ${students.size}",
                color = colors.primary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(10.dp))

            students.forEach { student ->

                val attempt = attempts.firstOrNull { result ->
                    (
                            student.studentUid.isNotBlank() &&
                                    result.studentUid == student.studentUid
                            ) || (
                            student.email.isNotBlank() &&
                                    result.studentEmail.equals(
                                        student.email,
                                        ignoreCase = true
                                    )
                            )
                }

                val isStudentExpanded =
                    expandedStudentId == student.id

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.surfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (attempt != null) {
                                    expandedStudentId =
                                        if (isStudentExpanded) null
                                        else student.id
                                }
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    student.name,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    "Roll No: ${student.rollNumber}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                if (attempt != null) {
                                    Text(
                                        "${attempt.marksObtained}/${attempt.totalMarks}",
                                        color = colors.primary,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        "Attempted",
                                        color = colors.primary,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                } else {
                                    Text(
                                        "Not Attempted",
                                        color = colors.error,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }

                        if (attempt != null && isStudentExpanded) {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(10.dp))

                            Text(
                                "Individual Result",
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                "Marks Obtained: ${attempt.marksObtained}/${attempt.totalMarks}"
                            )

                            Text(
                                "Correct Answers: ${attempt.correctAnswers}"
                            )

                            Text(
                                "Wrong Answers: ${attempt.wrongAnswers}"
                            )

                            Text(
                                "Unanswered: ${attempt.unanswered}"
                            )
                        }

                        if (attempt != null && !isStudentExpanded) {
                            Text(
                                "Tap to view result details",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
