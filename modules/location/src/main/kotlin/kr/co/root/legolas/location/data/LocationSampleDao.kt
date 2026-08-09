package kr.co.root.legolas.location.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

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
