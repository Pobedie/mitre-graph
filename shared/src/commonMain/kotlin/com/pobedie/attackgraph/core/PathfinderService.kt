package com.pobedie.attackgraph.core

import com.pobedie.attackgraph.core.entity.Edge
import com.pobedie.attackgraph.core.entity.EdgeState
import com.pobedie.attackgraph.core.entity.Node

class PathfinderService {

    data class PathfindingResult(
        val edges: List<Edge>,
        val optimalPaths: List<Pair<List<Edge>, Double>>,
        val probablePaths: List<Pair<List<Edge>, Double>>
    )

    fun calculatePaths(
        nodes: List<Node>,
        edges: List<Edge>,
        rootTechniques: List<Pair<String, String>>,
        targetTechniques: List<Pair<String, String>>
    ): PathfindingResult {
        val rootNodeIds = nodes.filter { node ->
            rootTechniques.any { it.first == node.techniqueId && it.second == node.hostId }
        }.map { it.id }

        val targetNodeIds = nodes.filter { node ->
            targetTechniques.any { it.first == node.techniqueId && it.second == node.hostId }
        }.map { it.id }

        val baseEdges = edges.map {
            if (it.state == EdgeState.Blocked) it else it.copy(state = EdgeState.Idle)
        }

        val allFoundPaths = mutableListOf<Pair<List<Edge>, Double>>()

        if (targetNodeIds.isNotEmpty()) {
            rootNodeIds.forEach { rootId ->
                val pathResult = findOptimalPath(
                    edges = baseEdges,
                    start = rootId,
                    targets = targetNodeIds
                )
                if (pathResult != null) {
                    allFoundPaths.add(pathResult)
                }
            }
        }

        val minCost = allFoundPaths.minOfOrNull { it.second }
        val optimalPaths = if (minCost != null) {
            allFoundPaths.filter { it.second <= minCost + 1e-9 }
        } else emptyList()

        val probablePaths = allFoundPaths.filter { it !in optimalPaths }

        val newEdges = if (optimalPaths.isNotEmpty()) {
            baseEdges.map { edge ->
                when {
                    optimalPaths.any { it.first.contains(edge) } -> edge.copy(state = EdgeState.MostOptimal)
                    probablePaths.any { it.first.contains(edge) } -> edge.copy(state = EdgeState.Probable)
                    else -> edge
                }
            }
        } else {
            baseEdges
        }

        return PathfindingResult(newEdges, optimalPaths, probablePaths)
    }
}
