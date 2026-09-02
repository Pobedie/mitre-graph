# Refactor `switchToAttackVectorBuildingStage` in `ViewModel.kt`

The goal is to refactor the `switchToAttackVectorBuildingStage` function in `ViewModel.kt` to improve readability and maintainability by dividing it into smaller, more focused functions.

## Proposed Changes

### [shared component](file:///home/oleg/Programming/Uni/AttackGraph/shared/src/commonMain/kotlin/com/pobedie/attackgraph/ui/ViewModel.kt)

#### [MODIFY] [ViewModel.kt](file:///home/oleg/Programming/Uni/AttackGraph/shared/src/commonMain/kotlin/com/pobedie/attackgraph/ui/ViewModel.kt)

I will refactor `switchToAttackVectorBuildingStage` and extract the following logic into private helper functions:

1.  **Node Generation**: `generateNodes(state: ViewState): List<Node>`
    *   Extracts the logic that maps hosts and techniques to `Node` objects.
2.  **Auto-Edge Building**: `buildEdgesFromAttackVectors(attackVectors: List<AttackVector>, nodes: List<Node>, mitigations: List<Mitigation>): List<Edge>`
    *   Extracts the nested loops that create edges based on attack vector case studies and mitigations.
3.  **Edge Merging and Processing**: `processFinalEdges(nodes: List<Node>, autoEdges: List<Edge>, currentState: ViewState): List<Edge>`
    *   Extracts the logic that merges existing edges with new auto-edges, applies firewall rules, and calculates probabilities.

The high-level flow of `switchToAttackVectorBuildingStage` will remain the same but will call these new functions, making it much easier to follow.

## Verification Plan

### Automated Tests
- I will verify that the project still builds successfully.
- If there are existing unit tests for `ViewModel`, I will run them.

### Manual Verification
- The user can verify that the "Attack Vector Building Stage" still functions as expected (nodes and edges are correctly generated when switching to this stage).
