package kr.co.root.legolas.pairing.ui

data class PairingUiState(
    val isLoading: Boolean = true,
    val serverUrl: String? = null,
    val errorMessage: String? = null,
    val shouldSuggestLogout: Boolean = false,
)
