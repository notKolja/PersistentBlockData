package gg.kpjm.persistentBlockData

import gg.kpjm.persistentBlockData.listener.BlockDataListener
import org.bukkit.plugin.java.JavaPlugin

class PersistentBlockData : JavaPlugin() {

    override fun onEnable() {
        instance = this
        logger.info("PersistentBlockData aktiviert!")

        server.pluginManager.registerEvents(BlockDataListener(), instance)
    }


    override fun onDisable() {
        logger.info("PersistentBlockData deaktiviert!")
    }

    companion object {
        lateinit var instance: PersistentBlockData
            private set
    }
}