# Atom Foundation - Technical Documentation

## Architecture Overview

The Atom plugin foundation is built with enterprise-grade architecture principles, optimized for Folia's regionized multithreading model.

### Core Components

#### 1. Data Models (`org.shotrush.atom.model`)

**SkillNode**
- Immutable tree node representing a skill in the hierarchy
- Builder pattern for safe construction
- Supports parent-child relationships with bidirectional navigation
- Thread-safe with concurrent collections for children
- Methods: `ancestors()`, `descendants()`, `depth()`, `isRoot()`, `isLeaf()`

**PlayerSkillData**
- Thread-safe container for player XP data
- Dirty flag tracking for efficient database writes
- Concurrent hash map for skill storage
- Automatic timestamp tracking on modifications
- Validation: prevents negative XP values

**EffectiveXp**
- Immutable record representing calculated XP values
- Separates intrinsic XP (earned) from honorary XP (inherited)
- Progress percentage calculation with bounds checking
- Factory methods for common cases (`zero()`, `of()`)

#### 2. Tree Structure (`org.shotrush.atom.tree`)

**SkillTree**
- Represents a single skill hierarchy
- Weighted for multi-tree aggregation
- Indexed node lookup (O(1) access by skill ID)
- Automatic indexing on construction
- Immutable after construction

**SkillTreeRegistry**
- Central registry for all skill trees
- Thread-safe concurrent collections
- Reverse index: skill ID → tree names
- Supports multiple trees containing same skill IDs
- Efficient lookup and traversal operations

#### 3. XP Calculation Engine (`org.shotrush.atom.engine`)

**XpCalculator**
- Implements the honorary XP algorithm
- Caffeine cache for performance (10,000 entries, 5-minute TTL)
- Cache key includes timestamp for automatic invalidation
- Recursive calculation with memoization
- Formula: `honorary_xp = remaining_capacity × parent_progress × 0.5`

**XpEngine**
- High-level API for XP operations
- Integrates calculator with tree registry
- Automatic XP capping at node max values
- Cache invalidation on XP changes
- Batch operations for efficiency

#### 4. Storage Layer (`org.shotrush.atom.storage`)

**StorageProvider Interface**
- Async-first design with CompletableFuture
- CRUD operations for player data
- Lifecycle management (initialize/shutdown)

**SQLiteStorage**
- HikariCP connection pooling (2 connections)
- Dedicated executor service for async operations
- Transactional batch writes
- Foreign key constraints with cascade delete
- Prepared statement caching
- Indexes on frequently queried columns

**Database Schema**
```sql
atom_players (
    player_id TEXT PRIMARY KEY,
    last_modified INTEGER NOT NULL
)

atom_skills (
    player_id TEXT NOT NULL,
    skill_id TEXT NOT NULL,
    intrinsic_xp INTEGER NOT NULL,
    PRIMARY KEY (player_id, skill_id),
    FOREIGN KEY (player_id) REFERENCES atom_players(player_id) ON DELETE CASCADE
)
```

#### 5. Player Data Management (`org.shotrush.atom.manager`)

**PlayerDataManager**
- In-memory cache for active players
- Lazy loading on player join
- Dirty tracking for efficient saves
- Batch save operations
- Automatic cleanup on player quit

#### 6. Configuration (`org.shotrush.atom.config`)

**TreeDefinition**
- Record-based configuration model
- Nested node definitions
- Supports arbitrary tree depth

**TreeBuilder**
- Converts definitions to runtime tree structures
- Recursive node construction
- Parent-child relationship linking

**DefaultTrees**
- Seven main skill clusters:
  1. **Farmer**: Crop farming, animal husbandry, land management
  2. **Guardsman**: Combat, defense
  3. **Miner**: Stone mining, ore mining
  4. **Healer**: Brewing, support, regeneration
  5. **Blacksmith**: Tool crafting, armor crafting
  6. **Builder**: Basic building, advanced building
  7. **Librarian**: Enchanting, knowledge

#### 7. Event Tracking (`org.shotrush.atom.listener`)

**PlayerConnectionListener**
- Folia-compatible player join/quit handling
- Uses entity scheduler for player-specific tasks
- Async data loading on join
- Automatic save and unload on quit

**SkillEventListener**
- Comprehensive event tracking for all skill categories
- Events monitored:
  - Block break (mining, crop harvesting)
  - Block place (building, crop planting)
  - Entity death (combat)
  - Entity breeding (animal husbandry)
  - Crafting (tools, armor)
  - Interactions (tilling, composting)
- Mature crop detection for harvest XP
- Material-based skill routing

