package com.pobedie.attackgraph.network

object DecisionParser {
    private val regex = Regex("""(\S+)\s+(\S+)\s+(\d*\.?\d+)""")

    fun parse(input: String): List<DecisionOutput> {
        return input.lines()
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
                    println("WARNING: LLM output didn't match required structure. Skipping. LLM output: ${line}")
                    null // Skip lines that don't match the pattern (headers, text, etc.)
                }
            }
    }
}