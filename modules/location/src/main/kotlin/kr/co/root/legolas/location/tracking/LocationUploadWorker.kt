package kr.co.root.legolas.location.tracking

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kr.co.root.legolas.location.data.LocationUploader
import kr.co.root.legolas.location.data.shouldRetryLocationUpload
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

class LocationUploadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val uploader = EntryPoints.get(
            applicationContext,
            LocationUploadEntryPoint::class.java,
        ).uploader()
        return try {
            uploader.flush()
            Result.success()
        } catch (exception: CancellationException) {
            throw exception
        } catch (throwable: Throwable) {
            if (throwable.shouldRetryLocationUpload()) Result.retry() else Result.failure()
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LocationUploadEntryPoint {
    fun uploader(): LocationUploader
}

@Singleton
class LocationUploadScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun retryWhenConnected() {
        val request = OneTimeWorkRequestBuilder<LocationUploadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private companion object {
        const val WORK_NAME = "legolas_location_upload"
    }
}
