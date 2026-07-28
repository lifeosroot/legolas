package kr.co.root.legolas.feature

import androidx.compose.runtime.Composable

interface LocationFeature {
    val isAvailable: Boolean

    @Composable
    fun Summary(serverUrl: String, onOpen: () -> Unit)

    @Composable
    fun Screen(serverUrl: String, onBack: () -> Unit)

    suspend fun disableAndClear()
}
