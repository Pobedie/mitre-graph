package com.pobedie.attackgraph.network

import com.pobedie.attackgraph.core.entity.AttackVector
import com.pobedie.attackgraph.core.entity.Mitigation
import com.pobedie.attackgraph.core.entity.Node

internal fun buildDecisionPrompt(
    techniqueIds: Set<String>,
    techniques: List<Node>,
    mitigations: List<Mitigation>,
    attackVectors: List<AttackVector>
): String {
    val relevantTechniques = techniques.filter { it.techniqueId in techniqueIds }
    val techniquesPrompt: List<String> = relevantTechniques.map {
        "\nID: ${it.techniqueId}\nNAME: ${it.name}\nMATURITY:${it.maturity.name}" +
                "\nSEVERITY SCORE:${it.severityScore}\nDESCRIPTION:${it.description}"
    }

    val relevantMitigations = mitigations.filter { it.isRelevant && it.targetTechnique in techniqueIds }
    val mitigationsPrompt: List<String> = relevantMitigations.map {
        "\nTARGET TECHNIQUE: ${it.targetTechnique}\nNAME: ${it.name}\nRELATIONSHIP WITH TECHNIQUE: ${it.relationshipDescription}" +
                "\nCATEGORIES: ${it.categories}\nLIFECYCLE PHASES: ${it.lifecyclePhases}" +
                "\nMITIGATION DESCRIPTION: ${it.mitigationDescription}"
    }

    val relevantAttackVectors = attackVectors.filter { it.targetTechnique in techniqueIds }
    val csAttackVectorsPrompt: List<String> = relevantAttackVectors.map {
        "\nLEADS FROM TACTIC: ${it.tactic}\nLEADS TO TECHNIQUES: ${it.targetTechnique}" +
                "\nBELONGS TO CASE-STUDY: ${it.caseStudyId}\nSTEP ID: ${it.stepId}\nLEADS TO STEP: ${it.leadsToStep}" +
                "\nDESCRIPTION: ${it.description}"
    }

    return "$SYSTEM_PROMPT\nHere is the list of techniques: ${techniquesPrompt.joinToString("\n")}" +
                "\nHere is the list of mitigations: ${mitigationsPrompt.joinToString("\n")}" +
                "\nHere is the list of case-study proven attack vectors: ${csAttackVectorsPrompt.joinToString("\n")}"
}

private const val SYSTEM_PROMPT = """
    You are a decision engine for a program that helps to calculate possible vector attacks on an AI system.
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
    You must read all the descriptions for available attack techniques, severity scores (set by user. it determines 
    how dangerous this attack is for the system), mitigations and case-study based attack vectors.
    You must analyze the data and determine if an attack vector from two techniques is possible.
    You can build an attack vector that is not present in case-studies based on your observations about the system.
"""
