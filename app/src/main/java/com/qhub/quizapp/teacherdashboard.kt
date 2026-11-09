
package com.qhub.quizapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TeacherDashboard(
    onLogout: () -> Unit
) {
    var showProfile by remember { mutableStateOf(false) }
    var showComingSoon by remember { mutableStateOf(false) }

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
                onClick = { showProfile = true }
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Teacher Profile",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // MAIN DASHBOARD CONTENT

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
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

            Spacer(modifier = Modifier.height(40.dp))

            // CREATE CLASS SECTION

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
                        onClick = { showComingSoon = true },
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
        }
    }

    // TEACHER PROFILE DIALOG

    if (showProfile) {

        AlertDialog(
            onDismissRequest = {
                showProfile = false
            },

            title = {
                Text(
                    text = "Teacher Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },

            text = {
                Column(
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

                    ProfileDetail(
                        label = "Teacher Name",
                        value = "Teacher Name"
                    )

                    ProfileDetail(
                        label = "Email ID",
                        value = "teacher@qhub.com"
                    )

                    ProfileDetail(
                        label = "Department",
                        value = "Department Name"
                    )

                    ProfileDetail(
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
                    Text("Close")
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
                        color = colors.error
                    )
                }
            }
        )
    }

    // CREATE CLASS PLACEHOLDER

    if (showComingSoon) {

        AlertDialog(
            onDismissRequest = {
                showComingSoon = false
            },

            title = {
                Text("Create New Class")
            },

            text = {
                Text(
                    "Class creation and student CSV upload " +
                            "will be added in the next development stage."
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        showComingSoon = false
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

// REUSABLE PROFILE INFORMATION

@Composable
fun ProfileDetail(
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
