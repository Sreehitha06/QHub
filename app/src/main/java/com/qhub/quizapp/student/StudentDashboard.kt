
package com.qhub.quizapp.student

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.qhub.quizapp.StudentDetails
import com.qhub.quizapp.StudentEnrollment
import com.qhub.quizapp.StudentRepository
import com.qhub.quizapp.data.StudentQuizRepository
import com.qhub.quizapp.data.StudentQuizRecord
import com.qhub.quizapp.data.StudentQuizStatus
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

private val Cream = Color(0xFFF0EEE8)
private val Taupe = Color(0xFFB1A496)
private val BlueGray = Color(0xFF6F8B9B)
private val DeepBlue = Color(0xFF2B4C63)
private val White = Color(0xFFFFFFFF)

/**
 * Checks whether the current time is inside the scheduled quiz window.
 * The quiz window begins at scheduledDate + scheduledTime
 * and ends after quiz.durationMinutes.
 */
private fun isQuizWithinSchedule(record: StudentQuizRecord): Boolean {
    return try {
        val quiz = record.quiz ?: return false

        val formatter = SimpleDateFormat(
            "yyyy-MM-dd HH:mm",
            Locale.getDefault()
        ).apply {
            isLenient = false
        }

        val startTime = formatter.parse(
            "${record.scheduledDate.trim()} ${record.scheduledTime.trim()}"
        )?.time ?: return false

        val duration = quiz.durationMinutes.coerceAtLeast(0)
        val endTime = startTime + duration * 60_000L
        val now = System.currentTimeMillis()

        now >= startTime && now < endTime
    } catch (e: Exception) {
        false
    }
}

