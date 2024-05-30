package com.leon.bugreport;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

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
				}
				completions.add("help");
			}
		} else if (command.getName().equalsIgnoreCase("bugreport")) {
			if (args.length == 1) {
				if (sender.hasPermission("bugreport.admin") || sender.hasPermission("bugreport.use")) {
					if (config.getBoolean("bug-category-tab-complete", true)) {
						for (Category category : bugReportManager.getReportCategories()) {
							completions.add(category.getName());
						}
					}
				}
			}
		}

		return completions;
	}
}
