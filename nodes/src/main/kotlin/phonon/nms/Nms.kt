package phonon.nodes.nms

import it.unimi.dsi.fastutil.ints.IntArrayList
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.numbers.BlankFormat
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.network.protocol.game.ClientboundSetScorePacket
import net.minecraft.world.entity.EntityType
import net.minecraft.world.phys.Vec3
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
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
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
private fun setTeamFields(team: PlayerTeam, prefix: String, suffix: String, color: ChatFormatting) {
    listOf(
        "playerPrefix" to Component.literal(prefix).withStyle(color), // Apply color to prefix Component
        "playerSuffix" to Component.literal(suffix),
        "nameTagVisibility" to Team.Visibility.ALWAYS,
        "collisionRule" to Team.CollisionRule.ALWAYS,
        "color" to color,
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
public fun Player.sendTeamCreate(teamName: String, prefix: String = "", suffix: String = "", color: ChatFormatting = ChatFormatting.RESET, members: Collection<String> = emptyList()) {
    val team = PlayerTeam(null, teamName)
    setTeamFields(team, prefix, suffix, color)
    members.forEach { team.players.add(it) }
    this.sendPacket(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true))
}

private const val CRYSTAL_FLAGS_ID = 0 // Byte: 0x20 = invisible
private const val CRYSTAL_BEAM_TARGET_ID = 8 // Optional<BlockPos>
private const val CRYSTAL_SHOW_BOTTOM_ID = 9 // Boolean: false to hide base plate

public fun Player.sendThickSkyBeam(
    x: Double,
    y: Double,
    z: Double,
    beamHeight: Double = 300.0,
    ringRadius: Double = 1.8,
    numRays: Int = 12,
): List<Int> {
    val entityIds = mutableListOf<Int>()
    val targetY = (y + beamHeight).toInt().coerceAtMost(319) // cap at build limit
    val centerTarget = NMSBlockPos(x.toInt(), targetY, z.toInt())

    // Unique base ID per beam (rough, low collision risk)
    val baseId = ((hashCode() + System.currentTimeMillis()).toInt() and 0x7FFFFFFF) * 100

    for (i in 0 until numRays) {
        val angle = 2.0 * PI * i / numRays
        val cx = x + ringRadius * cos(angle)
        val cz = z + ringRadius * sin(angle)
        val cy = y - 0.6 // Slightly below base to hide crystal models

        val entityId = baseId + i
        entityIds.add(entityId)

        // 1. Spawn packet
        val spawnPacket = ClientboundAddEntityPacket(
            entityId,
            UUID.randomUUID(),
            cx, cy, cz,
            0f, 0f, // rotation irrelevant
            EntityType.END_CRYSTAL,
            0, // initial data
            Vec3.ZERO,
            0.0,
        )

        // 2. Metadata packet (invisible + no bottom + beam target)
        val dataValues = listOf(
            NMSSynchedEntityData.DataValue(
                CRYSTAL_FLAGS_ID,
                NMSEntityDataSerializers.BYTE,
                0x20.toByte(), // invisible
            ),
            NMSSynchedEntityData.DataValue(
                CRYSTAL_SHOW_BOTTOM_ID,
                NMSEntityDataSerializers.BOOLEAN,
                false,
            ),
            NMSSynchedEntityData.DataValue(
                CRYSTAL_BEAM_TARGET_ID,
                NMSEntityDataSerializers.OPTIONAL_BLOCK_POS,
                Optional.of(centerTarget),
            ),
        )
        val metaPacket = ClientboundSetEntityDataPacket(entityId, dataValues)

        // Send both
        sendPacket(spawnPacket)
        sendPacket(metaPacket)
    }

    return entityIds
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

public fun Player.removeFakeBeam(entityIds: Collection<Int>) {
    if (entityIds.isEmpty()) return

    val intList = IntArrayList(entityIds.toIntArray()) // or IntArrayList().apply { addAll(entityIds) }

    val packet = ClientboundRemoveEntitiesPacket(intList)

    this.sendPacket(packet)
}

public fun Player.sendScore(objectiveName: String, entryName: String, score: Int) = this.sendPacket(ClientboundSetScorePacket(entryName, objectiveName, score, Optional.empty(), Optional.empty()))
