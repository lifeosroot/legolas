package kr.co.root.legolas.location.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.location.LocationManagerCompat

data class LocationPermissionStatus(
    val accuracy: LocationAccuracy,
    val hasBackgroundAccess: Boolean,
    val hasActivityRecognitionAccess: Boolean,
    val hasNotificationAccess: Boolean,
    val hasLocalNetworkAccess: Boolean,
    val isSystemLocationEnabled: Boolean,
) {
    val hasForegroundAccess: Boolean
        get() = accuracy != LocationAccuracy.None

    val hasPreciseAccess: Boolean
        get() = accuracy == LocationAccuracy.Precise

    val canTrackInBackground: Boolean
        get() = hasPreciseAccess &&
            hasBackgroundAccess &&
            hasActivityRecognitionAccess &&
            hasNotificationAccess &&
            isSystemLocationEnabled
}

fun Context.locationPermissionStatus(): LocationPermissionStatus =
    locationPermissionStatus(
        sdkInt = Build.VERSION.SDK_INT,
        hasFineAccess = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION),
        hasCoarseAccess = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION),
        hasBackgroundAccess = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
        hasActivityRecognitionAccess = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            hasPermission(Manifest.permission.ACTIVITY_RECOGNITION),
        hasNotificationAccess = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            hasPermission(Manifest.permission.POST_NOTIFICATIONS),
        hasLocalNetworkAccess = Build.VERSION.SDK_INT < Android17ApiLevel ||
            hasPermission(Manifest.permission.ACCESS_LOCAL_NETWORK),
        isSystemLocationEnabled = LocationManagerCompat.isLocationEnabled(
            getSystemService(Context.LOCATION_SERVICE) as LocationManager,
        ),
    )

internal fun locationPermissionStatus(
    sdkInt: Int,
    hasFineAccess: Boolean,
    hasCoarseAccess: Boolean,
    hasBackgroundAccess: Boolean,
    hasActivityRecognitionAccess: Boolean = true,
    hasNotificationAccess: Boolean = true,
    hasLocalNetworkAccess: Boolean = true,
    isSystemLocationEnabled: Boolean = true,
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
        hasActivityRecognitionAccess = sdkInt < Build.VERSION_CODES.Q ||
            hasActivityRecognitionAccess,
        hasNotificationAccess = sdkInt < Build.VERSION_CODES.TIRAMISU ||
            hasNotificationAccess,
        hasLocalNetworkAccess = sdkInt < Android17ApiLevel || hasLocalNetworkAccess,
        isSystemLocationEnabled = isSystemLocationEnabled,
    )
}

fun Context.appLocationSettingsIntent(): Intent =
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    )

fun systemLocationSettingsIntent(): Intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

private fun Context.hasPermission(permission: String): Boolean =
    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

private const val Android17ApiLevel = 37
