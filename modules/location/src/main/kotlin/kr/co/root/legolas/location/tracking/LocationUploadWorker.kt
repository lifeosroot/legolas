package kr.co.root.legolas.location.tracking

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoints
import kr.co.root.legolas.location.data.shouldRetryLocationUpload
import kotlinx.coroutines.CancellationException

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
