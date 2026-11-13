
package com.qhub.quizapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object TeacherClassRepository {

    private val db = FirebaseFirestore.getInstance()

    private fun classReference(
        teacherId: String,
        classId: String
    ) = db.collection("teachers")
        .document(teacherId)
        .collection("classes")
        .document(classId)

    private fun verifyTeacher(teacherId: String) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        require(
            !currentUid.isNullOrBlank() && currentUid == teacherId
        ) {
            "Please log in again before managing this class."
        }
    }

    suspend fun renameClass(
        teacherId: String,
        classId: String,
        newClassName: String
    ) {
        verifyTeacher(teacherId)

        val updatedName = newClassName.trim()

        require(updatedName.isNotBlank()) {
            "Class name cannot be empty."
        }

        val classRef = classReference(teacherId, classId)

        val classSnapshot = classRef.get().awaitResult()

        require(classSnapshot.exists()) {
            "This class no longer exists."
        }

        val rosterSnapshot = db.collection("studentRoster")
            .whereEqualTo("teacherId", teacherId)
            .whereEqualTo("classId", classId)
            .get()
            .awaitResult()

        val quizSnapshot = classRef.collection("quizzes")
            .get()
            .awaitResult()

        val references = mutableListOf<DocumentReference>()

        references.add(classRef)

        references.addAll(
            rosterSnapshot.documents.map { it.reference }
        )

        references.addAll(
            quizSnapshot.documents.map { it.reference }
        )

        // Update in batches to remain below Firestore's batch limit.
        references.chunked(450).forEach { chunk ->

            val batch = db.batch()

            chunk.forEach { reference ->
                batch.update(
                    reference,
                    "className",
                    updatedName
                )
            }

            batch.commit().awaitResult()
        }
    }

    suspend fun deleteClass(
        teacherId: String,
        classId: String
    ) {
        verifyTeacher(teacherId)

        val classRef = classReference(teacherId, classId)

        val classSnapshot = classRef.get().awaitResult()

        require(classSnapshot.exists()) {
            "This class no longer exists."
        }

        val referencesToDelete = mutableListOf<DocumentReference>()

        // Get only this teacher's roster documents for this class.
        val rosterSnapshot = db.collection("studentRoster")
            .whereEqualTo("teacherId", teacherId)
            .whereEqualTo("classId", classId)
            .get()
            .awaitResult()

        referencesToDelete.addAll(
            rosterSnapshot.documents.map { it.reference }
        )

        // Get quizzes belonging only to this class.
        val quizSnapshot = classRef.collection("quizzes")
            .get()
            .awaitResult()

        for (quizDocument in quizSnapshot.documents) {

            val quizRef = quizDocument.reference

            // Delete nested attempts.
            val attemptsSnapshot = quizRef.collection("attempts")
                .get()
                .awaitResult()

            referencesToDelete.addAll(
                attemptsSnapshot.documents.map { it.reference }
            )

            // Delete nested answer keys.
            val answerKeysSnapshot = quizRef.collection("answerKeys")
                .get()
                .awaitResult()

            referencesToDelete.addAll(
                answerKeysSnapshot.documents.map { it.reference }
            )

            // Delete quiz document.
            referencesToDelete.add(quizRef)
        }

        // Delete collected documents in batches.
        referencesToDelete.chunked(450).forEach { chunk ->

            val batch = db.batch()

            chunk.forEach { reference ->
                batch.delete(reference)
            }

            batch.commit().awaitResult()
        }

        // Delete the class document last.
        classRef.delete().awaitResult()
    }
}

@Composable
fun TeacherClassManagementActions(
    teacherId: String,
    classId: String,
    className: String
) {
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    var showEditDialog by remember {
        mutableStateOf(false)
    }

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    var editedClassName by remember(className) {
        mutableStateOf(className)
    }

    var isWorking by remember {
        mutableStateOf(false)
    }

    var message by remember {
        mutableStateOf<String?>(null)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        TextButton(
            enabled = !isWorking,
            onClick = {
                editedClassName = className
                message = null
                showEditDialog = true
            }
        ) {
            Text("Edit")
        }

        TextButton(
            enabled = !isWorking,
            onClick = {
                message = null
                showDeleteDialog = true
            }
        ) {
            Text(
                "Delete",
                color = colors.error
            )
        }
    }

    // EDIT CLASS DIALOG
    if (showEditDialog) {

        AlertDialog(
            onDismissRequest = {
                if (!isWorking) {
                    showEditDialog = false
                    message = null
                }
            },

            title = {
                Text("Edit Class")
            },

            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    Text("Change the class name.")

                    OutlinedTextField(
                        value = editedClassName,
                        onValueChange = {
                            editedClassName = it
                        },
                        label = {
                            Text("Class Name")
                        },
                        singleLine = true,
                        enabled = !isWorking,
                        modifier = Modifier.fillMaxWidth()
                    )

                    message?.let {
                        Text(
                            it,
                            color = colors.error
                        )
                    }

                    if (isWorking) {
                        CircularProgressIndicator()
                    }
                }
            },

            confirmButton = {

                TextButton(
                    enabled = !isWorking &&
                            editedClassName.isNotBlank(),

                    onClick = {

                        scope.launch {

                            isWorking = true
                            message = null

                            try {

                                TeacherClassRepository.renameClass(
                                    teacherId = teacherId,
                                    classId = classId,
                                    newClassName = editedClassName
                                )

                                showEditDialog = false

                            } catch (exception: Exception) {

                                message = exception.localizedMessage
                                    ?: "Could not update class."

                            } finally {
                                isWorking = false
                            }
                        }
                    }
                ) {
                    Text("Save Changes")
                }
            },

            dismissButton = {

                TextButton(
                    enabled = !isWorking,
                    onClick = {
                        showEditDialog = false
                        message = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // DELETE CLASS CONFIRMATION
    if (showDeleteDialog) {

        AlertDialog(
            onDismissRequest = {
                if (!isWorking) {
                    showDeleteDialog = false
                    message = null
                }
            },

            title = {
                Text("Delete Class?")
            },

            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    Text(
                        "Are you sure you want to delete \"$className\"?"
                    )

                    Text(
                        "This permanently deletes this class, its CSV roster, " +
                                "quizzes, quiz attempts and answer keys."
                    )

                    Text(
                        "Student accounts and your other classes will remain untouched."
                    )

                    Text(
                        "This action cannot be undone.",
                        color = colors.error
                    )

                    message?.let {
                        Text(
                            it,
                            color = colors.error
                        )
                    }

                    if (isWorking) {
                        CircularProgressIndicator()
                    }
                }
            },

            confirmButton = {

                TextButton(
                    enabled = !isWorking,

                    onClick = {

                        scope.launch {

                            isWorking = true
                            message = null

                            try {

                                TeacherClassRepository.deleteClass(
                                    teacherId = teacherId,
                                    classId = classId
                                )

                                showDeleteDialog = false

                            } catch (exception: Exception) {

                                message = exception.localizedMessage
                                    ?: "Could not delete class."

                            } finally {
                                isWorking = false
                            }
                        }
                    }
                ) {
                    Text(
                        "Delete Permanently",
                        color = colors.error
                    )
                }
            },

            dismissButton = {

                TextButton(
                    enabled = !isWorking,
                    onClick = {
                        showDeleteDialog = false
                        message = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

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
