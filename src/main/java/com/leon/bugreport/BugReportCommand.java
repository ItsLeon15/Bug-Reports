package com.leon.bugreport;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BugReportCommand implements CommandExecutor {
    private BugReportManager reportManager;

    public BugReportCommand(BugReportManager reportManager) {
        this.reportManager = reportManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length >= 1) {
            String message = String.join(" ", args);
            reportManager.submitBugReport(player, message);
            player.sendMessage(ChatColor.GREEN + "Bug report submitted successfully!");
        } else {
            player.sendMessage(ChatColor.RED + "Usage: /bugreport <message>");
        }

        return true;
    }
}