## Folia Compatibility

### Key Adaptations

1. **No Main Thread Assumptions**
   - All operations are region-aware
   - No `Bukkit.getScheduler()` usage
   - Uses `GlobalRegionScheduler` for periodic tasks
   - Uses `EntityScheduler` for player-specific operations

2. **Thread-Safe Data Structures**
   - `ConcurrentHashMap` for all shared state
   - Immutable models where possible
   - Atomic operations for state changes

3. **Async-First Design**
   - Database operations return `CompletableFuture`
   - Non-blocking I/O operations
   - Dedicated executor services

4. **Event Handling**
   - Events fire on region owning the entity/location
   - No cross-region data access
   - Cache lookups are thread-safe

### Plugin Metadata
```yaml
folia-supported: true
```

## Performance Optimizations

### Caching Strategy

1. **XP Calculation Cache**
   - 10,000 entry limit
   - 5-minute TTL
   - Timestamp-based invalidation
   - Prevents redundant recursive calculations

2. **Player Data Cache**
   - In-memory for online players
   - Dirty flag prevents unnecessary writes
   - Batch save operations

3. **Tree Node Index**
   - O(1) skill ID lookup
   - Pre-computed on tree construction
   - Reverse index for cross-tree queries

### Database Optimizations

1. **Connection Pooling**
   - HikariCP with 2 connections
   - Prepared statement caching
   - Connection timeout: 5 seconds

2. **Batch Operations**
   - Transactional skill updates
   - Single commit for all player skills
   - Rollback on failure

3. **Indexes**
   - `idx_player_modified` on last_modified
   - `idx_skill_lookup` on (player_id, skill_id)

### Memory Management

1. **Bounded Caches**
   - Maximum sizes configured
   - LRU eviction policies
   - Automatic cleanup

2. **Lazy Loading**
   - Data loaded on player join
   - Unloaded on player quit
   - No persistent memory leaks

## XP Calculation Algorithm

### Honorary XP Formula

For a given skill node:

```
intrinsic_xp = player's earned XP in this skill
parent_effective_xp = calculateEffectiveXp(parent)
parent_progress = parent_effective_xp.totalXp / parent.maxXp

remaining_capacity = max(0, node.maxXp - intrinsic_xp)
honorary_xp = remaining_capacity × parent_progress × 0.5

effective_xp = intrinsic_xp + honorary_xp
progress_percent = min(1.0, effective_xp / node.maxXp)
```

### Key Properties

1. **Recursive**: Honorary XP propagates up the tree
2. **Diminishing**: 50% multiplier prevents over-reliance on parents
3. **Capacity-Based**: Only fills remaining capacity
4. **Capped**: Cannot exceed node's max XP
5. **Requires Intrinsic**: 100% effectiveness needs 100% intrinsic XP

### Example Calculation

```
Player has:
- Diamond Axes: 10,000 XP (100% of max)
- Axes (parent): 10,000 XP (sum of children)
- Tool Crafting (grandparent): 10,000 XP

Stone Axes (0 intrinsic XP):
1. Parent (Axes) has 100% progress
2. Remaining capacity = 10,000 - 0 = 10,000
3. Honorary XP = 10,000 × 1.0 × 0.5 = 5,000
4. Effective XP = 0 + 5,000 = 5,000 (50% progress)
```

## Skill Tree Structure

### Hierarchy Depth

- **Level 0**: Root (all skills)
- **Level 1**: Main clusters (7 categories)
- **Level 2**: Subcategories
- **Level 3**: Specific skills
- **Level 4**: Skill variants (plant/harvest)

### Node Types

- **ROOT**: Top-level node (no parent)
- **BRANCH**: Intermediate node (has children)
- **LEAF**: Terminal node (no children)

### XP Values

- Root: 100,000 XP
- Main clusters: 40,000-50,000 XP
- Subcategories: 15,000-30,000 XP
- Specific skills: 5,000-10,000 XP
- Variants: 5,000 XP

## Event XP Awards

| Event Type | Skill | XP Award |
|------------|-------|----------|
| Mine stone | Stone Mining | 10 |
| Mine ore | Ore Mining | 10 |
| Harvest crop (mature) | Crop Farming | 5 |
| Plant crop | Crop Farming | 3 |
| Place basic block | Basic Building | 2 |
| Place advanced block | Advanced Building | 2 |
| Kill hostile mob | Combat | 15 |
| Breed animal | Animal Husbandry | 20 |
| Craft tool | Tool Crafting | 25 |
| Craft armor | Armor Crafting | 30 |
| Till soil | Land Management | 1 |
| Use composter | Land Management | 2 |

