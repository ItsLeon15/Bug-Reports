package com.leon.bugreport;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

public class UniversalTabCompleter implements TabCompleter {
	@Override
	public List<String> onTabComplete(CommandSender sender, @NotNull Command command, String alias, String[] args) {
		List<String> completions = new ArrayList<>();

		switch (command.getName().toLowerCase()) {
			case "buglist" -> {
				if (args.length == 1) {
					if (sender.hasPermission("bugreport.admin")) {
						completions.add("reload");
						completions.add("debug");
					}
					completions.add("help");
				}
			}
			default -> { }
		}

		return completions;
	}
}
