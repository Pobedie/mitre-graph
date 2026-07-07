package com.pobedie.attackgraph.network

import com.pobedie.attackgraph.core.entity.AttackVector
import com.pobedie.attackgraph.core.entity.Technique
import com.pobedie.attackgraph.core.entity.Mitigation
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText

class LlmService(private val client: HttpClient) {

    suspend fun fetchDecision(
        url: String,
        apiKey: String?,
        model: String,
        techniques: List<Technique>,
        mitigations: List<Mitigation>,
        attackVectors: List<AttackVector>,
    ): List<DecisionOutput> {
        val techniquesPrompt: List<String> = techniques.map {
           "\nID: ${it.tacticId}\nNAME: ${it.name}\nMATURITY:${it.maturity.name}\nSEVERITY SCORE:${it.severityScore}\nDESCRIPTION:${it.description}"
        }
        val mitigationsPrompt: List<String> = mitigations.filter { it.isRelevant }.map {
            "\nTARGET TECHNIQUE: ${it.targetTechnique}\nNAME: ${it.name}\nRELATIONSHIP WITH TECHNIQUE: ${it.relationshipDescription}" +
                    "\nCATEGORIES: ${it.categories}\nLIFECYCLE PHASES: ${it.lifecyclePhases}" +
                    "\nMITIGATION DESCRIPTION: ${it.mitigationDescription}"
        }
        val csAttackVectorsPrompt: List<String> = attackVectors.map {
            "\nLEADS FROM TACTIC: ${it.tactic}\nLEADS TO TECHNIQUES: ${it.targetTechnique}" +
                    "\nBELONGS TO CASE-STUDY: ${it.stepId}\nSTEP ID: ${it.stepId}\nLEADS TO STEP: ${it.leadsToStep}" +
                    "\nDESCRIPTION: ${it.description}"
        }
        val prompt: String =
            SYSTEM_PROMPT +
                    "\nHere is the list of techniques: $techniquesPrompt" +
                    "\nHere is the list of mitigations: $mitigationsPrompt" +
                    "\nHere is the list of case-study proven attack vectors: ${csAttackVectorsPrompt}"
        val response = client.post(url) {
            header("Authorization", "Bearer ${apiKey ?: "DUMMY_KEY"}")
            header("Content-Type", "application/json")
            setBody(Request(
                model = model,
                messages = listOf(
                    Message("system", "You are a decision engine. Return ONLY JSON."),
                    Message("user", prompt)
                ),
                response_format = ResponseFormat()
            ))
        }

        val responseBody = response.bodyAsText()
        return DecisionParser.parse(responseBody)
    }
}

//todo: add MITRE ATLAS explanation
private const val SYSTEM_PROMPT = """
    You are a decision engine for a programm that helps to calculate possible vector attacks on a AI system.
    The data is taken from MITRE ATLAS database and user-based scores. 
    You must read all the descriptions for available attack tehniques, severity scores (set by user. it determines 
    how dangerous this attack is for the system), mitigations and case-study based attack vectors.
    Your task is to return a list of possible paths from one technique to another in this form:
    ```
    [TECHNIQUE_ID1] [TECHNIQUE_ID2] [CONFIDENCE (from 0 to 1)]
    ```
    Here is an example:
    ```
    T0021 T0001 0.8
    T0032 T0054 0.3
    T0009 T0005.1 0.96
    T0021 T0032 0.34
    ```
    
    You MUST only output the list of paths as provided above!!! Do not include headers, explanations, or markdown formatting!
    
"""