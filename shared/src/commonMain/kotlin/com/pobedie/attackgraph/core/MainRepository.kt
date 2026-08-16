package com.pobedie.attackgraph.core

import com.pobedie.attackgraph.database.Atlas
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.pobedie.attackgraph.core.entity.AtlasYaml
import com.pobedie.attackgraph.core.entity.AttackVector
import com.pobedie.attackgraph.core.entity.Mitigation
import com.pobedie.attackgraph.core.entity.Tactic
import com.pobedie.attackgraph.core.entity.UserSettings
import com.pobedie.attackgraph.core.mappers.toAttackVector
import com.pobedie.attackgraph.core.mappers.toDomainModel
import com.pobedie.attackgraph.settings.UserSettingsDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

class MainRepository(
    val atlasDatabase: Atlas,
    val settingsDatabase: UserSettingsDb
) {

    private data class State(
        val isImportSuccessful: Boolean = false
    )
    private val _state = MutableStateFlow(State())

    fun saveUserSettings(settings: UserSettings) {
        settingsDatabase.user_settingsQueries.saveUserSettings(
            llm_url = settings.llmUrl,
            llm_api_key = settings.llmApiKey,
            llm_model = settings.llmModel
        )
    }

    fun getUserSettings(): UserSettings? {
        return settingsDatabase.user_settingsQueries.getUserSettings()
            .executeAsOneOrNull()
            ?.let {
                UserSettings(
                    llmUrl = it.llm_url,
                    llmApiKey = it.llm_api_key,
                    llmModel = it.llm_model
                )
            }
    }

    suspend fun importMitreAtlasData(yamlContent: String) = withContext(Dispatchers.IO) {
        val yaml = Yaml(
            configuration = YamlConfiguration(
                strictMode = false
            )
        )
        val atlasData = yaml.decodeFromString(AtlasYaml.serializer(), yamlContent)

        val tacticPositions = atlasData.relationships["ATLAS-matrix"]?.sequences?.associate { it.target to it.position } ?: emptyMap()

        atlasDatabase.transaction {
            atlasDatabase.metadataQueries.insertOrReplaceMetadata(
                id = atlasData.collection.id,
                formatVersion = atlasData.formatVersion,
                dataVersion = atlasData.collection.version,
                name = atlasData.collection.name,
                description = atlasData.collection.description
            )

            atlasData.tactics.values.forEach { tactic ->
                atlasDatabase.tacticsQueries.insertTactic(
                    id = tactic.id,
                    name = tactic.name,
                    description = tactic.description,
                    created_date = tactic.createdDate,
                    modified_date = tactic.modifiedDate,
                    position = tacticPositions[tactic.id]?.toLong() ?: 0L
                )
            }

            atlasData.techniques.values.forEach { technique ->
                // We need to find the tactic_id. 
                // In ATLAS YAML, techniques are linked to tactics via 'achieves' relationships.
                // We'll search for 'achieves' relationship where this technique is source and target is a tactic.
                val tacticId = atlasData.relationships[technique.id]?.achieves
                    ?.firstOrNull { it.target.startsWith("AML.TA") }?.target ?: "UNKNOWN"

                atlasDatabase.techniqueQueries.insertTechnique(
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
                atlasDatabase.mitigationQueries.insertMitigation(
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
                atlasDatabase.`case-studyQueries`.insertCaseStudy(
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

                    atlasDatabase.relationshipsQueries.insertRelationship(
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
                    
                    atlasDatabase.relationshipsQueries.insertRelationship(
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

                    atlasDatabase.relationshipsQueries.insertRelationship(
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

    suspend fun getTacticsWithTechniques(): List<Tactic> = withContext(Dispatchers.IO) {
        val tactics = atlasDatabase.tacticsQueries.selectAllTactics().executeAsList().map { tactic ->
            val techniques = atlasDatabase.techniqueQueries.selectTechniquesByTactic(tactic.id).executeAsList()
            tactic.toDomainModel(
                techniques = techniques.map { it.toDomainModel(tacticId = tactic.id) }
            )
        }.sortedBy { it.position }
        println("Tactics fetched from db:\n  ${tactics.map { it.id + " " + it.name }}")
        return@withContext tactics
    }

    suspend fun getAttackVectors(techniques: List<String>): List<AttackVector> = withContext(Dispatchers.IO){
        val relatedCaseStudies = techniques.flatMap { _techniqueId ->
            atlasDatabase.relationshipsQueries.selectRelationshipsByTargetTechnique(_techniqueId)
                .executeAsList()
                .mapNotNull {
                    if (it.relationship_type == "employs") it.source_id else null
                }
        }.distinct()

        val attackVectors = relatedCaseStudies.flatMap {
            atlasDatabase.relationshipsQueries.selectRelationshipsByCaseStudy(it)
                .executeAsList()
                .map { _relationship ->
                    _relationship.toAttackVector()
                }
        }
        println("For techniques: $techniques found attack vectors :\n\t${attackVectors.map { "${it.stepId} ${it.tactic}" }}")
        return@withContext attackVectors
    }

    suspend fun getMittigations(techniques: List<String>): List<Mitigation> = withContext(Dispatchers.IO) {
        val mitigations: List<Mitigation> = techniques.flatMap { _techId ->
                atlasDatabase.relationshipsQueries.selectRelationshipsByTargetTechnique(_techId)
                    .executeAsList()
                    .mapNotNull { _relationship ->
                        if (_relationship.relationship_type == "mitigates" && _relationship.target_id == _techId) {
                            val mitigations = atlasDatabase.mitigationQueries.selectMitigationById(_relationship.source_id)
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

    suspend fun getAllMitigations(): List<Mitigation> = withContext(Dispatchers.IO) {
        val relationships = atlasDatabase.relationshipsQueries.selectAllMitigationRelationships().executeAsList()
        val mitigations = relationships.mapNotNull { _relationship ->
            val mitigationData = atlasDatabase.mitigationQueries.selectMitigationById(_relationship.source_id)
                .executeAsOneOrNull() ?: return@mapNotNull null
            Mitigation(
                id = _relationship.source_id,
                name = mitigationData.name,
                mitigationDescription = mitigationData.description,
                relationshipDescription = _relationship.description,
                targetTechnique = _relationship.target_id,
                categories = mitigationData.categories,
                lifecyclePhases = mitigationData.lifecycle_phases,
                isRelevant = true
            )
        }
        return@withContext mitigations
    }

    val importState: Flow<Boolean> = _state.map { it.isImportSuccessful }

    suspend fun saveTechniqueEmbedding(techniqueId: String, embedding: FloatArray) = withContext(Dispatchers.IO) {
        atlasDatabase.embeddingsQueries.insertTechniqueEmbedding(
            technique_id = techniqueId,
            embedding = embedding.toByteArray()
        )
    }

    suspend fun saveMitigationEmbedding(mitigationId: String, embedding: FloatArray) = withContext(Dispatchers.IO) {
        atlasDatabase.embeddingsQueries.insertMitigationEmbedding(
            mitigation_id = mitigationId,
            embedding = embedding.toByteArray()
        )
    }

    suspend fun getTechniqueEmbedding(techniqueId: String): FloatArray? = withContext(Dispatchers.IO) {
        val blob = atlasDatabase.embeddingsQueries.getTechniqueEmbedding(techniqueId).executeAsOneOrNull()
            ?: return@withContext null
        return@withContext blob.toFloatArray()
    }

    suspend fun getMitigationEmbedding(mitigationId: String): FloatArray? = withContext(Dispatchers.IO) {
        val blob = atlasDatabase.embeddingsQueries.getMitigationEmbedding(mitigationId).executeAsOneOrNull()
            ?: return@withContext null
        return@withContext blob.toFloatArray()
    }

    suspend fun getRelevantMitigations(
        targetTechniqueIds: List<String>,
        graphCentroid: FloatArray?,
        topK: Int = 20
    ): List<Mitigation> = withContext(Dispatchers.IO) {
        val directMitigations = getMittigations(targetTechniqueIds)

        if (graphCentroid == null) return@withContext directMitigations

        val allMitigationData = atlasDatabase.mitigationQueries.selectAllMitigations().executeAsList()
        val allEmbeddings = atlasDatabase.embeddingsQueries.getAllMitigationEmbeddings().executeAsList()

        val scoredMitigations = allEmbeddings.mapNotNull { emb ->
            val mitigation = allMitigationData.find { it.id == emb.mitigation_id } ?: return@mapNotNull null
            val similarity = calculateCosineSimilarity(graphCentroid, emb.embedding.toFloatArray())
            mitigation to similarity
        }.sortedByDescending { it.second }.take(topK)

        val result = (directMitigations + scoredMitigations.map { (m, _) ->
            Mitigation(
                id = m.id,
                name = m.name,
                mitigationDescription = m.description,
                relationshipDescription = "Semantically relevant to the attack path",
                targetTechnique = "Semantic Match",
                categories = m.categories,
                lifecyclePhases = m.lifecycle_phases,
                isRelevant = true
            )
        }).distinctBy { it.id }

        return@withContext result
    }

    private fun calculateCosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denominator = kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB)
        return if (denominator > 0) dotProduct / denominator else 0f
    }

    private fun FloatArray.toByteArray(): ByteArray {
        val bytes = ByteArray(size * 4)
        ByteBuffer.wrap(bytes).asFloatBuffer().put(this)
        return bytes
    }

    private fun ByteArray.toFloatArray(): FloatArray {
        val floats = FloatArray(size / 4)
        ByteBuffer.wrap(this).asFloatBuffer().get(floats)
        return floats
    }
}
