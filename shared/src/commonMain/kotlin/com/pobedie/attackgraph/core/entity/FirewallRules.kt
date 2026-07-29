package com.pobedie.attackgraph.core.entity

data class FirewallRule(
    val sourceHostId: String,
    val sourceTechniqueId: String?,
    val targetHostId: String
)
