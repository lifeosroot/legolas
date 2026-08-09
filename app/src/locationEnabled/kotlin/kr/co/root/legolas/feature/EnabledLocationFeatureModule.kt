package kr.co.root.legolas.feature

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kr.co.root.legolas.location.data.LocationServerConfigProvider

@Module
@InstallIn(SingletonComponent::class)
abstract class EnabledLocationFeatureModule {
    @Binds
    abstract fun bindLocationFeature(feature: EnabledLocationFeature): LocationFeature

    @Binds
    abstract fun bindLocationServerConfigProvider(
        provider: PairedLocationServerConfigProvider,
    ): LocationServerConfigProvider
}
