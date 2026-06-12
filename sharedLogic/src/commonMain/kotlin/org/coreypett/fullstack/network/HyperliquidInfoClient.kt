package org.coreypett.fullstack.network

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

internal interface HyperliquidInfoClient {
    suspend fun post(body: JsonObject): JsonElement

    class Impl(
        private val httpClient: HttpClient = platformHttpClient(),
        private val json: Json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        },
    ) : HyperliquidInfoClient {
        override suspend fun post(body: JsonObject): JsonElement {
            val response = httpClient.post(HyperliquidInfoUrl) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(body.toString())
            }

            return json.parseToJsonElement(response.bodyAsText())
        }
    }
}

private const val HyperliquidInfoUrl = "https://api.hyperliquid.xyz/info"
