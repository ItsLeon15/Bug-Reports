package com.leon.bugreport;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import static com.leon.bugreport.BugReportManager.*;

public class BugListCommand implements CommandExecutor {
    public BugListCommand(BugReportManager reportManager) { }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(pluginColor + pluginTitle + " " + "This command can only be run by a player.");
            return true;
        }

        if (!player.hasPermission("bugreport.admin")) {
            if (args.length == 0) {
                player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }

			if (args[0].equalsIgnoreCase("help")) {
				player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.GREEN + "Commands:");
				player.sendMessage(ChatColor.GOLD + "/bugreport <Message>" + ChatColor.WHITE + " - " + ChatColor.GRAY + "Submits a bug report.");
				player.sendMessage(ChatColor.GOLD + "/bugreport help" + ChatColor.WHITE + " - " + ChatColor.GRAY + "Displays this help message.");
                return true;
            }

            player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            BugReportManager.setCurrentPage(player, 1);
            Inventory bugReportGUI = BugReportManager.getBugReportGUI(player);
            player.openInventory(bugReportGUI);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> {
                StringBuilder message = new StringBuilder();
                message.append(pluginColor).append(pluginTitle).append(" ").append(ChatColor.GREEN).append("Admin Commands:\n");
                String commandFormat = ChatColor.GOLD + "/%s" + ChatColor.WHITE + " - " + ChatColor.GRAY + "%s\n";

                message.append(String.format(commandFormat, "bugreport", "Submits a bug report."));
                message.append(String.format(commandFormat, "buglist", "Opens the buglist GUI."));
                message.append(String.format(commandFormat, "buglist help", "Displays this help message."));
                message.append(String.format(commandFormat, "buglist reload", "Reloads the plugin and config."));
                message.append(String.format(commandFormat, "buglistarchived", "Opens the buglist archived GUI."));
                message.append(String.format(commandFormat, "buglistsettings", "Opens the buglist settings GUI."));
                message.append(String.format(commandFormat, "buglinkdiscord", "Links Bug Report to a Discord channel."));

                player.sendMessage(message.toString());
            }
            case "reload" -> {
                BugReportLanguage.loadLanguageTexts(plugin, "languages.yml");
                BugReportManager.loadConfig();
                plugin.reloadConfig();
                BugReportDatabase.reloadConnection();

                player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.GREEN + "The plugin has been reloaded.");
            }
            case "debug" -> {
                if (args.length < 2) {
                    player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.RED + "Please enter a number between 0 and 1.");
                    return true;
                }

                try {
                    int debugMode = Integer.parseInt(args[1]);
                    if (debugMode >= 0 && debugMode <= 1) {
                        BugReportManager.setDebugMode(debugMode);
                        player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.GREEN + "Debug mode set to " + debugMode + ".");
                    } else {
                        player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.RED + "Please enter a number between 0 and 1.");
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.RED + "Please enter a valid number.");
                }
            }
            default -> {
                BugReportManager.setCurrentPage(player, 1);
                Inventory bugReportGUI = BugReportManager.getBugReportGUI(player);
                player.openInventory(bugReportGUI);
            }
        }

        return true;
    }
}
