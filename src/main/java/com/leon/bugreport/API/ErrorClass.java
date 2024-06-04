package com.leon.bugreport.API;

import java.io.File;
import java.io.FileWriter;
import java.util.Objects;

import static com.leon.bugreport.BugReportManager.plugin;

public class ErrorClass {
	private static final File LOG_DIR = new File("plugins/BugReport/logs");
	private static final File ERROR_FILE = new File(LOG_DIR, "error.log");

	public static void logErrorMessage(String errorMessage) {
		cacheDirectoryExists();
		createErrorLog();

		try {
			appendToFile(errorMessage);
		} catch (Exception e) {
			plugin.getLogger().warning("Failed to write to error log file");
			logErrorMessage("Failed to write to error log file");
		}
	}

	private static void cacheDirectoryExists() {
		if (!LOG_DIR.exists() && !LOG_DIR.mkdirs()) {
			plugin.getLogger().warning("Failed to create cache directory");
			logErrorMessage("Failed to create cache directory");
		}
	}

	public static void createErrorLog() {
		try {
			if (!ERROR_FILE.exists() && !ERROR_FILE.createNewFile()) {
				plugin.getLogger().warning("Failed to create error log file");
				logErrorMessage("Failed to create error log file");
			}
		} catch (Exception ignored) {

		}
	}

	private static void appendToFile(String error) {
		try {
			String currentTime = java.time.LocalDateTime.now().toString();
			String formattedTime = currentTime.substring(0, currentTime.indexOf('T')) + ' ' + currentTime.substring(currentTime.indexOf('T') + 1, currentTime.indexOf('.'));

			FileWriter fileWriter = new FileWriter(ErrorClass.ERROR_FILE, true);
			fileWriter.write('[' + formattedTime + "] " + error + '\n');
			fileWriter.close();
		} catch (Exception e) {
			plugin.getLogger().warning("Failed to write to error log file");
			logErrorMessage("Failed to write to error log file");
		}
	}

	public static void throwDebug(String Debug) {
		plugin.getLogger().info(Debug);
	}

	public static void throwWarning(String Warning) {
		plugin.getLogger().info(Warning);
		logErrorMessage(Warning);
	}

	public static void throwError(String Error) {
		plugin.getLogger().info(Error);
		logErrorMessage(Error);
	}
}
