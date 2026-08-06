package kr.co.root.legolas.location.tracking

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.root.legolas.location.data.LocationSampleRepository
import kr.co.root.legolas.location.data.LocationSampleRequest
import kr.co.root.legolas.location.data.LocationUploader
import kr.co.root.legolas.location.data.shouldRetryLocationUpload
import java.time.Instant
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject lateinit var fusedLocationClient: FusedLocationProviderClient
    @Inject lateinit var activityRecognitionClient: ActivityRecognitionClient
    @Inject lateinit var repository: LocationSampleRepository
    @Inject lateinit var sampleFactory: AndroidLocationSampleFactory
    @Inject lateinit var notification: LocationTrackingNotification
    @Inject lateinit var uploader: LocationUploader
    @Inject lateinit var uploadScheduler: LocationUploadScheduler

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var trackingStarted = false
    private var trackingGeneration = 0L
    private var initializationComplete = false
    private var initializationJob: Job? = null
    private var stoppingJob: Job? = null
    private val pendingActivityIntents = ArrayDeque<Intent>()
    private var currentMotion = RootActivityMotion.Unknown
    private var currentActivityType = AndroidActivityTypeUnknown
    private var currentMotionStartedAtMillis = System.currentTimeMillis()
    private var activePolicy = RootLocationRequestPolicy.forActivity(currentMotion, currentActivityType)
    private var lastAcceptedAt: Instant? = null
    private var movingCandidateStartedAtMillis: Long? = null
    private var pedestrianCandidateSuppressedUntilMillis: Long = 0L
    private var movingDegradedStillEvidenceCount: Int = 0
    private var movingStillEvidenceCount: Int = 0
    private var vehiclePedestrianEvidenceCount: Int = 0
    private var lastActivityRecognitionUpdateDiagnosticAtMillis: Long = 0L
    private var latestAccurateLocation: Location? = null
    private val motionGate = RootLocationMotionGate()
    private val sampleGate = RootLocationSampleGate()
    private val movingCandidateStartBuffer = RootMovingCandidateStartBuffer<Location>()
    private val movingCandidateDiagnostics = RootMovingCandidateDiagnostics()
    private val arrivalStopDeduper = RootArrivalStopDeduper()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations
                .sortedBy(Location::getTime)
                .forEach { location -> handleLocation(location) }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ActionStop -> {
                stopTracking(startId)
                return START_NOT_STICKY
            }
            ActionDisable -> {
                stopTracking(startId, disableTracking = true)
                return START_NOT_STICKY
            }
            ActionActivityUpdate -> {
                if (initializationComplete) {
                    handleActivityIntent(intent)
                } else {
                    pendingActivityIntents += Intent(intent)
                    startTracking(startId)
                }
            }
            else -> startTracking(startId)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        trackingStarted = false
        trackingGeneration += 1
        initializationComplete = false
        initializationJob?.cancel()
        stopLocationUpdates()
        unregisterActivityUpdates()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startTracking(startId: Int) {
        if (trackingStarted) {
            serviceScope.launch { uploadPendingSamples() }
            return
        }
        stoppingJob?.cancel()
        stoppingJob = null
        if (!hasLocationPermission()) {
            stoppingJob = serviceScope.launch {
                repository.setTrackingEnabled(false)
                repository.setLastErrorMessage("정확한 위치 권한이 없어 경로 저장을 시작할 수 없습니다.")
                repository.setServiceRunning(false)
                stopSelfResult(startId)
            }
            return
        }
        if (!hasRootLocationServicesEnabled()) {
            stoppingJob = serviceScope.launch {
                repository.setLastErrorMessage("기기의 위치 기능이 꺼져 있어 경로 저장을 시작할 수 없습니다.")
                repository.setServiceRunning(false)
                stopSelfResult(startId)
            }
            return
        }

        runCatching {
            ServiceCompat.startForeground(
                this,
                LocationTrackingNotification.NOTIFICATION_ID,
                notification.build(currentMotion),
                if (Build.VERSION.SDK_INT >= 29) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                } else {
                    0
                },
            )
        }.onFailure { throwable ->
            stoppingJob = serviceScope.launch {
                repository.setServiceRunning(false)
                repository.setLastErrorMessage(
                    throwable.localizedMessage ?: "위치 서비스를 시작할 수 없습니다.",
                )
                stopSelfResult(startId)
            }
            return
        }
        trackingStarted = true
        trackingGeneration += 1

        initializationJob = serviceScope.launch {
            try {
                val savedState = repository.trackingState.first()
                if (!trackingStarted) return@launch
                if (!canStartLocationTracking(
                        enabled = savedState.enabled,
                        hasPreciseLocation = hasRootLocationPermission(),
                        hasBackgroundLocation = hasRootBackgroundLocationPermission(),
                        hasActivityRecognition = hasRootActivityRecognitionPermission(),
                        hasNotifications = hasRootNotificationPermission(),
                        isSystemLocationEnabled = hasRootLocationServicesEnabled(),
                    )
                ) {
                    stopAfterInitialization(startId)
                    return@launch
                }
                repository.setServiceRunning(true)
                repository.setMotionState(currentMotion.storageKey)
                repository.refreshQueueCount()
                lastAcceptedAt = savedState.lastCollectedAtMillis?.let(Instant::ofEpochMilli)
                if (!trackingStarted) return@launch
                notification.update(currentMotion)
                registerActivityUpdates()
                if (!trackingStarted) return@launch
                restartLocationUpdates(activePolicy)
                if (!trackingStarted) return@launch
                requestBootstrapLocation()
                initializationComplete = true
                while (trackingStarted && pendingActivityIntents.isNotEmpty()) {
                    handleActivityIntent(pendingActivityIntents.removeFirst())
                }
                if (!trackingStarted) return@launch
                uploadPendingSamples()
            } catch (exception: CancellationException) {
                throw exception
            } catch (throwable: Throwable) {
                repository.setLastErrorMessage(
                    throwable.localizedMessage ?: "위치 서비스를 초기화할 수 없습니다.",
                )
                stopAfterInitialization(startId)
            }
        }
    }

    private fun stopTracking(startId: Int, disableTracking: Boolean = false) {
        trackingStarted = false
        trackingGeneration += 1
        initializationComplete = false
        pendingActivityIntents.clear()
        initializationJob?.cancel()
        initializationJob = null
        stopLocationUpdates()
        unregisterActivityUpdates()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        if (disableTracking) uploadScheduler.cancel()
        stoppingJob?.cancel()
        stoppingJob = serviceScope.launch {
            if (disableTracking) repository.setTrackingEnabled(false)
            repository.setServiceRunning(false)
            repository.setMotionState(RootActivityMotion.Unknown.storageKey)
            stopSelfResult(startId)
        }
    }

    private suspend fun stopAfterInitialization(startId: Int) {
        trackingStarted = false
        trackingGeneration += 1
        initializationComplete = false
        pendingActivityIntents.clear()
        stopLocationUpdates()
        unregisterActivityUpdates()
        repository.setServiceRunning(false)
        repository.setMotionState(RootActivityMotion.Unknown.storageKey)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelfResult(startId)
    }

    private fun stopWithRuntimeError(generation: Long, message: String) {
        if (!trackingStarted || trackingGeneration != generation) return
        trackingStarted = false
        trackingGeneration += 1
        initializationComplete = false
        pendingActivityIntents.clear()
        initializationJob?.cancel()
        initializationJob = null
        stopLocationUpdates()
        unregisterActivityUpdates()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stoppingJob?.cancel()
        stoppingJob = serviceScope.launch {
            repository.setLastErrorMessage(message)
            repository.setServiceRunning(false)
            repository.setMotionState(RootActivityMotion.Unknown.storageKey)
            stopSelf()
        }
    }

    private fun handleActivityIntent(intent: Intent) {
        if (!ActivityRecognitionResult.hasResult(intent)) {
            recordActivityRecognitionIssue(
                element = ActivityRecognitionDiagnosticIntentWithoutResult,
                message = "ActivityRecognition update received without a result",
            )
            return
        }
        val result = ActivityRecognitionResult.extractResult(intent) ?: run {
            recordActivityRecognitionIssue(
                element = ActivityRecognitionDiagnosticExtractResultNull,
                message = "ActivityRecognition result extraction returned null",
            )
            return
        }
        val classification = result.toRootActivityClassification(currentMotion, currentActivityType)
        val nowMillis = System.currentTimeMillis()
        recordActivityRecognitionUpdateDiagnostic(classification, nowMillis)
        if (classification.motion != RootActivityMotion.Still) {
            movingStillEvidenceCount = 0
        }
        val pedestrianActivityType = strongPedestrianActivityType(classification.activityConfidences)
        if (currentActivityType == AndroidActivityTypeInVehicle && pedestrianActivityType != null) {
            vehiclePedestrianEvidenceCount += 1
            if (vehiclePedestrianEvidenceCount >= VehiclePedestrianReleaseRequiredCount ||
                hasVehiclePedestrianGpsEvidence(nowMillis)
            ) {
                vehiclePedestrianEvidenceCount = 0
                applyMotionState(
                    motion = RootActivityMotion.Moving,
                    activityType = pedestrianActivityType,
                    nowMillis = nowMillis,
                    reason = MotionTransitionReasonActivityRecognition,
                )
                return
            }
        } else if (currentActivityType == AndroidActivityTypeInVehicle) {
            vehiclePedestrianEvidenceCount = 0
        }
        if (classification.activityType == AndroidActivityTypeInVehicle &&
            shouldExpireVehicleByGps(nowMillis, allowInactiveVehicle = true)
        ) {
            applyStillState(
                nowMillis = nowMillis,
                reason = MotionTransitionReasonVehicleExpired,
                arrivalLocation = latestAccurateLocation,
            )
            return
        }
        when (classification.motion) {
            RootActivityMotion.MovingCandidate -> handleMovingCandidateActivity(classification, nowMillis)
            RootActivityMotion.Still -> handleStillActivity(classification, nowMillis)
            RootActivityMotion.MovingDegraded -> {
                movingDegradedStillEvidenceCount = 0
            }

            RootActivityMotion.Moving,
            RootActivityMotion.Unknown,
            -> {
                if (currentMotion == RootActivityMotion.MovingDegraded) {
                    movingDegradedStillEvidenceCount = 0
                    return
                }
                applyMotionState(
                    motion = classification.motion,
                    activityType = classification.activityType,
                    nowMillis = nowMillis,
                    reason = MotionTransitionReasonActivityRecognition,
                )
            }
        }
    }

    private fun handleMovingCandidateActivity(
        classification: RootActivityClassification,
        nowMillis: Long,
    ) {
        if (currentMotion == RootActivityMotion.MovingDegraded) {
            movingDegradedStillEvidenceCount = 0
            return
        }
        if (promoteMovingCandidateByGpsEvidence(nowMillis)) return
        if (currentMotion == RootActivityMotion.Moving) return
        if (nowMillis < pedestrianCandidateSuppressedUntilMillis) return
        val rejectReason = movingCandidateRejectReason(nowMillis)
        if (rejectReason != null) {
            rejectMovingCandidate(nowMillis, rejectReason)
            return
        }
        applyMotionState(
            motion = classification.motion,
            activityType = classification.activityType,
            nowMillis = nowMillis,
            reason = MotionTransitionReasonActivityRecognition,
            activityConfidences = classification.activityConfidences,
        )
    }

    private fun handleStillActivity(
        classification: RootActivityClassification,
        nowMillis: Long,
    ) {
        when (currentMotion) {
            RootActivityMotion.MovingCandidate -> {
                when {
                    promoteMovingCandidateByGpsEvidence(nowMillis) -> Unit
                    movingCandidateDiagnostics.hasGpsMovementEvidence(
                        minimumTotalDistanceMeters = MovingCandidateExtensionQualifiedDistanceMeters,
                        minimumMaxDisplacementMeters = MovingCandidateExtensionQualifiedDisplacementMeters,
                    ) -> Unit
                    else -> rejectMovingCandidate(
                        nowMillis = nowMillis,
                        rejectReason = MotionCandidateRejectReasonActivityStill,
                    )
                }
            }

            RootActivityMotion.MovingDegraded -> {
                if (motionGate.hasStableStillWindow(nowMillis)) {
                    applyStillState(
                        nowMillis = nowMillis,
                        reason = MotionTransitionReasonGpsStill,
                        arrivalLocation = latestAccurateLocation,
                    )
                } else {
                    movingDegradedStillEvidenceCount += 1
                    if (movingDegradedStillEvidenceCount >= MovingDegradedStillRequiredCount) {
                        applyMotionState(
                            motion = RootActivityMotion.Still,
                            activityType = classification.activityType,
                            nowMillis = nowMillis,
                            reason = MotionTransitionReasonActivityRecognition,
                        )
                    }
                }
            }

            RootActivityMotion.Moving -> {
                if (motionGate.hasStableStillWindow(nowMillis)) {
                    applyStillState(
                        nowMillis = nowMillis,
                        reason = MotionTransitionReasonGpsStill,
                        arrivalLocation = latestAccurateLocation,
                    )
                } else {
                    movingStillEvidenceCount += 1
                    if (movingStillEvidenceCount >= MovingStillRequiredCount &&
                        hasRecentLowMovementStillWindow(nowMillis)
                    ) {
                        applyStillState(
                            nowMillis = nowMillis,
                            reason = MotionTransitionReasonActivityRecognition,
                            arrivalLocation = latestAccurateLocation,
                        )
                    }
                }
            }

            RootActivityMotion.Still,
            RootActivityMotion.Unknown,
            -> applyMotionState(
                motion = classification.motion,
                activityType = classification.activityType,
                nowMillis = nowMillis,
                reason = MotionTransitionReasonActivityRecognition,
            )
        }
    }

    private fun applyMotionState(
        motion: RootActivityMotion,
        activityType: String,
        nowMillis: Long = System.currentTimeMillis(),
        reason: String,
        activityConfidences: Map<String, Int> = emptyMap(),
        candidateRejectReason: String? = null,
        candidatePromotionReason: String? = null,
    ) {
        val candidateStartSample = if (currentMotion == RootActivityMotion.MovingCandidate &&
            motion == RootActivityMotion.Moving
        ) {
            createMovingCandidateStartSample()
        } else {
            null
        }
        val nextPolicy = RootLocationRequestPolicy.forActivity(motion, activityType)
        val shouldRestartLocationUpdates = nextPolicy != activePolicy
        val previousMotion = currentMotion
        val previousActivityType = currentActivityType
        val motionChanged = previousMotion != motion
        val activityTypeChanged = previousActivityType != activityType

        if (motion == RootActivityMotion.MovingCandidate && currentMotion != RootActivityMotion.MovingCandidate) {
            movingCandidateStartedAtMillis = nowMillis
            movingCandidateStartBuffer.clear()
            movingCandidateDiagnostics.start(
                nowMillis = nowMillis,
                activityType = activityType,
                activityConfidences = activityConfidences,
            )
            logMovingCandidateStart(
                nowMillis = nowMillis,
                activityType = activityType,
                activityConfidences = activityConfidences,
            )
        } else if (motion != RootActivityMotion.MovingCandidate) {
            if (currentMotion == RootActivityMotion.MovingCandidate) {
                finishMovingCandidateDiagnostics(
                    nowMillis = nowMillis,
                    endReason = reason,
                    rejectReason = candidateRejectReason,
                    promotionReason = candidatePromotionReason,
                )
            }
            movingCandidateStartedAtMillis = null
            movingCandidateStartBuffer.clear()
        }
        if (motion == RootActivityMotion.MovingDegraded && currentMotion != RootActivityMotion.MovingDegraded) {
            movingDegradedStillEvidenceCount = 0
        } else if (motion != RootActivityMotion.MovingDegraded) {
            movingDegradedStillEvidenceCount = 0
        }
        if (motion != RootActivityMotion.Moving) {
            movingStillEvidenceCount = 0
        }
        if (activityType != AndroidActivityTypeInVehicle) {
            vehiclePedestrianEvidenceCount = 0
        }
        if (motion == RootActivityMotion.Moving) {
            pedestrianCandidateSuppressedUntilMillis = 0L
        }

        if (motionChanged || activityTypeChanged) {
            logMotionTransition(
                fromMotion = previousMotion,
                fromActivityType = previousActivityType,
                toMotion = motion,
                toActivityType = activityType,
                reason = reason,
                nowMillis = nowMillis,
            )
        }
        if (motionChanged) {
            currentMotionStartedAtMillis = nowMillis
        }

        currentMotion = motion
        currentActivityType = activityType
        activePolicy = nextPolicy
        notification.update(motion)
        serviceScope.launch {
            repository.setMotionState(motion.storageKey)
        }
        if (motion == RootActivityMotion.Still) {
            motionGate.confirmStill()
        }
        candidateStartSample?.let(::persistSample)
        if (motionChanged) {
            persistMotionTransitionSample(
                motion = motion,
                activityType = activityType,
                source = nextPolicy.source,
                nowMillis = nowMillis,
                reason = reason,
            )
        }
        if (shouldRestartLocationUpdates && hasLocationPermission()) {
            restartLocationUpdates(activePolicy)
        }
    }

    private fun movingCandidateRejectReason(nowMillis: Long): String? {
        if (currentMotion != RootActivityMotion.MovingCandidate) return null
        extendMovingCandidateIfEligible(nowMillis)
        if (movingCandidatePromotionReason() != null) {
            return null
        }
        val movementSignals = movingCandidateDiagnostics.movementSignalCount()
        val startedAt = movingCandidateStartedAtMillis
        if (movementSignals == 0 && motionGate.hasCandidateStillWindow(nowMillis, sinceMillis = startedAt)) {
            return MotionCandidateRejectReasonStillWindow
        }
        val candidateAgeMillis = startedAt?.let { nowMillis - it } ?: 0L
        if (candidateAgeMillis < MovingCandidateTimeoutMillis) return null
        if (movingCandidateDiagnostics.isExtendedByEvidence() && candidateAgeMillis < MovingCandidateMaxDurationMillis) {
            return null
        }
        return when {
            movementSignals == 0 -> MotionCandidateRejectReasonNoMovementSignal
            movementSignals < MovingCandidateRequiredMovementSignals -> MotionCandidateRejectReasonInsufficientSignalCount
            else -> MotionCandidateRejectReasonTimeout
        }
    }

    private fun rejectMovingCandidate(nowMillis: Long, rejectReason: String) {
        pedestrianCandidateSuppressedUntilMillis = nowMillis + PedestrianCandidateCooldownMillis
        applyMotionState(
            motion = RootActivityMotion.Still,
            activityType = AndroidLocationStillActivityType,
            nowMillis = nowMillis,
            reason = MotionTransitionReasonMovingCandidateRejected,
            candidateRejectReason = rejectReason,
        )
    }

    private fun applyStillState(
        nowMillis: Long,
        reason: String,
        arrivalLocation: Location?,
    ) {
        val arrivalSampleMillis = arrivalBoundaryMillis(nowMillis, reason)
        applyMotionState(
            motion = RootActivityMotion.Still,
            activityType = AndroidLocationStillActivityType,
            nowMillis = nowMillis,
            reason = reason,
        )
        if (reason.isArrivalSampleReason()) {
            persistArrivalStopSample(
                location = arrivalLocation ?: latestAccurateLocation,
                sampleTimeMillis = arrivalSampleMillis,
            )
        }
    }

    private fun arrivalBoundaryMillis(nowMillis: Long, reason: String): Long =
        motionGate.stableStillWindowStartMillis(nowMillis)
            ?: if (reason == MotionTransitionReasonVehicleExpired ||
                reason == MotionTransitionReasonActivityRecognition
            ) {
                motionGate
                    .recentMovementWindow(
                        nowMillis = nowMillis,
                        windowMillis = if (reason == MotionTransitionReasonVehicleExpired) {
                            VehicleExpirationWindowMillis
                        } else {
                            MovingDegradedStillWindowMillis
                        },
                        maxAccuracyMeters = MovingDegradedStillMaxAccuracyMeters,
                    )
                    .startedAtMillis
                    ?: nowMillis
            } else {
                nowMillis
            }

    private fun persistArrivalStopSample(location: Location?, sampleTimeMillis: Long) {
        val transitionLocation = location
            ?.transitionTimestampCopy(sampleTimeMillis)
            ?: return
        val reading = transitionLocation
            .toRootLocationReading()
            ?.takeIf { it.hasArrivalAccuracy() }
            ?: return
        if (!arrivalStopDeduper.shouldSave(reading, sampleTimeMillis)) return
        val sample = sampleFactory.create(
            location = transitionLocation,
            source = ArrivalStopSource,
            activityType = AndroidLocationStillActivityType,
            saveReason = RootLocationBoundarySamplePolicy.ArrivalSaveReason,
        ) ?: return
        persistSample(sample)
    }

    private fun persistMotionTransitionSample(
        motion: RootActivityMotion,
        activityType: String,
        source: String,
        nowMillis: Long,
        reason: String,
    ) {
        if (motion !in TransitionSampleMotions) return
        if (motion == RootActivityMotion.Still && reason.isArrivalTransitionReason()) return
        val location = latestTransitionLocation(nowMillis) ?: return
        val markerActivityType = if (motion == RootActivityMotion.Moving) {
            AndroidLocationMovingActivityType
        } else {
            activityType
        }
        val markerSaveReason = if (motion == RootActivityMotion.Moving) {
            RootLocationBoundarySamplePolicy.MoveStartSaveReason
        } else {
            RootLocationBoundarySamplePolicy.MotionTransitionSaveReason
        }
        val sample = sampleFactory.create(
            location = location,
            source = source,
            activityType = markerActivityType,
            saveReason = markerSaveReason,
        ) ?: return
        persistSample(sample)
    }

    private fun latestTransitionLocation(nowMillis: Long): Location? =
        latestAccurateLocation
            ?.takeIf { location ->
                val ageMillis = nowMillis - location.time
                ageMillis in 0..TransitionSampleMaxLocationAgeMillis
            }
            ?.transitionTimestampCopy(nowMillis)

    private fun Location.transitionTimestampCopy(nowMillis: Long): Location =
        Location(this).apply {
            time = nowMillis
        }

    private fun String.isArrivalTransitionReason(): Boolean =
        this == MotionTransitionReasonGpsStill || this == MotionTransitionReasonVehicleExpired

    private fun String.isArrivalSampleReason(): Boolean =
        isArrivalTransitionReason() || this == MotionTransitionReasonActivityRecognition

    private fun promoteMovingCandidateByGpsEvidence(nowMillis: Long): Boolean {
        if (currentMotion != RootActivityMotion.MovingCandidate) return false
        extendMovingCandidateIfEligible(nowMillis)
        val promotionReason = movingCandidatePromotionReason() ?: return false
        applyMotionState(
            motion = RootActivityMotion.Moving,
            activityType = AndroidLocationMovingActivityType,
            nowMillis = nowMillis,
            reason = MotionTransitionReasonGpsMoving,
            candidatePromotionReason = promotionReason,
        )
        return true
    }

    private fun extendMovingCandidateIfEligible(nowMillis: Long) {
        if (currentMotion != RootActivityMotion.MovingCandidate ||
            movingCandidateDiagnostics.isExtendedByEvidence()
        ) {
            return
        }
        val startedAt = movingCandidateStartedAtMillis ?: return
        val candidateAgeMillis = nowMillis - startedAt
        if (candidateAgeMillis < MovingCandidateTimeoutMillis ||
            candidateAgeMillis >= MovingCandidateMaxDurationMillis
        ) {
            return
        }
        if (movingCandidateDiagnostics.shouldExtendCandidate(
                minimumMovementSignals = MovingCandidateExtensionMovementSignals,
                minimumQualifiedTotalDistanceMeters = MovingCandidateExtensionQualifiedDistanceMeters,
                minimumQualifiedMaxDisplacementMeters = MovingCandidateExtensionQualifiedDisplacementMeters,
            )
        ) {
            movingCandidateDiagnostics.markExtendedByEvidence()
        }
    }

    private fun movingCandidatePromotionReason(): String? =
        movingCandidateDiagnostics.promotionReason(
            requiredRecentMovementSignals = MovingCandidateRequiredMovementSignals,
            requiredTotalMovementSignals = MovingCandidateRequiredMovementSignals,
            minimumQualifiedTotalDistanceMeters = MovingCandidatePromotionQualifiedDistanceMeters,
            minimumQualifiedMaxDisplacementMeters = MovingCandidatePromotionQualifiedDisplacementMeters,
        )

    private fun handleLocation(location: Location) {
        recordLatestAccurateLocation(location)
        if (currentMotion == RootActivityMotion.MovingCandidate) {
            recordMovingCandidateStartLocation(location)
        }
        if (!handleLocationMotionSignal(location)) return
        if (!sampleGate.shouldAccept(location, currentMotion)) return
        val sample = sampleFactory.create(location, activePolicy.source, currentActivityType) ?: return
        persistSample(sample)
    }

    private fun recordLatestAccurateLocation(location: Location) {
        val reading = location.toRootLocationReading() ?: return
        if (!reading.hasArrivalAccuracy()) return
        latestAccurateLocation = Location(location)
    }

    private fun persistSample(sample: LocationSampleRequest) {
        val collectedAt = Instant.ofEpochMilli(sample.collectedAtMillis)
        if (!RootLocationBoundarySamplePolicy.shouldPersist(collectedAt, lastAcceptedAt, sample.saveReason)) {
            return
        }
        lastAcceptedAt = RootLocationBoundarySamplePolicy.nextLastAcceptedAt(
            current = lastAcceptedAt,
            collectedAt = collectedAt,
            saveReason = sample.saveReason,
        )
        serviceScope.launch {
            repository.enqueue(sample)
            uploadPendingSamples()
        }
    }

    private suspend fun uploadPendingSamples() {
        try {
            uploader.flush()
        } catch (exception: CancellationException) {
            throw exception
        } catch (throwable: Throwable) {
            repository.setLastErrorMessage(throwable.localizedMessage)
            if (throwable.shouldRetryLocationUpload()) {
                uploadScheduler.retryWhenConnected()
            }
        }
    }

    private fun recordMovingCandidateStartLocation(location: Location) {
        movingCandidateStartBuffer.record(
            value = Location(location),
            timeMillis = location.time,
            accuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
        )
    }

    private fun createMovingCandidateStartSample(): LocationSampleRequest? {
        val location = movingCandidateStartBuffer.best() ?: return null
        return sampleFactory.create(
            location = location,
            source = RootLocationRequestPolicy.forMotion(RootActivityMotion.MovingCandidate).source,
            activityType = AndroidLocationMovingActivityType,
            saveReason = RootLocationBoundarySamplePolicy.MoveStartSaveReason,
        )
    }

    private fun handleLocationMotionSignal(location: Location): Boolean {
        val nowMillis = System.currentTimeMillis()
        val motionResult = motionGate.onLocationWithDiagnostics(location)
        if (currentMotion == RootActivityMotion.MovingCandidate) {
            movingCandidateDiagnostics
                .recordSample(
                    reading = motionResult.evaluation.reading,
                    evaluation = motionResult.evaluation,
                )
                ?.let(::logMovingCandidateSample)
        }
        if (promoteMovingCandidateByGpsEvidence(nowMillis)) return true
        if (RootGpsDisplacementEscapePolicy.shouldEscape(currentMotion, motionResult.evaluation)) {
            applyMotionState(
                motion = RootActivityMotion.Moving,
                activityType = AndroidLocationMovingActivityType,
                nowMillis = nowMillis,
                reason = MotionTransitionReasonGpsDisplacementEscape,
            )
            return true
        }
        val gpsMotion = motionResult.motion
        if (shouldExpireVehicleByGps(nowMillis)) {
            applyStillState(
                nowMillis = nowMillis,
                reason = MotionTransitionReasonVehicleExpired,
                arrivalLocation = location,
            )
            return false
        }
        when (gpsMotion) {
            RootActivityMotion.Moving -> {
                if (currentMotion != RootActivityMotion.Moving) {
                    applyMotionState(
                        motion = RootActivityMotion.Moving,
                        activityType = AndroidLocationMovingActivityType,
                        nowMillis = nowMillis,
                        reason = MotionTransitionReasonGpsMoving,
                    )
                }
            }

            RootActivityMotion.Still -> {
                if (currentMotion != RootActivityMotion.Still) {
                    applyStillState(
                        nowMillis = nowMillis,
                        reason = MotionTransitionReasonGpsStill,
                        arrivalLocation = location,
                    )
                    return false
                }
            }

            RootActivityMotion.MovingCandidate,
            RootActivityMotion.MovingDegraded,
            RootActivityMotion.Unknown,
            null,
            -> {
                val rejectReason = movingCandidateRejectReason(nowMillis)
                if (rejectReason != null) {
                    rejectMovingCandidate(nowMillis, rejectReason)
                }
            }
        }
        return applySelfHealingTransitions(nowMillis, location)
    }

    private fun applySelfHealingTransitions(nowMillis: Long, location: Location): Boolean {
        when {
            shouldDegradeMoving(nowMillis) -> {
                if (motionGate.hasStableStillWindow(nowMillis)) {
                    applyStillState(
                        nowMillis = nowMillis,
                        reason = MotionTransitionReasonGpsStill,
                        arrivalLocation = location,
                    )
                    return false
                }
                applyMotionState(
                    motion = RootActivityMotion.MovingDegraded,
                    activityType = currentActivityType,
                    nowMillis = nowMillis,
                    reason = MotionTransitionReasonMovingDegraded,
                )
            }

            shouldExpireMovingDegraded(nowMillis) -> {
                if (shouldFinishMovingDegradedAsStill(nowMillis)) {
                    applyStillState(
                        nowMillis = nowMillis,
                        reason = MotionTransitionReasonGpsStill,
                        arrivalLocation = location,
                    )
                    return false
                }
                applyMotionState(
                    motion = RootActivityMotion.Unknown,
                    activityType = AndroidActivityTypeUnknown,
                    nowMillis = nowMillis,
                    reason = MotionTransitionReasonMovingDegradedTimeout,
                )
            }

            shouldEndUnknown(nowMillis) -> applyStillState(
                nowMillis = nowMillis,
                reason = MotionTransitionReasonUnknownTimeout,
                arrivalLocation = null,
            )
        }
        return true
    }

    private fun shouldDegradeMoving(nowMillis: Long): Boolean {
        if (currentMotion != RootActivityMotion.Moving) return false
        val referenceMillis = lastMeaningfulMovementOrStateStart(nowMillis, currentMotionStartedAtMillis)
        return nowMillis - referenceMillis >= MovingDecayMillis
    }

    private fun shouldExpireMovingDegraded(nowMillis: Long): Boolean {
        if (currentMotion != RootActivityMotion.MovingDegraded) return false
        return nowMillis - currentMotionStartedAtMillis >= MovingDegradedTimeoutMillis
    }

    private fun shouldFinishMovingDegradedAsStill(nowMillis: Long): Boolean {
        if (currentMotion != RootActivityMotion.MovingDegraded) return false
        if (motionGate.hasStableStillWindow(nowMillis)) return true
        return hasRecentLowMovementStillWindow(nowMillis)
    }

    private fun hasRecentLowMovementStillWindow(nowMillis: Long): Boolean {
        val window = motionGate.recentMovementWindow(
            nowMillis = nowMillis,
            windowMillis = MovingDegradedStillWindowMillis,
            maxAccuracyMeters = MovingDegradedStillMaxAccuracyMeters,
        )
        return window.sampleCount >= MovingDegradedStillMinimumSampleCount &&
            window.spanMillis >= MovingDegradedStillMinimumSpanMillis &&
            window.distanceMeters < MovingDegradedStillDistanceMeters &&
            motionGate.recentMovementSignalCount(
                nowMillis = nowMillis,
                windowMillis = MovingDegradedStillWindowMillis,
            ) == 0
    }

    private fun shouldEndUnknown(nowMillis: Long): Boolean {
        if (currentMotion != RootActivityMotion.Unknown) return false
        if (nowMillis - currentMotionStartedAtMillis < UnknownTimeoutMillis) return false
        val referenceMillis = lastMeaningfulMovementOrStateStart(nowMillis, currentMotionStartedAtMillis)
        return nowMillis - referenceMillis >= UnknownTimeoutMillis
    }

    private fun shouldExpireVehicleByGps(
        nowMillis: Long,
        allowInactiveVehicle: Boolean = false,
    ): Boolean {
        val window = motionGate.recentMovementWindow(
            nowMillis = nowMillis,
            windowMillis = VehicleExpirationWindowMillis,
            maxAccuracyMeters = VehicleExpirationMaxAccuracyMeters,
        )
        return window.sampleCount >= VehicleExpirationMinimumSampleCount &&
            window.spanMillis >= VehicleExpirationMinimumSpanMillis &&
            window.distanceMeters < VehicleExpirationDistanceMeters &&
            (currentActivityType == AndroidActivityTypeInVehicle || allowInactiveVehicle)
    }

    private fun hasVehiclePedestrianGpsEvidence(nowMillis: Long): Boolean {
        val window = motionGate.recentMovementWindow(
            nowMillis = nowMillis,
            windowMillis = VehiclePedestrianGpsWindowMillis,
            maxAccuracyMeters = VehicleExpirationMaxAccuracyMeters,
        )
        if (window.sampleCount < VehiclePedestrianGpsMinimumSampleCount ||
            window.spanMillis < VehiclePedestrianGpsMinimumSpanMillis
        ) {
            return false
        }
        val averageSpeedMps = window.distanceMeters / (window.spanMillis / 1_000f)
        return averageSpeedMps < VehiclePedestrianGpsMaxAverageSpeedMps
    }

    private fun strongPedestrianActivityType(activityConfidences: Map<String, Int>): String? =
        listOf("WALKING", "RUNNING", "ON_FOOT")
            .mapNotNull { activityType ->
                activityConfidences[activityType]?.let { confidence -> activityType to confidence }
            }
            .filter { (_, confidence) -> confidence >= VehiclePedestrianReleaseConfidence }
            .maxByOrNull { (_, confidence) -> confidence }
            ?.first

    private fun lastMeaningfulMovementOrStateStart(nowMillis: Long, stateStartedAtMillis: Long): Long =
        motionGate
            .lastMeaningfulMovementAtMillis()
            ?.takeIf { it >= stateStartedAtMillis && it <= nowMillis }
            ?: stateStartedAtMillis

    private fun logMotionTransition(
        fromMotion: RootActivityMotion,
        fromActivityType: String,
        toMotion: RootActivityMotion,
        toActivityType: String,
        reason: String,
        nowMillis: Long,
    ) {
        Log.i(
            LogTag,
            "MotionTransition timestamp=${Instant.ofEpochMilli(nowMillis)} " +
                "from=$fromMotion fromActivityType=$fromActivityType " +
                "to=$toMotion toActivityType=$toActivityType reason=$reason",
        )
    }

    private fun logMovingCandidateStart(
        nowMillis: Long,
        activityType: String,
        activityConfidences: Map<String, Int>,
    ) {
        Log.i(
            LogTag,
            "CandidateStart " +
                "timestamp=${Instant.ofEpochMilli(nowMillis)} " +
                "activityType=$activityType " +
                "activityConfidences=${activityConfidences.formatConfidenceMap()}",
        )
    }

    private fun logMovingCandidateSample(sample: RootMovingCandidateSampleDiagnostic) {
        Log.i(
            LogTag,
            "CandidateSample " +
                "timestamp=${sample.timestamp ?: "null"} " +
                "distance=${sample.distanceMeters.formatMeters()} " +
                "accuracy=${sample.accuracyMeters.formatMeters()} " +
                "requiredDistance=${sample.requiredDistanceMeters.formatMeters()} " +
                "accepted=${sample.accepted} " +
                "reason=${sample.reason} " +
                "movementEvidenceScore=${sample.movementEvidenceScore} " +
                "consecutiveMovementSignals=${sample.consecutiveMovementSignals}",
        )
    }

    private fun finishMovingCandidateDiagnostics(
        nowMillis: Long,
        endReason: String,
        rejectReason: String?,
        promotionReason: String?,
    ) {
        val summary = movingCandidateDiagnostics.finish(
            nowMillis = nowMillis,
            endReason = endReason,
            rejectReason = rejectReason,
            promotionReason = promotionReason,
        ) ?: return
        Log.i(LogTag, summary.toLogLine())
    }

    private fun LocationMotionCandidateSummary.toLogLine(): String =
        "CandidateSummary " +
            "candidateStart=$candidateStart " +
            "candidateEnd=$candidateEnd " +
            "duration=${durationSeconds}s " +
            "sampleCount=$sampleCount " +
            "movementSignals=$movementSignalCount " +
            "recentMovementSignalCount=$recentMovementSignalCount " +
            "totalMovementSignalCount=$totalMovementSignalCount " +
            "maxMovementEvidenceScore=$maxMovementEvidenceScore " +
            "maxConsecutiveMovementSignals=$maxConsecutiveMovementSignals " +
            "maxDistance=${maxDistanceMeters.formatMeters()} " +
            "totalDistance=${totalDistanceMeters.formatMeters()} " +
            "qualifiedTotalDistance=${qualifiedTotalDistanceMeters.formatMeters()} " +
            "qualifiedMaxDisplacement=${qualifiedMaxDisplacementMeters.formatMeters()} " +
            "candidateAge=${candidateAgeSeconds}s " +
            "candidateExtendedByEvidence=$candidateExtendedByEvidence " +
            "rejectedJumpCount=$rejectedJumpCount " +
            "endReason=$endReason " +
            "rejectReason=${rejectReason ?: "NONE"} " +
            "promotionReason=${promotionReason ?: "NONE"} " +
            "activityType=$activityType " +
            "activityConfidences=${activityConfidences.formatConfidenceMap()} " +
            "movementReasonCounts=${movementReasonCounts.formatReasonCounts()}"

    private fun Float?.formatMeters(): String =
        this?.let { String.format(Locale.US, "%.1fm", it) } ?: "null"

    private fun Map<String, Int>.formatConfidenceMap(): String =
        entries.joinToString(",", prefix = "{", postfix = "}") { (key, value) -> "$key=$value" }

    private fun Map<String, Int>.formatReasonCounts(): String =
        entries.joinToString(",", prefix = "{", postfix = "}") { (key, value) -> "$key=$value" }

    @SuppressLint("MissingPermission")
    private fun registerActivityUpdates() {
        if (!hasActivityRecognitionPermission()) {
            recordActivityRecognitionIssue(
                element = ActivityRecognitionDiagnosticPermissionMissing,
                message = "활동 인식 권한이 없어 이동 상태 전이를 받을 수 없습니다. sdk=${Build.VERSION.SDK_INT}",
            )
            return
        }
        val generation = trackingGeneration
        runCatching {
            activityRecognitionClient.requestActivityUpdates(
                ActivityUpdateIntervalMillis,
                activityPendingIntent(),
            )
                .addOnSuccessListener {
                    if (trackingStarted && trackingGeneration == generation) {
                        recordActivityRecognitionDiagnostic(
                            element = ActivityRecognitionDiagnosticRegistered,
                            message = "ActivityRecognition requestActivityUpdates registered",
                        )
                    } else if (!trackingStarted) {
                        unregisterActivityUpdates()
                    }
                }
                .addOnFailureListener { throwable ->
                    stopWithRuntimeError(
                        generation = generation,
                        message = throwable.localizedMessage ?: "활동 인식 업데이트를 등록할 수 없습니다.",
                    )
                }
        }.onFailure { throwable ->
            stopWithRuntimeError(
                generation = generation,
                message = throwable.localizedMessage ?: "활동 인식 업데이트를 등록할 수 없습니다.",
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun unregisterActivityUpdates() {
        runCatching {
            activityRecognitionClient.removeActivityUpdates(activityPendingIntent())
                .addOnFailureListener { throwable ->
                    recordActivityRecognitionIssue(
                        element = ActivityRecognitionDiagnosticUnregisterFailed,
                        message = "활동 인식 해제 실패",
                        throwable = throwable,
                    )
                }
        }.onFailure { throwable ->
            recordActivityRecognitionIssue(
                element = ActivityRecognitionDiagnosticUnregisterCallFailed,
                message = "활동 인식 해제 호출 실패",
                throwable = throwable,
            )
        }
    }

    private fun recordActivityRecognitionUpdateDiagnostic(
        classification: RootActivityClassification,
        nowMillis: Long,
    ) {
        if (
            nowMillis - lastActivityRecognitionUpdateDiagnosticAtMillis <
            ActivityRecognitionUpdateDiagnosticIntervalMillis
        ) {
            return
        }
        lastActivityRecognitionUpdateDiagnosticAtMillis = nowMillis
        recordActivityRecognitionDiagnostic(
            element = ActivityRecognitionDiagnosticUpdateReceived,
            message = "ActivityRecognition update received",
            nowMillis = nowMillis,
            detectedMotion = classification.motion.name,
            detectedActivityType = classification.activityType,
            activityConfidences = classification.activityConfidences,
        )
    }

    private fun recordActivityRecognitionIssue(
        element: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        recordActivityRecognitionDiagnostic(
            element = element,
            message = message,
            throwable = throwable,
            updateLastError = true,
        )
    }

    private fun recordActivityRecognitionDiagnostic(
        element: String,
        message: String,
        nowMillis: Long = System.currentTimeMillis(),
        throwable: Throwable? = null,
        updateLastError: Boolean = false,
        detectedMotion: String? = null,
        detectedActivityType: String? = null,
        activityConfidences: Map<String, Int> = emptyMap(),
    ) {
        val details = " element=$element timestamp=${Instant.ofEpochMilli(nowMillis)}" +
            " motion=${detectedMotion ?: currentMotion.name}" +
            " activity=${detectedActivityType ?: currentActivityType}" +
            " confidences=${activityConfidences.formatConfidenceMap()}"
        val fullMessage = throwable?.let {
            "$message: ${it.localizedMessage ?: it.message ?: it.javaClass.simpleName}"
        } ?: message
        if (throwable == null && !updateLastError) Log.i(LogTag, fullMessage + details)
        else Log.w(LogTag, fullMessage + details, throwable)
        if (updateLastError) {
            serviceScope.launch { repository.setLastErrorMessage(fullMessage) }
        }
    }

    @SuppressLint("MissingPermission")
    private fun restartLocationUpdates(policy: RootLocationPolicy) {
        stopLocationUpdates()
        val generation = trackingGeneration
        val request = LocationRequest.Builder(policy.priority, policy.intervalMillis)
            .setMinUpdateIntervalMillis(policy.minUpdateIntervalMillis)
            .setMinUpdateDistanceMeters(policy.minUpdateDistanceMeters)
            .setWaitForAccurateLocation(policy.waitForAccurateLocation)
            .build()

        runCatching {
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper(),
            )
                .addOnSuccessListener {
                    if (!trackingStarted) stopLocationUpdates()
                }
                .addOnFailureListener { throwable ->
                    stopWithRuntimeError(
                        generation = generation,
                        message = throwable.localizedMessage ?: "위치 업데이트를 등록할 수 없습니다.",
                    )
                }
        }.onFailure { throwable ->
            stopWithRuntimeError(
                generation = generation,
                message = throwable.localizedMessage ?: "위치 업데이트를 등록할 수 없습니다.",
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestBootstrapLocation() {
        if (!hasLocationPermission()) return
        val generation = trackingGeneration
        runCatching {
            fusedLocationClient
                .getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token,
                )
                .addOnSuccessListener { location ->
                    if (trackingStarted && trackingGeneration == generation && location != null) {
                        handleLocation(location)
                    }
                }
                .addOnFailureListener { throwable ->
                    serviceScope.launch {
                        repository.setLastErrorMessage(throwable.localizedMessage)
                    }
                }
        }.onFailure { throwable ->
            serviceScope.launch {
                repository.setLastErrorMessage(throwable.localizedMessage)
            }
        }
    }

    private fun stopLocationUpdates() {
        runCatching {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun activityPendingIntent(): PendingIntent =
        PendingIntent.getService(
            this,
            4001,
            Intent(this, LocationTrackingService::class.java)
                .setAction(ActionActivityUpdate),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )

    private fun hasLocationPermission(): Boolean =
        hasRootLocationPermission()

    private fun hasActivityRecognitionPermission(): Boolean =
        Build.VERSION.SDK_INT < 29 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED

    companion object {
        const val ActionStart = "kr.co.root.legolas.location.action.START"
        const val ActionStop = "kr.co.root.legolas.location.action.STOP"
        const val ActionDisable = "kr.co.root.legolas.location.action.DISABLE"
        const val ActionActivityUpdate = "kr.co.root.legolas.location.action.ACTIVITY_UPDATE"

        private const val ActivityUpdateIntervalMillis = 30_000L
    }
}

private const val AndroidLocationMovingActivityType = "LOCATION_MOVING"
private const val AndroidLocationStillActivityType = "LOCATION_STILL"
private const val AndroidActivityTypeInVehicle = "IN_VEHICLE"
private const val MovingCandidateTimeoutMillis = 3 * 60_000L
private const val MovingCandidateMaxDurationMillis = 5 * 60_000L
private const val MovingCandidateRequiredMovementSignals = 3
private const val MovingCandidateExtensionMovementSignals = 2
private const val MovingCandidateExtensionQualifiedDistanceMeters = 50f
private const val MovingCandidateExtensionQualifiedDisplacementMeters = 40f
private const val MovingCandidatePromotionQualifiedDistanceMeters = 100f
private const val MovingCandidatePromotionQualifiedDisplacementMeters = 70f
private const val PedestrianCandidateCooldownMillis = 2 * 60_000L
private const val MovingDecayMillis = 5 * 60_000L
private const val MovingStillRequiredCount = 2
private const val MovingDegradedTimeoutMillis = 2 * 60_000L
private const val MovingDegradedStillRequiredCount = 2
private const val MovingDegradedStillWindowMillis = 2 * 60_000L
private const val MovingDegradedStillMinimumSpanMillis = 60_000L
private const val MovingDegradedStillDistanceMeters = 20f
private const val MovingDegradedStillMaxAccuracyMeters = 50f
private const val MovingDegradedStillMinimumSampleCount = 2
private const val UnknownTimeoutMillis = 15 * 60_000L
private const val VehicleExpirationWindowMillis = 5 * 60_000L
private const val VehicleExpirationMinimumSpanMillis = 4 * 60_000L
private const val VehicleExpirationDistanceMeters = 30f
private const val VehicleExpirationMaxAccuracyMeters = 50f
private const val VehicleExpirationMinimumSampleCount = 2
private const val VehiclePedestrianReleaseConfidence = 70
private const val VehiclePedestrianReleaseRequiredCount = 2
private const val VehiclePedestrianGpsWindowMillis = 2 * 60_000L
private const val VehiclePedestrianGpsMinimumSpanMillis = 30_000L
private const val VehiclePedestrianGpsMinimumSampleCount = 2
private const val VehiclePedestrianGpsMaxAverageSpeedMps = 2.5f
private const val ActivityRecognitionUpdateDiagnosticIntervalMillis = 15 * 60_000L
private const val ArrivalStopSource = "android_arrival_stop"
private const val TransitionSampleMaxLocationAgeMillis = 5 * 60_000L
private const val MotionTransitionReasonActivityRecognition = "ACTIVITY_RECOGNITION"
private const val MotionTransitionReasonGpsMoving = "GPS_MOVING"
private const val MotionTransitionReasonGpsDisplacementEscape = "GPS_DISPLACEMENT_ESCAPE"
private const val MotionTransitionReasonGpsStill = "GPS_STILL"
private const val MotionTransitionReasonMovingCandidateRejected = "MOVING_CANDIDATE_REJECTED"
private const val MotionTransitionReasonMovingDegraded = "MOVING_DEGRADED"
private const val MotionTransitionReasonMovingDegradedTimeout = "MOVING_DEGRADED_TIMEOUT"
private const val MotionTransitionReasonUnknownTimeout = "UNKNOWN_TIMEOUT"
private const val MotionTransitionReasonVehicleExpired = "VEHICLE_EXPIRED"
private const val MotionCandidateRejectReasonNoMovementSignal = "NO_MOVEMENT_SIGNAL"
private const val MotionCandidateRejectReasonInsufficientSignalCount = "INSUFFICIENT_SIGNAL_COUNT"
private const val MotionCandidateRejectReasonStillWindow = "CANDIDATE_STILL_WINDOW"
private const val MotionCandidateRejectReasonTimeout = "TIMEOUT"
private const val MotionCandidateRejectReasonActivityStill = "ACTIVITY_STILL"
private const val ActivityRecognitionDiagnosticRegistered = "REGISTERED"
private const val ActivityRecognitionDiagnosticUnregisterFailed = "UNREGISTER_FAILED"
private const val ActivityRecognitionDiagnosticUnregisterCallFailed = "UNREGISTER_CALL_FAILED"
private const val ActivityRecognitionDiagnosticPermissionMissing = "PERMISSION_MISSING"
private const val ActivityRecognitionDiagnosticIntentWithoutResult = "INTENT_WITHOUT_RESULT"
private const val ActivityRecognitionDiagnosticExtractResultNull = "EXTRACT_RESULT_NULL"
private const val ActivityRecognitionDiagnosticUpdateReceived = "UPDATE_RECEIVED"
private const val LogTag = "Legolas"

private val TransitionSampleMotions = setOf(
    RootActivityMotion.Moving,
    RootActivityMotion.MovingDegraded,
    RootActivityMotion.Still,
)

private fun RootLocationReading.hasArrivalAccuracy(): Boolean =
    accuracyMeters != null && accuracyMeters >= 0f && accuracyMeters <= VehicleExpirationMaxAccuracyMeters

internal fun canStartLocationTracking(
    enabled: Boolean,
    hasPreciseLocation: Boolean,
    hasBackgroundLocation: Boolean,
    hasActivityRecognition: Boolean,
    hasNotifications: Boolean,
    isSystemLocationEnabled: Boolean,
): Boolean = enabled &&
    hasPreciseLocation &&
    hasBackgroundLocation &&
    hasActivityRecognition &&
    hasNotifications &&
    isSystemLocationEnabled
