package kr.co.root.legolas.location.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LocationSampleEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class LocationDatabase : RoomDatabase() {
    abstract fun samples(): LocationSampleDao
}
