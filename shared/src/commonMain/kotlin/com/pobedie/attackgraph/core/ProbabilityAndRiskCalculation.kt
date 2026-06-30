package com.pobedie.attackgraph.core

import com.pobedie.attackgraph.core.entity.Edge
import com.pobedie.attackgraph.core.entity.Node

/**
 * Calculates probabilities and risks for each edge in the graph based on node maturity and distance to goal.
 *
 * Step 1: Quantify Maturity (already in TechniqueMaturity enum)
 * Step 2: Calculate Edge Probability
 * Step 3: Calculate Edge Risk
 */
fun calculateProbabilitiesAndRisks(
    edges: List<Edge>,
    nodes: List<Node>,
    target: String
): List<Edge> {
    val omega = 0.1f
    val nodeMap = nodes.associateBy { it.id }

    // 1. Calculate D(j, Goal) - Shortest distance to goal node using BFS on reversed graph
    val reversedAdj = edges.groupBy { it.endNode }
    val distToGoal = mutableMapOf<String, Int>()
    val queue = mutableListOf<String>()

    if (target.isNotEmpty() && nodeMap.containsKey(target)) {
        distToGoal[target] = 0
        queue.add(target)
    }

    var head = 0
    while (head < queue.size) {
        val current = queue[head++]
        val d = distToGoal[current]!!
        reversedAdj[current]?.forEach { edge ->
            val neighbor = edge.startNode
            if (!distToGoal.containsKey(neighbor)) {
                distToGoal[neighbor] = d + 1
                queue.add(neighbor)
            }
        }
    }

    // 2. Calculate probabilities and risks for each edge
    val adj = edges.groupBy { it.startNode }

    return edges.map { edge ->
        val i = edge.startNode
        val j = edge.endNode

        val nodeJ = nodeMap[j]
        val mj = nodeJ?.maturity?.probabilityMult ?: 0.0f

        // Sum of maturity of all nodes reachable from i
        val sumMk = adj[i]?.sumOf {
            val targetNode = nodeMap[it.endNode]
            (targetNode?.maturity?.probabilityMult ?: 0.0f).toDouble()
        }?.toFloat() ?: 0.0f

        // p_ij = M_j / (sum(M_k) + omega)
        val pij = if (sumMk + omega > 0) mj / (sumMk + omega) else 0.0f

        val djGoal = distToGoal[j]
        // Risk factor based on distance to goal.
        // Formula: 1 / D(j, Goal). We use maxOf(1, djGoal) to avoid division by zero.
        val riskFactor = if (djGoal != null) {
            1.0f / maxOf(1, djGoal)
        } else {
            0.0f // Goal not reachable from j
        }

        // r_ij = p_ij * (M_j * (1 / D(j, Goal)))
        val rij = pij * mj * riskFactor

        edge.copy(probability = pij, risk = rij)
    }
}
