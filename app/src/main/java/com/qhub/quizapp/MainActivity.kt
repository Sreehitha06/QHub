
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

// ----------------------------------------------------
// QHUB COLOURS
// ----------------------------------------------------

private val White = Color(0xFFFFFFFF)
private val Beige = Color(0xFFE6D5C3)
private val LightBeige = Color(0xFFF5EDE4)
private val BlueGray = Color(0xFF6F8B9B)
private val DeepBlue = Color(0xFF2B4C63)

// ----------------------------------------------------
// MAIN ACTIVITY
// ----------------------------------------------------

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = DeepBlue,
                    onPrimary = White,
                    secondary = BlueGray,
                    onSecondary = White,
                    background = White,
                    onBackground = DeepBlue,
                    surface = White,
                    onSurface = DeepBlue,
                    surfaceVariant = LightBeige,
                    onSurfaceVariant = BlueGray,
                    outline = Beige
                )
            ) {

                QHubLoginScreen()
            }
        }
    }
}

// ----------------------------------------------------
// MAIN LOGIN SCREEN
// ----------------------------------------------------

@Composable
fun QHubLoginScreen() {

    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    var selectedRole by remember {
        mutableStateOf("Student")
    }

    var isRegistering by remember {
        mutableStateOf(false)
    }

    var name by remember {
        mutableStateOf("")
    }

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    // Dashboard navigation state

    var showTeacherDashboard by remember {
        mutableStateOf(false)
    }

    var teacherName by remember {
        mutableStateOf("Teacher")
    }

    var teacherEmail by remember {
        mutableStateOf("")
    }

    // Show teacher dashboard after successful login

    if (showTeacherDashboard) {

        TeacherDashboard(
            teacherName = teacherName,
            teacherEmail = teacherEmail,
            onLogout = {

                auth.signOut()

                showTeacherDashboard = false

                selectedRole = "Student"
                email = ""
                password = ""
                name = ""
                isRegistering = false

                Toast.makeText(
                    context,
                    "Logged out successfully",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        return
    }

    // LOGIN SCREEN UI

    Box(
        modifier = Modifier
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

            Spacer(modifier = Modifier.height(24.dp))

            QHubLogo()

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Your Online Quiz Platform",
                fontSize = 15.sp,
                color = BlueGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = if (isRegistering) {
                    "Create Account"
                } else {
                    "Welcome Back!"
                },
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                color = DeepBlue
            )

            Spacer(modifier = Modifier.height(7.dp))

            Text(
                text = if (isRegistering) {
                    "Create your account to get started"
                } else {
                    "Login to continue to your account"
                },
                fontSize = 14.sp,
                color = BlueGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ROLE SELECTOR

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = LightBeige,
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "SELECT YOUR ROLE",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlue,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    RoleButton(
                        text = "Student",
                        selected = selectedRole == "Student",
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedRole = "Student"
                    }

                    RoleButton(
                        text = "Teacher",
                        selected = selectedRole == "Teacher",
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedRole = "Teacher"
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // NAME FIELD

            if (isRegistering) {

                QHubTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full Name"
                )

                Spacer(modifier = Modifier.height(15.dp))
            }

            // EMAIL FIELD

            QHubTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email Address",
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(15.dp))

            // PASSWORD FIELD

            QHubTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                isPassword = true
            )

            Spacer(modifier = Modifier.height(25.dp))

            // LOGIN / REGISTER BUTTON

            Button(
                onClick = {

                    if (
                        email.isBlank() ||
                        password.isBlank()
                    ) {

                        Toast.makeText(
                            context,
                            "Please enter email and password",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@Button
                    }

                    if (
                        isRegistering &&
                        name.isBlank()
                    ) {

                        Toast.makeText(
                            context,
                            "Please enter your name",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@Button
                    }

                    isLoading = true

                    if (isRegistering) {

                        // FIREBASE REGISTRATION

                        auth.createUserWithEmailAndPassword(
                            email.trim(),
                            password
                        ).addOnCompleteListener { task ->

                            isLoading = false

                            if (task.isSuccessful) {

                                val user = auth.currentUser

                                val profileUpdates =
                                    UserProfileChangeRequest.Builder()
                                        .setDisplayName(name.trim())
                                        .build()

                                user?.updateProfile(profileUpdates)

                                user?.uid?.let { uid ->

                                    context.getSharedPreferences(
                                        "QHubPrefs",
                                        Context.MODE_PRIVATE
                                    ).edit()
                                        .putString(
                                            "role_$uid",
                                            selectedRole
                                        )
                                        .apply()
                                }

                                Toast.makeText(
                                    context,
                                    "Registration successful! Welcome $selectedRole",
                                    Toast.LENGTH_LONG
                                ).show()

                                // Return to login screen

                                isRegistering = false
                                name = ""

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

                        // FIREBASE LOGIN

                        auth.signInWithEmailAndPassword(
                            email.trim(),
                            password
                        ).addOnCompleteListener { task ->

                            isLoading = false

                            if (task.isSuccessful) {

                                val user = auth.currentUser

                                val uid = user?.uid

                                val savedRole = uid?.let {

                                    context.getSharedPreferences(
                                        "QHubPrefs",
                                        Context.MODE_PRIVATE
                                    ).getString(
                                        "role_$it",
                                        selectedRole
                                    )

                                } ?: selectedRole

                                if (savedRole == "Teacher") {

                                    teacherName =
                                        user?.displayName
                                            ?: "Teacher"

                                    teacherEmail =
                                        user?.email ?: email

                                    showTeacherDashboard = true

                                    Toast.makeText(
                                        context,
                                        "Welcome $teacherName",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                } else {

                                    // Student dashboard will be developed separately.

                                    auth.signOut()

                                    Toast.makeText(
                                        context,
                                        "Student dashboard will be available soon.",
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
                },

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
                        modifier = Modifier.size(22.dp),
                        color = White,
                        strokeWidth = 2.dp
                    )

                } else {

                    Text(
                        text = if (isRegistering) {
                            "Create Account"
                        } else {
                            "Login"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // LOGIN / REGISTER SWITCH

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {

                Text(
                    text = if (isRegistering) {
                        "Already have an account? "
                    } else {
                        "Don't have an account? "
                    },
                    color = BlueGray,
                    fontSize = 14.sp
                )

                TextButton(
                    onClick = {
                        isRegistering = !isRegistering
                    },
                    contentPadding = PaddingValues(0.dp)
                ) {

                    Text(
                        text = if (isRegistering) {
                            "Login"
                        } else {
                            "Register"
                        },
                        color = DeepBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Learn  •  Practice  •  Achieve",
                color = BlueGray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ----------------------------------------------------
// TEACHER DASHBOARD
// ----------------------------------------------------

@Composable
fun TeacherDashboard(
    teacherName: String,
    teacherEmail: String,
    onLogout: () -> Unit
) {

    var showProfile by remember {
        mutableStateOf(false)
    }

    var showCreateClassMessage by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
    ) {

        QHubBackgroundPattern()

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            // TOP BAR

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepBlue)
                    .padding(
                        horizontal = 20.dp,
                        vertical = 12.dp
                    ),

                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "QHub",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = White
                )

                TextButton(
                    onClick = {
                        showProfile = true
                    }
                ) {

                    Text(
                        text = "👤",
                        fontSize = 26.sp
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    Text(
                        text = "Profile",
                        color = White,
                        fontSize = 14.sp
                    )
                }
            }

            // MAIN DASHBOARD

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 25.dp),

                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                // QHUB HEADING

                Text(
                    text = "QHub",
                    fontSize = 55.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepBlue,
                    letterSpacing = (-2).sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // GREETING

                Text(
                    text = "Hello, Teacher!",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlue
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Welcome to your teaching dashboard",
                    fontSize = 15.sp,
                    color = BlueGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(45.dp))

                // CREATE CLASS CARD

                Card(
                    modifier = Modifier.fillMaxWidth(),

                    shape = RoundedCornerShape(20.dp),

                    colors = CardDefaults.cardColors(
                        containerColor = LightBeige
                    ),

                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 3.dp
                    )
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),

                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = "Create New Class",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepBlue
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Create and manage your classes in QHub.",
                            fontSize = 14.sp,
                            color = BlueGray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        Button(
                            onClick = {
                                showCreateClassMessage = true
                            },

                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),

                            shape = RoundedCornerShape(13.dp),

                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepBlue,
                                contentColor = White
                            )
                        ) {

                            Text(
                                text = "+  Create Class",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------
    // TEACHER PROFILE
    // ------------------------------------------------

    if (showProfile) {

        AlertDialog(
            onDismissRequest = {
                showProfile = false
            },

            shape = RoundedCornerShape(22.dp),

            containerColor = LightBeige,

            title = {

                Text(
                    text = "Teacher Profile",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlue
                )
            },

            text = {

                Column(
                    modifier = Modifier.fillMaxWidth(),

                    horizontalAlignment = Alignment.CenterHorizontally,

                    verticalArrangement = Arrangement.spacedBy(15.dp)
                ) {

                    // PROFILE ICON

                    Text(
                        text = "👤",
                        fontSize = 55.sp
                    )

                    // TEACHER NAME

                    TeacherProfileDetail(
                        label = "Teacher Name",
                        value = teacherName
                    )

                    // EMAIL

                    TeacherProfileDetail(
                        label = "Email Address",
                        value = teacherEmail
                    )

                    // PLACEHOLDER INFORMATION

                    TeacherProfileDetail(
                        label = "Department",
                        value = "Not provided"
                    )

                    TeacherProfileDetail(
                        label = "Designation",
                        value = "Faculty"
                    )
                }
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        showProfile = false
                    }
                ) {

                    Text(
                        text = "Close",
                        color = DeepBlue
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showProfile = false
                        onLogout()
                    }
                ) {

                    Text(
                        text = "Logout",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    // ------------------------------------------------
    // CREATE CLASS PLACEHOLDER
    // ------------------------------------------------

    if (showCreateClassMessage) {

        AlertDialog(
            onDismissRequest = {
                showCreateClassMessage = false
            },

            title = {
                Text(
                    text = "Create New Class",
                    color = DeepBlue
                )
            },

            text = {
                Text(
                    text = "Class creation and CSV student upload " +
                            "will be added in the next development stage.",
                    color = BlueGray
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        showCreateClassMessage = false
                    }
                ) {
                    Text("OK", color = DeepBlue)
                }
            }
        )
    }
}

// ----------------------------------------------------
// REUSABLE PROFILE DETAIL
// ----------------------------------------------------

@Composable
fun TeacherProfileDetail(
    label: String,
    value: String
) {

    Column(
        modifier = Modifier.fillMaxWidth(),

        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = BlueGray
        )

        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeepBlue
        )
    }
}

// ----------------------------------------------------
// QHUB LOGO - GRADUATION CAP
// ----------------------------------------------------

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

            Box(
                modifier = Modifier.wrapContentSize()
            ) {

                Text(
                    text = "Q",
                    fontSize = 82.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepBlue,
                    letterSpacing = (-3).sp
                )

                Canvas(
                    modifier = Modifier
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

                    drawPath(
                        path = capTop,
                        color = DeepBlue
                    )

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
                text = "Hub",
                fontSize = 82.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepBlue,
                letterSpacing = (-3).sp
            )
        }
    }
}

// ----------------------------------------------------
// BACKGROUND PATTERN
// ----------------------------------------------------

@Composable
fun QHubBackgroundPattern() {

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {

        val w = size.width
        val h = size.height

        drawCircle(
            color = LightBeige,
            radius = 100.dp.toPx(),
            center = Offset(
                -15.dp.toPx(),
                30.dp.toPx()
            )
        )

        drawCircle(
            color = White,
            radius = 72.dp.toPx(),
            center = Offset(
                -10.dp.toPx(),
                30.dp.toPx()
            )
        )

        drawCircle(
            color = LightBeige,
            radius = 115.dp.toPx(),
            center = Offset(
                w + 30.dp.toPx(),
                h - 15.dp.toPx()
            )
        )

        drawCircle(
            color = White,
            radius = 82.dp.toPx(),
            center = Offset(
                w + 25.dp.toPx(),
                h - 15.dp.toPx()
            )
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

// ----------------------------------------------------
// ROLE BUTTON
// ----------------------------------------------------

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
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ----------------------------------------------------
// TEXT FIELD
// ----------------------------------------------------

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
                text = label,
                color = BlueGray
            )
        },

        singleLine = true,

        shape = RoundedCornerShape(14.dp),

        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) {
                KeyboardType.Password
            } else {
                keyboardType
            }
        ),

        visualTransformation = if (isPassword) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },

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
