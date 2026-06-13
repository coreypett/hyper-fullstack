package org.coreypett.fullstack.market.price.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class AllMidsSubscriptionDto(
    val type: String,
    val dex: String? = null,
)
