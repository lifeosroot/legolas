package kr.co.root.legolas.feature

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DisabledLocationFeatureModule {
    @Binds
    abstract fun bindLocationFeature(feature: DisabledLocationFeature): LocationFeature
}
