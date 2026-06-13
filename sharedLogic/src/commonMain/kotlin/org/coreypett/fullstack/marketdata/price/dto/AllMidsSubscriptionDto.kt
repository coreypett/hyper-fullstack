package org.coreypett.fullstack.marketdata.price.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class AllMidsSubscriptionDto(
    val type: String,
    val dex: String? = null,
)
