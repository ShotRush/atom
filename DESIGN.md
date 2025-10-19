# Atom - Design Document

## Overview
Atom is a Minecraft plugin that implements a dynamic skill progression system based on hierarchical XP trees. The system tracks player actions across various tasks (mining, farming, building, combat, etc.) and provides benefits/penalties based on skill levels.

---

## Core Concepts

### 1. Hierarchical XP Tree System
Players earn XP in specific tasks, which are organized in a tree structure where parent nodes represent broader skill categories and child nodes represent specific actions.

#### Key Principles:
- **Parent XP Calculation**: Parent nodes accumulate XP as the sum (or function) of their children's XP
- **Effective XP Formula**: `Effective_XP(child) = max(parent_XP / 2, child_XP)`
  - This allows sibling skills to boost each other
  - Can be applied recursively up the tree
  - Ensures 100% effectiveness requires 100% intrinsic XP (cannot be achieved purely through relations)

#### Honorary XP System:
- Parent nodes impart "honorary XP" to children equal to **50% of remaining capacity**
- Formula: `Honorary_XP = (1 - Intrinsic_XP_Percent) × (Parent_Effective_XP_Percent × 0.5)`
- Example: If a child has 50% intrinsic XP and parent has 50% effective XP:
  - Child receives: `(1 - 0.5) × (0.5 × 0.5) = 0.125 = 12.5%` additional honorary XP
  - Total effective XP: 62.5%

---

## XP Tree Structure

### Example Tree (Single Hierarchy)

```
Guard (7 XP total - sum of children)
├── Killing Spiders (5 XP earned)
├── Killing Zombies (2 XP earned, treated as 3.5 XP since 7/2 > 2)
└── Killing Skeletons (0 XP, treated as 3.5 XP since 7/2 > 0)

Mining Tasks
├── Stone Mining
├── Andesite Mining
└── Ore Mining
    ├── Copper
    ├── Iron
    └── [Other Ores]

Farming Tasks
├── Block Events
│   ├── Till Soil
│   └── Use Composter
├── Animal Husbandry
│   ├── Breed Cow
│   ├── Breed Sheep
│   └── [Other Animals]
└── Crop Farming
    ├── Carrots
    │   ├── Plant
    │   └── Harvest
    ├── Wheat
    │   ├── Plant Seeds
    │   └── Harvest
    └── [Other Crops]

Building Tasks
├── Basic
│   ├── Place Dirt
│   └── Place Wood & Variants
└── Advanced
    └── Stone Bricks
```

**Note**: The tree structure is partially static (fundamental relationships like "planting crops → farmer") and partially dynamic (for ML-based classification).

---

## Multi-Tree Model

### Concept
Instead of a single hierarchy, multiple trees can represent different ways to group skills:
- **Tree 1**: Group by tool type (Axes, Shovels, Pickaxes)
- **Tree 2**: Group by material (Wood Tools, Stone Tools, Iron Tools, Diamond Tools)

### Benefits:
- Respects multiple intuitive relationships
- Weighted clustering in a digestible form
- Easier to configure and add new relationships
- Encourages hyperspecialization while providing limited proficiency in related skills

### Example: Tool Crafting Trees

#### Tree 1: By Tool Type
```
Crafting Tools
├── Axes
│   ├── Wooden Axes
│   ├── Stone Axes
│   ├── Iron Axes
│   └── Diamond Axes (100% filled)
├── Pickaxes
│   └── [All pickaxe types get honorary XP from parent]
└── Shovels
    └── [Each shovel type gets 25% honorary XP]
```

#### Tree 2: By Material
```
Crafting Tools
├── Diamond Tools
│   ├── Diamond Axes (100% filled)
│   ├── Diamond Pickaxes (gets honorary XP)
│   └── Diamond Shovels (gets honorary XP)
├── Iron Tools
└── Stone Tools
```

### Aggregation Strategy:
Average the influence of each tree configuration on child nodes to achieve weighted clustering.

---

## Skill Progression Mechanics

### Novice Penalties (Beginners)
Players start with penalties in all categories until they gain XP:

#### Mining:
- **Mining Fatigue** effect applied

