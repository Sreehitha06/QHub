
package com.qhub.quizapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StudentDetailsScreen(
    email: String,
    existingDetails: StudentDetails? = null,
    onSave: (
        String,
        String,
        String,
        String,
        String
    ) -> Unit,
    onBack: () -> Unit = {}
) {
    var name by remember(existingDetails) {
        mutableStateOf(existingDetails?.name ?: "")
    }

    var rollNumber by remember(existingDetails) {
        mutableStateOf(existingDetails?.rollNumber ?: "")
    }

    var department by remember(existingDetails) {
        mutableStateOf(existingDetails?.department ?: "")
    }

    var section by remember(existingDetails) {
        mutableStateOf(existingDetails?.section ?: "")
    }

    var semester by remember(existingDetails) {
        mutableStateOf(existingDetails?.semester ?: "")
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // BACK BUTTON - VISIBLE FOR FIRST-TIME AND EXISTING STUDENTS
        TextButton(
            onClick = onBack,
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = "← Back to Dashboard",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = if (existingDetails == null)
                "Student Details"
            else
                "Edit Student Details",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Please enter your details",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = rollNumber,
            onValueChange = { rollNumber = it },
            label = { Text("Roll Number") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = email,
            onValueChange = {},
            label = { Text("Email ID") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            singleLine = true
        )

        OutlinedTextField(
            value = department,
            onValueChange = { department = it },
            label = { Text("Department") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = section,
            onValueChange = { section = it },
            label = { Text("Section") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = semester,
            onValueChange = { semester = it },
            label = { Text("Semester") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = {
                if (
                    name.isBlank() ||
                    rollNumber.isBlank() ||
                    email.isBlank() ||
                    department.isBlank() ||
                    section.isBlank() ||
                    semester.isBlank()
                ) {
                    errorMessage = "Please fill in all the fields."
                } else {
                    errorMessage = ""
                    onSave(
                        name,
                        rollNumber,
                        department,
                        section,
                        semester
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Save Details")
        }
    }
}
