package com.example.focus

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

data class FocusLockPlatformState(
    val alertsGranted: Boolean = false,
    val overlayHookGranted: Boolean = false,
    val usageAccessHookGranted: Boolean = false,
    val dndPolicyGranted: Boolean = false,
    val isScreenPinned: Boolean = false,
    val photosAndFilesReady: Boolean = true
)

/**
 * Play-Policy-compliant Focus Lock & Device Integration Bridge.
 *
 * - Provides full in-app strict focus lock, back-button trapping, and high-priority return alerts.
 * - Exposes native Android Screen Pinning (`startLockTask`) + read/launch integration hooks for
 *   Display Over Other Apps, Usage Access, and Notification Policy when packaged as a native build,
 *   without declaring restricted Play Store permissions (`SYSTEM_ALERT_WINDOW`, `PACKAGE_USAGE_STATS`).
 * - Photo & study file uploads continue to use the zero-permission Android Photo Picker & SAF.
 */
object FocusLockBridge {

    private const val CHANNEL_ID = "unisync_focus_lock"
    private const val RETURN_ALERT_NOTIFICATION_ID = 4201

    fun readPlatformState(context: Context): FocusLockPlatformState {
        val alertsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

        val overlayGranted = runCatching {
            Settings.canDrawOverlays(context)
        }.getOrDefault(false)

        val usageGranted = runCatching {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        }.getOrDefault(false)

        val dndGranted = runCatching {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.isNotificationPolicyAccessGranted
        }.getOrDefault(false)

        val pinnedNow = runCatching {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
        }.getOrDefault(false)

        return FocusLockPlatformState(
            alertsGranted = alertsGranted,
            overlayHookGranted = overlayGranted,
            usageAccessHookGranted = usageGranted,
            dndPolicyGranted = dndGranted,
            isScreenPinned = pinnedNow,
            photosAndFilesReady = true
        )
    }

    fun toggleScreenPinning(context: Context, enable: Boolean): Boolean {
        val activity = context.findActivity() ?: return false
        return runCatching {
            if (enable) {
                activity.startLockTask()
            } else {
                activity.stopLockTask()
            }
            true
        }.getOrDefault(false)
    }

    fun openNotificationSettings(context: Context): Boolean = runCatching {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    fun openOverlaySettings(context: Context): Boolean = runCatching {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    fun openUsageAccessSettings(context: Context): Boolean = runCatching {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    @SuppressLint("MissingPermission")
    fun sendFocusReturnAlertNotification(
        context: Context,
        title: String,
        message: String
    ) {
        val state = readPlatformState(context)
        if (!state.alertsGranted) return

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        "UniSync Focus Lock Alerts",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Alerts when leaving an active Pomodoro focus lock session or hitting a 30-minute milestone."
                    }
                    nm.createNotificationChannel(channel)
                }
            }

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(context)
                .notify(RETURN_ALERT_NOTIFICATION_ID, notification)
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
