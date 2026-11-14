
package com.qhub.quizapp.student

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.qhub.quizapp.MainActivity
import com.qhub.quizapp.data.StudentQuizRecord
import com.qhub.quizapp.data.StudentQuizStatus
import java.text.SimpleDateFormat
import java.util.Locale

object QuizNotificationScheduler {

    private const val CHANNEL_ID = "qhub_quiz_reminders"
    private const val PREFS_NAME = "qhub_notification_prefs"
    private const val SCHEDULED_KEYS = "scheduled_quiz_keys"

    private const val REMINDER_BEFORE_MILLIS = 10 * 60 * 1000L

    fun schedule(
        context: Context,
        records: List<StudentQuizRecord>
    ) {
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val preferences = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val previousKeys =
            preferences.getStringSet(SCHEDULED_KEYS, emptySet())?.toSet()
                ?: emptySet()

        val activeKeys = mutableSetOf<String>()
        val currentTime = System.currentTimeMillis()

        records
            .filter { it.status == StudentQuizStatus.UPCOMING }
            .forEach { record ->

                val key =
                    "${record.teacherId}|${record.classId}|${record.quizId}"

                val quizStartTime = parseSchedule(
                    record.scheduledDate,
                    record.scheduledTime
                )

                if (quizStartTime == null) {
                    Log.e(
                        "QHubNotification",
                        "Could not parse quiz schedule: ${record.scheduledDate} ${record.scheduledTime}"
                    )
                    return@forEach
                }

                val reminderTime =
                    quizStartTime - REMINDER_BEFORE_MILLIS

                if (reminderTime <= currentTime) {
                    return@forEach
                }

                activeKeys.add(key)

                val intent = Intent(
                    context,
                    QuizReminderReceiver::class.java
                ).apply {
                    data = Uri.parse(
                        "qhub://quiz-reminder/${Uri.encode(key)}"
                    )
                    putExtra("subjectName", record.subjectName)
                    putExtra("quizId", record.quizId)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    key.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                            PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                reminderTime,
                                pendingIntent
                            )
                            Log.d(
                                "QHubNotification",
                                "Exact reminder scheduled for $reminderTime"
                            )
                        } else {
                            alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                reminderTime,
                                pendingIntent
                            )
                            Log.w(
                                "QHubNotification",
                                "Exact alarm access not granted. Using inexact alarm."
                            )
                        }

                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminderTime,
                            pendingIntent
                        )

                    } else {

                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            reminderTime,
                            pendingIntent
                        )
                    }

                } catch (e: SecurityException) {
                    Log.e(
                        "QHubNotification",
                        "Unable to schedule reminder",
                        e
                    )
                }
            }

        // Cancel reminders for quizzes that are no longer upcoming.
        (previousKeys - activeKeys).forEach { key ->

            val intent = Intent(
                context,
                QuizReminderReceiver::class.java
            ).apply {
                data = Uri.parse(
                    "qhub://quiz-reminder/${Uri.encode(key)}"
                )
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                key.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }

        preferences.edit()
            .putStringSet(SCHEDULED_KEYS, activeKeys)
            .apply()
    }

    private fun parseSchedule(
        date: String,
        time: String
    ): Long? {
        return try {
            val formatter = SimpleDateFormat(
                "yyyy-MM-dd HH:mm",
                Locale.US
            ).apply {
                isLenient = false
            }

            formatter.parse("$date $time")?.time

        } catch (e: Exception) {
            Log.e(
                "QHubNotification",
                "Schedule date/time parsing failed",
                e
            )
            null
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Quiz Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description =
                    "Reminders before scheduled QHub quizzes"
            }

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        context: Context,
        subjectName: String,
        quizId: String
    ) {
        createNotificationChannel(context)

        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(
                "QHubNotification",
                "Notification permission not granted"
            )
            return
        }

        val openAppIntent = Intent(
            context,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            quizId.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(com.qhub.quizapp.R.drawable.ic_launcher_foreground)
            .setContentTitle("Quiz starting soon!")
            .setContentText("$subjectName quiz starts in 10 minutes.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "$subjectName quiz starts in 10 minutes. Get ready!"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(quizId.hashCode(), notification)

            Log.d(
                "QHubNotification",
                "Notification displayed for $subjectName"
            )

        } catch (e: SecurityException) {
            Log.e(
                "QHubNotification",
                "Unable to display notification",
                e
            )
        }
    }
}

class QuizReminderReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        val subjectName =
            intent.getStringExtra("subjectName") ?: "Your"

        val quizId =
            intent.getStringExtra("quizId") ?: return

        Log.d(
            "QHubNotification",
            "Reminder alarm received for $subjectName"
        )

        QuizNotificationScheduler.showNotification(
            context,
            subjectName,
            quizId
        )
    }
}
