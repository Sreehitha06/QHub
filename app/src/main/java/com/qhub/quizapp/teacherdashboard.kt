
package com.qhub.quizapp

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private data class QHubCsvStudent(
    val name: String,
    val rollNumber: String,
    val department: String,
    val section: String,
    val email: String
)

private data class QHubTeacherClass(
    val id: String,
    val name: String
)

@Composable
fun TeacherDashboard(
    onLogout: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser
    val uid = user?.uid
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showProfile by remember { mutableStateOf(false) }
    var showCreateClass by remember { mutableStateOf(false) }
    var showCsvUpload by remember { mutableStateOf(false) }
    var showCreateQuiz by remember { mutableStateOf(false) }

    // MANAGE QUIZZES, EDIT QUIZ AND RESULTS NAVIGATION
    var showManageQuizzes by remember { mutableStateOf(false) }
    var showQuizResults by remember { mutableStateOf(false) }
    var showEditQuiz by remember { mutableStateOf(false) }
    var selectedQuizId by remember { mutableStateOf("") }

    var teacherName by remember {
        mutableStateOf(user?.displayName ?: "Teacher")
    }

    val teacherEmail = user?.email ?: "Email not available"

    var department by remember { mutableStateOf("Department Name") }
    var designation by remember { mutableStateOf("Faculty") }

    var editName by remember { mutableStateOf("") }
    var editDepartment by remember { mutableStateOf("") }
    var editDesignation by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }

    var classes by remember {
        mutableStateOf(listOf<QHubTeacherClass>())
    }

    var classesWithRoster by remember {
        mutableStateOf(setOf<String>())
    }

    var selectedTeacherClass by remember {
        mutableStateOf<QHubTeacherClass?>(null)
    }

    var selectedClassId by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf("") }

    var newClassName by remember { mutableStateOf("") }
    var subjectName by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf("") }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var uploadMessage by remember { mutableStateOf<String?>(null) }

    val colors = MaterialTheme.colorScheme

    // LISTEN TO CLASSES AND CSV ROSTERS
    DisposableEffect(uid) {
        var classRegistration: ListenerRegistration? = null
        var rosterRegistration: ListenerRegistration? = null

        if (uid != null) {
            classRegistration = db.collection("teachers")
                .document(uid)
                .collection("classes")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        errorMessage = error.localizedMessage
                            ?: "Could not load classes"
                    } else if (snapshot != null) {
                        classes = snapshot.documents.mapNotNull { document ->
                            val name = document.getString("className")
                            if (name != null) {
                                QHubTeacherClass(document.id, name)
                            } else {
                                null
                            }
                        }
                    }
                }

            rosterRegistration = db.collection("studentRoster")
                .whereEqualTo("teacherId", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        errorMessage = error.localizedMessage
                            ?: "Could not check uploaded CSV files"
                    } else if (snapshot != null) {
                        classesWithRoster = snapshot.documents
                            .mapNotNull { it.getString("classId") }
                            .toSet()
                    }
                }
        } else {
            errorMessage = "Please log in again."
        }

        onDispose {
            classRegistration?.remove()
            rosterRegistration?.remove()
        }
    }

    // CSV PICKER
    val csvPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            if (uid == null) {
                uploadMessage = "Please log in again."
                return@rememberLauncherForActivityResult
            }

            if (
                selectedClassId.isBlank() ||
                selectedClass.isBlank() ||
                subjectName.isBlank() ||
                semester.isBlank()
            ) {
                uploadMessage = "Enter class, subject and semester."
                return@rememberLauncherForActivityResult
            }

            scope.launch {
                isSaving = true
                uploadMessage = "Reading CSV file..."

                try {
                    val students = withContext(Dispatchers.IO) {
                        readQHubCsv(context, uri)
                    }

                    if (students.isEmpty()) {
                        throw Exception("CSV contains no student records.")
                    }

                    uploadMessage = "Uploading ${students.size} students..."

                    val normalizedSubject = subjectName.trim()
                    val normalizedClass = selectedClass.trim()
                    val normalizedSemester = semester.trim()
                    val normalizedClassId = selectedClassId

                    val teacherId = uid
                    val email = teacherEmail

                    val documents = students.map { student ->
                        val key = listOf(
                            teacherId,
                            normalizedClassId,
                            normalizedSubject.lowercase(),
                            normalizedSemester.lowercase(),
                            student.email.lowercase()
                        ).joinToString("|")

                        val documentId = sha256(key)

                        val data = hashMapOf(
                            "name" to student.name,
                            "email" to student.email.trim().lowercase(),
                            "rollNumber" to student.rollNumber.trim(),
                            "department" to student.department.trim(),
                            "section" to student.section.trim(),
                            "classId" to normalizedClassId,
                            "subjectName" to normalizedSubject,
                            "className" to normalizedClass,
                            "semester" to normalizedSemester,
                            "teacherId" to teacherId,
                            "teacherEmail" to email,
                            "createdAt" to System.currentTimeMillis()
                        )

                        documentId to data
                    }

                    withContext(Dispatchers.IO) {
                        documents.chunked(450).forEach { chunk ->
                            val batch = db.batch()

                            chunk.forEach { (documentId, data) ->
                                val ref = db.collection("studentRoster")
                                    .document(documentId)
                                batch.set(ref, data)
                            }

                            batch.commit().awaitResult()
                        }
                    }

                    uploadMessage =
                        "Successfully uploaded ${students.size} students!"

                } catch (e: Exception) {
                    uploadMessage = e.localizedMessage
                        ?: "CSV upload failed."
                } finally {
                    isSaving = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {

        // TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.primary)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "QHub",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onPrimary
            )

            IconButton(
                onClick = {
                    isEditing = false
                    showProfile = true
                }
            ) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = "Teacher Profile",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // DASHBOARD OR SELECTED CLASS
        if (selectedTeacherClass == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "QHub",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    "Hello, $teacherName!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Welcome to your teaching dashboard",
                    color = colors.onSurfaceVariant
                )

                Spacer(Modifier.height(32.dp))

                // CREATE CLASS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(38.dp)
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            "Create New Class",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Create and organise your classes in QHub.",
                            color = colors.onSurfaceVariant
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                newClassName = ""
                                errorMessage = null
                                showCreateClass = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Create Class")
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // CSV UPLOAD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.UploadFile,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(38.dp)
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            "Upload Student CSV",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Upload a student roster for a class, subject and semester.",
                            color = colors.onSurfaceVariant
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                selectedClassId = ""
                                selectedClass = ""
                                subjectName = ""
                                semester = ""
                                uploadMessage = null
                                showCsvUpload = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = classes.isNotEmpty()
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Upload CSV")
                        }

                        if (classes.isEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Create a class before uploading its roster.",
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                Text(
                    "My Classes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                if (classes.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = colors.surfaceVariant
                        )
                    ) {
                        Text(
                            "No classes created yet. Tap Create Class to add your first class.",
                            modifier = Modifier.padding(20.dp),
                            color = colors.onSurfaceVariant
                        )
                    }
                } else {
                    classes.forEachIndexed { index, teacherClass ->
                        val hasRoster = teacherClass.id in classesWithRoster

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clickable(enabled = hasRoster) {
                                    selectedTeacherClass = teacherClass
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (hasRoster)
                                    colors.surface
                                else
                                    colors.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = if (hasRoster)
                                        colors.primary
                                    else
                                        colors.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )

                                Spacer(Modifier.width(14.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        teacherClass.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        "Class ${index + 1}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant
                                    )

                                    Spacer(Modifier.height(4.dp))

                                    Text(
                                        if (hasRoster) "CSV uploaded"
                                        else "Upload CSV to unlock this class",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (hasRoster)
                                            colors.primary
                                        else
                                            colors.error
                                    )
                                }

                                Text(
                                    if (hasRoster) "Open →" else "Locked",
                                    color = if (hasRoster)
                                        colors.primary
                                    else
                                        colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                errorMessage?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = colors.error)
                }
            }

        } else {

            // SELECTED CLASS NAVIGATION
            val currentClass = selectedTeacherClass!!

            when {

                // QUIZ RESULTS SCREEN
                showQuizResults -> {
                    val currentTeacherId = uid

                    if (
                        currentTeacherId != null &&
                        selectedQuizId.isNotBlank()
                    ) {
                        TeacherQuizResultsScreen(
                            teacherId = currentTeacherId,
                            classId = currentClass.id,
                            quizId = selectedQuizId,
                            onBack = {
                                showQuizResults = false
                                showManageQuizzes = true
                            }
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Unable to open quiz results. Please log in again.",
                                color = colors.error
                            )

                            Spacer(Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    showQuizResults = false
                                    showManageQuizzes = true
                                }
                            ) {
                                Text("Back")
                            }
                        }
                    }
                }

                // EDIT QUIZ SCREEN
                showEditQuiz -> {
                    val currentTeacherId = uid

                    if (
                        currentTeacherId != null &&
                        selectedQuizId.isNotBlank()
                    ) {
                        EditQuizScreen(
                            teacherId = currentTeacherId,
                            classId = currentClass.id,
                            className = currentClass.name,
                            quizId = selectedQuizId,
                            onBack = {
                                showEditQuiz = false
                                showManageQuizzes = true
                            }
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Unable to edit this quiz. Please log in again.",
                                color = colors.error
                            )

                            Spacer(Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    showEditQuiz = false
                                    showManageQuizzes = true
                                }
                            ) {
                                Text("Back")
                            }
                        }
                    }
                }

                // MANAGE QUIZZES SCREEN
                showManageQuizzes -> {
                    val currentTeacherId = uid

                    if (currentTeacherId != null) {
                        ManageQuizzesScreen(
                            teacherId = currentTeacherId,
                            classId = currentClass.id,
                            className = currentClass.name,
                            onBack = {
                                showManageQuizzes = false
                                showEditQuiz = false
                                showQuizResults = false
                            },
                            onQuizClick = { quizId ->
                                selectedQuizId = quizId
                                showQuizResults = true
                                showEditQuiz = false
                            },
                            onEditQuiz = { quizId ->
                                selectedQuizId = quizId
                                showEditQuiz = true
                                showQuizResults = false
                            }
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Please log in again.",
                                color = colors.error
                            )

                            Button(
                                onClick = {
                                    showManageQuizzes = false
                                }
                            ) {
                                Text("Back")
                            }
                        }
                    }
                }

                // CREATE QUIZ SCREEN
                showCreateQuiz -> {
                    CreateQuizScreen(
                        className = currentClass.name,

                        onBack = {
                            showCreateQuiz = false
                        },

                        onSaveQuiz = {
                                title,
                                questions,
                                duration,
                                date,
                                time,
                                shuffleQuestions,
                                shuffleOptions,
                                onResult ->

                            val teacherId = FirebaseAuth.getInstance()
                                .currentUser?.uid

                            if (teacherId == null) {
                                onResult(false, "Please log in again.")
                            } else {
                                TeacherQuizRepository.saveQuiz(
                                    teacherId = teacherId,
                                    classId = currentClass.id,
                                    className = currentClass.name,
                                    title = title,
                                    questions = questions,
                                    durationMinutes = duration,
                                    scheduledDate = date,
                                    scheduledTime = time,
                                    shuffleQuestions = shuffleQuestions,
                                    shuffleOptions = shuffleOptions,

                                    onSuccess = {
                                        onResult(true, null)
                                    },

                                    onFailure = { error ->
                                        onResult(false, error)
                                    }
                                )
                            }
                        }
                    )
                }

                // CLASS HOME SCREEN
                else -> {
                    TeacherClassHomeScreen(
                        teacherClass = currentClass,

                        onBack = {
                            selectedTeacherClass = null
                            showCreateQuiz = false
                            showManageQuizzes = false
                            showQuizResults = false
                            showEditQuiz = false
                            selectedQuizId = ""
                        },

                        onCreateQuiz = {
                            showCreateQuiz = true
                            showManageQuizzes = false
                            showQuizResults = false
                            showEditQuiz = false
                        },

                        onManageQuizzes = {
                            showManageQuizzes = true
                            showCreateQuiz = false
                            showQuizResults = false
                            showEditQuiz = false
                        }
                    )
                }
            }
        }
    }

    // CREATE CLASS DIALOG
    if (showCreateClass) {
        AlertDialog(
            onDismissRequest = {
                if (!isSaving) showCreateClass = false
            },
            title = {
                Text("Create New Class", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Enter your class name.")

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = newClassName,
                        onValueChange = { newClassName = it },
                        label = { Text("Class Name") },
                        placeholder = { Text("Example: BCA 2nd Year") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    errorMessage?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = colors.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = newClassName.isNotBlank() &&
                            !isSaving && uid != null,
                    onClick = {
                        val className = newClassName.trim()

                        if (uid == null) {
                            errorMessage = "Please log in again."
                            return@TextButton
                        }

                        isSaving = true
                        errorMessage = null

                        val classData = hashMapOf(
                            "className" to className,
                            "teacherId" to uid,
                            "teacherEmail" to teacherEmail,
                            "createdAt" to System.currentTimeMillis()
                        )

                        db.collection("teachers")
                            .document(uid)
                            .collection("classes")
                            .add(classData)
                            .addOnSuccessListener {
                                isSaving = false
                                newClassName = ""
                                showCreateClass = false
                            }
                            .addOnFailureListener { exception ->
                                isSaving = false
                                errorMessage = exception.localizedMessage
                                    ?: "Could not save class"
                            }
                    }
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Create")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        showCreateClass = false
                        newClassName = ""
                        errorMessage = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // CSV UPLOAD DIALOG
    if (showCsvUpload) {
        AlertDialog(
            onDismissRequest = {
                if (!isSaving) showCsvUpload = false
            },
            title = {
                Text("Upload Student CSV", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Choose the class for this roster.")

                    var expanded by remember { mutableStateOf(false) }

                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (selectedClass.isBlank())
                                    "Select Class"
                                else
                                    selectedClass
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            classes.forEach { teacherClass ->
                                DropdownMenuItem(
                                    text = { Text(teacherClass.name) },
                                    onClick = {
                                        selectedClassId = teacherClass.id
                                        selectedClass = teacherClass.name
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = subjectName,
                        onValueChange = { subjectName = it },
                        label = { Text("Subject Name") },
                        placeholder = { Text("Example: Mathematics") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = semester,
                        onValueChange = { semester = it },
                        label = { Text("Semester") },
                        placeholder = { Text("Example: Semester 3") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        "CSV columns required: Full Name, Roll Number, Department, Section, Email",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )

                    uploadMessage?.let {
                        Text(
                            it,
                            color = if (it.startsWith("Successfully"))
                                colors.primary
                            else
                                colors.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isSaving &&
                            selectedClassId.isNotBlank() &&
                            subjectName.isNotBlank() &&
                            semester.isNotBlank(),
                    onClick = {
                        csvPicker.launch(arrayOf("*/*"))
                    }
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Choose CSV File")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        showCsvUpload = false
                        uploadMessage = null
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }

    // TEACHER PROFILE
    if (showProfile) {
        AlertDialog(
            onDismissRequest = {
                showProfile = false
                isEditing = false
            },
            title = {
                Text(
                    if (isEditing) "Edit Profile" else "Teacher Profile",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier
                            .size(64.dp)
                            .align(Alignment.CenterHorizontally)
                    )

                    if (isEditing) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Teacher Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = teacherEmail,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Email ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = editDepartment,
                            onValueChange = { editDepartment = it },
                            label = { Text("Department") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = editDesignation,
                            onValueChange = { editDesignation = it },
                            label = { Text("Designation") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    } else {
                        QHubTeacherProfileDetailRow("Teacher Name", teacherName)
                        QHubTeacherProfileDetailRow("Email ID", teacherEmail)
                        QHubTeacherProfileDetailRow("Department", department)
                        QHubTeacherProfileDetailRow("Designation", designation)
                    }
                }
            },
            confirmButton = {
                if (isEditing) {
                    TextButton(
                        onClick = {
                            if (editName.isNotBlank()) {
                                teacherName = editName.trim()

                                val updates =
                                    com.google.firebase.auth
                                        .UserProfileChangeRequest.Builder()
                                        .setDisplayName(teacherName)
                                        .build()

                                auth.currentUser?.updateProfile(updates)
                            }

                            department = editDepartment.trim()
                            designation = editDesignation.trim()
                            isEditing = false
                        }
                    ) {
                        Text("Save Changes")
                    }
                } else {
                    TextButton(
                        onClick = {
                            editName = teacherName
                            editDepartment = department
                            editDesignation = designation
                            isEditing = true
                        }
                    ) {
                        Text("Edit Profile")
                    }
                }
            },
            dismissButton = {
                if (isEditing) {
                    TextButton(onClick = { isEditing = false }) {
                        Text("Cancel")
                    }
                } else {
                    Row {
                        TextButton(onClick = { showProfile = false }) {
                            Text("Close")
                        }

                        TextButton(
                            onClick = {
                                showProfile = false
                                onLogout()
                            }
                        ) {
                            Text("Logout", color = colors.error)
                        }
                    }
                }
            }
        )
    }
}

// CLASS HOME SCREEN
@Composable
private fun TeacherClassHomeScreen(
    teacherClass: QHubTeacherClass,
    onBack: () -> Unit,
    onCreateQuiz: () -> Unit,
    onManageQuizzes: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        TextButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Back to Dashboard")
        }

        Spacer(Modifier.height(24.dp))

        Text(
            teacherClass.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = colors.primary
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Class Dashboard",
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Manage quizzes and student performance for this class.",
            color = colors.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Icon(
                    Icons.Default.Quiz,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(42.dp)
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    "Create a Quiz",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Create a new quiz for ${teacherClass.name}. Add questions, four options, correct answers, duration and schedule.",
                    color = colors.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onCreateQuiz,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Create Quiz")
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Icon(
                    Icons.Default.Assessment,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(42.dp)
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    "Manage Quizzes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "View quizzes created for ${teacherClass.name}, manage them and check the marks of students who have attempted each quiz.",
                    color = colors.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))

                OutlinedButton(
                    onClick = onManageQuizzes,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Manage Quizzes")
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Class ID: ${teacherClass.id}",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant
        )
    }
}

// CSV READER
private fun readQHubCsv(
    context: Context,
    uri: Uri
): List<QHubCsvStudent> {
    val text = context.contentResolver.openInputStream(uri)
        ?.bufferedReader(Charsets.UTF_8)
        ?.use { it.readText() }
        ?: throw Exception("Could not read selected CSV file.")

    val rows = parseQHubCsv(text)

    if (rows.isEmpty()) {
        throw Exception("CSV file is empty.")
    }

    val headers = rows.first().map {
        it.trim().removePrefix("\uFEFF").lowercase()
    }

    fun headerIndex(vararg names: String): Int {
        return headers.indexOfFirst { it in names }
    }

    val nameIndex = headerIndex("full name", "name", "student name")
    val rollIndex = headerIndex("roll number", "rollnumber", "roll no", "rollno")
    val departmentIndex = headerIndex("department", "dept")
    val sectionIndex = headerIndex("section")
    val emailIndex = headerIndex("email", "email id", "email address")

    if (
        listOf(
            nameIndex,
            rollIndex,
            departmentIndex,
            sectionIndex,
            emailIndex
        ).any { it == -1 }
    ) {
        throw Exception(
            "Missing CSV headers. Required: Full Name, Roll Number, Department, Section, Email."
        )
    }

    val students = mutableListOf<QHubCsvStudent>()
    val seenEmails = mutableSetOf<String>()

    rows.drop(1).forEachIndexed { index, row ->
        if (row.all { it.isBlank() }) return@forEachIndexed

        val requiredIndex = listOf(
            nameIndex,
            rollIndex,
            departmentIndex,
            sectionIndex,
            emailIndex
        ).maxOrNull() ?: 0

        if (row.size <= requiredIndex) {
            throw Exception("Invalid data on CSV row ${index + 2}.")
        }

        val name = row[nameIndex].trim()
        val roll = row[rollIndex].trim()
        val dept = row[departmentIndex].trim()
        val section = row[sectionIndex].trim()
        val email = row[emailIndex].trim().lowercase()

        if (
            name.isBlank() ||
            roll.isBlank() ||
            dept.isBlank() ||
            section.isBlank() ||
            email.isBlank()
        ) {
            throw Exception("Missing student details on CSV row ${index + 2}.")
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            throw Exception("Invalid email on CSV row ${index + 2}: $email")
        }

        if (!seenEmails.add(email)) {
            throw Exception("Duplicate email in CSV: $email")
        }

        students.add(
            QHubCsvStudent(
                name = name,
                rollNumber = roll,
                department = dept,
                section = section,
                email = email
            )
        )
    }

    return students
}

// CSV PARSER
private fun parseQHubCsv(text: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    val row = mutableListOf<String>()
    val field = StringBuilder()

    var insideQuotes = false
    var index = 0

    while (index < text.length) {
        val char = text[index]

        when {
            char == '"' &&
                    insideQuotes &&
                    index + 1 < text.length &&
                    text[index + 1] == '"' -> {
                field.append('"')
                index++
            }

            char == '"' -> {
                insideQuotes = !insideQuotes
            }

            char == ',' && !insideQuotes -> {
                row.add(field.toString())
                field.clear()
            }

            (char == '\n' || char == '\r') && !insideQuotes -> {
                row.add(field.toString())
                field.clear()

                if (row.any { it.isNotBlank() }) {
                    rows.add(row.toList())
                }

                row.clear()

                if (
                    char == '\r' &&
                    index + 1 < text.length &&
                    text[index + 1] == '\n'
                ) {
                    index++
                }
            }

            else -> field.append(char)
        }

        index++
    }

    if (insideQuotes) {
        throw Exception("CSV has an unclosed quotation mark.")
    }

    if (field.isNotEmpty() || row.isNotEmpty()) {
        row.add(field.toString())
        rows.add(row.toList())
    }

    return rows
}

// SHA-256
private fun sha256(value: String): String {
    val bytes = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))

    return bytes.joinToString("") {
        "%02x".format(it)
    }
}

// FIREBASE TASK AWAIT
private suspend fun <T> Task<T>.awaitResult(): T =
    suspendCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result)
        }

        addOnFailureListener { exception ->
            continuation.resumeWithException(exception)
        }

        addOnCanceledListener {
            continuation.resumeWithException(
                Exception("Firebase operation was cancelled.")
            )
        }
    }

// TEACHER PROFILE DETAIL
@Composable
fun QHubTeacherProfileDetailRow(
    label: String,
    value: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
