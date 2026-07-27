package kr.co.root.legolas.location.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import java.util.UUID

data class LocationSampleRequest(
    val clientSampleId: String = UUID.randomUUID().toString(),
    val collectedAtMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val horizontalAccuracyM: Double?,
    val source: String,
    val activityType: String?,
    val saveReason: String? = null,
)

@Entity(
    tableName = "location_sample_outbox",
    indices = [Index(value = ["collectedAtMillis"])],
)
data class LocationSampleEntity(
    @PrimaryKey val clientSampleId: String,
    val collectedAtMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val horizontalAccuracyM: Double?,
    val source: String,
    val activityType: String?,
    val saveReason: String?,
)

@Dao
interface LocationSampleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(sample: LocationSampleEntity)

    @Query("SELECT COUNT(*) FROM location_sample_outbox")
    suspend fun count(): Int

    @Query("SELECT * FROM location_sample_outbox ORDER BY collectedAtMillis LIMIT :limit")
    suspend fun oldest(limit: Int): List<LocationSampleEntity>

    @Query("DELETE FROM location_sample_outbox WHERE clientSampleId IN (:clientSampleIds)")
    suspend fun delete(clientSampleIds: List<String>)

    @Query("DELETE FROM location_sample_outbox")
    suspend fun deleteAll()
}

@Database(
    entities = [LocationSampleEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class LocationDatabase : RoomDatabase() {
    abstract fun samples(): LocationSampleDao
}

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
