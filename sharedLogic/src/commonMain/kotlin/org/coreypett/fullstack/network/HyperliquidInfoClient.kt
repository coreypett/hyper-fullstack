package org.coreypett.fullstack.network

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Hyperliquid HTTP info endpoint transport.
 *
 * Docs: https://hyperliquid.gitbook.io/hyperliquid-docs/for-developers/api/info-endpoint
 */
internal interface HyperliquidInfoClient {
    suspend fun <Request : Any> post(request: Request, serializer: KSerializer<Request>): JsonElement

    class Impl(
        private val httpClient: HttpClient = platformHttpClient(),
        private val json: Json = HyperliquidJson,
    ) : HyperliquidInfoClient {
        override suspend fun <Request : Any> post(
            request: Request,
            serializer: KSerializer<Request>,
        ): JsonElement {
            val response = httpClient.post(HyperliquidInfoUrl) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(json.encodeToString(serializer, request))
            }

            return json.parseToJsonElement(response.bodyAsText())
        }
    }
}

private const val HyperliquidInfoUrl = "https://api.hyperliquid.xyz/info"
