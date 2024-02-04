package com.leon.bugreport;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BugReportLanguage {
    private final static Map<String, ConfigurationSection> languageTexts = new HashMap<>();

    public BugReportLanguage(Plugin plugin, String languageFilePath) {
        loadLanguageTexts(plugin, languageFilePath);
    }

    public static void loadLanguageTexts(@NotNull Plugin plugin, String languageFilePath) {
        File languageFile = new File(plugin.getDataFolder(), languageFilePath);
        if (!languageFile.exists()) {
            plugin.saveResource(languageFilePath, false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(languageFile);
        ConfigurationSection languageSection = config.getConfigurationSection("languages");
        if (languageSection != null) {
            for (String language : languageSection.getKeys(false)) {
                ConfigurationSection textsSection = languageSection.getConfigurationSection(language);
                if (textsSection != null) {
                    languageTexts.put(language, textsSection);
                }
            }
        }
    }

    public static String getEnglishVersionFromLanguage(String displayName) {
		String[] englishTitles = {
				"Back", "Forward", "Page %currentPage% of %totalPages%", "Settings", "Close", "Page",
				"Enable Discord Webhook", "Enable Bug Report Notifications", "Enable Category Selection",
				"Set Max Reports Per Player", "Set Language",
				"On", "Off", "Language",
				"Cancelled", "Cancel", "Archive", "Unarchive", "Delete", "Other Settings",
				"Enable Title Message", "Enable Player Heads"
		};

		for (String lang : languageTexts.keySet()) {
			ConfigurationSection buttonNamesSection = languageTexts.get(lang).getConfigurationSection("buttonNames");
			if (buttonNamesSection != null) {
				String[] currentLangTitles = buttonNamesSection.getKeys(false).stream()
					.map(buttonNamesSection::getString)
					.toArray(String[]::new);
				int index = Arrays.asList(currentLangTitles).indexOf(ChatColor.stripColor(displayName));
				if (index != -1 && index < englishTitles.length) {
 					return englishTitles[index];
				}
			}
		}
		return ChatColor.stripColor(displayName);
	}

    public static @Nullable String getText(String language, String textName) {
        ConfigurationSection texts = languageTexts.get(language);
		return texts != null ? texts.getString(textName) : null;
    }

    public static @Nullable String getTitleFromLanguage(String key) {
        ConfigurationSection languageSection = languageTexts.get(BugReportManager.language);
        if (languageSection != null) {
            ConfigurationSection buttonNamesSection = languageSection.getConfigurationSection("buttonNames");
            return buttonNamesSection != null ? buttonNamesSection.getString(key) : null;
        }
        return null;
    }
}
