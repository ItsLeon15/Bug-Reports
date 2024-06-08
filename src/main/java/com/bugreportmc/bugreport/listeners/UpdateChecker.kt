package com.bugreportmc.bugreport.listeners

import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import java.io.IOException
import java.net.URL
import java.util.*
import java.util.function.Consumer

class UpdateChecker(private val plugin: Plugin, private val resourceId: Int) {
	fun getVersion(consumer: Consumer<String>) {
		object : BukkitRunnable() {
			override fun run() {
				try {
					URL("https://api.spigotmc.org/legacy/update.php?resource=$resourceId").openStream().use { `is` ->
						Scanner(`is`).use { scanner ->
							if (scanner.hasNext()) {
								consumer.accept(scanner.next())
							}
						}
					}
				} catch (e: IOException) {
					plugin.logger.warning("Unable to check for updates: " + e.message)
				}
			}
		}.runTaskAsynchronously(plugin)
	}
}
