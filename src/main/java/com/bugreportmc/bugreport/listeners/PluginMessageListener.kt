package com.bugreportmc.bugreport.listeners

import com.bugreportmc.bugreport.BugReportManager.Companion.config
import com.bugreportmc.bugreport.BugReportManager.Companion.endingPluginTitleColor
import com.bugreportmc.bugreport.BugReportManager.Companion.pluginColor
import com.bugreportmc.bugreport.BugReportManager.Companion.pluginTitle
import com.bugreportmc.bugreport.BugReportPlugin
import com.bugreportmc.bugreport.BugReportPlugin.Companion.plugin
import com.google.common.io.ByteStreams
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener
import java.io.*
import java.util.*

class PluginMessageListener : PluginMessageListener {
	override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
		if (channel != "BungeeCord") {
			return
		}

		val receive: Boolean = config.getBoolean("enableBungeeCordReceiveMessage", true)

		if (receive) {
			val `in` = DataInputStream(ByteArrayInputStream(message))
			val subchannel = `in`.readUTF()
			if (subchannel == "Bugreport") {
				try {
					val len = `in`.readShort()
					val msgbytes = ByteArray(len.toInt())
					`in`.readFully(msgbytes)

					val msgin = DataInputStream(ByteArrayInputStream(msgbytes))
					val data = msgin.readUTF()
					val somenumber = msgin.readShort()
					val dataParts = data.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
					if (dataParts.size >= 2) {
						val playerName = dataParts[0]
						val serverName = dataParts[1]

						for (onlinePlayer in Bukkit.getOnlinePlayers()) {
							if (onlinePlayer.hasPermission("bugreport.admin")) {
								onlinePlayer.sendMessage(
									"$pluginColor$pluginTitle " + Objects.requireNonNullElse(
										endingPluginTitleColor, ChatColor.GREEN
									) + "New Report submitted by " + playerName + " from " + serverName + "!"
								)
							}
						}
					}
				} catch (e: IOException) {
					e.printStackTrace()
					throw RuntimeException(e)
				}
			}
		}
	}

	companion object {
		fun sendPluginMessage(player: Player) {
			val server: String = config.getString("serverName", "MyServer").toString()
			val send: Boolean = config.getBoolean("enableBungeeCordSendMessage", true)

			if (send) {
				val out = ByteStreams.newDataOutput()

				out.writeUTF("Forward")
				out.writeUTF("ALL")
				out.writeUTF("Bugreport")

				val msgbytes = ByteArrayOutputStream()
				val msgout = DataOutputStream(msgbytes)
				try {
					msgout.writeUTF(player.name + ";" + server)
					msgout.writeShort(1)
				} catch (exception: IOException) {
					exception.printStackTrace()
				}

				out.writeShort(msgbytes.toByteArray().size)
				out.write(msgbytes.toByteArray())

				player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray())
			}
		}
	}
}
