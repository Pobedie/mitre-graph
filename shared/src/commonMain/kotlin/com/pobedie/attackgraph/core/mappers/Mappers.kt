package com.pobedie.attackgraph.core.mappers

import com.pobedie.attackgraph.core.entity.AttackVector
import com.pobedie.attackgraph.core.entity.Tactic
import com.pobedie.attackgraph.core.entity.Technique
import com.pobedie.attackgraph.database.Case_study
import com.pobedie.attackgraph.database.Relationship


fun com.pobedie.attackgraph.database.Tactic.toDomainModel(
    techniques: List<Technique>
): Tactic =
    Tactic(
        id = id,
        name = name,
        description = description,
        techniques = techniques
    )


fun com.pobedie.attackgraph.database.Technique.toDomainModel(
    tacticId: String
): Technique =
    Technique(
        id = id,
        name = name,
        description = description,
        tacticId = tacticId
    )

fun Relationship.toAttackVector(): AttackVector =
    AttackVector(
        caseStudyId = source_id,
        step = step_id.removePrefix("S").toInt(),
        tactic = tactic_id,
        targetTechnique = target_id,
        description = description
    )