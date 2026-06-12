package com.pobedie.attackgraph.core.entity

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AtlasYaml(
    @SerialName("format-version") val formatVersion: String,
    val collection: AtlasCollection,
    val matrix: AtlasMatrix,
    val tactics: Map<String, AtlasTactic>,
    val techniques: Map<String, AtlasTechnique>,
    val mitigations: Map<String, AtlasMitigation>,
    @SerialName("case-studies") val caseStudies: Map<String, AtlasCaseStudy>,
    val relationships: Map<String, AtlasRelationshipGroup>
)

@Serializable
data class AtlasCollection(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    @SerialName("created-date") val createdDate: String,
    @SerialName("modified-date") val modifiedDate: String,
    val uuid: String,
)

@Serializable
data class AtlasMatrix(
    val id: String,
    val name: String,
    val description: String,
    val uuid: String,
    @SerialName("created-date") val createdDate: String,
    @SerialName("modified-date") val modifiedDate: String,
)

@Serializable
data class AtlasTactic(
    val id: String,
    val name: String,
    val description: String = "",
    val uuid: String,
    @SerialName("attack-reference") val attackReference: Map<String, String> = mapOf(),
    @SerialName("created-date") val createdDate: String,
    @SerialName("modified-date") val modifiedDate: String,
)

@Serializable
data class AtlasTechnique(
    val id: String,
    val name: String,
    val description: String,
    val uuid: String,
    @SerialName("attack-reference") val attackReference: Map<String, String> = mapOf(),
    @SerialName("created-date") val createdDate: String,
    @SerialName("modified-date") val modifiedDate: String,
    val platforms: List<String> = emptyList(),
    val maturity: String = ""
)

@Serializable
data class AtlasMitigation(
    val id: String,
    val name: String,
    val description: String,
    @SerialName("created-date") val createdDate: String,
    @SerialName("modified-date") val modifiedDate: String,
    @SerialName("lifecycle-phases") val lifecyclePhases: List<String> = emptyList(),
    val categories: List<String> = emptyList()
)

@Serializable
data class AtlasCaseStudy(
    val id: String,
    val name: String,
    val description: String,
    @SerialName("created-date") val createdDate: String,
    @SerialName("modified-date") val modifiedDate: String,
    val type: String = "",
    val date: String = ""
)

@Serializable
data class AtlasRelationshipGroup(
    val employs: List<AtlasRelationship> = emptyList(),
    val achieves: List<AtlasRelationship> = emptyList(),
    val specializes: List<AtlasRelationship> = emptyList(),
    val mitigates: List<AtlasRelationship> = emptyList()
)

@Serializable
data class AtlasRelationship(
    val source: String,
    val target: String,
    @SerialName("relationship-type") val relationshipType: String,
    val description: String = "",
    val tactic: String = "",
    @SerialName("step-id") val stepId: String = "",
    @SerialName("leads-to") val leadsTo: List<String> = emptyList()
)

