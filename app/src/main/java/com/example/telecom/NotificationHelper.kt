package com.example.telecom

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.ui.incall.InCallActivity

object NotificationHelper {
    const val CHANNEL_INCOMING_CALLS = "echodeck_incoming_calls"
    const val CHANNEL_VOICEMAILS = "echodeck_voicemails"
    const val NOTIFICATION_ID_ACTIVE_CALL = 1001
    private const val NOTIFICATION_ID_VOICEMAIL_BASE = 2000

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val callChannel = NotificationChannel(
            CHANNEL_INCOMING_CALLS,
            context.getString(R.string.notification_channel_calls_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_calls_desc)
            setSound(
                ringtoneUri,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val voicemailChannel = NotificationChannel(
            CHANNEL_VOICEMAILS,
            context.getString(R.string.notification_channel_voicemail_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_voicemail_desc)
        }

        manager.createNotificationChannel(callChannel)
        manager.createNotificationChannel(voicemailChannel)
    }

    fun showIncomingCallNotification(context: Context, callerDisplay: String, callerNumber: String) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val fullScreenIntent = Intent(context, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
        }
        val fullScreenPending = PendingIntent.getActivity(
            context,
            10,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val answerPending = createActionPendingIntent(context, CallActionReceiver.ACTION_ANSWER, 11)
        val machinePending = createActionPendingIntent(context, CallActionReceiver.ACTION_MACHINE, 12)
        val declinePending = createActionPendingIntent(context, CallActionReceiver.ACTION_DECLINE, 13)

        val notification = NotificationCompat.Builder(context, CHANNEL_INCOMING_CALLS)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle(callerDisplay.ifBlank { callerNumber })
            .setContentText("Incoming call • EchoDeck")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenPending)
            .setFullScreenIntent(fullScreenPending, true)
            .addAction(0, context.getString(R.string.action_answer), answerPending)
            .addAction(0, context.getString(R.string.action_send_to_machine), machinePending)
            .addAction(0, context.getString(R.string.action_decline), declinePending)
            .build()

        manager.notify(NOTIFICATION_ID_ACTIVE_CALL, notification)
    }

    fun showAnsweringMachineActiveNotification(context: Context, callerDisplay: String, stageLabel: String) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val openInCall = PendingIntent.getActivity(
            context,
            14,
            Intent(context, InCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val endPending = createActionPendingIntent(context, CallActionReceiver.ACTION_DECLINE, 15)

        val notification = NotificationCompat.Builder(context, CHANNEL_VOICEMAILS)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Answering Machine • $callerDisplay")
            .setContentText(stageLabel)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openInCall)
            .addAction(0, "End & Save", endPending)
            .build()

        manager.notify(NOTIFICATION_ID_ACTIVE_CALL, notification)
    }

    fun cancelCallNotification(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.cancel(NOTIFICATION_ID_ACTIVE_CALL)
    }

    fun showNewVoicemailNotification(
        context: Context,
        messageId: Long,
        callerDisplay: String,
        durationSeconds: Int
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val openInbox = PendingIntent.getActivity(
            context,
            200 + messageId.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_tab", "inbox")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_VOICEMAILS)
            .setSmallIcon(android.R.drawable.stat_notify_voicemail)
            .setContentTitle("New message from $callerDisplay")
            .setContentText("Recorded ${durationSeconds}s answering machine message")
            .setAutoCancel(true)
            .setContentIntent(openInbox)
            .build()

        manager.notify(NOTIFICATION_ID_VOICEMAIL_BASE + messageId.toInt(), notification)
    }

    private fun createActionPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, CallActionReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
