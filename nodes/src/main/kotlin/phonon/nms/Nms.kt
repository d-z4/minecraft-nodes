package phonon.nodes.nms

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.numbers.BlankFormat
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.network.protocol.game.ClientboundSetScorePacket
import net.minecraft.world.scores.Objective
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Team
import net.minecraft.world.scores.criteria.ObjectiveCriteria
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.util.CraftMagicNumbers
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot
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
internal fun Player.sendPacket(p: NMSPacket<*>) = (this as CraftPlayer).handle.connection.send(p)

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

/**
 * Scoreboard team packet helper
 */
private fun setTeamFields(team: PlayerTeam, prefix: String, suffix: String) {
    listOf(
        "playerPrefix" to Component.literal(prefix),
        "playerSuffix" to Component.literal(suffix),
        "nameTagVisibility" to Team.Visibility.ALWAYS,
        "collisionRule" to Team.CollisionRule.ALWAYS,
        "color" to ChatFormatting.RESET,
    ).forEach { (fieldName, value) ->
        PlayerTeam::class.java.getDeclaredField(fieldName).apply {
            isAccessible = true
            set(team, value)
        }
    }
}

/**
 * Send team packet to player
 */
public fun Player.sendTeamCreate(teamName: String, prefix: String = "", suffix: String = "", members: Collection<String> = emptyList()) {
    val team = PlayerTeam(null, teamName)
    setTeamFields(team, prefix, suffix)
    members.forEach { team.players.add(it) }
    this.sendPacket(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true))
}

public fun Player.sendTeamRemove(teamName: String) = this.sendPacket(ClientboundSetPlayerTeamPacket.createRemovePacket(PlayerTeam(null, teamName)))

public fun Player.sendTeamAddPlayers(teamName: String, members: Collection<String>) = this.sendPacket(ClientboundSetPlayerTeamPacket.createMultiplePlayerPacket(PlayerTeam(null, teamName), members, ClientboundSetPlayerTeamPacket.Action.ADD))

/**
 * Objective/score packet helpers, used for scoreboard in minimap
 */
public fun Player.sendObjectiveCreate(objectiveName: String, displayName: String) = this.sendPacket(
    ClientboundSetObjectivePacket(
        Objective(
            null,
            objectiveName,
            ObjectiveCriteria.DUMMY,
            Component.literal(displayName),
            ObjectiveCriteria.RenderType.INTEGER,
            false,
            BlankFormat.INSTANCE,
        ),
        0,
    ),
)

public fun Player.sendObjectiveRemove(objectiveName: String) = this.sendPacket(
    ClientboundSetObjectivePacket(
        Objective(
            null,
            objectiveName,
            ObjectiveCriteria.DUMMY,
            Component.literal(""),
            ObjectiveCriteria.RenderType.INTEGER,
            false,
            BlankFormat.INSTANCE,
        ),
        1,
    ),
)

public fun Player.sendObjectiveDisplay(objectiveName: String, slot: DisplaySlot) {
    val nmsSlot = when (slot) {
        DisplaySlot.SIDEBAR -> net.minecraft.world.scores.DisplaySlot.SIDEBAR
        DisplaySlot.PLAYER_LIST -> net.minecraft.world.scores.DisplaySlot.LIST
        DisplaySlot.BELOW_NAME -> net.minecraft.world.scores.DisplaySlot.BELOW_NAME
        else -> net.minecraft.world.scores.DisplaySlot.SIDEBAR
    }
    val objective = if (objectiveName.isEmpty()) {
        null
    } else {
        Objective(
            null,
            objectiveName,
            ObjectiveCriteria.DUMMY,
            Component.literal(""),
            ObjectiveCriteria.RenderType.INTEGER,
            false,
            BlankFormat.INSTANCE,
        )
    }
    this.sendPacket(ClientboundSetDisplayObjectivePacket(nmsSlot, objective))
}

public fun Player.sendScore(objectiveName: String, entryName: String, score: Int) = this.sendPacket(ClientboundSetScorePacket(entryName, objectiveName, score, Optional.empty(), Optional.empty()))