@Composable
fun StudentDashboard(
    onLogout: () -> Unit = {
        FirebaseAuth.getInstance().signOut()
    },
    onEditProfile: (StudentDetails) -> Unit = {}
) {
    val context = LocalContext.current

    var currentScreen by remember {
        mutableStateOf("dashboard")
    }

    var profileMenuExpanded by remember {
        mutableStateOf(false)
    }

    var selectedRecord by remember {
        mutableStateOf<StudentQuizRecord?>(null)
    }

    var submittedAnswers by remember {
        mutableStateOf<List<Int>>(emptyList())
    }

    var resultCorrect by remember {
        mutableIntStateOf(0)
    }

    var resultWrong by remember {
        mutableIntStateOf(0)
    }

    var resultUnanswered by remember {
        mutableIntStateOf(0)
    }

    var showQuizUnavailableDialog by remember {
        mutableStateOf(false)
    }

    var submissionError by remember {
        mutableStateOf("")
    }

    var studentQuizRecords by remember {
        mutableStateOf<List<StudentQuizRecord>>(emptyList())
    }

    var quizzesLoading by remember {
        mutableStateOf(false)
    }

    var quizzesError by remember {
        mutableStateOf("")
    }

    var studentEnrollments by remember {
        mutableStateOf<List<StudentEnrollment>>(emptyList())
    }

    var subjectsLoading by remember {
        mutableStateOf(false)
    }

    var subjectsError by remember {
        mutableStateOf("")
    }

    val studentRepository = remember {
        StudentRepository()
    }

    // Create the notification channel.
    // Permission is requested only once from MainActivity.
    LaunchedEffect(Unit) {
        QuizNotificationScheduler.createNotificationChannel(context)
    }

    fun loadStudentQuizzes() {
        quizzesLoading = true
        quizzesError = ""

        StudentQuizRepository.loadStudentQuizzes(
            onSuccess = { records ->

                studentQuizRecords = records

                // Register reminders for upcoming quizzes.
                QuizNotificationScheduler.schedule(
                    context = context,
                    records = records
                )

                quizzesLoading = false
                quizzesError = ""
            },
            onFailure = { error ->
                quizzesLoading = false
                quizzesError = error
            }
        )
    }

    LaunchedEffect(currentScreen) {

        if (currentScreen == "semester") {
            subjectsLoading = true
            subjectsError = ""

            studentRepository.getVerifiedStudentEnrollments(
                onSuccess = { enrollments ->
                    studentEnrollments = enrollments
                    subjectsLoading = false
                    subjectsError = ""
                },
                onFailure = { error ->
                    subjectsLoading = false
                    subjectsError = error
                }
            )
        }

        if (
            currentScreen == "dashboard" ||
            currentScreen == "upcoming" ||
            currentScreen == "attempted" ||
            currentScreen == "unattempted"
        ) {
            loadStudentQuizzes()
        }
    }

    // Keep checking the quiz time while the student is attempting it.
    // Return to Upcoming Quizzes when the scheduled duration expires.
    LaunchedEffect(currentScreen, selectedRecord) {
        if (currentScreen == "quiz") {
            val record = selectedRecord

            if (record == null || !isQuizWithinSchedule(record)) {
                submissionError = "The scheduled quiz time is not active."
                currentScreen = "upcoming"
            } else {
                while (currentScreen == "quiz") {
                    delay(1000)

                    if (!isQuizWithinSchedule(record)) {
                        submissionError =
                            "The scheduled quiz time has expired. Your quiz attempt can no longer continue."

                        currentScreen = "upcoming"
                        break
                    }
                }
            }
        }
    }

    when (currentScreen) {

        "profile" -> {
            StudentProfileScreen(
                onBack = {
                    currentScreen = "dashboard"
                },
                onEdit = { details ->
                    onEditProfile(details)
                }
            )
        }

        "semester" -> {
            PatternBackground {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    BackButton {
                        currentScreen = "dashboard"
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        "Semester",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepBlue
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Your Assigned Subjects",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepBlue
                    )

                    Spacer(Modifier.height(16.dp))

                    when {
                        subjectsLoading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(35.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(
                                        color = DeepBlue
                                    )

                                    Spacer(Modifier.height(12.dp))

                                    Text(
                                        "Loading your subjects...",
                                        color = BlueGray,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        subjectsError.isNotBlank() -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = White
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        subjectsError,
                                        color = Color.Red,
                                        textAlign = TextAlign.Center,
                                        fontSize = 14.sp
                                    )

                                    Spacer(Modifier.height(15.dp))

                                    Button(
                                        onClick = {
                                            currentScreen = "dashboard"
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = DeepBlue
                                        )
                                    ) {
                                        Text("Back to Dashboard")
                                    }
                                }
                            }
                        }

                        studentEnrollments.isEmpty() -> {
                            EmptyMessage(
                                "No subjects have been assigned to your verified email ID yet."
                            )
                        }

                        else -> {
                            val groupedEnrollments =
                                studentEnrollments.groupBy {
                                    it.semester.trim().ifBlank {
                                        "Semester not specified"
                                    }
                                }

                            groupedEnrollments.forEach { (semester, enrollments) ->

                                Text(
                                    if (semester == "Semester not specified") {
                                        semester
                                    } else {
                                        "Semester $semester"
                                    },
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepBlue
                                )

                                Spacer(Modifier.height(10.dp))

                                enrollments.forEach { enrollment ->

                                    val subtitleParts = buildList {
                                        if (enrollment.className.isNotBlank()) {
                                            add(enrollment.className)
                                        }

                                        if (enrollment.teacherEmail.isNotBlank()) {
                                            add("Teacher: ${enrollment.teacherEmail}")
                                        }
                                    }

                                    InfoCard(
                                        title = enrollment.subjectName,
                                        subtitle = subtitleParts.joinToString(" • ")
                                    )
                                }

                                Spacer(Modifier.height(18.dp))
                            }
                        }
                    }
                }
            }
        }

        "attempted" -> {
            if (quizzesLoading || quizzesError.isNotBlank()) {
                QuizListStatusScreen(
                    title = "Attempted Quizzes",
                    loading = quizzesLoading,
                    error = quizzesError,
                    onBack = {
                        currentScreen = "dashboard"
                    },
                    onRetry = {
                        loadStudentQuizzes()
                    }
                )
            } else {
                StudentQuizListScreen(
                    title = "Attempted Quizzes",
                    quizzes = studentQuizRecords.filter {
                        it.status == StudentQuizStatus.ATTEMPTED
                    },
                    showMarks = true,
                    onBack = {
                        currentScreen = "dashboard"
                    }
                )
            }
        }

        "unattempted" -> {
            if (quizzesLoading || quizzesError.isNotBlank()) {
                QuizListStatusScreen(
                    title = "Unattempted Quizzes",
                    loading = quizzesLoading,
                    error = quizzesError,
                    onBack = {
                        currentScreen = "dashboard"
                    },
                    onRetry = {
                        loadStudentQuizzes()
                    }
                )
            } else {
                StudentQuizListScreen(
                    title = "Unattempted Quizzes",
                    quizzes = studentQuizRecords.filter {
                        it.status == StudentQuizStatus.UNATTEMPTED
                    },
                    onBack = {
                        currentScreen = "dashboard"
                    }
                )
            }
        }

        "upcoming" -> {
            if (quizzesLoading || quizzesError.isNotBlank()) {
                QuizListStatusScreen(
                    title = "Upcoming Quizzes",
                    loading = quizzesLoading,
                    error = quizzesError,
                    onBack = {
                        currentScreen = "dashboard"
                    },
                    onRetry = {
                        loadStudentQuizzes()
                    }
                )
            } else {
                StudentQuizListScreen(
                    title = "Upcoming Quizzes",
                    quizzes = studentQuizRecords.filter {
                        it.status == StudentQuizStatus.UPCOMING
                    },
                    showSchedule = true,
                    onBack = {
                        currentScreen = "dashboard"
                    },
                    onAttemptQuiz = { record ->
                        if (record.quiz == null) {
                            showQuizUnavailableDialog = true
                        } else if (!isQuizWithinSchedule(record)) {
                            submissionError =
                                "This quiz is not currently available. You can attempt it only during its scheduled time."
                        } else {
                            selectedRecord = record
                            currentScreen = "instructions"
                        }
                    }
                )
            }
        }

        "instructions" -> {
            val record = selectedRecord
            val quiz = record?.quiz

            if (quiz != null && record != null) {
                QuizInstructions(
                    quiz = quiz,
                    scheduledDate = record.scheduledDate,
                    scheduledTime = record.scheduledTime,
                    onBack = {
                        currentScreen = "upcoming"
                    },
                    onStartQuiz = {
                        // Recheck the schedule when Start Quiz is tapped.
                        if (isQuizWithinSchedule(record)) {
                            currentScreen = "quiz"
                        } else {
                            submissionError =
                                "The quiz is not currently within its scheduled time."
                        }
                    }
                )
            } else {
                currentScreen = "upcoming"
            }
        }

        "quiz" -> {
            val quiz = selectedRecord?.quiz
            val record = selectedRecord

            if (quiz != null && record != null) {
                QuizScreen(
                    quiz = quiz,
                    onSubmit = { answers: List<Int> ->

                        submittedAnswers = answers
                        submissionError = ""

                        // Do not allow submission after the scheduled end time.
                        if (!isQuizWithinSchedule(record)) {
                            submissionError =
                                "The scheduled quiz time has expired. Your answers cannot be submitted."
                        } else {
                            StudentQuizRepository.submitQuizAttempt(
                                record = record,
                                answers = answers,

                                onSuccess = { attemptedRecord, correct, wrong, unanswered ->

                                    resultCorrect = correct
                                    resultWrong = wrong
                                    resultUnanswered = unanswered

                                    selectedRecord = attemptedRecord

                                    studentQuizRecords =
                                        studentQuizRecords.map { existing ->
                                            if (
                                                existing.teacherId == attemptedRecord.teacherId &&
                                                existing.classId == attemptedRecord.classId &&
                                                existing.quizId == attemptedRecord.quizId
                                            ) {
                                                attemptedRecord
                                            } else {
                                                existing
                                            }
                                        }

                                    currentScreen = "result"
                                },

                                onFailure = { error ->
                                    submissionError = error
                                }
                            )
                        }
                    }
                )
            } else {
                currentScreen = "upcoming"
            }
        }

        "result" -> {
            val quiz = selectedRecord?.quiz

            if (quiz != null) {
                QuizResultScreen(
                    quiz = quiz,
                    correct = resultCorrect,
                    wrong = resultWrong,
                    unanswered = resultUnanswered,
                    marksObtained = selectedRecord?.marksObtained
                        ?: resultCorrect,
                    totalMarks = selectedRecord?.totalMarks
                        ?: quiz.questions.size,
                    onBackToDashboard = {
                        selectedRecord = null
                        submittedAnswers = emptyList()

                        resultCorrect = 0
                        resultWrong = 0
                        resultUnanswered = 0

                        currentScreen = "dashboard"
                    }
                )
            } else {
                currentScreen = "dashboard"
            }
        }

        else -> {
            PatternBackground {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "QHub",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepBlue
                        )

                        Box(
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            IconButton(
                                onClick = {
                                    profileMenuExpanded = true
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(DeepBlue, CircleShape)
                            ) {
                                ProfileIcon()
                            }

                            DropdownMenu(
                                expanded = profileMenuExpanded,
                                onDismissRequest = {
                                    profileMenuExpanded = false
                                }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text("View Profile", color = DeepBlue)
                                    },
                                    onClick = {
                                        profileMenuExpanded = false
                                        currentScreen = "profile"
                                    }
                                )

                                HorizontalDivider()

                                DropdownMenuItem(
                                    text = {
                                        Text("Log Out", color = Color.Red)
                                    },
                                    onClick = {
                                        profileMenuExpanded = false
                                        onLogout()
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(30.dp))

                    Text(
                        "Hello, Student!",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepBlue,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        "Ready to test your knowledge?",
                        fontSize = 16.sp,
                        color = BlueGray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(85.dp))

                    DashboardButton("Semester") {
                        currentScreen = "semester"
                    }

                    Spacer(Modifier.height(18.dp))

                    DashboardButton("Attempted Quizzes") {
                        currentScreen = "attempted"
                    }

                    Spacer(Modifier.height(18.dp))

                    DashboardButton("Unattempted Quizzes") {
                        currentScreen = "unattempted"
                    }

                    Spacer(Modifier.height(18.dp))

                    DashboardButton("Upcoming Quizzes") {
                        currentScreen = "upcoming"
                    }
                }
            }
        }
    }

    if (showQuizUnavailableDialog) {
        AlertDialog(
            onDismissRequest = {
                showQuizUnavailableDialog = false
            },
            title = {
                Text("Quiz not ready")
            },
            text = {
                Text("The quiz questions are not available yet.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showQuizUnavailableDialog = false
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

    if (submissionError.isNotBlank()) {
        AlertDialog(
            onDismissRequest = {
                submissionError = ""
            },
            title = {
                Text("Quiz Notice")
            },
            text = {
                Text(submissionError)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        submissionError = ""
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun QuizListStatusScreen(
    title: String,
    loading: Boolean,
    error: String,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    PatternBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                BackButton(onClick = onBack)
            }

            Spacer(Modifier.height(20.dp))

            Text(
                title,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = DeepBlue
            )

            Spacer(Modifier.height(35.dp))

            if (loading) {
                CircularProgressIndicator(color = DeepBlue)

                Spacer(Modifier.height(15.dp))

                Text(
                    "Loading quizzes from Firebase...",
                    color = BlueGray,
                    fontSize = 14.sp
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Unable to load quizzes",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepBlue
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            error,
                            color = Color.Red,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )

                        Spacer(Modifier.height(18.dp))

                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepBlue
                            )
                        ) {
                            Text("Try Again", color = White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentProfileScreen(
    onBack: () -> Unit,
    onEdit: (StudentDetails) -> Unit
) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    var studentDetails by remember {
        mutableStateOf<StudentDetails?>(null)
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    LaunchedEffect(uid) {
        if (uid == null) {
            loading = false
            errorMessage = "Please log in again."
        } else {
            StudentRepository().getStudentDetails(
                uid = uid,
                onSuccess = { details ->
                    studentDetails = details
                    loading = false
                },
                onFailure = { error ->
                    errorMessage = error
                    loading = false
                }
            )
        }
    }

    PatternBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            BackButton(onClick = onBack)

            Spacer(Modifier.height(15.dp))

            Text(
                "My Profile",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = DeepBlue
            )

            Spacer(Modifier.height(20.dp))

            when {
                loading -> {
                    CircularProgressIndicator(color = DeepBlue)
                }

                studentDetails != null -> {
                    val details = studentDetails!!

                    ProfileDetailCard("Name", details.name)
                    ProfileDetailCard("Roll Number", details.rollNumber)
                    ProfileDetailCard("Email ID", details.email)
                    ProfileDetailCard("Department", details.department)
                    ProfileDetailCard("Section", details.section)
                    ProfileDetailCard("Semester", details.semester)

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            onEdit(details)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeepBlue
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Edit Profile",
                            color = White,
                            fontSize = 16.sp
                        )
                    }
                }

                else -> {
                    Text(
                        errorMessage.ifBlank {
                            "Unable to load student details."
                        },
                        color = Color.Red
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileDetailCard(
    label: String,
    value: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                label,
                fontSize = 13.sp,
                color = BlueGray,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(5.dp))

            Text(
                value.ifBlank {
                    "Not provided"
                },
                fontSize = 16.sp,
                color = DeepBlue,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PatternBackground(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        Spacer(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    val patternColor = Taupe.copy(alpha = 0.55f)
                    val ringWidth = 22.dp.toPx()

                    drawCircle(
                        color = patternColor,
                        radius = 105.dp.toPx(),
                        center = Offset(-45.dp.toPx(), -20.dp.toPx()),
                        style = Stroke(width = ringWidth)
                    )

                    drawCircle(
                        color = patternColor,
                        radius = 125.dp.toPx(),
                        center = Offset(-45.dp.toPx(), -20.dp.toPx()),
                        style = Stroke(width = 3.dp.toPx())
                    )

                    drawCircle(
                        color = patternColor,
                        radius = 145.dp.toPx(),
                        center = Offset(-45.dp.toPx(), -20.dp.toPx()),
                        style = Stroke(width = 3.dp.toPx())
                    )

                    drawCircle(
                        color = patternColor,
                        radius = 85.dp.toPx(),
                        center = Offset(
                            size.width + 45.dp.toPx(),
                            size.height + 15.dp.toPx()
                        ),
                        style = Stroke(width = ringWidth)
                    )

                    drawCircle(
                        color = patternColor,
                        radius = 110.dp.toPx(),
                        center = Offset(
                            size.width + 45.dp.toPx(),
                            size.height + 15.dp.toPx()
                        ),
                        style = Stroke(width = 3.dp.toPx())
                    )

                    drawCircle(
                        color = patternColor,
                        radius = 130.dp.toPx(),
                        center = Offset(
                            size.width + 45.dp.toPx(),
                            size.height + 15.dp.toPx()
                        ),
                        style = Stroke(width = 3.dp.toPx())
                    )

                    for (row in 0..4) {
                        for (column in 0..4) {
                            drawCircle(
                                color = patternColor,
                                radius = 2.5.dp.toPx(),
                                center = Offset(
                                    size.width - 55.dp.toPx() +
                                            column * 9.dp.toPx(),
                                    45.dp.toPx() + row * 9.dp.toPx()
                                )
                            )
                        }
                    }

                    for (i in 0..3) {
                        drawCircle(
                            color = patternColor,
                            radius = (5 + i * 3).dp.toPx(),
                            center = Offset(
                                25.dp.toPx() + i * 18.dp.toPx(),
                                size.height - 45.dp.toPx()
                            ),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    for (row in 0..3) {
                        for (column in 0..3) {
                            drawCircle(
                                color = patternColor,
                                radius = 2.dp.toPx(),
                                center = Offset(
                                    size.width - 30.dp.toPx() -
                                            column * 10.dp.toPx(),
                                    size.height - 100.dp.toPx() +
                                            row * 10.dp.toPx()
                                )
                            )
                        }
                    }

                    drawCircle(
                        color = patternColor,
                        radius = 35.dp.toPx(),
                        center = Offset(
                            size.width - 10.dp.toPx(),
                            125.dp.toPx()
                        ),
                        style = Stroke(width = 3.dp.toPx())
                    )

                    drawCircle(
                        color = patternColor,
                        radius = 48.dp.toPx(),
                        center = Offset(
                            size.width - 10.dp.toPx(),
                            125.dp.toPx()
                        ),
                        style = Stroke(width = 3.dp.toPx())
                    )

                    for (row in 0..2) {
                        for (column in 0..2) {
                            drawCircle(
                                color = patternColor,
                                radius = 2.dp.toPx(),
                                center = Offset(
                                    15.dp.toPx() + column * 10.dp.toPx(),
                                    size.height * 0.55f + row * 10.dp.toPx()
                                )
                            )
                        }
                    }
                }
        )

        content()
    }
}

@Composable
private fun ProfileIcon() {
    Canvas(
        modifier = Modifier.size(27.dp)
    ) {
        val strokeWidth = 2.dp.toPx()

        drawCircle(
            color = White,
            radius = size.width * 0.16f,
            center = Offset(size.width / 2, size.height * 0.30f)
        )

        drawArc(
            color = White,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(
                size.width * 0.16f,
                size.height * 0.43f
            ),
            size = androidx.compose.ui.geometry.Size(
                size.width * 0.68f,
                size.height * 0.65f
            ),
            style = Stroke(width = strokeWidth)
        )
    }
}

@Composable
private fun DashboardButton(
    title: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.82f)
            .height(62.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DeepBlue,
            contentColor = White
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 3.dp
        )
    ) {
        Text(
            title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = White
        )
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            "← Back",
            color = DeepBlue,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun InfoCard(
    title: String,
    subtitle: String = ""
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = DeepBlue
            )

            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(4.dp))

                Text(
                    subtitle,
                    fontSize = 13.sp,
                    color = BlueGray
                )
            }
        }
    }
}

@Composable
private fun EmptyMessage(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = White
        )
    ) {
        Text(
            message,
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            color = BlueGray,
            textAlign = TextAlign.Center,
            fontSize = 14.sp
        )
    }
}
