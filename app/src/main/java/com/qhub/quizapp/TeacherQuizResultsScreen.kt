
package com.qhub.quizapp

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

private data class QuizResultStudent(
    val name: String,
    val email: String,
    val rollNumber: String,
    val marksObtained: Int? = null,
    val totalMarks: Int = 0,
    val attempted: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherQuizResultsScreen(
    teacherId: String,
    classId: String,
    quizId: String,
    onBack: () -> Unit
) {
    val db = remember {
        FirebaseFirestore.getInstance()
    }

    var quizTitle by remember {
        mutableStateOf("Quiz Results")
    }

    var subjectName by remember {
        mutableStateOf("")
    }

    var semester by remember {
        mutableStateOf("")
    }

    var totalMarks by remember {
        mutableIntStateOf(0)
    }

    var students by remember {
        mutableStateOf<List<QuizResultStudent>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    fun loadResults() {
        isLoading = true
        errorMessage = ""

        val quizRef = db.collection("teachers")
            .document(teacherId)
            .collection("classes")
            .document(classId)
            .collection("quizzes")
            .document(quizId)

        quizRef.get()
            .addOnSuccessListener { quizDocument ->

                if (!quizDocument.exists()) {
                    isLoading = false
                    errorMessage = "Quiz not found."
                    return@addOnSuccessListener
                }

                quizTitle =
                    quizDocument.getString("title") ?: "Quiz Results"

                subjectName =
                    quizDocument.getString("subjectName") ?: ""

                semester =
                    quizDocument.getString("semester") ?: ""

                totalMarks =
                    (quizDocument.getLong("totalMarks") ?: 0L).toInt()

                if (subjectName.isBlank() || semester.isBlank()) {
                    isLoading = false
                    errorMessage =
                        "Quiz subject or semester information is missing."
                    return@addOnSuccessListener
                }

                db.collection("studentRoster")
                    .whereEqualTo("teacherId", teacherId)
                    .whereEqualTo("classId", classId)
                    .get()
                    .addOnSuccessListener { rosterSnapshot ->

                        val matchingRoster =
                            rosterSnapshot.documents.filter { document ->

                                document.getString("subjectName")
                                    .orEmpty()
                                    .trim()
                                    .equals(
                                        subjectName.trim(),
                                        ignoreCase = true
                                    ) &&
                                        document.getString("semester")
                                            .orEmpty()
                                            .trim()
                                            .equals(
                                                semester.trim(),
                                                ignoreCase = true
                                            )
                            }

                        val attemptsRef =
                            quizRef.collection("attempts")

                        attemptsRef.get()
                            .addOnSuccessListener { attemptsSnapshot ->

                                val attemptsByEmail =
                                    attemptsSnapshot.documents.associateBy {
                                        it.getString("studentEmail")
                                            ?.trim()
                                            ?.lowercase()
                                            .orEmpty()
                                    }

                                students = matchingRoster.map { rosterDocument ->

                                    val email = rosterDocument
                                        .getString("email")
                                        .orEmpty()
                                        .trim()
                                        .lowercase()

                                    val attempt = attemptsByEmail[email]

                                    val marks = attempt
                                        ?.getLong("marksObtained")
                                        ?.toInt()

                                    val attemptTotal = attempt
                                        ?.getLong("totalMarks")
                                        ?.toInt()
                                        ?: totalMarks

                                    QuizResultStudent(
                                        name = rosterDocument.getString("name")
                                            ?: "Unknown Student",

                                        email = rosterDocument.getString("email")
                                            ?: "",

                                        rollNumber =
                                            rosterDocument.getString("rollNumber")
                                                ?: "",

                                        marksObtained = marks,
                                        totalMarks = attemptTotal,
                                        attempted = attempt != null
                                    )
                                }.sortedBy {
                                    it.rollNumber
                                }

                                isLoading = false
                            }
                            .addOnFailureListener { exception ->
                                isLoading = false
                                errorMessage =
                                    exception.localizedMessage
                                        ?: "Could not load quiz attempts."
                            }
                    }
                    .addOnFailureListener { exception ->
                        isLoading = false
                        errorMessage =
                            exception.localizedMessage
                                ?: "Could not load students."
                    }
            }
            .addOnFailureListener { exception ->
                isLoading = false
                errorMessage =
                    exception.localizedMessage
                        ?: "Could not load quiz details."
            }
    }

    LaunchedEffect(teacherId, classId, quizId) {
        loadResults()
    }

    val attemptedCount = students.count {
        it.attempted
    }

    val notAttemptedCount = students.size - attemptedCount

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Quiz Results",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { loadResults() },
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F6FA))
                .padding(padding)
                .padding(16.dp)
        ) {

            // Quiz Information

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = quizTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF173B5E)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Subject: $subjectName",
                        color = Color.DarkGray
                    )

                    Text(
                        text = "Semester: $semester",
                        color = Color.DarkGray
                    )

                    Text(
                        text = "Total Marks: $totalMarks",
                        color = Color.DarkGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Summary Cards

            if (!isLoading && errorMessage.isEmpty()) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ResultSummaryCard(
                        title = "Total",
                        count = students.size,
                        modifier = Modifier.weight(1f),
                        color = Color(0xFFE8EAF6)
                    )

                    ResultSummaryCard(
                        title = "Attempted",
                        count = attemptedCount,
                        modifier = Modifier.weight(1f),
                        color = Color(0xFFE4F5E9)
                    )

                    ResultSummaryCard(
                        title = "Not Attempted",
                        count = notAttemptedCount,
                        modifier = Modifier.weight(1f),
                        color = Color(0xFFFFE9E7)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            Text(
                text = "Student Results",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF173B5E)
            )

            Spacer(modifier = Modifier.height(10.dp))

            when {

                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                errorMessage.isNotEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { loadResults() }
                        ) {
                            Text("Try Again")
                        }
                    }
                }

                students.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No students found in this subject roster.",
                            color = Color.Gray
                        )
                    }
                }

                else -> {

                    // Horizontal scrolling table

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .horizontalScroll(rememberScrollState())
                    ) {

                        LazyColumn(
                            modifier = Modifier
                                .width(360.dp)
                                .fillMaxHeight()
                        ) {

                            // Table Header

                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Color(0xFF294D68),
                                            RoundedCornerShape(
                                                topStart = 10.dp,
                                                topEnd = 10.dp
                                            )
                                        )
                                        .padding(vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TableHeaderCell(
                                        text = "Name",
                                        width = 110.dp
                                    )

                                    TableHeaderCell(
                                        text = "Roll No.",
                                        width = 90.dp
                                    )

                                    TableHeaderCell(
                                        text = "Attempted",
                                        width = 90.dp
                                    )

                                    TableHeaderCell(
                                        text = "Marks",
                                        width = 70.dp
                                    )
                                }
                            }

                            // Student Rows

                            items(
                                items = students,
                                key = { it.email }
                            ) { student ->

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (students.indexOf(student) % 2 == 0) {
                                                Color.White
                                            } else {
                                                Color(0xFFF0F3F7)
                                            }
                                        )
                                        .padding(vertical = 15.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    TableBodyCell(
                                        text = student.name,
                                        width = 110.dp,
                                        bold = true
                                    )

                                    TableBodyCell(
                                        text = student.rollNumber,
                                        width = 90.dp
                                    )

                                    TableBodyCell(
                                        text = if (student.attempted) "Yes" else "No",
                                        width = 90.dp,
                                        color = if (student.attempted) {
                                            Color(0xFF218838)
                                        } else {
                                            Color(0xFFC62828)
                                        },
                                        bold = true
                                    )

                                    TableBodyCell(
                                        text = if (student.attempted) {
                                            "${student.marksObtained ?: 0}/${student.totalMarks}"
                                        } else {
                                            "--/${student.totalMarks}"
                                        },
                                        width = 70.dp,
                                        bold = true
                                    )
                                }

                                HorizontalDivider(
                                    color = Color(0xFFE1E5EA)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultSummaryCard(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun TableHeaderCell(
    text: String,
    width: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 5.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun TableBodyCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    color: Color = Color(0xFF263238),
    bold: Boolean = false
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 5.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = color,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 11.sp,
            maxLines = 2
        )
    }
}
