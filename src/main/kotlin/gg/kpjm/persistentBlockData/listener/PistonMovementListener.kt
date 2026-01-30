package gg.kpjm.persistentBlockData.listener

import gg.kpjm.persistentBlockData.PersistentBlockData
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
import org.bukkit.plugin.Plugin
import java.util.*


class PistonMovementListener(): Listener {

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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPiston(event: BlockPistonExtendEvent) {
        onPiston(event.blocks, event)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPiston(event: BlockPistonRetractEvent) {
        onPiston(event.blocks, event)
    }

    private fun onPiston(blocks: MutableList<Block?>, bukkitEvent: BlockPistonEvent) {
        val dataToMove: MutableMap<Block, NBTCustomBlock> = LinkedHashMap()
        val direction = bukkitEvent.direction

        // Schritt 1: Alle NBT-Daten VORHER auslesen und speichern
        blocks.forEach { block ->
            if (block == null) return@forEach

            val reaction = block.pistonMoveReaction
            if (reaction == PistonMoveReaction.BREAK) {
                return@forEach
            }

            if (NBTCustomBlock.hasCustomNBT(block)) {
                val nbt = getData(block)
                val destinationBlock = block.getRelative(direction)
                dataToMove[destinationBlock] = nbt
            }
        }

        // Schritt 2: Alle alten Daten löschen
        blocks.forEach { block ->
            if (block != null) {
                removeData(block)
            }
        }

        // Schritt 3: Neue Daten NACH der Piston-Bewegung setzen (1 tick später)
        Bukkit.getScheduler().runTask(PersistentBlockData.instance, Runnable {
            reverse(dataToMove).forEach { (destinationBlock, nbt) ->
                nbt.copyTo(destinationBlock)
            }
        })
    }


    private fun <K, V> reverse(map: MutableMap<K, V>): MutableMap<K, V> {
        val reversed = LinkedHashMap<K, V>()
        val keys: MutableList<K> = ArrayList(map.keys)
        keys.reverse()
        keys.forEach { key -> reversed[key] = map[key]!! }
        return reversed
    }
}