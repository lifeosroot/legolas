package kr.co.root.legolas.location.data

import java.io.IOException

internal class LocationUploadException(
    message: String,
    val retryable: Boolean,
) : IOException(message)

internal fun Throwable.shouldRetryLocationUpload(): Boolean = when (this) {
    is LocationUploadException -> retryable
    is IOException -> true
    else -> false
}
