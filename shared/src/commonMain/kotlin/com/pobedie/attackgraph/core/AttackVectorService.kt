package com.pobedie.attackgraph.core

import com.pobedie.attackgraph.core.entity.AttackVector
import com.pobedie.attackgraph.core.entity.Edge
import com.pobedie.attackgraph.core.entity.EdgeState
import com.pobedie.attackgraph.core.entity.FirewallRule
import com.pobedie.attackgraph.core.entity.Host
import com.pobedie.attackgraph.core.entity.Mitigation
import com.pobedie.attackgraph.core.entity.Node
import com.pobedie.attackgraph.core.entity.NodeTactic
import com.pobedie.attackgraph.core.entity.Tactic
import com.pobedie.attackgraph.core.entity.Technique
import com.pobedie.attackgraph.network.DecisionOutput
import com.pobedie.attackgraph.ui.generateColorFromId

class AttackVectorService {

    fun buildNodes(
        hosts: List<Host>,
        tactics: List<Tactic>,
        allTechniques: List<Technique>
    ): List<Node> {
        return hosts.flatMap { host ->
            host.techniquesIds.mapNotNull { techniqueId ->
                val technique = allTechniques.find { it.id == techniqueId } ?: return@mapNotNull null
                val tactic = tactics.findLast { it.id == technique.tacticId } ?: return@mapNotNull null
                val color = generateColorFromId(technique.tacticId)
                Node(
                    id = "${host.id}_${technique.id}",
                    techniqueId = technique.id,
                    hostId = host.id,
                    hostName = host.name,
                    name = technique.name,
                    description = technique.description,
                    maturity = technique.maturity,
                    severityScore = technique.severityScore,
                    tactic = NodeTactic(
                        id = technique.tacticId,
                        name = tactic.name,
                        color = color,
                        position = tactic.position
                    ),
                )
            }
        }
    }

    fun buildEdgesFromCaseStudies(
        nodes: List<Node>,
        attackVectors: List<AttackVector>,
        mitigations: List<Mitigation>,
        firewallRules: List<FirewallRule>
    ): List<Edge> {
        val autoEdges = mutableListOf<Edge>()
        val avByCaseStudy = attackVectors.groupBy { it.caseStudyId }

        for ((_, steps) in avByCaseStudy) {
            val sortedSteps = steps.sortedBy { it.step }
            for (j in sortedSteps.indices) {
                val currentStep = sortedSteps[j]

                val targetStepIds = currentStep.leadsToStep
                val targets = if (targetStepIds.isNotEmpty()) {
                    steps.filter { it.stepId in targetStepIds }
                } else if (j + 1 < sortedSteps.size) {
                    listOf(sortedSteps[j + 1])
                } else emptyList()

                for (targetStep in targets) {
                    val sourceTechId = currentStep.targetTechnique
                    val targetTechId = targetStep.targetTechnique

                    val sourceNodes = nodes.filter { it.techniqueId == sourceTechId }
                    val targetNodes = nodes.filter { it.techniqueId == targetTechId }

                    for (u in sourceNodes) {
                        for (v in targetNodes) {
                            if (u.id == v.id) continue

                            val hasRelevantMitigation = mitigations.any {
                                it.targetTechnique == v.techniqueId && it.isRelevant
                            }

                            val state = if (hasRelevantMitigation) EdgeState.Blocked else EdgeState.CaseStudyProven
                            autoEdges.add(
                                Edge(
                                    startNode = u.id,
                                    endNode = v.id,
                                    state = state
                                )
                            )
                        }
                    }
                }
            }
        }

        return autoEdges.distinctBy { it.startNode to it.endNode }.map { edge ->
            if (isEdgeAllowed(edge, firewallRules, nodes)) {
                edge.copy(state = if (edge.state == EdgeState.Blocked) EdgeState.Blocked else EdgeState.Idle)
            } else {
                edge.copy(state = EdgeState.Blocked)
            }
        }
    }

    fun processLlmDecisions(
        decisions: List<DecisionOutput>,
        hosts: List<Host>,
        existingEdges: List<Edge>,
        nodes: List<Node>
    ): List<Edge> {
        val llmEdges = decisions.flatMapTo(mutableSetOf()) { decision ->
            val startHosts = hosts.filter { it.techniquesIds.contains(decision.sourceId) }
            val endHosts = hosts.filter { it.techniquesIds.contains(decision.targetId) }
            
            val edges = mutableListOf<Edge>()
            startHosts.forEach { start ->
                endHosts.forEach { end ->
                    edges.add(Edge(
                        startNode = "${start.id}_${decision.sourceId}",
                        endNode = "${end.id}_${decision.targetId}",
                        llmConfidence = decision.confidence
                    ))
                }
            }
            edges
        }
        
        return (existingEdges + llmEdges).distinctBy { it.startNode to it.endNode }
    }
}
