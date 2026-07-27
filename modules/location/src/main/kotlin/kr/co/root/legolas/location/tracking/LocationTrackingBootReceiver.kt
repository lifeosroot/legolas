package kr.co.root.legolas.location.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.root.legolas.location.data.LocationSettingsRepository
import javax.inject.Inject

@AndroidEntryPoint
class LocationTrackingBootReceiver : BroadcastReceiver() {
    @Inject lateinit var settings: LocationSettingsRepository
    @Inject lateinit var commander: LocationTrackingCommander

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (settings.enabled.first()) {
                    commander.start(requireBackgroundLocation = true)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
