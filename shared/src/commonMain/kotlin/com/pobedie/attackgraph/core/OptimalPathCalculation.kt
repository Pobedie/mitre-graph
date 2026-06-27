package com.pobedie.attackgraph.core

import com.pobedie.attackgraph.core.entity.Edge
import com.pobedie.attackgraph.core.entity.Node
import java.util.PriorityQueue
import kotlin.collections.forEach
import kotlin.math.ln

// returns the most optimal path and its cost
fun findOptimalPath(
    edges: List<Edge>,
    start: String,
    target: String,
    alpha: Float = 0.0f
): Pair<List<Edge>, Double>? {

    val dist = mutableMapOf<String, Double>().withDefault { Double.POSITIVE_INFINITY }
    val prev = mutableMapOf<String, String?>()
    val visited = mutableSetOf<String>()

    dist[start] = 0.0
    val queue = PriorityQueue<Pair<Double, String>>(compareBy { it.first })
    queue.add(0.0 to start)

    queueLoop@ while (queue.isNotEmpty()) {
        val (currentDist, currentNode) = queue.poll()
        if (currentNode == target) break
        if (currentNode in visited) continue
        visited.add(currentNode)

        edges.forEach { _edge ->
            if (_edge.probability != null && _edge.risk != null) {
                val weight = edgeWeight(_edge.probability, _edge.risk, alpha )
                if (weight.isInfinite()) continue@queueLoop
                val target = _edge.endNode
                val newDist = currentDist + weight
                if (newDist < dist.getValue(target)) {
                    dist[target] = newDist
                    prev[target] = currentNode
                    queue.add(newDist to target)
                }
            }
        }
    }

    // rebuilding path
    if (dist.getValue(target).isInfinite()) return null

    val path = mutableListOf<String>()
    var currentRebuildNode: String? = target
    while (currentRebuildNode != null) {
        path.add(currentRebuildNode)
        if (currentRebuildNode == start) break
        currentRebuildNode = prev[currentRebuildNode]
    }
    path.reverse()
    return if (path.first() == start && path.last() == target) {
        path.zipWithNext().mapNotNull { (_start, _end) ->
            edges.find { it.startNode == _start && it.endNode == _end }
        } to dist.getValue(target)
    } else null
}

fun edgeWeight(prob: Float, risk: Float, alpha: Float): Double {
    if (prob <= 0.0) return Double.POSITIVE_INFINITY
    val riskPart =  alpha * risk
    return -ln(prob.toDouble()) + riskPart
}
