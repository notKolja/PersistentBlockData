package gg.kpjm.persistentBlockData.listener

import gg.kpjm.persistentBlockData.nbt.NBTCustomBlock
import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.block.PistonMoveReaction
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import java.util.*
import java.util.function.Consumer


class PistonMovementListener: Listener {

    init {
        Bukkit.getLogger().info { "PistonMovementListener was registered!" }
    }

    private fun getData(block: Block): NBTCustomBlock {
        return NBTCustomBlock(block)
    }

    private fun removeData(block: Block) {
        getData(block).clearNBT()
    }

    private fun removeDataFromList(blockList: List<Block>) {
        blockList.forEach {
            getData(it).clearNBT()
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        removeData(event.block)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntity(event: EntityChangeBlockEvent) {
        if (event.to != event.block.type) {
            removeData(event.block)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onExplode(event: BlockExplodeEvent) {
        removeDataFromList(event.blockList())
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onExplode(event: EntityExplodeEvent) {
        removeDataFromList(event.blockList())
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBurn(event: BlockBurnEvent) {
        removeData(event.block)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPiston(event: BlockPistonExtendEvent) {
        onPiston(event.blocks, event)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPiston(event: BlockPistonRetractEvent) {
        onPiston(event.blocks, event)
    }

    private fun onPiston(blocks: MutableList<Block?>, bukkitEvent: BlockPistonEvent) {
        val dataToMove: MutableMap<Block, NBTCustomBlock> = LinkedHashMap()
        val direction = bukkitEvent.direction

        blocks.forEach { block ->
            if (block == null) return@forEach

            val reaction = block.pistonMoveReaction
            if (reaction == PistonMoveReaction.BREAK) {
                return@forEach
            }

            val nbt = getData(block)
            if (NBTCustomBlock.hasCustomNBT(block)) {
                val destinationBlock = block.getRelative(direction)
                dataToMove[destinationBlock] = nbt.copy()
            }
        }

        blocks.forEach { block ->
            if (block != null) {
                removeData(block)
            }
        }

        reverse(dataToMove).forEach { (destinationBlock, nbt) ->
            nbt.copyTo(destinationBlock)
        }
    }


    private fun <K, V> reverse(map: MutableMap<K, V>): MutableMap<K, V> {
        val reversed = LinkedHashMap<K, V>()
        val keys: MutableList<K> = ArrayList<K>(map.keys)
        keys.reverse()
        keys.forEach(Consumer { key: K -> reversed[key] = map[key]!! })
        return reversed
    }
}