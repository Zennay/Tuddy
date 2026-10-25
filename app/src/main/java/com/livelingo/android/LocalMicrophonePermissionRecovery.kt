package com.livelingo.android

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings

/**
 * Foreground, user-initiated recovery from denied Android microphone access.
 * Opening system app details NEVER opens a microphone or changes a setting.
 * The Activity rechecks the real permission when it resumes; it must not
 * automatically restart recognition after a visit to Android Settings.
 */
internal object LocalMicrophonePermissionRecovery {
    fun granted(context: Context): Boolean = runCatching {
        context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    fun mayAskAgain(activity: Activity?): Boolean = runCatching {
        activity?.shouldShowRequestPermissionRationale(
            Manifest.permission.RECORD_AUDIO
        ) == true
    }.getOrDefault(false)

    fun openSystemAppDetails(context: Context): Boolean {
        if (context.packageName.isBlank()) return false
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // No reflection, hidden APIs, privileged settings write or forced
        // permission dialog. The user remains in control of Android settings.
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
