package com.pobedie.attackgraph.core

import com.pobedie.attackgraph.core.entity.Edge
import com.pobedie.attackgraph.core.entity.EdgeState
import com.pobedie.attackgraph.core.entity.FirewallRule
import com.pobedie.attackgraph.core.entity.Node

/**
 * Calculates probabilities for each edge in the graph based on node maturity, ease coefficient and detection constant.
 *
 * Formula: P_ij = (M_j * E_j) / (sum(M_k * E_k) + Omega)
 */
fun calculateProbabilities(
    edges: List<Edge>,
    nodes: List<Node>,
    firewallRules: List<FirewallRule>
): List<Edge> {
    val omega = 0.5f
    val nodeMap = nodes.associateBy { it.id }

    val adj = edges.groupBy { it.startNode }

    return edges.map { edge ->
        val i = edge.startNode
        val j = edge.endNode

        if (!isEdgeAllowed(edge, firewallRules, nodes)) {
            return@map edge.copy(probability = 0f, state = EdgeState.Blocked)
        }

        val nodeJ = nodeMap[j]
        val mj = nodeJ?.maturity?.probabilityMult ?: 0.0f
        val ej = mapSeverityToEase(nodeJ?.severityScore ?: 3)

        // Sum of attractiveness (M_k * E_k) of all nodes reachable from i
        val sumAttrK = adj[i]?.sumOf {
            val nodeK = nodeMap[it.endNode]
            val mk = nodeK?.maturity?.probabilityMult ?: 0.0f
            val ek = mapSeverityToEase(nodeK?.severityScore ?: 3)
            (mk * ek).toDouble()
        }?.toFloat() ?: 0.0f

        // p_ij = (M_j * E_j) / (sum(M_k * E_k) + omega)
        val pij = if (sumAttrK + omega > 0) (mj * ej) / (sumAttrK + omega) else 0.0f

        edge.copy(probability = pij)
    }
}

fun calculateProbabilitiesSimple(
    edges: List<Edge>,
    nodes: List<Node>,
): List<Edge> {
    val nodeMap = nodes.associateBy { it.id }

    return edges.map { edge ->
        val j = edge.endNode

        if (edge.state == EdgeState.Blocked) {
            return@map edge.copy(probability = 0f)
        }

        val nodeJ = nodeMap[j]
        val mj = nodeJ?.maturity?.probabilityMult ?: 0.0f
        val ej = mapSeverityToEase(nodeJ?.severityScore ?: 3)
        val cj = edge.llmConfidence ?: 1f

        val pij = (mj * ej * cj)

        edge.copy(probability = pij)
    }
}

fun isEdgeAllowed(
    edge: Edge,
    firewallRules: List<FirewallRule>,
    nodes: List<Node>
): Boolean {
    val sourceNode = nodes.find { it.id == edge.startNode } ?: return false
    val targetNode = nodes.find { it.id == edge.endNode } ?: return false

    return firewallRules.any { rule ->
        rule.sourceHostId == sourceNode.hostId &&
                rule.targetHostId == targetNode.hostId &&
                (rule.sourceTechniqueId == null || rule.sourceTechniqueId == sourceNode.techniqueId)
    }
}

private fun mapSeverityToEase(severity: Int): Float {
    return when (severity) {
        1 -> 0.1f
        2 -> 0.3f
        3 -> 0.5f
        4 -> 0.7f
        5 -> 0.9f
        else -> 0.5f
    }
}
