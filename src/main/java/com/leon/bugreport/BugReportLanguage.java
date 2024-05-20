package com.leon.bugreport;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BugReportLanguage {
	static List<String> languageCodes = List.of(
			"en_US", "es_ES", "de_DE", "fr_FR",
			"it_IT", "pt_BR", "ru_RU", "zh_CN",
			"zh_TW"
	);
	private static File langFolder;
	private static String languageCode;
	private static YamlConfiguration langConfig;
	private static YamlConfiguration enLangConfig;

	public BugReportLanguage(@NotNull Plugin plugin) {
		langFolder = new File(plugin.getDataFolder(), "languages");
		languageCode = plugin.getConfig().getString("languages", "en_US");

		loadLanguageFiles(plugin);
	}

	public static String getValueFromLanguageFile(String key, String defaultValue) {
		String newKey = ChatColor.stripColor(key);
		String value = langConfig.getString(languageCode + "." + newKey);

		System.out.println("key: " + newKey);
		System.out.println("value1: " + value);
		System.out.println("languageCode: " + languageCode);
		System.out.println("langConfig: " + langConfig);

		if (value == null) {
			value = defaultValue;
		}

		return value;
	}

	public static @Nullable String getEnglishValueFromValue(String value) {
		if (enLangConfig == null) {
			System.out.println("enLangConfig is null");
			return null;
		}

		String newValue = ChatColor.stripColor(value);

		for (Map.Entry<String, Object> entry : enLangConfig.getValues(true).entrySet()) {
			if (entry.getValue() != null && entry.getValue().equals(newValue)) {
				String entryGetKey = entry.getKey();
				String outputValue = enLangConfig.getString(entryGetKey);
				return outputValue;
			}
		}

		System.out.println("value: " + newValue);

		return null;
	}

	public static void loadLanguageFiles(Plugin plugin) {
		if (!langFolder.exists()) {
			langFolder.mkdirs();
		}

		File[] files = langFolder.listFiles();
		if (files == null || files.length == 0) {
			plugin.getLogger().warning("No language files found in the 'languages' folder.");
			for (String languageCode : languageCodes) {
				plugin.saveResource("languages/" + languageCode + ".yml", false);
			}
		} else {
			for (File file : files) {
				if (file.getName().equalsIgnoreCase("en_US.yml")) {
					enLangConfig = YamlConfiguration.loadConfiguration(file);
					break;
				}
			}
			for (String languageCode : languageCodes) {
				boolean found = false;
				for (File file : files) {
					if (file.getName().equalsIgnoreCase(languageCode + ".yml")) {
						found = true;
						break;
					}
				}
				if (!found) {
					System.out.println("Saving language file: " + languageCode);
					boolean resourceAvailable = plugin.getResource("languages/" + languageCode + ".yml") != null;
					if (resourceAvailable) {
						plugin.saveResource("languages/" + languageCode + ".yml", false);
					} else {
						plugin.getLogger().warning("Language file '" + languageCode + ".yml' not found in resources.");
					}
				}
			}
		}

		if (enLangConfig == null) {
			plugin.getLogger().warning("English language file 'en_US.yml' not found.");
		}

		langConfig = new YamlConfiguration();

		for (File file : Objects.requireNonNull(langFolder.listFiles())) {
			try {
				langConfig.load(file);
			} catch (Exception e) {
				plugin.getLogger().warning("Error loading language file: " + file.getName());
			}
		}

		plugin.getLogger().info("Loaded " + langConfig.getKeys(true).size() + " language keys.");
	}
}
