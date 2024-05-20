package com.leon.bugreport;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public class BugReportLanguage {
	private static File langFolder;
	private static String languageCode;
	private static YamlConfiguration langConfig;
	private static YamlConfiguration enLangConfig;

	public BugReportLanguage(@NotNull Plugin plugin) {
		langFolder = new File(plugin.getDataFolder(), "lang");
		languageCode = getLanguageCode(plugin);

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

		if (langFolder.listFiles() == null || langFolder.listFiles().length == 0) {
			plugin.getLogger().warning("No language files found in the 'languages' folder.");
			return;
		}

		for (File file : langFolder.listFiles()) {
			if (file.getName().equalsIgnoreCase("en_US.yml")) {
				enLangConfig = YamlConfiguration.loadConfiguration(file);
				break;
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

	@Contract("_ -> !null")
	private static String getLanguageCode(@NotNull Plugin plugin) {
		return plugin.getConfig().getString("language", "en_US");
	}

	public void setLanguageCode(@NotNull Plugin plugin, String languageCode) {
		plugin.getConfig().set("language", languageCode);
		plugin.saveConfig();
	}
}
