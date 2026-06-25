package com.pobedie.attackgraph.core

import com.pobedie.attackgraph.database.Atlas
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.pobedie.attackgraph.core.entity.AtlasYaml
import com.pobedie.attackgraph.core.entity.AttackVector
import com.pobedie.attackgraph.core.entity.Mitigation
import com.pobedie.attackgraph.core.entity.Tactic
import com.pobedie.attackgraph.core.mappers.toAttackVector
import com.pobedie.attackgraph.core.mappers.toDomainModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class MainRepository(
    val database: Atlas
) {

    private data class State(
        val isImportSuccessful: Boolean = false
    )
    private val _state = MutableStateFlow(State())

    suspend fun importMitreAtlasData(yamlContent: String) = withContext(Dispatchers.IO) {
        val yaml = Yaml(
            configuration = YamlConfiguration(
                strictMode = false
            )
        )
        val atlasData = yaml.decodeFromString(AtlasYaml.serializer(), yamlContent)

        database.transaction {
            database.metadataQueries.insertOrReplaceMetadata(
                id = atlasData.collection.id,
                formatVersion = atlasData.formatVersion,
                dataVersion = atlasData.collection.version,
                name = atlasData.collection.name,
                description = atlasData.collection.description
            )

            atlasData.tactics.values.forEach { tactic ->
                database.tacticsQueries.insertTactic(
                    id = tactic.id,
                    name = tactic.name,
                    description = tactic.description,
                    created_date = tactic.createdDate,
                    modified_date = tactic.modifiedDate
                )
            }

            atlasData.techniques.values.forEach { technique ->
                // We need to find the tactic_id. 
                // In ATLAS YAML, techniques are linked to tactics via 'achieves' relationships.
                // We'll search for 'achieves' relationship where this technique is source and target is a tactic.
                val tacticId = atlasData.relationships[technique.id]?.achieves
                    ?.firstOrNull { it.target.startsWith("AML.TA") }?.target ?: "UNKNOWN"

                database.techniqueQueries.insertTechnique(
                    id = technique.id,
                    name = technique.name,
                    description = technique.description,
                    created_date = technique.createdDate,
                    modified_date = technique.modifiedDate,
                    platforms = technique.platforms,
                    maturity = technique.maturity,
                    tactic_id = tacticId
                )
            }

            // Insert Mitigations
            atlasData.mitigations.values.forEach { mitigation ->
                database.mitigationQueries.insertMitigation(
                    id = mitigation.id,
                    name = mitigation.name,
                    description = mitigation.description,
                    created_date = mitigation.createdDate,
                    modified_date = mitigation.modifiedDate,
                    lifecycle_phases = mitigation.lifecyclePhases,
                    categories = mitigation.categories
                )
            }

            // Insert Case Studies
            atlasData.caseStudies.values.forEach { caseStudy ->
                database.`case-studyQueries`.insertCaseStudy(
                    id = caseStudy.id,
                    name = caseStudy.name,
                    description = caseStudy.description,
                    created_date = caseStudy.createdDate,
                    modified_date = caseStudy.modifiedDate,
                    type = caseStudy.type,
                    date = caseStudy.date
                )
            }

            atlasData.relationships.values.forEach { group ->
                group.mitigates.forEach { rel ->
                    val tacticId = rel.tactic.takeIf { it.isNotEmpty() }
                        ?: atlasData.relationships[rel.target]?.achieves
                            ?.firstOrNull { it.target.startsWith("AML.T") }?.target
                        ?: "UNKNOWN"

                    database.relationshipsQueries.insertRelationship(
                        step_id = rel.stepId,
                        source_id = rel.source,
                        target_id = rel.target,
                        relationship_type = rel.relationshipType,
                        description = rel.description,
                        tactic_id = tacticId,
                        leads_to = rel.leadsTo
                    )
                }
                group.employs.forEach { rel ->
                    // relationship schema requires tactic_id
                    // Sometimes tactic is present in 'employs' relationship
                    val tacticId = rel.tactic.takeIf { it.isNotEmpty() } 
                        ?: atlasData.relationships[rel.target]?.achieves
                            ?.firstOrNull { it.target.startsWith("AML.TA") }?.target 
                        ?: "UNKNOWN"
                    
                    database.relationshipsQueries.insertRelationship(
                        step_id = rel.stepId,
                        source_id = rel.source,
                        target_id = rel.target,
                        relationship_type = rel.relationshipType,
                        description = rel.description,
                        tactic_id = tacticId,
                        leads_to = rel.leadsTo
                    )
                }
                group.achieves.forEach { rel ->
                    val tacticId = rel.tactic.takeIf { it.isNotEmpty() }
                        ?: atlasData.relationships[rel.target]?.achieves
                            ?.firstOrNull { it.target.startsWith("AML.TA") }?.target
                        ?: "UNKNOWN"

                    database.relationshipsQueries.insertRelationship(
                        step_id = rel.stepId,
                        source_id = rel.source,
                        target_id = rel.target,
                        relationship_type = rel.relationshipType,
                        description = rel.description,
                        tactic_id = tacticId,
                        leads_to = rel.leadsTo
                    )
                }
            }
        }

        // todo: add error handling
        _state.update { it.copy(isImportSuccessful = true) }
    }

    suspend fun getTactics(): List<Tactic> = withContext(Dispatchers.IO) {
        val tactics = database.tacticsQueries.selectAllTactics().executeAsList().map { tactic ->
            val techniques = database.techniqueQueries.selectTechniquesByTactic(tactic.id).executeAsList()
            tactic.toDomainModel(
                techniques = techniques.map { it.toDomainModel(tacticId = tactic.id) }
            )
        }
            .sortedBy { it.id }
        println("Tactics fetched from db:\n  ${tactics.map { it.id + " " + it.name }}")
        return@withContext tactics
    }

    suspend fun getAttackVectors(targetTechnique: String ): List<AttackVector> = withContext(Dispatchers.IO){
        val relatedCaseStudies = database.relationshipsQueries.selectRelationshipsByTargetTechnique(targetTechnique)
            .executeAsList()
            .mapNotNull {
                if (it.relationship_type == "employs") it.source_id else null
            }
        val attackVectors = relatedCaseStudies.flatMap {
            database.relationshipsQueries.selectRelationshipsByCaseStudy(it)
                .executeAsList()
                .map { _relationship ->
                    _relationship.toAttackVector()
                }
        }
        println("For target technique: $targetTechnique found attack vectors :\n\t${attackVectors}")
        return@withContext attackVectors
    }

    suspend fun getMittigations(techniques: List<String>): List<Mitigation> = withContext(Dispatchers.IO) {
        val mitigations: List<Mitigation> = techniques.flatMap { _techId ->
                database.relationshipsQueries.selectRelationshipsByTargetTechnique(_techId)
                    .executeAsList()
                    .mapNotNull { _relationship ->
                        if (_relationship.relationship_type == "mitigates" && _relationship.target_id == _techId) {
                            val mitigations = database.mitigationQueries.selectMitigationById(_relationship.source_id)
                                .executeAsOne()
                            Mitigation(
                                id = _relationship.source_id,
                                name = mitigations.name,
                                mitigationDescription = mitigations.description,
                                relationshipDescription = _relationship.description,
                                targetTechnique = _techId,
                                categories = mitigations.categories,
                                lifecyclePhases = mitigations.lifecycle_phases,
                                isRelevant = true
                            )
                    } else null
            }
        }
        return@withContext mitigations
    }

    val importState: Flow<Boolean> = _state.map { it.isImportSuccessful }
}
