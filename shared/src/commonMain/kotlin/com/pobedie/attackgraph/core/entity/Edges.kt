package com.pobedie.attackgraph.core.entity

data class Edge(
    val startNode: String,
    val endNode: String,
    val probability: Float? = null,
    val risk: Float? = null,
    val state: EdgeState = EdgeState.Idle
)

enum class EdgeState{
    Idle, CaseStudyProven, Unsuccessful, Probable, MostOptimal
}
