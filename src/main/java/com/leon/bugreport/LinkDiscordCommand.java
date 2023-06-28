package com.leon.bugreport;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LinkDiscordCommand implements CommandExecutor {
    private BugReportManager reportManager;

    public LinkDiscordCommand(BugReportManager reportManager) {
        this.reportManager = reportManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (player.hasPermission("bugreport.admin")) {
            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "Usage: /buglinkdiscord <webhook URL>");
                return true;
            }

            String webhookURL = args[0];

            if (!isWebhookURLValid(webhookURL)) {
                player.sendMessage(ChatColor.RED + "Invalid webhook URL.");
                return true;
            }

            reportManager.setWebhookURL(webhookURL);

            player.sendMessage(ChatColor.GREEN + "Webhook URL has been set successfully.");
        } else {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
        }

        return true;
    }

    private boolean isWebhookURLValid(String webhookURL) {
        return webhookURL.startsWith("https://discord.com/api/webhooks/");
    }
}