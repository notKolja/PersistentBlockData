package gg.kpjm.persistentBlockData.nbt

import de.tr7zw.changeme.nbtapi.NBTChunk
import de.tr7zw.changeme.nbtapi.NBTCompound
import de.tr7zw.changeme.nbtapi.NBTContainer
import org.bukkit.block.Block

/**
 * A custom NBT wrapper for non-tile entity [Block]s.
 * Since these blocks don't have their own NBT storage, this class stores data
 * in the chunk's PersistentDataContainer, organized by block coordinates.
 *
 * @param block The block to which this NBT data is attached.
 */
class NBTCustomBlock(private val block: Block) : NBTContainer(), NBTCustom {
    private val blockTag = "${block.x}_${block.y}_${block.z}"
    private val chunkData = NBTChunk(block.chunk).persistentDataContainer

    init {
        val blocksInChunk = chunkData.getOrCreateCompound("blocks")
        if (block.type.isAir) {
            blocksInChunk.removeKey(blockTag)
            if (blocksInChunk.keys.isEmpty()) {
                chunkData.removeKey("blocks")
            }
        } else {
            blocksInChunk.getCompound(blockTag)?.let(::mergeCompound)
        }
    }

    /**
     * Saves the current NBT data back to the chunk's PersistentDataContainer.
     * If the block is now air or the NBT data is empty, it cleans up the stored data.
     */
    override fun saveCompound() {
        if (block.type.isAir) {
            clearNBT()
        }

        val blocksInChunk = chunkData.getOrCreateCompound("blocks")
        if (keys.isEmpty()) {
            blocksInChunk.removeKey(blockTag)
            if (blocksInChunk.keys.isEmpty()) {
                chunkData.removeKey("blocks")
            }
        } else {
            blocksInChunk.getOrCreateCompound(blockTag).apply {
                clearNBT()
                mergeCompound(this@NBTCustomBlock)
            }
        }
    }

    /**
     * Returns a string representation of the NBT data, including the block's ID and coordinates.
     */
    override fun toString(): String {
        return NBTContainer().apply {
            mergeCompound(this@NBTCustomBlock)
            setString("id", block.type.key.toString())
            setInteger("x", block.x)
            setInteger("y", block.y)
            setInteger("z", block.z)
        }.toString()
    }

    /**
     * Deletes the custom "custom" NBT compound from this block's data.
     */
    override fun deleteCustomNBT() {
        removeKey("custom")
    }

    /**
     * Creates and returns a deep copy of this NBT compound.
     */
    override val copy: NBTCompound
        get() = NBTContainer().apply { mergeCompound(this@NBTCustomBlock) }

    /**
     * Gets the dedicated compound for custom data, creating it if it doesn't exist.
     */
    override val customNBT: NBTCompound
        get() = getOrCreateCompound("custom")

    companion object {
        /**
         * Efficiently checks if a block has any custom NBT data stored in the chunk.
         * This avoids creating a full [NBTCustomBlock] instance if no data exists.
         */
        fun hasCustomNBT(block: Block): Boolean {
            val chunkData = NBTChunk(block.chunk).persistentDataContainer
            if (!chunkData.hasTag("blocks")) return false

            val blocksInChunk = chunkData.getCompound("blocks") ?: return false
            val blockTag = "${block.x}_${block.y}_${block.z}"
            return blocksInChunk.hasTag(blockTag)
        }
    }
}