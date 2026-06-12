package org.coreypett.fullstack.orderbook.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class L2BookLevelDto(
    @SerialName("px")
    val price: String,
    @SerialName("sz")
    val size: String,
    @SerialName("n")
    val orderCount: Int,
) {
    val priceValue: Double?
        get() = price.toDoubleOrNull()

    val sizeValue: Double?
        get() = size.toDoubleOrNull()
}
