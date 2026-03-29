package com.typewritermc.entity.entries.data.minecraft.living

import com.github.retrooper.packetevents.protocol.world.damagetype.DamageTypes
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDamageEvent
import com.typewritermc.engine.paper.entry.entity.SinglePropertyCollectorSupplier
import com.typewritermc.engine.paper.entry.entries.EntityProperty
import me.tofaa.entitylib.wrapper.WrapperEntity

data class DamagedProperty(val damaged: Boolean) : EntityProperty {
    companion object : SinglePropertyCollectorSupplier<DamagedProperty>(DamagedProperty::class)
}

fun applyDamagedData(entity: WrapperEntity, property: DamagedProperty) {
    if (!property.damaged) return
    entity.sendPacketToViewers(
        WrapperPlayServerDamageEvent(entity.entityId, DamageTypes.GENERIC, 0, 0, null)
    )
}