#### Farming:
- **Decreased drop rate** for crops
- **Hoes break significantly faster** (novice doesn't know how to use tools properly)

#### Other Categories:
- TBD based on category

### Progression Benefits
As players gain XP in specific tasks:
- Penalties are reduced/removed
- Bonuses are applied (e.g., increased drop rates, tool durability)
- Related skills receive honorary XP benefits

**Example**: Gaining XP in "Carrot Farming" increases:
1. Carrot drop rate (direct benefit)
2. General "Farming Tasks" XP (parent node)
3. Drop rates for other crops via honorary XP (sibling benefit)

---

## Feature Ideas

### 1. Tool Reinforcement
**Description**: Reinforce stone tools with iron to increase durability before obtaining an anvil.

**Implementation Notes**:
- Alternative progression path for early game
- Bridges gap between stone and iron tool tiers
- Requires both stone tool and iron ingots

---

### 2. Sound and Message on Failed Craft
**Description**: Provide immediate audio and visual feedback when a craft fails.

**Rationale**: 
- Positive reinforcement improves UX
- Players know immediately if something worked
- Reduces confusion and frustration

**Implementation Notes**:
- Play negative sound effect (e.g., `ENTITY_VILLAGER_NO`)
- Display actionbar/chat message explaining why craft failed
- Consider cooldown to prevent spam

---

### 3. Farmer Milestone System
**Description**: TBD - Progressive unlocks for farming-related features

**Implementation Notes**:
- Define specific milestones (XP thresholds)
- Unlock new crops, breeding options, or farming techniques
- Visual indicators in achievements menu

---

### 4. Tiered Builder Support System
**Description**: Simple building materials are affected by gravity beyond a certain distance (like scaffolding).

**Implementation Notes**:
- Applies to basic blocks (dirt, wood, cobblestone)
- Encourages proper support structures
- Distance threshold based on builder XP level
- Higher builder XP = can build further without support

---

### 5. XP Removal/Transfer System
**Description**: Allow players to extract XP into consumable items.

**Options**:
1. **Books**: Store XP in written books
2. **Bottles**: XP bottles (like vanilla but for Atom XP)
3. **GUI**: Interface to manage XP extraction/storage
4. **Commands**: `/atom xp extract <amount> <skill>`

**Implementation Notes**:
- Prevents total XP loss on death (if configured)
- Allows XP trading between players
- Could have efficiency loss (e.g., 80% extraction rate)

---

## Dynamic Classification System

### Machine Learning Approach
Use **k-means clustering** to dynamically classify player roles based on XP distribution.

#### Process:
1. **Vector Representation**: Each player is a vector of XP values across all tasks
   - Example: `[mining_xp, farming_xp, building_xp, combat_xp, ...]`

2. **Clustering**: Apply k-means to group similar players
   - Determine optimal k (number of classes) via elbow method or silhouette analysis

3. **Labeling**: Assign meaningful labels to each cluster
   - "Miner", "Farmer", "Builder", "Warrior", "Generalist", etc.
   - Based on which XP categories are highest in cluster centroid

4. **Dynamic Updates**: Re-cluster periodically as players gain XP
   - Could be done on server restart or scheduled task
   - Players can transition between classes naturally

#### Benefits:
- No rigid class system - players emerge into roles organically
- Adapts to how players actually play
- Can identify hybrid roles (e.g., "Combat Miner", "Farming Builder")

#### Challenges:
- Requires sufficient player data for meaningful clusters
- Label assignment needs careful design
- Computational cost (mitigated by periodic updates)

---

## Visualization: Achievements Menu

### Requirements:
- **NOT a traditional GUI** - use the **Achievements/Advancements menu**
- Display XP tree visually
- Show current XP, effective XP, and honorary XP
- Indicate locked/unlocked nodes
- Show progression paths

### Implementation Strategy:
1. **Custom Advancement Trees**: Create advancement JSON files for each skill tree
2. **Dynamic Updates**: Update advancement progress based on XP
3. **Visual Indicators**:
   - Green = Intrinsic XP earned
   - Yellow = Honorary XP from parents
   - Gray = Locked/No XP
4. **Tooltips**: Show detailed XP breakdown on hover

### Technical Notes:
- Use Bukkit's advancement API
- Generate advancement files dynamically or use templates
- Update player advancement progress via `Player.getAdvancementProgress()`

---

## Implementation Roadmap

### Phase 1: Foundation (CRITICAL - Must be bug-free)
1. **XP Storage System**
   - Database schema (SQLite/MySQL)
   - Player XP data structure
   - Save/load functionality

2. **Tree Structure Definition**
   - Define tree hierarchy (YAML/JSON config)
   - Parent-child relationships
   - XP calculation engine

3. **Effective XP Calculation**
   - Implement recursive honorary XP algorithm
   - Optimize for performance (caching)
   - Unit tests for edge cases

4. **Event Listeners**
   - Track all relevant player actions
   - Award XP appropriately
   - Handle edge cases (creative mode, etc.)

### Phase 2: Core Features
1. **Penalty/Bonus System**
   - Apply novice penalties
   - Scale benefits with XP
   - Balance testing

2. **Multi-Tree Support**
   - Support multiple tree configurations
   - Aggregation logic
   - Config management

3. **Achievements Menu Integration**
   - Generate advancement files
   - Update progress dynamically
   - Visual polish

### Phase 3: Advanced Features
1. **XP Transfer System**
2. **Tool Reinforcement**
3. **Builder Support Mechanics**
4. **Sound/Message Feedback**

### Phase 4: Machine Learning (Optional)
1. **Data Collection Pipeline**
2. **K-means Clustering Implementation**
3. **Role Classification & Labeling**
4. **Dynamic Updates**

---

## Technical Stack

### Required:
- **Bukkit/Spigot API**: Minecraft plugin framework
- **Database**: SQLite (lightweight) or MySQL (scalable)
- **Config**: YAML for tree definitions and settings

### Optional (for ML):
- **Apache Commons Math**: K-means clustering in Java
- **Weka**: Machine learning library for Java
- **External Service**: Python microservice for advanced ML (overkill for this)

---

## Configuration Structure

### Example: `trees.yml`
```yaml
trees:
  - name: "tool_type"
    weight: 0.5
    root: "Crafting Tools"
    nodes:
      "Crafting Tools":
        children: ["Axes", "Pickaxes", "Shovels"]
      "Axes":
        children: ["Wooden Axes", "Stone Axes", "Iron Axes", "Diamond Axes"]
      # ... more nodes

  - name: "tool_material"
    weight: 0.5
    root: "Crafting Tools"
    nodes:
      "Crafting Tools":
        children: ["Diamond Tools", "Iron Tools", "Stone Tools", "Wooden Tools"]
      "Diamond Tools":
        children: ["Diamond Axes", "Diamond Pickaxes", "Diamond Shovels"]
      # ... more nodes
```

### Example: `skills.yml`
```yaml
skills:
  "Diamond Axes":
    xp_per_craft: 10
    max_xp: 1000
    events:
      - type: CRAFT_ITEM
        material: DIAMOND_AXE
    
  "Carrot Farming":
    xp_per_harvest: 2
    max_xp: 5000
    events:
      - type: BLOCK_BREAK
        material: CARROTS
        condition: "is_mature"
```

---

## Design Philosophy

### Goals:
1. **No Rigid Classes**: Players aren't boxed into predefined roles
2. **Semantic Grouping**: Actions grouped by functional/semantic similarity
3. **Natural Progression**: Players will have XP across multiple subtrees
4. **Encourage Specialization**: Honorary XP provides limited proficiency, but mastery requires focus
5. **Ease Limitations**: Beginners aren't completely blocked, just penalized
6. **Intuitive Relationships**: Multiple trees respect different logical groupings

### Principles:
- **Foundation First**: Build a rock-solid, bug-free base before adding features
- **Performance**: Optimize XP calculations (caching, async operations)
- **Configurability**: Server admins can customize trees, XP rates, penalties
- **Extensibility**: Easy to add new skills, trees, and features
- **Balance**: Playtest extensively to ensure fair progression

---

## Notes & Considerations

### Static vs Dynamic Tree Structure:
- **Static Portions**: Fundamental relationships (e.g., "planting crops" → "farmer")
- **Dynamic Portions**: ML-based classification of player roles
- Hybrid approach: Static tree for XP calculation, dynamic classification for role labels

### Balancing Honorary XP:
- 50% of remaining capacity prevents over-reliance on parent XP
- Requires intrinsic XP for full effectiveness
- Encourages trying related skills while rewarding specialization

### Database Performance:
- Cache frequently accessed XP values in memory
- Batch database writes (save every N minutes or on logout)
- Index player UUID and skill name columns

### Edge Cases to Handle:
- Creative mode (should XP be awarded?)
- World-specific XP (separate XP per world?)
- XP decay over time (optional feature)
- Maximum XP caps per skill
- Prestige/reset system (optional)

---

## Resources & Research

### Use the Web:
- Research existing skill plugins (McMMO, SkillAPI, Heroes)
- Study k-means clustering implementations in Java
- Look into Minecraft advancement JSON structure
- Find balanced XP curves (exponential vs linear)

### Libraries to Explore:
- **Apache Commons Math**: For clustering algorithms
- **HikariCP**: For database connection pooling
- **Caffeine**: For high-performance caching
- **Adventure API**: For modern text components and messages

---

## Questions to Resolve

1. **XP Curve**: Linear, exponential, or logarithmic progression?
2. **Max XP**: Should skills have caps? If so, what values?
3. **Penalty Severity**: How harsh should novice penalties be?
4. **Tree Depth**: Maximum depth of tree hierarchy?
5. **ML Frequency**: How often to re-cluster for role classification?
6. **Cross-World**: Should XP be shared across worlds or separate?
7. **Death Penalty**: Should players lose XP on death?
8. **Party System**: Should nearby players share XP?

---

## Current Implementation Status

**Status**: Early development - foundation phase

**Completed**:
- Basic plugin structure
- Gradle build configuration

**In Progress**:
- Design documentation (this file)

**Next Steps**:
1. Define initial tree structure in YAML
2. Implement XP storage (database schema)
3. Create XP calculation engine
4. Add event listeners for XP tracking

---

*Last Updated: 2025-10-19*
