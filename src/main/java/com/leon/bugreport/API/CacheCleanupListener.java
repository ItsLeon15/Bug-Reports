package com.leon.bugreport.API;

import org.bukkit.scheduler.BukkitRunnable;

import static com.leon.bugreport.BugReportManager.plugin;

public class CacheCleanupListener {
	private static final int TICKS_PER_SECOND = 20;
	private static final int SECONDS_PER_MINUTE = 60;
	private static final int MINUTES_PER_HOUR = 60;
	private static final long TWO_HOUR_TICKS = TICKS_PER_SECOND * SECONDS_PER_MINUTE * MINUTES_PER_HOUR * 2; // 2 hours in ticks

	public CacheCleanupListener() {
		new BukkitRunnable() {
			@Override
			public void run() {
				DataSource.cleanOutdatedCache(false);
			}
		}.runTaskTimerAsynchronously(plugin, 0L, TWO_HOUR_TICKS); // Every 2 hours
	}
}
