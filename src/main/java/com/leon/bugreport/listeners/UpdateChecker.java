package com.leon.bugreport.listeners;

import com.leon.bugreport.logging.ErrorMessages;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.function.Consumer;

import static com.leon.bugreport.API.ErrorClass.logErrorMessage;

public class UpdateChecker {
	private final JavaPlugin plugin;
	private final int resourceId;

	public UpdateChecker(JavaPlugin plugin, int resourceId) {
		this.plugin = plugin;
		this.resourceId = resourceId;
	}

	public void getVersion(final Consumer<String> consumer) {
		new BukkitRunnable() {
			@Override
			public void run() {
				try (InputStream is = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId).openStream()) {
					try (Scanner scanner = new Scanner(is)) {
						if (scanner.hasNext()) {
							consumer.accept(scanner.next());
						}
					}
				} catch (IOException e) {
					String errorMessage = ErrorMessages.getErrorMessage(33);

					plugin.getLogger().warning(errorMessage);
					logErrorMessage(errorMessage);
				}
			}
		}.runTaskAsynchronously(plugin);
	}
}
