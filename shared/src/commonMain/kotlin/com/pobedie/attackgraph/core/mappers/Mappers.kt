package com.pobedie.attackgraph.core.mappers

import com.pobedie.attackgraph.core.entity.AttackVector
import com.pobedie.attackgraph.core.entity.Tactic
import com.pobedie.attackgraph.core.entity.Technique
import com.pobedie.attackgraph.core.entity.TechniqueMaturity
import com.pobedie.attackgraph.database.Relationship


fun com.pobedie.attackgraph.database.Tactic.toDomainModel(
    techniques: List<String>
): Tactic =
    Tactic(
        id = id,
        name = name,
        description = description,
        position = position.toInt(),
        techniquesIds = techniques
    )


fun com.pobedie.attackgraph.database.Technique.toDomainModel(
    tacticId: String
): Technique =
    Technique(
        id = id,
        name = name,
        description = description,
        tacticId = tacticId,
        maturity = when (maturity) {
            "Demonstrated" -> TechniqueMaturity.Demonstrated
            "Feasible" -> TechniqueMaturity.Feasible
            "Realized" -> TechniqueMaturity.Realized
            else -> TechniqueMaturity.Unknown
        },
        severityScore = 3
    )

fun Relationship.toAttackVector(): AttackVector =
    AttackVector(
        caseStudyId = source_id,
        step = try { step_id.removePrefix("S").toInt() } catch (e: Exception) { 0 },
        stepId = step_id,
        tactic = tactic_id,
        targetTechnique = target_id,
        description = description,
        leadsToStep = leads_to
    )
