package com.leon.bugreport;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static com.leon.bugreport.BugReportManager.pluginColor;
import static com.leon.bugreport.BugReportManager.pluginTitle;

public class LinkDiscordCommand implements CommandExecutor {
    private final BugReportManager reportManager;

    public LinkDiscordCommand(BugReportManager reportManager) {
        this.reportManager = reportManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command."); // TODO: Language support
            return true;
        }

        Player player = (Player) sender;

        if (player.hasPermission("bugreport.admin")) {
            if (args.length < 1) {
                player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.RED + "Usage: /buglinkdiscord <webhook URL>"); // TODO: Language support
                return true;
            }

            String webhookURL = args[0];

            if (!isWebhookURLValid(webhookURL)) {
                player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.RED + "Invalid webhook URL."); // TODO: Language support
                return true;
            }

            reportManager.setWebhookURL(webhookURL);

            player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.GREEN + "Webhook URL has been set successfully."); // TODO: Language support
        } else {
            player.sendMessage(pluginColor + pluginTitle + " " + ChatColor.RED + "You don't have permission to use this command."); // TODO: Language support
        }

        return true;
    }

    private boolean isWebhookURLValid(String webhookURL) {
        return webhookURL.startsWith("https://discord.com/api/webhooks/");
    }
}