package com.pobedie.attackgraph.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class LlmResponse(
    val choices: List<LlmChoice>
)

@Serializable
private data class LlmChoice(
    val message: LlmMessage
)

@Serializable
private data class LlmMessage(
    val content: String
)

object DecisionParser {
    private val regex = Regex("""(\S+)\s+(\S+)\s+(\d*\.?\d+)""")
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(input: String): List<DecisionOutput> {

        val content = try {
            json.decodeFromString<LlmResponse>(input).choices.firstOrNull()?.message?.content ?: ""
        } catch (e: Exception) {
            println("WARNING: LLM output is not valid JSON or doesn't match expected structure. Trying to parse as raw text. Error: ${e.message}")
            input
        }

        return content.lines()
            .mapNotNull { line ->
                val match = regex.find(line.trim())
                if (match != null) {
                    val (id1, id2, conf) = match.destructured
                    DecisionOutput(
                        sourceId = id1,
                        targetId = id2,
                        confidence = conf.toFloatOrNull() ?: 0.0f
                    )
                } else {
                    if (line.isNotBlank()) {
                        println("WARNING: LLM output line didn't match required structure. Skipping. Line: ${line}")
                    }
                    null // Skip lines that don't match the pattern (headers, text, etc.)
                }
            }
    }
}
