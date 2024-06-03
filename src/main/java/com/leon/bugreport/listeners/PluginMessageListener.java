package com.leon.bugreport.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.leon.bugreport.BugReportPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.Objects;

import static com.leon.bugreport.BugReportManager.*;

public class PluginMessageListener implements org.bukkit.plugin.messaging.PluginMessageListener {
	public static void sendPluginMessage(Player player) {

		String server = config.getString("serverName", "MyServer");
		boolean send = config.getBoolean("enableBungeeCordSendMessage", true);

		if (send) {
			ByteArrayDataOutput out = ByteStreams.newDataOutput();

			out.writeUTF("Forward");
			out.writeUTF("ALL");
			out.writeUTF("Bugreport");

			ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
			DataOutputStream msgout = new DataOutputStream(msgbytes);
			try {
				msgout.writeUTF(player.getName() + ";" + server);
				msgout.writeShort(1);
			} catch (IOException exception) {
				exception.printStackTrace();
			}

			out.writeShort(msgbytes.toByteArray().length);
			out.write(msgbytes.toByteArray());

			player.sendPluginMessage(BugReportPlugin.getPlugin(), "BungeeCord", out.toByteArray());
		}
	}


	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		if (!channel.equals("BungeeCord")) {
			return;
		}

		boolean receive = config.getBoolean("enableBungeeCordReceiveMessage", true);

		if (receive) {
			ByteArrayDataInput in = ByteStreams.newDataInput(message);
			String subchannel = in.readUTF();
			if (subchannel.equals("Bugreport")) {
				try {
					short len = in.readShort();
					byte[] msgbytes = new byte[len];
					in.readFully(msgbytes);

					DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(msgbytes));
					String data = msgin.readUTF();
					short somenumber = msgin.readShort();
					String[] dataParts = data.split(";");
					if (dataParts.length >= 2) {
						String playerName = dataParts[0];
						String serverName = dataParts[1];

						for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
							if (onlinePlayer.hasPermission("bugreport.admin")) {
								onlinePlayer.sendMessage(pluginColor + pluginTitle + " " + Objects.requireNonNullElse(endingPluginTitleColor, ChatColor.GREEN) + "New Report submitted by " + playerName + " from " + serverName + "!");
							}
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}
		}
	}
}
