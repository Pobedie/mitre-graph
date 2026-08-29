package com.pobedie.attackgraph.core.entity


data class Host(
    val name: String,
    val id: String,
    val techniquesIds: List<String>,
)
