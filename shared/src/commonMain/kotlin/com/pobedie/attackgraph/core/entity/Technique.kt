package com.pobedie.attackgraph.core.entity

data class Technique(
    val id: String,
    val name: String,
    val description: String,
    val tacticId: String,
    val maturity: TechniqueMaturity
)

enum class TechniqueMaturity(val probabilityMult: Float) {
    Demonstrated(probabilityMult = 0.3f),
    Feasible(probabilityMult = 0.65f),
    Realized(probabilityMult = 0.95f),
    Unknown(probabilityMult = 0f)
}
