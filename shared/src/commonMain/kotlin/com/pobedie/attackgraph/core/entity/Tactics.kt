package com.pobedie.attackgraph.core.entity

data class Tactic(
    val id: String,
    val name: String,
    val description: String,
    val techniques: List<Technique>,
)
