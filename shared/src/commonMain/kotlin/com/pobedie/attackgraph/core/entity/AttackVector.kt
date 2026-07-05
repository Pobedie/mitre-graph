package com.pobedie.attackgraph.core.entity

data class AttackVector(
    val caseStudyId: String,
    val step: Int,
    val stepId: String,
    val tactic: String,
    val targetTechnique: String,
    val description: String,
    val leadsTo: List<String>
)
