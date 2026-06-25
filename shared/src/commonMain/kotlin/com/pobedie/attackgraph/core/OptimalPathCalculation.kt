package com.pobedie.attackgraph.core

import com.pobedie.attackgraph.core.entity.Edge
import java.util.PriorityQueue
import kotlin.math.ln

/**
 * Calculates the most optimal path using Dijkstra's algorithm.
 * 
 * Weights are calculated using probability (logarithmic) and risk (linear).
 * @return A pair containing the list of edges in the path and the total cost, or null if no path exists.
 */
fun findOptimalPath(
    edges: List<Edge>,
    start: String,
    target: String,
    alpha: Float = 0.0f
): Pair<List<Edge>, Double>? {
    val adj = edges.groupBy { it.startNode }

    val dist = mutableMapOf<String, Double>().withDefault { Double.POSITIVE_INFINITY }
    val prevEdge = mutableMapOf<String, Edge?>()
    val visited = mutableSetOf<String>()

    dist[start] = 0.0
    val queue = PriorityQueue<Pair<Double, String>>(compareBy { it.first })
    queue.add(0.0 to start)

    while (queue.isNotEmpty()) {
        val (currentDist, currentNode) = queue.poll()
        
        if (currentNode == target) break
        if (currentNode in visited) continue
        visited.add(currentNode)

        adj[currentNode]?.forEach { edge ->
            val prob = edge.probability
            val risk = edge.risk
            
            if (prob != null && risk != null) {
                val weight = edgeWeight(prob, risk, alpha)
                if (weight.isInfinite()) return@forEach
                
                val neighbor = edge.endNode
                val newDist = currentDist + weight
                
                if (newDist < dist.getValue(neighbor)) {
                    dist[neighbor] = newDist
                    prevEdge[neighbor] = edge
                    queue.add(newDist to neighbor)
                }
            }
        }
    }

    if (dist.getValue(target) == Double.POSITIVE_INFINITY) return null
    val path = mutableListOf<Edge>()
    var currentRebuildNode = target
    while (currentRebuildNode != start) {
        val edge = prevEdge[currentRebuildNode] ?: break
        path.add(edge)
        currentRebuildNode = edge.startNode
    }
    if (currentRebuildNode != start) return null
    path.reverse()
    return path to dist.getValue(target)
}

fun edgeWeight(prob: Float, risk: Float, alpha: Float): Double {
    if (prob <= 0.0) return Double.POSITIVE_INFINITY
    val riskPart = alpha * risk
    return -ln(prob.toDouble()) + riskPart
}
