package com.qhub.quizapp

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

private val White = Color(0xFFFFFFFF)
private val Beige = Color(0xFFE6D5C3)
private val LightBeige = Color(0xFFF5EDE4)
private val BlueGray = Color(0xFF6F8B9B)
private val DeepBlue = Color(0xFF2B4C63)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = DeepBlue,
                    secondary = BlueGray,
                    background = White,
                    surface = White
                )
            ) {
                QHubLoginScreen()
            }
        }
    }
}

private enum class QHubScreen {
    AUTH,
    STUDENT_DETAILS,
    STUDENT_DASHBOARD_PLACEHOLDER
}

@Composable
fun QHubLoginScreen() {

    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val repository = remember { StudentRepository() }

    var selectedRole by remember { mutableStateOf("Student") }
    var isRegistering by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf(QHubScreen.AUTH) }
    var isLoading by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var activeUid by remember { mutableStateOf("") }

    var name by remember { mutableStateOf("") }
    var rollNumber by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf("") }

    var subjects by remember { mutableStateOf<List<String>>(emptyList()) }
    var isFindingSubjects by remember { mutableStateOf(false) }
    var subjectsLoaded by remember { mutableStateOf(false) }

    fun openStudentDetails(uid: String, userEmail: String) {

        activeUid = uid
        email = userEmail

        FirebaseFirestore.getInstance()
            .collection("students")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    name = document.getString("name").orEmpty()
                    rollNumber = document.getString("rollNumber").orEmpty()
                    department = document.getString("department").orEmpty()
                    section = document.getString("section").orEmpty()
                    semester = document.getString("semester").orEmpty()

                    subjects = (document.get("subjects") as? List<*>)
                        ?.filterIsInstance<String>()
                        .orEmpty()

                    subjectsLoaded = true

                    currentScreen =
                        QHubScreen.STUDENT_DASHBOARD_PLACEHOLDER

                } else {

                    subjects = emptyList()
                    subjectsLoaded = false

                    currentScreen = QHubScreen.STUDENT_DETAILS

                    findSubjectsForStudent(
                        repository = repository,
                        userEmail = userEmail,
                        onSuccess = { found ->
                            subjects = found
                            subjectsLoaded = true
                        },
                        onFailure = { message ->
                            Toast.makeText(
                                context,
                                message,
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        setLoading = {
                            isFindingSubjects = it
                        }
                    )
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    context,
                    it.message ?: "Unable to load student profile",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ----------------------------------------------------
    // FIREBASE SESSION HANDLING
    // ----------------------------------------------------

    LaunchedEffect(Unit) {

        val currentUser = auth.currentUser

        if (currentUser != null) {

            val savedRole = context.getSharedPreferences(
                "QHubPrefs",
                Context.MODE_PRIVATE
            ).getString("role_${currentUser.uid}", null)

            activeUid = currentUser.uid
            email = currentUser.email.orEmpty()

            when (savedRole) {

                "Student" -> {

                    selectedRole = "Student"

                    openStudentDetails(
                        uid = currentUser.uid,
                        userEmail = currentUser.email.orEmpty()
                    )
                }

                "Teacher" -> {

                    selectedRole = "Teacher"
                }

                else -> {

                    auth.signOut()

                    activeUid = ""
                    email = ""
                }
            }
        }
    }

    // ----------------------------------------------------
    // SCREEN NAVIGATION
    // ----------------------------------------------------

    when (currentScreen) {

        QHubScreen.AUTH -> {

            AuthScreen(
                selectedRole = selectedRole,
                onRoleChange = { selectedRole = it },

                isRegistering = isRegistering,
                onRegisteringChange = {
                    isRegistering = it
                },

                email = email,
                onEmailChange = { email = it },

                password = password,
                onPasswordChange = { password = it },

                isLoading = isLoading,

                onSubmit = submit@{

                    val cleanedEmail = email.trim().lowercase()

                    if (cleanedEmail.isBlank() || password.isBlank()) {

                        Toast.makeText(
                            context,
                            "Please enter email and password",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@submit
                    }

                    isLoading = true

                    // REGISTRATION

                    if (isRegistering) {

                        auth.createUserWithEmailAndPassword(
                            cleanedEmail,
                            password
                        ).addOnCompleteListener { task ->

                            isLoading = false

                            if (task.isSuccessful) {

                                val user = auth.currentUser
                                val uid = user?.uid

                                if (user == null || uid == null) {

                                    Toast.makeText(
                                        context,
                                        "Unable to get account details",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    return@addOnCompleteListener
                                }

                                user.updateProfile(
                                    UserProfileChangeRequest.Builder()
                                        .setDisplayName(
                                            cleanedEmail.substringBefore("@")
                                        )
                                        .build()
                                )

                                context.getSharedPreferences(
                                    "QHubPrefs",
                                    Context.MODE_PRIVATE
                                ).edit()
                                    .putString("role_$uid", selectedRole)
                                    .apply()

                                if (selectedRole == "Student") {

                                    openStudentDetails(
                                        uid,
                                        user.email.orEmpty()
                                    )

                                } else {

                                    Toast.makeText(
                                        context,
                                        "Teacher account created",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                            } else {

                                Toast.makeText(
                                    context,
                                    task.exception?.message
                                        ?: "Registration failed",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                    } else {

                        // LOGIN

                        auth.signInWithEmailAndPassword(
                            cleanedEmail,
                            password
                        ).addOnCompleteListener { task ->

                            isLoading = false

                            if (task.isSuccessful) {

                                val user = auth.currentUser
                                val uid = user?.uid

                                if (user == null || uid == null) {

                                    Toast.makeText(
                                        context,
                                        "Unable to get account details",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    return@addOnCompleteListener
                                }

                                val savedRole =
                                    context.getSharedPreferences(
                                        "QHubPrefs",
                                        Context.MODE_PRIVATE
                                    ).getString("role_$uid", null)

                                if (
                                    savedRole != null &&
                                    savedRole != selectedRole
                                ) {

                                    Toast.makeText(
                                        context,
                                        "Please select the correct role: $savedRole",
                                        Toast.LENGTH_LONG
                                    ).show()

                                } else if (selectedRole == "Student") {

                                    openStudentDetails(
                                        uid,
                                        user.email.orEmpty()
                                    )

                                } else {

                                    Toast.makeText(
                                        context,
                                        "Teacher login successful",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                            } else {

                                Toast.makeText(
                                    context,
                                    task.exception?.message
                                        ?: "Login failed",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            )
        }

        // ------------------------------------------------
        // STUDENT DETAILS
        // ------------------------------------------------

        QHubScreen.STUDENT_DETAILS -> {

            StudentDetailsScreen(
                name = name,
                onNameChange = { name = it },

                rollNumber = rollNumber,
                onRollNumberChange = { rollNumber = it },

                department = department,
                onDepartmentChange = { department = it },

                section = section,
                onSectionChange = { section = it },

                semester = semester,
                onSemesterChange = { semester = it },

                email = email,
                subjects = subjects,
                subjectsLoaded = subjectsLoaded,
                isFindingSubjects = isFindingSubjects,
                isLoading = isLoading,

                onRefreshSubjects = {

                    findSubjectsForStudent(
                        repository = repository,
                        userEmail = email.trim().lowercase(),
                        onSuccess = { found ->
                            subjects = found
                            subjectsLoaded = true
                        },
                        onFailure = { message ->
                            Toast.makeText(
                                context,
                                message,
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        setLoading = {
                            isFindingSubjects = it
                        }
                    )
                },

                // CORRECTED STUDENT DETAILS VALIDATION

                onProceed = proceed@{

                    val cleanedName = name.trim()
                    val cleanedRollNumber = rollNumber.trim()
                    val cleanedDepartment = department.trim()
                    val cleanedSection = section.trim()
                    val cleanedSemester = semester.trim()
                    val cleanedEmail = email.trim().lowercase()

                    if (
                        cleanedName.isBlank() ||
                        cleanedRollNumber.isBlank() ||
                        cleanedDepartment.isBlank() ||
                        cleanedSection.isBlank() ||
                        cleanedSemester.isBlank()
                    ) {

                        Toast.makeText(
                            context,
                            "Please complete all student details",
                            Toast.LENGTH_LONG
                        ).show()

                        return@proceed
                    }

                    val semesterNumber = cleanedSemester.toIntOrNull()

                    if (
                        semesterNumber == null ||
                        semesterNumber <= 0
                    ) {

                        Toast.makeText(
                            context,
                            "Please enter a valid semester number",
                            Toast.LENGTH_LONG
                        ).show()

                        return@proceed
                    }

                    if (isFindingSubjects) {

                        Toast.makeText(
                            context,
                            "Please wait while subjects are loading",
                            Toast.LENGTH_LONG
                        ).show()

                        return@proceed
                    }

                    if (!subjectsLoaded || subjects.isEmpty()) {

                        Toast.makeText(
                            context,
                            "No subjects found for this email yet. Please check with your teacher.",
                            Toast.LENGTH_LONG
                        ).show()

                        return@proceed
                    }

                    if (activeUid.isBlank()) {

                        Toast.makeText(
                            context,
                            "Your account session is missing. Please login again.",
                            Toast.LENGTH_LONG
                        ).show()

                        return@proceed
                    }

                    isLoading = true

                    repository.verifyAndSaveStudent(
                        uid = activeUid,
                        name = cleanedName,
                        rollNumber = cleanedRollNumber,
                        email = cleanedEmail,
                        department = cleanedDepartment,
                        section = cleanedSection,
                        semester = cleanedSemester,

                        onSuccess = { savedSubjects ->

                            isLoading = false
                            subjects = savedSubjects

                            currentScreen =
                                QHubScreen.STUDENT_DASHBOARD_PLACEHOLDER
                        },

                        onFailure = { message ->

                            isLoading = false

                            Toast.makeText(
                                context,
                                message,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }
            )
        }

        // ------------------------------------------------
        // TEMPORARY STUDENT DASHBOARD HANDOFF
        // ------------------------------------------------

        QHubScreen.STUDENT_DASHBOARD_PLACEHOLDER -> {

            StudentDashboardHandoffScreen(
                name = name,
                subjects = subjects,

                onLogout = {

                    auth.signOut()

                    context.getSharedPreferences(
                        "QHubPrefs",
                        Context.MODE_PRIVATE
                    ).edit()
                        .remove("role_$activeUid")
                        .apply()

                    email = ""
                    password = ""
                    activeUid = ""

                    name = ""
                    rollNumber = ""
                    department = ""
                    section = ""
                    semester = ""

                    subjects = emptyList()
                    subjectsLoaded = false
                    isFindingSubjects = false

                    currentScreen = QHubScreen.AUTH
                    isRegistering = false
                    selectedRole = "Student"
                }
            )
        }
    }
}

// --------------------------------------------------------
// FIND SUBJECTS
// --------------------------------------------------------

private fun findSubjectsForStudent(
    repository: StudentRepository,
    userEmail: String,
    onSuccess: (List<String>) -> Unit,
    onFailure: (String) -> Unit,
    setLoading: (Boolean) -> Unit
) {

    setLoading(true)

    repository.findSubjects(
        email = userEmail.trim().lowercase(),

        onSuccess = {
            setLoading(false)
            onSuccess(it)
        },

        onFailure = {
            setLoading(false)
            onFailure(it)
        }
    )
}

// --------------------------------------------------------
// AUTH SCREEN
// --------------------------------------------------------

@Composable
private fun AuthScreen(
    selectedRole: String,
    onRoleChange: (String) -> Unit,
    isRegistering: Boolean,
    onRegisteringChange: (Boolean) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isLoading: Boolean,
    onSubmit: () -> Unit
) {

    Box(
        Modifier
            .fillMaxSize()
            .background(White)
    ) {

        QHubBackgroundPattern()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp),

            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Spacer(Modifier.height(24.dp))

            QHubLogo()

            Spacer(Modifier.height(4.dp))

            Text(
                "Your Online Quiz Platform",
                fontSize = 15.sp,
                color = BlueGray,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(30.dp))

            Text(
                if (isRegistering) "Create Account"
                else "Welcome Back!",

                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                color = DeepBlue
            )

            Spacer(Modifier.height(7.dp))

            Text(
                if (isRegistering)
                    "Create your account to get started"
                else
                    "Login to continue to your account",

                fontSize = 14.sp,
                color = BlueGray,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        LightBeige,
                        RoundedCornerShape(22.dp)
                    )
                    .padding(18.dp),

                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    "SELECT YOUR ROLE",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlue,
                    letterSpacing = 1.2.sp
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    RoleButton(
                        "Student",
                        selectedRole == "Student",
                        Modifier.weight(1f)
                    ) {
                        onRoleChange("Student")
                    }

                    RoleButton(
                        "Teacher",
                        selectedRole == "Teacher",
                        Modifier.weight(1f)
                    ) {
                        onRoleChange("Teacher")
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            QHubTextField(
                email,
                onEmailChange,
                "Email Address",
                KeyboardType.Email
            )

            Spacer(Modifier.height(15.dp))

            QHubTextField(
                password,
                onPasswordChange,
                "Password",
                isPassword = true
            )

            Spacer(Modifier.height(25.dp))

            Button(
                onClick = onSubmit,

                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),

                shape = RoundedCornerShape(15.dp),

                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepBlue
                ),

                enabled = !isLoading
            ) {

                if (isLoading) {

                    CircularProgressIndicator(
                        Modifier.size(22.dp),
                        color = White,
                        strokeWidth = 2.dp
                    )

                } else {

                    Text(
                        if (isRegistering) "Create Account"
                        else "Login",

                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {

                Text(
                    if (isRegistering)
                        "Already have an account? "
                    else
                        "Don't have an account? ",

                    color = BlueGray,
                    fontSize = 14.sp
                )

                TextButton(
                    onClick = {
                        onRegisteringChange(!isRegistering)
                    },

                    contentPadding = PaddingValues(0.dp)
                ) {

                    Text(
                        if (isRegistering) "Login"
                        else "Register",

                        color = DeepBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                "Learn  •  Practice  •  Achieve",
                color = BlueGray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(20.dp))
        }
    }
}

// --------------------------------------------------------
// STUDENT DETAILS SCREEN
// --------------------------------------------------------

@Composable
private fun StudentDetailsScreen(
    name: String,
    onNameChange: (String) -> Unit,

    rollNumber: String,
    onRollNumberChange: (String) -> Unit,

    department: String,
    onDepartmentChange: (String) -> Unit,

    section: String,
    onSectionChange: (String) -> Unit,

    semester: String,
    onSemesterChange: (String) -> Unit,

    email: String,
    subjects: List<String>,
    subjectsLoaded: Boolean,
    isFindingSubjects: Boolean,
    isLoading: Boolean,

    onRefreshSubjects: () -> Unit,
    onProceed: () -> Unit
) {

    Box(
        Modifier
            .fillMaxSize()
            .background(White)
    ) {

        QHubBackgroundPattern()

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp),

            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(22.dp))

            QHubLogo()

            Text(
                "Your Student Details",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = DeepBlue
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "Confirm your details and subjects",
                color = BlueGray,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(22.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = LightBeige
                ),
                shape = RoundedCornerShape(18.dp)
            ) {

                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {

                    Text(
                        "Email ID",
                        fontWeight = FontWeight.SemiBold,
                        color = DeepBlue
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(email, color = BlueGray)

                    Spacer(Modifier.height(14.dp))

                    QHubTextField(
                        name,
                        onNameChange,
                        "Full Name"
                    )

                    Spacer(Modifier.height(12.dp))

                    QHubTextField(
                        rollNumber,
                        onRollNumberChange,
                        "College Roll Number"
                    )

                    Spacer(Modifier.height(12.dp))

                    QHubTextField(
                        department,
                        onDepartmentChange,
                        "Department (e.g. CSE)"
                    )

                    Spacer(Modifier.height(12.dp))

                    QHubTextField(
                        section,
                        onSectionChange,
                        "Section (e.g. C)"
                    )

                    Spacer(Modifier.height(12.dp))

                    QHubTextField(
                        semester,
                        onSemesterChange,
                        "Semester",
                        KeyboardType.Number
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = White
                ),
                shape = RoundedCornerShape(18.dp)
            ) {

                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {

                    Text(
                        "Your Subjects",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepBlue
                    )

                    Spacer(Modifier.height(8.dp))

                    if (isFindingSubjects) {

                        CircularProgressIndicator(
                            color = DeepBlue,
                            modifier = Modifier.size(24.dp)
                        )

                    } else if (!subjectsLoaded) {

                        Text(
                            "Finding subjects using your email ID…",
                            color = BlueGray
                        )

                    } else if (subjects.isEmpty()) {

                        Text(
                            "No subjects found for this email ID.",
                            color = BlueGray
                        )

                        Spacer(Modifier.height(8.dp))

                        TextButton(
                            onClick = onRefreshSubjects
                        ) {
                            Text(
                                "Check again",
                                color = DeepBlue
                            )
                        }

                    } else {

                        subjects.forEach { subject ->

                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),

                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Text(
                                    "•",
                                    color = DeepBlue,
                                    fontSize = 18.sp
                                )

                                Spacer(Modifier.width(8.dp))

                                Text(
                                    subject,
                                    color = DeepBlue
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            Button(
                onClick = onProceed,

                enabled = !isLoading &&
                        !isFindingSubjects &&
                        subjectsLoaded &&
                        subjects.isNotEmpty(),

                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),

                shape = RoundedCornerShape(15.dp),

                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepBlue
                )
            ) {

                if (isLoading) {

                    CircularProgressIndicator(
                        Modifier.size(22.dp),
                        color = White,
                        strokeWidth = 2.dp
                    )

                } else {

                    Text(
                        "Proceed",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

// --------------------------------------------------------
// TEMPORARY STUDENT DASHBOARD HANDOFF
// --------------------------------------------------------

@Composable
private fun StudentDashboardHandoffScreen(
    name: String,
    subjects: List<String>,
    onLogout: () -> Unit
) {

    Box(
        Modifier
            .fillMaxSize()
            .background(White),

        contentAlignment = Alignment.Center
    ) {

        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),

            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            QHubLogo()

            Text(
                "Details saved successfully",
                color = DeepBlue,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Welcome, $name",
                color = BlueGray
            )

            Text(
                "${subjects.size} subjects found",
                color = BlueGray
            )

            Spacer(Modifier.height(20.dp))

            Text(
                "Student dashboard will be connected by Teammate 2.",
                color = BlueGray,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            TextButton(onClick = onLogout) {
                Text("Logout", color = DeepBlue)
            }
        }
    }
}

// --------------------------------------------------------
// QHUB LOGO
// --------------------------------------------------------

@Composable
fun QHubLogo() {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(135.dp),

        contentAlignment = Alignment.Center
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {

            Box(Modifier.wrapContentSize()) {

                Text(
                    "Q",
                    fontSize = 82.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepBlue,
                    letterSpacing = (-3).sp
                )

                Canvas(
                    Modifier
                        .width(52.dp)
                        .height(38.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = (-17).dp)
                ) {

                    val w = size.width
                    val h = size.height

                    val capTop = Path().apply {
                        moveTo(w * 0.02f, h * 0.42f)
                        lineTo(w * 0.52f, h * 0.08f)
                        lineTo(w * 0.98f, h * 0.42f)
                        lineTo(w * 0.52f, h * 0.70f)
                        close()
                    }

                    drawPath(capTop, DeepBlue)

                    drawRoundRect(
                        color = DeepBlue,
                        topLeft = Offset(
                            w * 0.28f,
                            h * 0.46f
                        ),
                        size = Size(
                            w * 0.48f,
                            h * 0.25f
                        ),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )

                    drawLine(
                        color = Beige,
                        start = Offset(
                            w * 0.96f,
                            h * 0.42f
                        ),
                        end = Offset(
                            w * 0.91f,
                            h * 0.82f
                        ),
                        strokeWidth = 2.dp.toPx()
                    )

                    drawCircle(
                        color = Beige,
                        radius = 3.dp.toPx(),
                        center = Offset(
                            w * 0.91f,
                            h * 0.85f
                        )
                    )
                }
            }

            Text(
                "Hub",
                fontSize = 82.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepBlue,
                letterSpacing = (-3).sp
            )
        }
    }
}

// --------------------------------------------------------
// BACKGROUND PATTERN
// --------------------------------------------------------

@Composable
fun QHubBackgroundPattern() {

    Canvas(Modifier.fillMaxSize()) {

        val w = size.width
        val h = size.height

        drawCircle(
            LightBeige,
            100.dp.toPx(),
            Offset(-15.dp.toPx(), 30.dp.toPx())
        )

        drawCircle(
            White,
            72.dp.toPx(),
            Offset(-10.dp.toPx(), 30.dp.toPx())
        )

        drawCircle(
            LightBeige,
            115.dp.toPx(),
            Offset(w + 30.dp.toPx(), h - 15.dp.toPx())
        )

        drawCircle(
            White,
            82.dp.toPx(),
            Offset(w + 25.dp.toPx(), h - 15.dp.toPx())
        )

        for (i in 0..3) {
            for (j in 0..3) {

                drawCircle(
                    color = Beige.copy(alpha = 0.45f),
                    radius = 2.5.dp.toPx(),

                    center = Offset(
                        w - 58.dp.toPx() + i * 12.dp.toPx(),
                        55.dp.toPx() + j * 12.dp.toPx()
                    )
                )
            }
        }
    }
}

// --------------------------------------------------------
// ROLE BUTTON
// --------------------------------------------------------

@Composable
fun RoleButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,

        modifier = modifier.height(50.dp),

        shape = RoundedCornerShape(14.dp),

        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) DeepBlue else White,
            contentColor = if (selected) White else DeepBlue
        ),

        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp
        )
    ) {

        Text(
            text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// --------------------------------------------------------
// TEXT FIELD
// --------------------------------------------------------

@Composable
fun QHubTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,

        modifier = Modifier.fillMaxWidth(),

        label = {
            Text(
                label,
                color = BlueGray
            )
        },

        singleLine = true,

        shape = RoundedCornerShape(14.dp),

        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword)
                KeyboardType.Password
            else
                keyboardType
        ),

        visualTransformation = if (isPassword)
            PasswordVisualTransformation()
        else
            VisualTransformation.None,

        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DeepBlue,
            unfocusedBorderColor = Beige,
            focusedTextColor = DeepBlue,
            unfocusedTextColor = DeepBlue,
            cursorColor = DeepBlue,
            focusedContainerColor = White,
            unfocusedContainerColor = White
        )
    )
}