package com.bugreportmc.bugreport.api

import com.bugreportmc.bugreport.BugReportManager
import com.bugreportmc.bugreport.BugReportPlugin.Companion.plugin
import com.bugreportmc.bugreport.api.DataSource.cleanOutdatedCache
import org.bukkit.scheduler.BukkitRunnable

class CacheCleanupListener {
	init {
		object : BukkitRunnable() {
			override fun run() {
				cleanOutdatedCache(false)
			}
		}.runTaskTimerAsynchronously(plugin, 0L, TWO_HOUR_TICKS)
	}

	companion object {
		private const val TICKS_PER_SECOND = 20
		private const val SECONDS_PER_MINUTE = 60
		private const val MINUTES_PER_HOUR = 60
		private const val TWO_HOUR_TICKS = (TICKS_PER_SECOND * SECONDS_PER_MINUTE * MINUTES_PER_HOUR * 2).toLong()
	}
}
