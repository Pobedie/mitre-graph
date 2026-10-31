package com.pobedie.attackgraph.network

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class DmRequestException(
    val statusCode: Int?,
    val errorCode: String?,
    override val message: String,
    cause: Throwable? = null
) : Exception(message, cause)

internal fun dmResponseError(statusCode: Int, statusMessage: String, body: String): DmRequestException? {
    val payload = runCatching { Json.parseToJsonElement(body) as? JsonObject }.getOrNull()
    val error = payload?.get("error")
    if (statusCode in 200..299 && (error == null || error.toString() == "null")) return null

    fun JsonObject.text(key: String): String? =
        (get(key) as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

    val details = error as? JsonObject
    val code = details?.text("code") ?: payload?.text("code")
    val message = details?.text("message")
        ?: (error as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
        ?: payload?.text("message")
        ?: payload?.text("detail")
        ?: body.trim().takeIf { it.isNotEmpty() }
        ?: statusMessage
    return DmRequestException(statusCode, code, message)
}

// Keep successful batches in the caller and repeat only this HTTP request on user demand.
internal suspend fun requestDmBody(
    onError: suspend (DmRequestException) -> Boolean,
    request: suspend () -> HttpResponse
): String {
    while (true) {
        val error = try {
            val response = request()
            val body = response.bodyAsText()
            dmResponseError(response.status.value, response.status.description, body) ?: return body
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DmRequestException(null, null, e.message ?: e.toString(), e)
        }
        if (!onError(error)) throw CancellationException("DM request cancelled by user")
    }
}
