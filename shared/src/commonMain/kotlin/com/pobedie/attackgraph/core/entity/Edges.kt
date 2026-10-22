package com.pobedie.attackgraph.core.entity

data class Edge(
    val startNode: String,
    val endNode: String,
    val probability: Float? = null,
    val dmConfidence: Float? = 1f, // by default 1 in case it wasn't set by dm
    val state: EdgeState = EdgeState.Idle
)

enum class EdgeState{
    Idle, CaseStudyProven, Blocked, Probable, MostOptimal
}
