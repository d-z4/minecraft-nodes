package phonon.nodes.nms

import net.minecraft.network.chat.Component
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.util.CraftMagicNumbers
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import java.util.Optional
import net.minecraft.core.BlockPos as NMSBlockPos
import net.minecraft.network.protocol.Packet as NMSPacket
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket as NMSPacketLevelChunkWithLightPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket as NMSPacketSetEntityData
import net.minecraft.network.syncher.EntityDataSerializers as NMSEntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData as NMSSynchedEntityData
import net.minecraft.server.level.ServerPlayer as NMSPlayer
import net.minecraft.world.level.block.state.BlockState as NMSBlockState
import net.minecraft.world.level.chunk.LevelChunk as NMSChunk

// re-exported type aliases
internal typealias NMSBlockPos = NMSBlockPos
internal typealias NMSBlockState = NMSBlockState
internal typealias NMSChunk = NMSChunk
internal typealias NMSPlayer = NMSPlayer
internal typealias NMSPacketLevelChunkWithLightPacket = NMSPacketLevelChunkWithLightPacket
internal typealias NMSPacketSetEntityData = NMSPacketSetEntityData
internal typealias CraftWorld = CraftWorld
internal typealias CraftPlayer = CraftPlayer
internal typealias CraftMagicNumbers = CraftMagicNumbers

/**
 * Wrapper for getting Bukkit player connection and sending packet.
 */
internal fun Player.sendPacket(p: NMSPacket<*>) {
    return (this as CraftPlayer).handle.connection.send(p)
}

/**
 * Create custom name packet for armor stand entity.
 */
public fun ArmorStand.createArmorStandNamePacket(name: String): NMSPacketSetEntityData {
    val entityId = (this as CraftEntity).handle.id
    val nameComponent = Optional.of(Component.literal(name) as Component)

    val dataValues = ArrayList<NMSSynchedEntityData.DataValue<*>>()
    dataValues.add(NMSSynchedEntityData.DataValue(0, NMSEntityDataSerializers.BYTE, 0x20.toByte())) // invisible
    dataValues.add(NMSSynchedEntityData.DataValue(2, NMSEntityDataSerializers.OPTIONAL_COMPONENT, nameComponent))
    dataValues.add(NMSSynchedEntityData.DataValue(3, NMSEntityDataSerializers.BOOLEAN, true))
    dataValues.add(NMSSynchedEntityData.DataValue(5, NMSEntityDataSerializers.BOOLEAN, true))

    val constructor = NMSPacketSetEntityData::class.java.getDeclaredConstructor(
        Int::class.javaPrimitiveType,
        List::class.java,
    )
    constructor.isAccessible = true

    return constructor.newInstance(entityId, dataValues) as NMSPacketSetEntityData
}
