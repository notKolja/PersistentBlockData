# PersistentBlockData — Plugin Documentation

**Package:** `gg.kpjm.persistentBlockData`

---

## Table of Contents

1. [Overview](#1-overview)
2. [Architecture](#2-architecture)
3. [Class Reference](#3-class-reference)
   - 3.1 [PersistentBlockData (Main Plugin)](#31-persistentblockdata-main-plugin)
   - 3.2 [NBTCustom (Interface)](#32-nbtcustom-interface)
   - 3.3 [NBTCustomBlock](#33-nbtcustomblock)
   - 3.4 [BlockDataListener](#34-blockdatalistener)
4. [Data Storage Model](#4-data-storage-model)
5. [Piston Movement Logic](#5-piston-movement-logic)
6. [Event Handling & Cleanup](#6-event-handling--cleanup)
7. [Usage Examples](#7-usage-examples)
8. [Migration Notes](#8-migration-notes)

---

## 1. Overview

**PersistentBlockData** is a Bukkit plugin that attaches custom NBT data to any block in the world — including blocks that are not tile entities. Vanilla Minecraft only persists NBT for tile entities (e.g., chests, signs, command blocks). This plugin extends that capability to *all* block types by storing the data inside the owning chunk's `PersistentDataContainer`.

Key features:

- Attach arbitrary NBT compounds to any block, regardless of type.
- Data survives chunk saves, reloads, and server restarts.
- Automatic cleanup on block removal (break, explosion, burn, entity change).
- Full NBT transfer when blocks are moved by pistons.

---

## 2. Architecture

```
PersistentBlockData (JavaPlugin)
│
├── registers ──► BlockDataListener
│                   └── listens to: BlockBreak, BlockPiston(Extend/Retract),
│                                   BlockExplode, EntityExplode,
│                                   BlockBurn, EntityChangeBlock, StructureGrowEvent
│
└── uses ──► NBTCustomBlock
               └── implements ──► NBTCustom (interface)
                                   └── defines the contract for custom-NBT access
```

### Data Flow

```
[Block in World]
      │
      ▼
NBTCustomBlock (reads/writes)
      │
      ▼
Chunk PersistentDataContainer
      │  └── "blocks"  (NBTCompound)
      │        └── "x_y_z"  (NBTCompound — one per block)
      │              └── "custom"  (NBTCompound — user data lives here)
      │
      ▼
[Persisted to Region File (.mca)]
```

---

## 3. Class Reference

### 3.1 `PersistentBlockData` — Main Plugin

**File:** `PersistentBlockData.kt`  
**Extends:** `JavaPlugin`

The entry point of the plugin. Responsible for initialization and providing a static plugin instance to all other classes.

| Member | Description                                                                                                                    |
|---|--------------------------------------------------------------------------------------------------------------------------------|
| `instance` | Static singleton reference to the active plugin instance. Read-only from outside the class. Set automatically on `onEnable()`. |
| `onEnable()` | Registers the `BlockDataListener` and logs activation.                                                                         |
| `onDisable()` | Logs deactivation.                                                                                                             |

---

### 3.2 `NBTCustom` — Interface

**File:** `NBTCustom.kt`  
**Package:** `gg.kpjm.persistentBlockData.nbt`

Defines the contract that any NBT-holding wrapper must fulfill. This decouples the storage logic from the rest of the codebase and allows for future implementations (e.g., for tile entities or entities).

| Member | Type | Description |
|---|---|---|
| `customNBT` | `NBTCompound` (property) | Returns the dedicated `"custom"` sub-compound. Create it if absent. This is the single entry point for all user-written data. |
| `copy` | `NBTCompound` (property) | Returns a deep copy of the full NBT tree of the object. Useful for snapshots before destructive operations. |
| `deleteCustomNBT()` | `fun` | Removes the `"custom"` compound entirely. Does *not* remove other top-level keys. |
| `saveCompound()` | `fun` | Persists in-memory changes back to the underlying storage (e.g., the chunk). Must be called explicitly after modifications. |

#### Companion Object Constants

| Constant | Value | Description |
|---|---|---|
| `KEY` | `"persistentblockdata-custom"` | The current key used internally to namespace data. |
| `OLD_KEY` | `NamespacedKey(instance, "custom-nbt")` | Deprecated legacy key. Kept for potential migration logic. |

---

### 3.3 `NBTCustomBlock`

**File:** `NBTCustomBlock.kt`  
**Package:** `gg.kpjm.persistentBlockData.nbt`  
**Extends:** `NBTContainer`  
**Implements:** `NBTCustom`

The core class of the plugin. It wraps a non-tile-entity `Block` and provides read/write access to NBT data stored in the chunk's `PersistentDataContainer`.

#### Constructor Behavior

When an `NBTCustomBlock` is instantiated, the following happens immediately:

1. The chunk's `PersistentDataContainer` is accessed.
2. The compound at key `"blocks"` is located (or created).
3. If the target block **is air**, any existing data at that block's coordinate key is *deleted* and the compound is cleaned up if empty.
4. If the target block **is not air**, existing data is loaded into memory via `mergeCompound`.

> **Important:** Construction alone does not persist anything. You must call `saveCompound()` after any writes.

#### Instance Methods & Properties

| Member | Description |
|---|---|
| `saveCompound()` | Writes the in-memory NBT back to the chunk. Cleans up empty structures automatically. If the block has become air since construction, all data is cleared. |
| `deleteCustomNBT()` | Removes the `"custom"` key from this block's NBT. |
| `customNBT` | Returns (or creates) the `"custom"` sub-compound where user data should live. |
| `copy` | Returns a deep copy of the entire NBT compound for this block. |
| `copy()` | Returns a new `NBTCustomBlock` instance for the same block (re-reads from chunk). |
| `copyTo(targetBlock)` | Copies all NBT data from this block to a `targetBlock`. Clears the target first, then merges and saves. Used by the piston listener to relocate data. |
| `toString()` | Debug representation. Returns a compound containing the block's `id`, `x`, `y`, `z`, and all stored NBT data. |

#### Static Methods (Companion Object)

| Method | Description |
|---|---|
| `hasCustomNBT(block: Block): Boolean` | Efficiently checks whether a block has *any* stored NBT data without constructing a full `NBTCustomBlock`. Useful as a fast pre-check before heavier operations. |

#### Storage Key Format

Data for a block at coordinates `(x, y, z)` is stored under the key:

```
"blocks" → "${x}_${y}_${z}"
```

Example for block at (128, 64, -300):

```
blocks → "128_64_-300" → { custom → { ... } }
```

---

### 3.4 `BlockDataListener`

**File:** `BlockDataListener.kt`  
**Package:** `gg.kpjm.persistentBlockData.listener`  
**Implements:** `Listener`

Listens to all relevant block lifecycle events and ensures that custom NBT data is correctly maintained — cleaned up on destruction and transferred on piston movement.

#### Registered Events

| Event                     | Priority | Purpose                                                                   |
|---------------------------|----------|---------------------------------------------------------------------------|
| `BlockBreakEvent`         | MONITOR  | Deletes NBT when a block is broken by a player.                           |
| `EntityChangeBlockEvent`  | MONITOR  | Deletes NBT when an entity changes the block type (e.g., frosting water). |
| `BlockExplodeEvent`       | MONITOR  | Deletes NBT for all blocks destroyed in a block explosion.                |
| `EntityExplodeEvent`      | MONITOR  | Deletes NBT for all blocks destroyed in an entity explosion.              |
| `BlockBurnEvent`          | MONITOR  | Deletes NBT when a block burns (e.g., fire spread).                       |
| `BlockPistonExtendEvent`  | HIGHEST  | Handles NBT transfer for extending pistons.                               |
| `BlockPistonRetractEvent` | HIGHEST  | Handles NBT transfer for retracting pistons.                              |
| `StructureGrowEvent`      | MONITOR  | Handles NBT transfer for Trees Growing.                                   |


> All cleanup events use `MONITOR` priority with `ignoreCancelled = true`, meaning they only act after the event has been confirmed as not cancelled by other plugins.  
> Piston events use `HIGHEST` priority to capture block state as early as possible, before other plugins can interfere.

---

## 4. Data Storage Model

Custom data is stored inside each chunk's `PersistentDataContainer`, which is automatically saved and loaded by Minecraft with the chunk data.

### Chunk-Level Structure

```
PersistentDataContainer (NBTChunk)
 └── "blocks"                        ← top-level compound, created on demand
       ├── "12_64_300"               ← block at x=12, y=64, z=300
       │     └── "custom"            ← user-facing data compound
       │           ├── "myTag"       ← example user tag
       │           └── ...
       ├── "14_64_300"               ← another block in the same chunk
       │     └── "custom"
       │           └── ...
       └── ...
```

### Cleanup Rules

The plugin aggressively cleans up empty structures to avoid data bloat:

- If a block's NBT becomes empty after a write, the block's coordinate key is removed from `"blocks"`.
- If `"blocks"` itself becomes empty after that removal, it is removed from the chunk's `PersistentDataContainer`.

This means a chunk with no custom block data leaves zero footprint.

---

## 5. Piston Movement Logic

Piston handling is the most complex part of the plugin. The challenge is that Bukkit fires the piston event *before* the blocks actually move, but the NBT must end up at the *new* positions after movement.

### Execution Order

The `onPiston` method executes in three distinct phases:

**Phase 1 — Snapshot (before movement)**
Iterates over all blocks in the piston's push list. For each block:
- Skips blocks with `PistonMoveReaction.BREAK` (they will be destroyed, not moved).
- Checks `hasCustomNBT()` as a fast pre-filter.
- If data exists, reads the full NBT and stores it in a map keyed by the *destination* block (current position + piston direction).

**Phase 2 — Clear (before movement)**
Iterates over all blocks again and clears their NBT unconditionally. This must happen before the blocks move so that the old coordinate keys are correctly targeted.

**Phase 3 — Write (1 tick after movement)**
A scheduled task runs on the next server tick (after Bukkit has physically moved the blocks). It iterates over the snapshot map in *reverse* order and writes each NBT compound to its destination block via `copyTo()`.

> **Why reverse order?** When a piston pushes a line of blocks, the block farthest from the piston moves first. Writing in reverse (farthest first) ensures that data is never overwritten by a subsequent write to the same position.

### Diagram

```
Tick 0 (event fires, blocks have NOT moved yet):
  ┌─────┐   ┌─────┐   ┌─────┐   ┌─────┐
  │  A  │──►│  B  │──►│  C  │──►│  .  │   (► = push direction)
  └─────┘   └─────┘   └─────┘   └─────┘
  Phase 1: Snapshot A→destA, B→destB, C→destC
  Phase 2: Clear A, B, C

Tick 1 (blocks have moved):
  ┌─────┐   ┌─────┐   ┌─────┐   ┌─────┐
  │piston│  │  A  │   │  B  │   │  C  │
  └─────┘   └─────┘   └─────┘   └─────┘
  Phase 3: Write C→destC, B→destB, A→destA  (reverse order)
```

---

## 6. Event Handling & Cleanup

All block-destruction events are handled uniformly: any custom NBT associated with the affected block(s) is deleted. The plugin does **not** attempt to drop or transfer data on destruction — the data is simply removed.

| Scenario | Event(s) Handled | Behavior |
|---|---|---|
| Player breaks block | `BlockBreakEvent` | NBT deleted |
| Block destroyed by explosion | `BlockExplodeEvent`, `EntityExplodeEvent` | NBT deleted for all blocks in the explosion list |
| Block burns (fire) | `BlockBurnEvent` | NBT deleted |
| Entity changes block | `EntityChangeBlockEvent` | NBT deleted if the block type changed |
| Piston pushes block | `BlockPistonExtendEvent`, `BlockPistonRetractEvent` | NBT transferred to new position |

---

## 7. Usage Examples

### Reading Custom Data from a Block

```kotlin

fun readData(block: Block) {
    // Fast pre-check — avoids constructing NBTCustomBlock unnecessarily
    if (!NBTCustomBlock.hasCustomNBT(block)) {
        println("No custom data on this block.")
        return
    }

    val nbt = NBTCustomBlock(block)
    val custom = nbt.customNBT  // the "custom" sub-compound

    val value = custom.getString("myKey")
    println("Value: $value")
}
```

### Writing Custom Data to a Block

```kotlin
fun writeData(block: Block, key: String, value: String) {
    val nbt = NBTCustomBlock(block)
    nbt.customNBT.setString(key, value)
    nbt.saveCompound()  // MUST call save after writing
}
```

### Deleting Custom Data

```kotlin
fun clearData(block: Block) {
    val nbt = NBTCustomBlock(block)
    nbt.deleteCustomNBT()   // removes only the "custom" compound
    nbt.saveCompound()      // persist the change
}
```

### Copying Data Between Blocks

```kotlin
fun copyData(source: Block, target: Block) {
    val nbt = NBTCustomBlock(source)
    nbt.copyTo(target)  // clears target, copies all data, and saves automatically
}
```

---

## 8. Migration Notes

### Legacy Key (`OLD_KEY`)

The interface `NBTCustom` defines an `OLD_KEY` (`NamespacedKey` with value `"custom-nbt"`). This suggests a previous storage format that used Bukkit's `NamespacedKey`-based `PersistentDataContainer` API directly. The current implementation uses plain string keys inside an `NBTCompound` instead.

If you are upgrading from an older version of this plugin that used `OLD_KEY`, a manual or scripted migration of existing block data to the new structure (`"blocks" → "x_y_z" → "custom"`) will be required. No automatic migration logic is currently present in the codebase.

### Dependency

This plugin relies on the **NBTAPI** library (`de.tr7zw.changeme.nbtapi`). Ensure the correct version is included in your server's `lib/` folder or bundled with the plugin JAR, and that it is compatible with your Minecraft/Bukkit version.
