
package com.qhub.quizapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TeacherDashboard(
    onLogout: () -> Unit
) {
    var showProfile by remember { mutableStateOf(false) }
    var showCreateClass by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }

    // Teacher profile details
    var teacherName by remember { mutableStateOf("Teacher Name") }
    val teacherEmail = "teacher@qhub.com"
    var department by remember { mutableStateOf("Department Name") }
    var designation by remember { mutableStateOf("Faculty") }

    // Temporary profile editing details
    var editName by remember { mutableStateOf("") }
    var editDepartment by remember { mutableStateOf("") }
    var editDesignation by remember { mutableStateOf("") }

    // Created classes
    var classes by remember { mutableStateOf(listOf<String>()) }
    var newClassName by remember { mutableStateOf("") }

    val colors = MaterialTheme.colorScheme

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
                text = "QHub",
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
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Teacher Profile",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // MAIN DASHBOARD
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "QHub",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Hello, Teacher!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Welcome to your teaching dashboard",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // CREATE CLASS CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(38.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Create New Class",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Create and organise your classes in QHub.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            newClassName = ""
                            showCreateClass = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text("Create Class")
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // CREATED CLASSES SECTION
            Text(
                text = "My Classes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (classes.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "No classes created yet. Tap Create Class to add your first class.",
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            } else {
                classes.forEachIndexed { index, className ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = colors.surface
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(32.dp)
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = className,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onSurface
                                )

                                Text(
                                    text = "Class ${index + 1}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // CREATE CLASS DIALOG
    if (showCreateClass) {
        AlertDialog(
            onDismissRequest = {
                showCreateClass = false
            },
            title = {
                Text(
                    text = "Create New Class",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your class or subject name."
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = newClassName,
                        onValueChange = {
                            newClassName = it
                        },
                        label = {
                            Text("Class / Subject Name")
                        },
                        placeholder = {
                            Text("Example: Business Statistics")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = newClassName.trim().isNotEmpty(),
                    onClick = {
                        val className = newClassName.trim()

                        if (className.isNotEmpty()) {
                            classes = classes + className
                            newClassName = ""
                            showCreateClass = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateClass = false
                        newClassName = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // TEACHER PROFILE DIALOG
    if (showProfile) {
        AlertDialog(
            onDismissRequest = {
                showProfile = false
                isEditing = false
            },
            title = {
                Text(
                    text = if (isEditing) "Edit Profile"
                    else "Teacher Profile",
                    style = MaterialTheme.typography.headlineSmall,
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
                        imageVector = Icons.Default.AccountCircle,
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
                        QHubTeacherProfileDetailRow(
                            label = "Teacher Name",
                            value = teacherName
                        )

                        QHubTeacherProfileDetailRow(
                            label = "Email ID",
                            value = teacherEmail
                        )

                        QHubTeacherProfileDetailRow(
                            label = "Department",
                            value = department
                        )

                        QHubTeacherProfileDetailRow(
                            label = "Designation",
                            value = designation
                        )
                    }
                }
            },
            confirmButton = {
                if (isEditing) {
                    TextButton(
                        onClick = {
                            teacherName = editName.trim()
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
                    TextButton(
                        onClick = {
                            editName = teacherName
                            editDepartment = department
                            editDesignation = designation
                            isEditing = false
                        }
                    ) {
                        Text("Cancel")
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                showProfile = false
                            }
                        ) {
                            Text("Close")
                        }

                        TextButton(
                            onClick = {
                                showProfile = false
                                onLogout()
                            }
                        ) {
                            Text(
                                text = "Logout",
                                color = colors.error
                            )
                        }
                    }
                }
            }
        )
    }
}

// REUSABLE TEACHER PROFILE INFORMATION
@Composable
fun QHubTeacherProfileDetailRow(
    label: String,
    value: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
