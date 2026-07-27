package kr.co.root.legolas.location.di

import android.content.Context
import androidx.room.Room
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kr.co.root.legolas.location.data.LocationDatabase
import kr.co.root.legolas.location.data.LocationSampleDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationDataModule {
    @Provides
    fun fusedLocationClient(
        @ApplicationContext context: Context,
    ): FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    @Provides
    fun activityRecognitionClient(
        @ApplicationContext context: Context,
    ): ActivityRecognitionClient = ActivityRecognition.getClient(context)

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): LocationDatabase =
        Room.databaseBuilder(context, LocationDatabase::class.java, "location.db").build()

    @Provides
    fun locationSampleDao(database: LocationDatabase): LocationSampleDao = database.samples()
}
