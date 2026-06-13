package com.pobedie.attackgraph.core.mappers

import com.pobedie.attackgraph.core.entity.Tactic
import com.pobedie.attackgraph.core.entity.Technique


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
