package org.coreypett.fullstack.market.price.model

sealed interface LivePriceUiState {
    data object Connecting : LivePriceUiState
    data class Live(val price: LivePrice) : LivePriceUiState
    data class Failed(val message: String) : LivePriceUiState
}
