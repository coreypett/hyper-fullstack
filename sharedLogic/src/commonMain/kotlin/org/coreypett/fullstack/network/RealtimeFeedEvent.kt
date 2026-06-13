package org.coreypett.fullstack.network

internal sealed interface RealtimeFeedEvent<out Value> {
    data class Live<Value>(val value: Value) : RealtimeFeedEvent<Value>
    data class Reconnecting(
        val attempt: Long,
        val delayMillis: Long,
        val reason: String?,
    ) : RealtimeFeedEvent<Nothing>
}
