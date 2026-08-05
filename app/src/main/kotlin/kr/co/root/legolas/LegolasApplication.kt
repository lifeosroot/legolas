package kr.co.root.legolas

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kr.co.root.legolas.pairing.data.PairingHealthMonitor
import javax.inject.Inject

@HiltAndroidApp
class LegolasApplication : Application() {
    @Inject lateinit var pairingHealthMonitor: PairingHealthMonitor

    override fun onCreate() {
        super.onCreate()
        pairingHealthMonitor.start()
    }
}
