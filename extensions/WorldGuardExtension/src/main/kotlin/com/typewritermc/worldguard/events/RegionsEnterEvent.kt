package com.typewritermc.worldguard.events

import com.sk89q.worldguard.protection.regions.ProtectedRegion
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

class RegionsEnterEvent(player: Player, val regions: Set<ProtectedRegion>) : PlayerEvent(player) {
    operator fun contains(regionName: String) = regions.any { it.id == regionName }

    override fun getHandlers(): HandlerList = HANDLER_LIST

    companion object {
        @JvmStatic
        val HANDLER_LIST = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLER_LIST
    }
}