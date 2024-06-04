package com.leon.bugreport;

import com.leon.bugreport.API.ErrorClass;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.leon.bugreport.BugReportManager.debugMode;

public class BugReportLanguage {
	public static final List<String> languageCodes = List.of("en_US", "es_ES", "de_DE", "fr_FR", "it_IT", "pt_BR", "ru_RU", "zh_CN", "zh_TW");
	private static File enLangTempFile;
	private static File langFolder;
	private static String languageCode;
	private static Map<String, String> langConfig;
	private static Map<String, String> enLangConfig;
	private static Plugin plugin;

	public BugReportLanguage(@NotNull Plugin plugin) {
		if (debugMode) {
			ErrorClass.throwDebug("BugReportLanguage: Starting BugReportLanguage");
		}
		BugReportLanguage.plugin = plugin;
		langFolder = new File(plugin.getDataFolder(), "languages");
		languageCode = plugin.getConfig().getString("language", "en_US");

		if (!langFolder.exists()) {
			langFolder.mkdirs();
		}

		if (!languageCodes.contains(languageCode)) {
			ErrorClass.throwError("Error 051: Invalid language code '" + languageCode + "'. Defaulting to 'en_US'.");
			languageCode = "en_US";
		}

		loadLanguageFiles();
	}

	public static String getValueFromLanguageFile(String key, String defaultValue) {
		String strippedKey = ChatColor.stripColor(key);
		String value = langConfig.get(strippedKey);

		if (value == null) {
			value = defaultValue;
		}

		return value;
	}

	private static void ensureTempEnglishFileExists() {
		if (debugMode) {
			ErrorClass.throwDebug("BugReportLanguage: Starting ensureTemEnglishFileExists");
		}
		if (enLangTempFile == null) {
			enLangTempFile = new File(langFolder, "temp/en_US_temp.yml");
		}

		enLangTempFile.getParentFile().mkdirs();
		plugin.saveResource("languages/temp/en_US_temp.yml", true);
	}

	private static void reloadEnglishTempConfig() {
		enLangConfig = flattenYamlConfiguration(YamlConfiguration.loadConfiguration(enLangTempFile));
	}

	private static void checkIfEnglishFileModified() {
		ensureTempEnglishFileExists();
		reloadEnglishTempConfig();
	}

	public static @Nullable String getEnglishValueFromValue(String value) {
		if (debugMode) {
			ErrorClass.throwDebug("BugReportLanguage: Starting getEnglishValueFromValue");
		}
		checkIfEnglishFileModified();

		String strippedValue = ChatColor.stripColor(value);

		for (Map.Entry<String, String> entry : langConfig.entrySet()) {
			if (entry.getValue().equals(strippedValue)) {
				String key = entry.getKey();
				String englishValue = enLangConfig.get("en_US." + key);
				return englishValue != null ? englishValue : strippedValue;
			}
		}

		return strippedValue;
	}

	public static void loadLanguageFiles() {
		ensureTempEnglishFileExists();

		File[] files = langFolder.listFiles();
		if (files == null || files.length == 0) {
			ErrorClass.throwError("Error 052: No language files found in the 'languages' folder.");
			for (String languageCode : languageCodes) {
				plugin.saveResource("languages/" + languageCode + ".yml", false);
			}
		} else {
			for (String languageCode : languageCodes) {
				boolean found = false;
				for (File file : files) {
					if (file.getName().equalsIgnoreCase(languageCode + ".yml")) {
						if (isFileEmpty(file)) {
							ErrorClass.throwWarning("Language file '" + languageCode + ".yml' is empty.");
							ErrorClass.throwWarning("Creating new file.");
							plugin.saveResource("languages/" + languageCode + ".yml", true);
						}
						found = true;
						break;
					}
				}
				if (!found) {
					boolean resourceAvailable = plugin.getResource("languages/" + languageCode + ".yml") != null;
					if (resourceAvailable) {
						plugin.saveResource("languages/" + languageCode + ".yml", false);
					} else {
						ErrorClass.throwError("Error 053: Language file '" + languageCode + ".yml' not found in resources.");
					}
				}
			}
		}

		checkIfEnglishFileModified();

		langConfig = new HashMap<>();

		File langFile = new File(langFolder, languageCode + ".yml");
		if (langFile.exists()) {
			YamlConfiguration yamlConfig = YamlConfiguration.loadConfiguration(langFile);
			ConfigurationSection langSection = yamlConfig.getConfigurationSection(languageCode);
			langConfig = flattenYamlConfiguration(Objects.requireNonNullElse(langSection, yamlConfig));
		} else {
			ErrorClass.throwError("Error 054: Language file '" + languageCode + ".yml' not found.");
		}

		if (debugMode) {
			ErrorClass.throwDebug("Loaded " + langConfig.size() + " language keys.");
		}
	}

	private static boolean isFileEmpty(@NotNull File file) {
		YamlConfiguration yamlConfig = YamlConfiguration.loadConfiguration(file);
		return yamlConfig.getKeys(false).isEmpty();
	}

	private static @NotNull Map<String, String> flattenYamlConfiguration(@NotNull ConfigurationSection section) {
		Map<String, String> flattenedConfig = new HashMap<>();
		for (String key : section.getKeys(true)) {
			if (section.isString(key)) {
				flattenedConfig.put(key, section.getString(key));
			}
		}
		return flattenedConfig;
	}

	public static void setPluginLanguage(String lC) {
		plugin.getConfig().set("language", lC);
		plugin.saveConfig();

		languageCode = lC;

		loadLanguageFiles();
	}
}