## Error Handling

### Validation

- Null checks on all public API methods
- Negative XP prevention
- Max XP capping
- Progress percent bounds (0.0-1.0)

### Exception Types

- `StorageException`: Database operation failures
- `IllegalArgumentException`: Invalid parameters
- `NullPointerException`: Null required parameters

### Graceful Degradation

- Missing skill IDs: silently ignored
- Cache misses: recomputed on demand
- Database failures: logged and propagated
- Player data not found: creates new data

## Testing Recommendations

### Unit Tests

1. **XpCalculator**
   - Honorary XP calculation accuracy
   - Cache hit/miss behavior
   - Edge cases (0 XP, max XP, no parent)

2. **SkillNode**
   - Tree traversal (ancestors, descendants)
   - Depth calculation
   - Equality and hashing

3. **PlayerSkillData**
   - Concurrent modifications
   - Dirty flag tracking
   - XP addition/setting

### Integration Tests

1. **Storage Layer**
   - Save/load round-trip
   - Transaction rollback
   - Concurrent access

2. **Event Listeners**
   - XP award on events
   - Skill ID routing
   - Edge cases (cancelled events, wrong materials)

3. **Tree Registry**
   - Multi-tree registration
   - Skill lookup across trees
   - Node indexing

### Performance Tests

1. **Cache Efficiency**
   - Hit rate measurement
   - Memory usage under load
   - Eviction behavior

2. **Database Throughput**
   - Batch save performance
   - Connection pool saturation
   - Query execution time

3. **Concurrent Load**
   - Multiple players joining simultaneously
   - Simultaneous XP awards
   - Cache contention

## Future Extensibility

### Designed for Growth

1. **Multi-Tree Support**
   - Already implemented in registry
   - Weighted aggregation ready
   - Easy to add new trees

2. **Custom XP Formulas**
   - Calculator is isolated
   - Can be swapped or extended
   - Formula parameters configurable

3. **Additional Storage Backends**
   - Interface-based design
   - Easy to add MySQL, PostgreSQL, etc.
   - No business logic coupling

4. **Event System Extension**
   - Listener pattern allows additions
   - Skill ID routing is data-driven
   - No hardcoded dependencies

5. **ML Integration Points**
   - Player data export ready
   - XP vectors easily extracted
   - Classification results can be stored

## Maintenance Notes

### Auto-Save

- Runs every 5 minutes via GlobalRegionScheduler
- Only saves dirty player data
- Logs number of players saved

### Shutdown Procedure

1. Save all cached player data
2. Wait for completion (blocking)
3. Close database connections
4. Shutdown executor services
5. 10-second timeout before force shutdown

### Logging

- Info: Initialization, auto-saves, tree registration
- Severe: Startup failures, database errors
- Stack traces: All exceptions

## Dependencies

- **Paper API**: 1.21.8-R0.1-SNAPSHOT
- **HikariCP**: 5.1.0 (connection pooling)
- **SQLite JDBC**: 3.46.1.0 (database)
- **Caffeine**: 3.1.8 (caching)
- **SnakeYAML**: 2.2 (configuration)
- **ACF**: 0.5.1-SNAPSHOT (commands - future use)

All dependencies are shaded and relocated to prevent conflicts.

## Code Quality

### Principles Followed

1. **Immutability**: Models are immutable where possible
2. **Thread Safety**: All shared state is concurrent-safe
3. **Null Safety**: Explicit null checks and Optional usage
4. **Fail Fast**: Validation at boundaries
5. **Single Responsibility**: Each class has one clear purpose
6. **Interface Segregation**: Small, focused interfaces
7. **Dependency Injection**: Constructor-based DI
8. **No Comments**: Self-documenting code with clear names

### Design Patterns

- **Builder**: SkillNode, SkillTree construction
- **Factory**: EffectiveXp creation
- **Registry**: SkillTreeRegistry
- **Strategy**: StorageProvider interface
- **Observer**: Event listeners
- **Cache-Aside**: XpCalculator caching
- **Repository**: PlayerDataManager

## Summary

This foundation provides:

✅ **Robust**: Enterprise-grade error handling and validation  
✅ **Performant**: Multi-level caching and optimized queries  
✅ **Scalable**: Thread-safe, async-first design  
✅ **Folia-Ready**: No main thread assumptions  
✅ **Extensible**: Interface-based, modular architecture  
✅ **Maintainable**: Clean code, clear separation of concerns  
✅ **Future-Proof**: Designed for ML integration and multi-tree systems  

The system is production-ready for the core XP tracking and calculation functionality.
