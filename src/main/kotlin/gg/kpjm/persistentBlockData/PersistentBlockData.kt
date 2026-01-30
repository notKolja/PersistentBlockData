package gg.kpjm.persistentBlockData

import org.bukkit.plugin.java.JavaPlugin

class PersistentBlockData : JavaPlugin() {

    override fun onEnable() {
        instance = this
        logger.info("PersistentBlockData aktiviert!")
    }

    override fun onDisable() {
        logger.info("PersistentBlockData deaktiviert!")
    }

    companion object {
        lateinit var instance: PersistentBlockData
            private set
    }
}