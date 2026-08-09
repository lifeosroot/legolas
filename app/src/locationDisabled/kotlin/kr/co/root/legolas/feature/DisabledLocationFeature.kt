package kr.co.root.legolas.feature

import androidx.compose.runtime.Composable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DisabledLocationFeature @Inject constructor() : LocationFeature {
    override val isAvailable = false

    @Composable
    override fun Summary(serverUrl: String, onOpen: () -> Unit) = Unit

    @Composable
    override fun Screen(serverUrl: String, onBack: () -> Unit) = Unit

    override suspend fun disableAndClear() = Unit
}
