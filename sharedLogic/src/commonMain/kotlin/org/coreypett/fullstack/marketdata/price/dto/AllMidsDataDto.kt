package org.coreypett.fullstack.marketdata.price.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class AllMidsDataDto(
    val mids: Map<String, String>,
)
