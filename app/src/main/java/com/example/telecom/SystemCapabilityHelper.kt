package com.example.telecom

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat

data class SystemReadiness(
    val isDefaultDialer: Boolean = false,
    val isCallScreeningApp: Boolean = false,
    val hasRecordAudio: Boolean = false,
    val hasPhonePermissions: Boolean = false,
    val hasNotifications: Boolean = false
) {
    val allReady: Boolean
        get() = isDefaultDialer && hasRecordAudio && hasPhonePermissions && hasNotifications
}

object SystemCapabilityHelper {

    fun checkReadiness(context: Context): SystemReadiness {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val isDialerRole = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            roleManager?.isRoleHeld(RoleManager.ROLE_DIALER) == true ||
                telecomManager?.defaultDialerPackage == context.packageName
        } else {
            telecomManager?.defaultDialerPackage == context.packageName
        }

        val isScreeningRole = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        } else {
            false
        }

        val hasRecordAudio = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val hasPhoneState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        val hasAnswerCalls = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ANSWER_PHONE_CALLS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val hasCallPhone = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return SystemReadiness(
            isDefaultDialer = isDialerRole,
            isCallScreeningApp = isScreeningRole,
            hasRecordAudio = hasRecordAudio,
            hasPhonePermissions = hasPhoneState && hasAnswerCalls && hasCallPhone,
            hasNotifications = hasNotifications
        )
    }

    fun createRequestDefaultDialerIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            }
        }
        return Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
            putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
        }
    }

    fun createRequestCallScreeningIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            }
        }
        return null
    }

    fun placeCall(context: Context, rawNumber: String): Boolean {
        val cleaned = rawNumber.trim()
        if (cleaned.isEmpty()) return false
        val uri = Uri.fromParts("tel", cleaned, null)
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCallPermission) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            val placed = runCatching {
                telecomManager?.placeCall(uri, Bundle())
                true
            }.getOrDefault(false)
            if (placed) return true
        }

        return runCatching {
            val intent = Intent(Intent.ACTION_DIAL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
