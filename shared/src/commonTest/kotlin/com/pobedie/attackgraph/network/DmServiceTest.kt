package com.pobedie.attackgraph.network

import androidx.compose.ui.graphics.Color
import com.pobedie.attackgraph.core.entity.Node
import com.pobedie.attackgraph.core.entity.NodeTactic
import com.pobedie.attackgraph.core.entity.TechniqueMaturity
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertFalse

class DmServiceTest {
    @Test
    fun readsStructuredPlainAndEmptyErrors() {
        val structured = dmResponseError(429, "Too Many Requests", """{"error":{"code":"rate_limit","message":"Please wait"}}""")!!
        assertEquals(429, structured.statusCode)
        assertEquals("rate_limit", structured.errorCode)
        assertEquals("Please wait", structured.message)
        assertEquals("Model missing", dmResponseError(404, "Not Found", """{"error":"Model missing"}""")!!.message)
        assertEquals("Unavailable", dmResponseError(503, "Service Unavailable", "Unavailable")!!.message)
        assertEquals("Service Unavailable", dmResponseError(503, "Service Unavailable", "")!!.message)
        assertEquals("Invalid input", dmResponseError(422, "Unprocessable Entity", """{"detail":"Invalid input"}""")!!.message)
        assertEquals("Model error", dmResponseError(200, "OK", """{"error":{"message":"Model error"}}""")!!.message)
        assertNull(dmResponseError(200, "OK", """{"answers":{}}"""))
    }

    @Test
    fun retryResendsOnlyFailedBatchAndPreservesProgress() = runBlocking {
        val bodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            val body = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            bodies += body
            if (bodies.size == 2) {
                respond("""{"error":{"code":"overloaded","message":"Try again"}}""", HttpStatusCode.ServiceUnavailable)
            } else {
                val payload = Json.decodeFromString<DmRequest>(body)
                val target = payload.questions.getValue("potential_attacks").criteria.keys.first()
                respond("""{"answers":{"potential_attacks":{"probabilities":{"$target":0.8}}}}""")
            }
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json() } }
        try {
            val progress = mutableListOf<Int>()
            var errors = 0
            val decisions = DmService(client).fetchDecision(
                "http://dm.test/v1", null, "selected-model", listOf(node("a"), node("b"), node("c")),
                emptyList(), emptyList(),
                onProgress = { processed, _ -> progress += processed },
                onError = { error ->
                    errors++
                    assertEquals(503, error.statusCode)
                    assertEquals("overloaded", error.errorCode)
                    assertEquals("Try again", error.message)
                    true
                }
            )
            assertEquals(1, errors)
            assertEquals(4, bodies.size)
            assertEquals(bodies[1], bodies[2])
            assertEquals(listOf("a", "b", "c"), decisions.map { it.sourceId })
            assertEquals(listOf(0, 1, 2, 3), progress)
        } finally {
            client.close()
        }
    }

    @Test
    fun cancellingErrorStopsRequests() = runBlocking {
        var attempts = 0
        val client = HttpClient(MockEngine {
            attempts++
            respond("""{"message":"Invalid API key"}""", HttpStatusCode.Unauthorized)
        })
        try {
            assertFailsWith<CancellationException> {
                DmService(client).fetchModels("http://dm.test/v1", "bad-key", onError = {
                    assertEquals(401, it.statusCode)
                    assertEquals("Invalid API key", it.message)
                    false
                })
            }
            assertEquals(1, attempts)
        } finally {
            client.close()
        }
    }

    @Test
    fun modelListingSupportsRetry() = runBlocking {
        var attempts = 0
        val client = HttpClient(MockEngine {
            attempts++
            if (attempts == 1) respond("Please wait", HttpStatusCode.TooManyRequests)
            else respond("""{"data":[{"id":"selected-model"}]}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        })
        try {
            assertEquals(listOf("selected-model"), DmService(client).fetchModels("http://dm.test/v1", null, onError = { true }))
            assertEquals(2, attempts)
        } finally {
            client.close()
        }
    }

    @Test
    fun waitsForUserDecisionBeforeRetrying() = runBlocking {
        var attempts = 0
        val shownError = CompletableDeferred<DmRequestException>()
        val retryDecision = CompletableDeferred<Boolean>()
        val client = HttpClient(MockEngine {
            attempts++
            if (attempts == 1) respond("Temporarily unavailable", HttpStatusCode.ServiceUnavailable)
            else respond("""{"data":[]}""")
        })
        try {
            withTimeout(5_000) {
                val pending = async {
                    DmService(client).fetchModels("http://dm.test/v1", null, onError = {
                        shownError.complete(it)
                        retryDecision.await()
                    })
                }
                assertEquals(503, shownError.await().statusCode)
                assertEquals(1, attempts)
                assertFalse(pending.isCompleted)
                retryDecision.complete(true)
                assertEquals(emptyList(), pending.await())
                assertEquals(2, attempts)
            }
        } finally {
            client.close()
        }
    }

    private fun node(id: String) = Node(
        id, id, "host", "Host", id, "Description", TechniqueMaturity.Realized, 3,
        NodeTactic("tactic", "Tactic", Color.Gray, 1)
    )
}
