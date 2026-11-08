package com.aistudio.yoruhime.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aistudio.yoruhime.MainActivity
import com.aistudio.yoruhime.R
import com.aistudio.yoruhime.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

class BedtimeWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val customPhrase = try {
            AppDatabase.getDatabase(appContext).bondDao().getBond()?.customBedtimePhrase
        } catch (e: Exception) {
            null
        }
        showBedtimeNotification(appContext, customPhrase)
        Result.success()
    }

    companion object {
        const val CHANNEL_ID = "yoruhime_bedtime"
        const val NOTIFICATION_ID = 1001
        const val WORK_NAME = "ayaka_bedtime_worker"

        const val DEFAULT_CURFEW_PHRASE =
            "It's 11:00 AM curfew! Put your phone down right now and take a rest, you hopeless slacker!"

        fun scheduleCurfew(context: Context, hour: Int = 11, minute: Int = 0) {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now) || equals(now)) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            val initialDelayMillis = target.timeInMillis - now.timeInMillis
            val safeDelay = maxOf(initialDelayMillis, 1000L)

            val workRequest = PeriodicWorkRequestBuilder<BedtimeWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(safeDelay, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(androidx.work.BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
                .setConstraints(Constraints.NONE)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

        fun scheduleDailyMidnight(context: Context) {
            scheduleCurfew(context, 11, 0)
        }

        fun showBedtimeNotification(context: Context, customPhrase: String? = null) {
            createNotificationChannel(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val message = if (!customPhrase.isNullOrBlank()) {
                customPhrase
            } else {
                val scoldMessages = listOf(
                    DEFAULT_CURFEW_PHRASE,
                    "Do you have any idea what time it is?! Look at the clock! Stop staring at your screen or you'll collapse!",
                    "Still glued to your phone?! If you push yourself too hard and get sick, I'm NOT nursing you! Take a break already!"
                )
                scoldMessages.random()
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Curfew Warning from Ayaka")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
            } catch (e: SecurityException) {
                // If notification permission not yet granted on Android 13+
            }
        }

        private fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Ayaka Bedtime & Curfew"
                val descriptionText = "Curfew alerts and health scoldings from Ayaka"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    enableVibration(true)
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}
