package com.bugreportmc.bugreport.api

import com.bugreportmc.bugreport.BugReportManager
import com.bugreportmc.bugreport.BugReportPlugin.Companion.plugin
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime

object ErrorClass {
	private val LOG_DIR = File("plugins/BugReport/logs")
	private val ERROR_FILE = File(LOG_DIR, "error.log")

	@JvmStatic
	fun logErrorMessage(errorMessage: String) {
		cacheDirectoryExists()
		createErrorLog()

		try {
			appendToFile(errorMessage)
		} catch (e: Exception) {
			plugin.logger.warning("Failed to write to error log file")
			logErrorMessage("Failed to write to error log file")
		}
	}

	private fun cacheDirectoryExists() {
		if (!LOG_DIR.exists() && !LOG_DIR.mkdirs()) {
			plugin.logger.warning("Failed to create cache directory")
			logErrorMessage("Failed to create cache directory")
		}
	}

	private fun createErrorLog() {
		try {
			if (!ERROR_FILE.exists() && !ERROR_FILE.createNewFile()) {
				plugin.logger.warning("Failed to create error log file")
				logErrorMessage("Failed to create error log file")
			}
		} catch (ignored: Exception) {
		}
	}

	private fun appendToFile(error: String) {
		try {
			val currentTime = LocalDateTime.now().toString()
			val formattedTime = currentTime.substring(
				0, currentTime.indexOf('T')
			) + ' ' + currentTime.substring(currentTime.indexOf('T') + 1, currentTime.indexOf('.'))

			val fileWriter = FileWriter(ERROR_FILE, true)
			fileWriter.write("[$formattedTime] $error\n")
			fileWriter.close()
		} catch (e: Exception) {
			plugin.logger.warning("Failed to write to error log file")
			logErrorMessage("Failed to write to error log file")
		}
	}
}
