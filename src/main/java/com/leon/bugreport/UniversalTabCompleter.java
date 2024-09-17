package com.leon.bugreport;

import com.leon.bugreport.logging.ErrorMessages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.leon.bugreport.BugReportManager.debugMode;
import static com.leon.bugreport.BugReportManager.plugin;

public class UniversalTabCompleter implements TabCompleter {

	private final BugReportManager bugReportManager;
	private final FileConfiguration config;

	public UniversalTabCompleter(BugReportManager bugReportManager, FileConfiguration config) {
		this.bugReportManager = bugReportManager;
		this.config = config;
	}

	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
		List<String> completions = new ArrayList<>();

		if (command.getName().equalsIgnoreCase("buglist")) {
			if (args.length == 1) {
				if (sender.hasPermission("bugreport.admin") || sender.hasPermission("bugreport.list")) {
					completions.add("reload");
					completions.add("debug");
					completions.add("version");
					completions.add("export");
				}
				completions.add("help");
			}
		} else if (command.getName().equalsIgnoreCase("bugreport")) {
			if (config.getBoolean("enablePluginReportCategoriesGUI") && !config.getBoolean("enablePluginReportCategoriesTabComplete")) {
				if (debugMode) {
					plugin.getLogger().info("No tab-completion for categories.");
				}
			} else if (config.getBoolean("enablePluginReportCategoriesGUI") && config.getBoolean("enablePluginReportCategoriesTabComplete")) {
				String errorMessage = ErrorMessages.getErrorMessage(29);
				plugin.getLogger().warning(errorMessage);
			} else if (!config.getBoolean("enablePluginReportCategoriesGUI") && config.getBoolean("enablePluginReportCategoriesTabComplete")) {
				if (args.length == 1) {
					if (sender.hasPermission("bugreport.admin") || sender.hasPermission("bugreport.use")) {
						for (Category category : bugReportManager.getReportCategories()) {
							String categoryName = category.getName();
							if (categoryName.contains(" ")) {
								categoryName = categoryName.replace(" ", "-");
							}
							completions.add(categoryName);
						}
					} else {
						completions.add("help");
					}
				}
			}
		}
		return completions;
	}
}
