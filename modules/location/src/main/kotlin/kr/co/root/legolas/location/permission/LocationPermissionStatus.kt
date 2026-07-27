package kr.co.root.legolas.location.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

enum class LocationAccuracy {
    None,
    Approximate,
    Precise,
}

data class LocationPermissionStatus(
    val accuracy: LocationAccuracy,
    val hasBackgroundAccess: Boolean,
) {
    val hasForegroundAccess: Boolean
        get() = accuracy != LocationAccuracy.None

    val canTrackInBackground: Boolean
        get() = hasForegroundAccess && hasBackgroundAccess
}

fun Context.locationPermissionStatus(): LocationPermissionStatus =
    locationPermissionStatus(
        sdkInt = Build.VERSION.SDK_INT,
        hasFineAccess = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION),
        hasCoarseAccess = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION),
        hasBackgroundAccess = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
    )

internal fun locationPermissionStatus(
    sdkInt: Int,
    hasFineAccess: Boolean,
    hasCoarseAccess: Boolean,
    hasBackgroundAccess: Boolean,
): LocationPermissionStatus {
    val accuracy = when {
        hasFineAccess -> LocationAccuracy.Precise
        hasCoarseAccess -> LocationAccuracy.Approximate
        else -> LocationAccuracy.None
    }
    return LocationPermissionStatus(
        accuracy = accuracy,
        hasBackgroundAccess = accuracy != LocationAccuracy.None &&
            (sdkInt < Build.VERSION_CODES.Q || hasBackgroundAccess),
    )
}

fun Context.appLocationSettingsIntent(): Intent =
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    )

private fun Context.hasPermission(permission: String): Boolean =
    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
