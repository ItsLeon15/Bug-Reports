package com.bugreportmc.bugreport

import com.bugreportmc.bugreport.BugReportManager.Companion.debugMode
import com.bugreportmc.bugreport.BugReportManager.Companion.getReportCategories
import com.bugreportmc.bugreport.BugReportPlugin.Companion.plugin
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.configuration.file.FileConfiguration

class UniversalTabCompleter(private val config: FileConfiguration) : TabCompleter {
	override fun onTabComplete(
		sender: CommandSender,
		command: Command,
		alias: String,
		args: Array<String>,
	): List<String> {
		val completions: MutableList<String> = ArrayList()

		if (command.name.equals("buglist", ignoreCase = true)) {
			if (args.size == 1) {
				if (sender.hasPermission("bugreport.admin") || sender.hasPermission("bugreport.list")) {
					completions.add("reload")
					completions.add("debug")
					completions.add("version")
				}
				completions.add("help")
			}
		} else if (command.name.equals("bugreport", ignoreCase = true)) {
			if (config.getBoolean("enablePluginReportCategoriesGUI") && !config.getBoolean("enablePluginReportCategoriesTabComplete")) {
				if (debugMode) {
					plugin.logger.info("No tab-completion for categories.")
				}
			} else if (config.getBoolean("enablePluginReportCategoriesGUI") && config.getBoolean("enablePluginReportCategoriesTabComplete")) {
				plugin.logger.warning("enablePluginReportCategoriesGUI and enablePluginReportCategoriesTabComplete are both true! Either one of them has to be true or both false. Using GUI now.")
			} else if (!config.getBoolean("enablePluginReportCategoriesGUI") && config.getBoolean("enablePluginReportCategoriesTabComplete")) {
				if (args.size == 1) {
					if (sender.hasPermission("bugreport.admin") || sender.hasPermission("bugreport.use")) {
						for (category in getReportCategories()) {
							var categoryName: String = category.getName()
							if (categoryName.contains(" ")) {
								categoryName = categoryName.replace(" ", "-")
							}
							completions.add(categoryName)
						}
					} else {
						completions.add("help")
					}
				}
			}
		}
		return completions
	}
}
