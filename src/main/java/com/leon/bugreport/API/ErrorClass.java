package com.leon.bugreport.API;

import com.leon.bugreport.logging.ErrorMessages;

import java.io.File;
import java.io.FileWriter;

import static com.leon.bugreport.BugReportManager.plugin;

public class ErrorClass {
	private static final File LOG_DIR = new File("plugins/BugReport/logs");
	private static final File ERROR_FILE = new File(LOG_DIR, "error.log");

	public static void logErrorMessage(String message) {
		int errorTries = 0;
		while (errorTries < 3) {
			try {
				logErrorMessageInternal(message);
				return;
			} catch (Exception e) {
				errorTries++;
			}
		}
	}

	private static void logErrorMessageInternal(String message) {
		cacheDirectoryExists();
		createErrorLog();

		try {
			appendToFile(message);
		} catch (Exception e) {
			String errorMessage = ErrorMessages.getErrorMessage(11);
			plugin.getLogger().warning(errorMessage);
			logErrorMessage(errorMessage);
		}
	}

	private static void cacheDirectoryExists() {
		if (!LOG_DIR.exists() && !LOG_DIR.mkdirs()) {
			String errorMessage = ErrorMessages.getErrorMessage(12);

			plugin.getLogger().warning(errorMessage);
			logErrorMessage(errorMessage);
		}
	}

	public static void createErrorLog() {
		try {
			if (!ERROR_FILE.exists() && !ERROR_FILE.createNewFile()) {
				String errorMessage = ErrorMessages.getErrorMessage(13);

				plugin.getLogger().warning(errorMessage);
				logErrorMessage(errorMessage);
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
			String errorMessage = ErrorMessages.getErrorMessage(14);

			plugin.getLogger().warning(errorMessage);
			logErrorMessage(errorMessage);
		}
	}
}
