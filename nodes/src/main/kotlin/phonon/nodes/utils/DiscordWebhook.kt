package phonon.nodes.utils

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.bukkit.Bukkit
import phonon.nodes.Nodes
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

object DiscordWebhook {

    fun sendReport(url: String, reporter: String, content: String, location: String, townNation: String) {
        Bukkit.getAsyncScheduler().runNow(Nodes.plugin!!) { _ ->
            try {
                // main payload
                val json = JsonObject()
                
                // embeds array
                val embeds = JsonArray()
                val embed = JsonObject()
                
                embed.addProperty("title", "New Player Report")
                embed.addProperty("description", content)
                embed.addProperty("color", 16711680) // Red color (0xFF0000)

                // fields
                val fields = JsonArray()
                
                // Reporter field
                val fieldReporter = JsonObject()
                fieldReporter.addProperty("name", "Reporter")
                fieldReporter.addProperty("value", reporter)
                fieldReporter.addProperty("inline", true)
                fields.add(fieldReporter)

                // Location field
                val fieldLocation = JsonObject()
                fieldLocation.addProperty("name", "Location")
                fieldLocation.addProperty("value", location)
                fieldLocation.addProperty("inline", true)
                fields.add(fieldLocation)

                // Town/Nation field
                val fieldTown = JsonObject()
                fieldTown.addProperty("name", "Town / Nation")
                fieldTown.addProperty("value", townNation)
                fieldTown.addProperty("inline", true)
                fields.add(fieldTown)

                embed.add("fields", fields)
                embed.addProperty("timestamp", java.time.Instant.now().toString())

                embeds.add(embed)
                json.add("embeds", embeds)

                val jsonString = json.toString()
                
                // Send POST request
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("User-Agent", "NodesPlugin/1.0")

                connection.outputStream.use { os ->
                    val input = jsonString.toByteArray(StandardCharsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    Nodes.logger?.warning("Failed to send webhook report: $responseCode")
                }

            } catch (e: Exception) {
                Nodes.logger?.warning("Error sending discord webhook: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
