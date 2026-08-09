package com.pobedie.attackgraph.network

import com.pobedie.attackgraph.core.entity.AttackVector
import com.pobedie.attackgraph.core.entity.Mitigation
import com.pobedie.attackgraph.core.entity.Node
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText

class LlmService(private val client: HttpClient) {

    suspend fun fetchModels(
        url: String,
        apiKey: String?
    ): List<String> {
        val fullUrl = if (url.endsWith("/models")) url else "${url.removeSuffix("/")}/models"
        val response = client.get(fullUrl) {
            header("Authorization", "Bearer ${apiKey ?: "DUMMY_KEY"}")
        }
        val responseBody = response.body<ModelsResponse>()
        return responseBody.data.map { it.id }
    }

    suspend fun fetchDecision(
        url: String,
        apiKey: String?,
        model: String,
        techniques: List<Node>,
        mitigations: List<Mitigation>,
        attackVectors: List<AttackVector>,
    ): List<DecisionOutput> {

        val fullUrl = if (url.endsWith("/chat/completions")) url else "${url.removeSuffix("/")}/chat/completions"

        val techniquesPrompt: List<String> = techniques.map {
            "\nID: ${it.techniqueId}\nNAME: ${it.name}\nMATURITY:${it.maturity.name}" +
                    "\nSEVERITY SCORE:${it.severityScore}\nDESCRIPTION:${it.description}"
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
        val userPrompt: String =
            "\nHere is the list of techniques: $techniquesPrompt" +
                    "\nHere is the list of mitigations: $mitigationsPrompt" +
                    "\nHere is the list of case-study proven attack vectors: ${csAttackVectorsPrompt}"
        val response = client.post(fullUrl) {
            header("Authorization", "Bearer ${apiKey ?: "DUMMY_KEY"}")
            header("Content-Type", "application/json")
            setBody(
                Request(
                    model = model,
                    messages = listOf(
                        Message("system", SYSTEM_PROMPT),
                        Message("user", userPrompt)
                    ),
                )
            )
        }

        val responseBody = response.bodyAsText()
        println("INFO LLM API response :  ${responseBody.trim()}")
        return DecisionParser.parse(responseBody)
    }
}

private const val SYSTEM_PROMPT = """
    You are a decision engine for a programm that helps to calculate possible vector attacks on an AI system.
    The data is taken from MITRE ATLAS database and user-based scores. 
    MITRE ATLAS is a knowledge base for adversarial tactics and techniques targeting AI and machine learning (ML) systems. It organizes known AI attack behaviors using a matrix similar to MITRE ATT&CK, but focuses specifically on threats against ML pipelines, models, data, and AI-enabled applications.

    Relationships in MITRE ATLAS:

    Tactics represent the high-level objectives an attacker is trying to achieve (the "why"), such as evading detection, manipulating model behavior, or exfiltrating models.
    Techniques describe the specific methods used to accomplish a tactic (the "how"). Each technique belongs to one or more tactics and details a concrete attack approach.
    Mitigations are defensive measures that reduce the likelihood or impact of one or more techniques. A single mitigation can address multiple techniques, and a technique may have multiple applicable mitigations.
    Case Studies are documented real-world incidents, research demonstrations, or proof-of-concept attacks that illustrate how one or more techniques were used. Case studies provide evidence linking techniques to actual adversarial activity and often reference the corresponding tactics and applicable mitigations.
    A tactic groups related techniques by attacker objective.
    A technique implements one or more tactics.
    Mitigations defend against techniques.
    Case studies demonstrate techniques in practice and validate the mapping between tactics and techniques.
    You must read all the descriptions for available attack tehniques, severity scores (set by user. it determines 
    how dangerous this attack is for the system), mitigations and case-study based attack vectors.
    You must analyze the data and determine if an attack vector from two techniques is possible.
    You can build an attack vector that is not present in case-studies based on your observations about the system, 
    but such vectors must have lower confidence score!
    Your task is to return a list of possible paths from one technique to another in this form:
    ```
    [TECHNIQUE_ID1] [TECHNIQUE_ID2] [CONFIDENCE (from 0 to 1)]
    ```
    Here is an example:
    ```
    AML.T0021 AML.T0001 0.8
    AML.T0032 AML.T0054 0.3
    AML.T0009 AML.T0005.1 0.96
    AML.T0021 AML.T0032 0.34
    ```
    
    Don't include techniques that are not present in the list of techniqeus!
    You MUST only output the list of paths as provided above!!! Do not include headers, explanations, or markdown formatting!
    
"""