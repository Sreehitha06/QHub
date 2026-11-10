
package com.qhub.quizapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
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
    val status: String
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
                                durationMinutes = document.getLong("durationMinutes") ?: 0L,
                                scheduledDate = document.getString("scheduledDate") ?: "",
                                scheduledTime = document.getString("scheduledTime") ?: "",
                                totalQuestions = document.getLong("totalQuestions") ?: 0L,
                                totalMarks = document.getLong("totalMarks") ?: 0L,
                                status = document.getString("status") ?: "UNKNOWN"
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
                            }
                        }
                    }
                }
            }
        }
    }
}
