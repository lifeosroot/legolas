package kr.co.root.legolas.location.data

fun LocationSampleRequest.asEntity() = LocationSampleEntity(
    clientSampleId = clientSampleId,
    collectedAtMillis = collectedAtMillis,
    latitude = latitude,
    longitude = longitude,
    horizontalAccuracyM = horizontalAccuracyM,
    source = source,
    activityType = activityType,
    saveReason = saveReason,
)

fun LocationSampleEntity.asRequest() = LocationSampleRequest(
    clientSampleId = clientSampleId,
    collectedAtMillis = collectedAtMillis,
    latitude = latitude,
    longitude = longitude,
    horizontalAccuracyM = horizontalAccuracyM,
    source = source,
    activityType = activityType,
    saveReason = saveReason,
)
