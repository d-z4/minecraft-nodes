/**
 * Boost serialization and deserialization
 */
package phonon.nodes.serdes

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import phonon.nodes.objects.Boost
import phonon.nodes.objects.BoostSaveState
import phonon.nodes.objects.BoostType
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.Path
import java.util.UUID

object BoostSerdes {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Serialize boosts to JSON and save to file
     */
    fun saveBoostsToFile(boosts: List<BoostSaveState>, path: Path) {
        try {
            FileWriter(path.toString()).use { writer ->
                val jsonObject = mapOf("boosts" to boosts)
                gson.toJson(jsonObject, writer)
            }
        } catch (e: Exception) {
            println("[Nodes] Error saving boosts to $path: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Deserialize boosts from JSON file
     */
    fun loadBoostsFromFile(path: Path): List<Boost> {
        val boosts = mutableListOf<Boost>()

        try {
            val json = JsonParser.parseReader(FileReader(path.toString()))
            val jsonObj = json.asJsonObject

            val jsonBoosts = jsonObj.getAsJsonArray("boosts")
            if (jsonBoosts != null) {
                for (jsonBoost in jsonBoosts) {
                    val boostObj = jsonBoost.asJsonObject

                    val id = UUID.fromString(boostObj.get("id").asString)
                    val type = BoostType.valueOf(boostObj.get("type").asString)
                    val multiplier = boostObj.get("multiplier").asDouble
                    val targetId = if (boostObj.has("targetId") && !boostObj.get("targetId").isJsonNull) {
                        UUID.fromString(boostObj.get("targetId").asString)
                    } else {
                        null
                    }
                    val startTime = boostObj.get("startTime").asLong
                    val duration = boostObj.get("duration").asLong
                    val purchaser = if (boostObj.has("purchaser") && !boostObj.get("purchaser").isJsonNull) {
                        UUID.fromString(boostObj.get("purchaser").asString)
                    } else {
                        null
                    }

                    val boost = Boost(
                        id = id,
                        type = type,
                        multiplier = multiplier,
                        targetId = targetId,
                        startTime = startTime,
                        duration = duration,
                        purchaser = purchaser,
                    )

                    boosts.add(boost)
                }
            }
        } catch (e: Exception) {
            println("[Nodes] Error loading boosts from $path: ${e.message}")
            e.printStackTrace()
        }

        return boosts
    }
}
