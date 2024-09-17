package com.leon.bugreport;

import com.leon.bugreport.logging.ErrorMessages;
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

import static com.leon.bugreport.API.ErrorClass.logErrorMessage;
import static com.leon.bugreport.BugReportManager.debugMode;

public class BugReportLanguage {
	public static final List<String> languageCodes = List.of(
			"en_US", "es_ES", "de_DE", "fr_FR", "it_IT", "ko_KR", "pt_BR", "ru_RU", "zh_CN", "zh_TW", "cs_CZ"
	);
	private static File langFolder;
	private static String languageCode;
	private static Map<String, String> langConfig;
	private static Plugin plugin;

	public BugReportLanguage(@NotNull Plugin plugin) {
		BugReportLanguage.plugin = plugin;
		langFolder = new File(plugin.getDataFolder(), "languages");
		languageCode = plugin.getConfig().getString("language", "en_US");

		if (!langFolder.exists()) {
			langFolder.mkdirs();
		}

		if (!languageCodes.contains(languageCode)) {
			String errorMessage = ErrorMessages.getErrorMessage(16);
			String finalErrorMessage = errorMessage.replaceAll("%languageCode%", languageCode);

			plugin.getLogger().warning(finalErrorMessage);
			logErrorMessage(finalErrorMessage);
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

	public static @Nullable String getKeyFromTranslation(String translation) {
		String cleanedTranslation = ChatColor.stripColor(translation);
		if (langConfig != null) {
			for (Map.Entry<String, String> entry : langConfig.entrySet()) {
				if (entry.getValue().equals(cleanedTranslation)) {
					return entry.getKey();
				}
			}
		}
		return null;
	}

	public static void loadLanguageFiles() {
		File[] files = langFolder.listFiles();
		if (files == null || files.length == 0) {
			String errorMessage = ErrorMessages.getErrorMessage(17);

			plugin.getLogger().warning(errorMessage);
			logErrorMessage(errorMessage);
			for (String languageCode : languageCodes) {
				plugin.saveResource("languages/" + languageCode + ".yml", false);
			}
		} else {
			for (String languageCode : languageCodes) {
				boolean found = false;
				for (File file : files) {
					if (file.getName().equalsIgnoreCase(languageCode + ".yml")) {
						if (isFileEmpty(file)) {
							String errorMessage = ErrorMessages.getErrorMessage(18);
							String finalErrorMessage = errorMessage.replaceAll("%languageCode%", languageCode);

							plugin.getLogger().warning(finalErrorMessage);
							logErrorMessage(finalErrorMessage);
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
						String errorMessage = ErrorMessages.getErrorMessage(19);
						String finalErrorMessage = errorMessage.replaceAll("%languageCode%", languageCode);

						plugin.getLogger().warning(finalErrorMessage);
						logErrorMessage(finalErrorMessage);
					}
				}
			}
		}

		langConfig = new HashMap<>();

		File langFile = new File(langFolder, languageCode + ".yml");
		if (langFile.exists()) {
			YamlConfiguration yamlConfig = YamlConfiguration.loadConfiguration(langFile);
			ConfigurationSection langSection = yamlConfig.getConfigurationSection(languageCode);
			if (langSection == null) {
				langSection = yamlConfig;
			}
			langConfig = flattenYamlConfiguration(langSection);
		} else {
			String errorMessage = ErrorMessages.getErrorMessage(19);
			String finalErrorMessage = errorMessage.replaceAll("%languageCode%", languageCode);

			plugin.getLogger().warning(finalErrorMessage);
			logErrorMessage(finalErrorMessage);
		}

		if (debugMode) {
			plugin.getLogger().info("Loaded " + langConfig.size() + " language keys.");
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
