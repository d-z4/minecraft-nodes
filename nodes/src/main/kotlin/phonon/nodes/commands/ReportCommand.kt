package phonon.nodes.commands

import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import phonon.nodes.Config
import phonon.nodes.Nodes
import phonon.nodes.utils.DiscordWebhook

class ReportCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}This command is only for players.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("${ChatColor.RED}Usage: /report <message>")
            return true
        }

        val message = args.joinToString(" ")
        val resident = Nodes.getResident(sender)
        
        // Gathers info
        val locationStr = "${sender.world.name}, ${sender.location.blockX}, ${sender.location.blockY}, ${sender.location.blockZ}"
        
        var townNationStr = "None"
        var webhookUrl = Config.discordWebhooks["default"]
        
        if (resident != null) {
            val town = resident.town
            if (town != null) {
                townNationStr = town.name
                val nation = town.nation
                if (nation != null) {
                    townNationStr += " / ${nation.name}"
                    
                    // Check if there is a specific webhook for this nation
                    val nationWebhook = Config.discordWebhooks[nation.name]
                    if (nationWebhook != null) {
                        webhookUrl = nationWebhook
                    }
                }
            }
        }

        if (webhookUrl == null) {
            sender.sendMessage("${ChatColor.RED}Report system is not configured. Please contact an admin.")
            Nodes.logger?.warning("Report attempted but 'default' webhook is missing in config.")
            return true
        }

        // Send payload
        DiscordWebhook.sendReport(
            url = webhookUrl,
            reporter = sender.name,
            content = message,
            location = locationStr,
            townNation = townNationStr
        )

        sender.sendMessage("${ChatColor.GREEN}Report sent to your regions staff. Thank you!")
        return true
    }
}
