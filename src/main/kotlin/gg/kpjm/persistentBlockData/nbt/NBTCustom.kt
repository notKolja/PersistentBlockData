package gg.kpjm.persistentBlockData.nbt

import de.tr7zw.changeme.nbtapi.NBTCompound
import gg.kpjm.persistentBlockData.PersistentBlockData.Companion.instance
import org.bukkit.NamespacedKey
import org.jetbrains.annotations.NotNull

/**
 * An interface for objects that can hold custom NBT data.
 * Provides a standardized way to access and manipulate a dedicated "custom" NBT compound.
 */
interface NBTCustom {
    /**
     * Deletes the entire custom NBT compound from the object.
     */
    fun deleteCustomNBT()

    /**
     * A copy of the entire NBT compound of the object.
     */
    @get:NotNull
    val copy: NBTCompound

    /**
     * The custom NBT compound for this object.
     * This is where custom data should be stored to avoid conflicts with vanilla NBT tags.
     */
    @get:NotNull
    val customNBT: NBTCompound

    /**
     * Saves the modified NBT data back to the original object.
     */
    fun saveCompound()

    companion object {
        /**
         * The old, deprecated namespaced key for custom NBT data.
         */
        val OLD_KEY: NamespacedKey = NamespacedKey(instance, "custom-nbt")

        /**
         * The modern key used to store the custom NBT compound.
         */
        const val KEY: String = "persistentblockdata-custom"
    }
}