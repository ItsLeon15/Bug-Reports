package com.bugreportmc.bugreport

import com.bugreportmc.bugreport.BugReportManager.Companion.debugMode
import com.bugreportmc.bugreport.BugReportPlugin.Companion.plugin
import org.bukkit.ChatColor
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.*

class BugReportLanguage(plugin: Plugin) {
	init {
		langFolder = File(plugin.dataFolder, "languages")
		languageCode = plugin.config.getString("language", "en_US")!!

		if (!langFolder.exists()) {
			langFolder.mkdirs()
		}

		if (!languageCodes.contains(languageCode)) {
			plugin.logger.warning("Invalid language code '$languageCode'. Defaulting to 'en_US'.")
			languageCode = "en_US"
		}

		loadLanguageFiles()
	}

	companion object {
		val languageCodes: List<String> =
			listOf("en_US", "es_ES", "de_DE", "fr_FR", "it_IT", "pt_BR", "ru_RU", "zh_CN", "zh_TW")
		private var enLangTempFile: File? = null
		private lateinit var langFolder: File
		private lateinit var languageCode: String
		private var langConfig: Map<String?, String?>? = null
		private var enLangConfig: Map<String?, String?>? = null

		fun getValueFromLanguageFile(key: String, defaultValue: String?): String {
			val strippedKey = ChatColor.stripColor(key)
			var value = langConfig!![strippedKey]

			if (value == null) {
				value = defaultValue
			}

			return value!!
		}

		private fun ensureTempEnglishFileExists() {
			if (enLangTempFile == null) {
				enLangTempFile = File(langFolder, "temp/en_US_temp.yml")
			}

			enLangTempFile!!.parentFile.mkdirs()
			plugin.saveResource("languages/temp/en_US_temp.yml", true)
		}

		private fun reloadEnglishTempConfig() {
			enLangConfig = flattenYamlConfiguration(
				YamlConfiguration.loadConfiguration(
					enLangTempFile!!
				)
			)
		}

		private fun checkIfEnglishFileModified() {
			ensureTempEnglishFileExists()
			reloadEnglishTempConfig()
		}

		fun getEnglishValueFromValue(value: String?): String? {
			checkIfEnglishFileModified()

			val strippedValue = ChatColor.stripColor(value)

			for ((key, value1) in langConfig!!) {
				if (value1 == strippedValue) {
					val englishValue = enLangConfig!!["en_US.$key"]
					return englishValue ?: strippedValue
				}
			}

			return strippedValue
		}

		@JvmStatic
		fun loadLanguageFiles() {
			ensureTempEnglishFileExists()

			val files = langFolder.listFiles()
			if (files == null || files.isEmpty()) {
				plugin.logger.warning("No language files found in the 'languages' folder.")
				for (languageCode in languageCodes) {
					plugin.saveResource("languages/$languageCode.yml", false)
				}
			} else {
				for (languageCode in languageCodes) {
					var found = false
					for (file in files) {
						if (file.name.equals("$languageCode.yml", ignoreCase = true)) {
							if (isFileEmpty(file)) {
								plugin.logger.warning("Language file '$languageCode.yml' is empty.")
								plugin.logger.warning("Creating new file.")
								plugin.saveResource("languages/$languageCode.yml", true)
							}
							found = true
							break
						}
					}
					if (!found) {
						val resourceAvailable = plugin.getResource("languages/$languageCode.yml") != null
						if (resourceAvailable) {
							plugin.saveResource("languages/$languageCode.yml", false)
						} else {
							plugin.logger.warning("Language file '$languageCode.yml' not found in resources.")
						}
					}
				}
			}

			checkIfEnglishFileModified()

			langConfig = HashMap()

			val langFile = File(langFolder, "$languageCode.yml")
			if (langFile.exists()) {
				val yamlConfig = YamlConfiguration.loadConfiguration(langFile)
				val langSection = yamlConfig.getConfigurationSection(languageCode)!!
				langConfig = flattenYamlConfiguration(Objects.requireNonNullElse(langSection, yamlConfig))
			} else {
				plugin.logger.warning("Language file '$languageCode.yml' not found.")
			}

			if (debugMode) {
				plugin.logger.info("Loaded " + langConfig!!.size + " language keys.")
			}
		}

		private fun isFileEmpty(file: File): Boolean {
			val yamlConfig = YamlConfiguration.loadConfiguration(file)
			return yamlConfig.getKeys(false).isEmpty()
		}

		private fun flattenYamlConfiguration(section: ConfigurationSection): Map<String?, String?> {
			val flattenedConfig: MutableMap<String?, String?> = HashMap()
			for (key in section.getKeys(true)) {
				if (section.isString(key)) {
					flattenedConfig[key] = section.getString(key)
				}
			}
			return flattenedConfig
		}

		fun setPluginLanguage(lC: String) {
			plugin.config["language"] = lC
			plugin.saveConfig()

			languageCode = lC

			loadLanguageFiles()
		}
	}
}
