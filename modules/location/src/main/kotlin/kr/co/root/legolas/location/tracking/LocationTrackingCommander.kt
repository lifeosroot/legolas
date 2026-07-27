package kr.co.root.legolas.location.tracking

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kr.co.root.legolas.location.data.LocationSampleRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationTrackingCommander @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: LocationSampleRepository,
    private val uploadScheduler: LocationUploadScheduler,
) {
    suspend fun setTrackingEnabled(enabled: Boolean) {
        if (enabled) {
            repository.setTrackingEnabled(true)
            start(requireBackgroundLocation = true)
        } else {
            repository.setTrackingEnabled(false)
            uploadScheduler.cancel()
            stop()
        }
    }

    suspend fun disableAndClear() {
        repository.setTrackingEnabled(false)
        uploadScheduler.cancel()
        stop()
        repository.clearQueue()
    }

    suspend fun start(requireBackgroundLocation: Boolean = false) {
        if (!context.hasRootLocationPermission()) {
            repository.setTrackingEnabled(false)
            repository.setServiceRunning(false)
            repository.setLastErrorMessage("위치 권한이 없어 경로 저장을 시작할 수 없습니다.")
            return
        }

        if (!context.hasRootLocationServicesEnabled()) {
            repository.setServiceRunning(false)
            repository.setLastErrorMessage("기기의 위치 기능이 꺼져 있어 위치 수집을 시작할 수 없습니다.")
            return
        }

        if (requireBackgroundLocation && !context.hasRootBackgroundLocationPermission()) {
            repository.setServiceRunning(false)
            repository.setLastErrorMessage("자동 위치 추적을 다시 시작하려면 위치 권한을 '항상 허용'으로 바꿔 주세요.")
            return
        }

        if (!context.hasRootActivityRecognitionPermission()) {
            repository.setServiceRunning(false)
            repository.setLastErrorMessage("활동 인식 권한이 없어 위치 수집을 시작할 수 없습니다.")
            return
        }

        if (!context.hasRootNotificationPermission()) {
            repository.setServiceRunning(false)
            repository.setLastErrorMessage("알림 권한이 없어 위치 수집을 시작할 수 없습니다.")
            return
        }

        runCatching {
            ContextCompat.startForegroundService(
                context,
                Intent(context, LocationTrackingService::class.java)
                    .setAction(LocationTrackingService.ActionStart),
            )
        }.onFailure { throwable ->
            repository.setServiceRunning(false)
            repository.setLastErrorMessage(
                throwable.localizedMessage ?: "위치 서비스를 시작할 수 없습니다.",
            )
        }
    }

    fun stop() {
        context.startService(
            Intent(context, LocationTrackingService::class.java)
                .setAction(LocationTrackingService.ActionStop),
        )
    }
}
