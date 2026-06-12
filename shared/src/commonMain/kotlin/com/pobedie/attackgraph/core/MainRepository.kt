package com.pobedie.attackgraph.core

import com.pobedie.attackgraph.database.Atlas
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.pobedie.attackgraph.core.entity.AtlasYaml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainRepository(
    val database: Atlas
) {

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

            // Insert Relationships (only 'employs' for now as per schema requirements)
            atlasData.relationships.values.forEach { group ->
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
            }
        }
    }
}
