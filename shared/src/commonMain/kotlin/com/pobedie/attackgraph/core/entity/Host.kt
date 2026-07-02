package com.pobedie.attackgraph.core.entity


data class Host(
    val name: String,
    val id: String,
    val techniques: List<Technique>,
)