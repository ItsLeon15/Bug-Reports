package com.leon.bugreport;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BugReportLanguage {
    private static Map<String, Map<String, String>> languageTexts = null;

    public BugReportLanguage(Plugin plugin, String languageFilePath) {
        languageTexts = new HashMap<>();
        loadLanguageTexts();
    }

    static void loadLanguageTexts() {
        File languageFile = new File(BugReportManager.plugin.getDataFolder(), "languages.yml");
        if (!languageFile.exists()) {
            BugReportManager.plugin.saveResource("languages.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(languageFile);
        ConfigurationSection languageSection = config.getConfigurationSection("languages");
        if (languageSection != null) {
            for (String language : languageSection.getKeys(false)) {
                ConfigurationSection textsSection = languageSection.getConfigurationSection(language);
                if (textsSection != null) {
                    Map<String, String> texts = new HashMap<>();
                    for (String text : textsSection.getKeys(false)) {
                        String value = textsSection.getString(text);
                        if (value != null) {
                            value = value.replace("\"", "");
                            texts.put(text, value);
                        }
                    }
                    languageTexts.put(language, texts);
                }
            }
        }
    }

    public String getText(String language, String textName) {
        Map<String, String> texts = languageTexts.get(language);
        if (texts != null) {
            return texts.get(textName);
        }
        return null;
    }

    public static String[] getTitleFromLanguage() {
        Map<String, String[]> buttonNames = new HashMap<>();
        buttonNames.put("en", new String[]{
                "Back", "Forward", "Page %currentPage% of %totalPages%", "Settings", "Close", "Page", // 0-5
                "Enable Discord Webhook", "Enable Bug Report Notifications", "Enable Category Selection", // 6-8
                "Set Max Reports Per Player", "Set Language", // 9-10
                "On", "Off", "Language", // 11-13
                "Cancelled", "Cancel" // 14-15
        });
        buttonNames.put("es", new String[]{
                "Atrás", "Adelante", "Página %currentPage% de %totalPages%", "Ajustes", "Cerrar", "Página", // 0-5
                "Habilitar Webhook de Discord", "Habilitar Notificaciones de Informes de Errores", "Habilitar Selección de Categoría", // 6-8
                "Establecer Máximo de Informes por Jugador", "Establecer Idioma", // 9-10
                "Activado", "Desactivado", "Idioma", // 11-13
                "Cancelado", "Cancelar" // 14-15
        });
        buttonNames.put("fr", new String[]{
                "Dos", "Avant", "Page %currentPage% sur %totalPages%", "Paramètres", "Fermer", "Page", // 0-5
                "Activer le Webhook Discord", "Activer les Notifications de Rapport de Bug", "Activer la Sélection de Catégorie", // 6-8
                "Définir le Nombre Maximal de Rapports par Joueur", "Définir la Langue", // 9-10
                "Activé", "Désactivé", "Langue", // 11-13
                "Annulé", "Annuler" // 14-15
        });
        buttonNames.put("de", new String[]{
                "Zurück", "Vorwärts", "Seite %currentPage% von %totalPages%", "Einstellungen", "Schließen", "Seite", // 0-5
                "Discord Webhook aktivieren", "Bug Report Benachrichtigungen aktivieren", "Kategorieauswahl aktivieren", // 6-8
                "Maximale Berichte pro Spieler festlegen", "Sprache festlegen",
                "Aktiviert", "Deaktiviert", "Sprache", // 11-13
                "Abgebrochen", "Abbrechen" // 14-15
        });
        buttonNames.put("it", new String[]{
                "Indietro", "Inoltrare", "Pagina %currentPage% di %totalPages%", "Impostazioni", "Chiudere", "Pagina", // 0-5
                "Abilita Discord Webhook", "Abilita Notifiche Bug Report", "Abilita Selezione Categoria", // 6-8
                "Imposta Massimo Report per Giocatore", "Imposta Lingua", // 9-10
                "Attivato", "Disattivato", "Lingua", // 11-13
                "Annullato", "Annulla" // 14-15
        });

        return buttonNames.get(BugReportManager.config.getString("language"));
    }

    public static String[] getEnglishTitles() {
        Map<String, String[]> buttonNames = new HashMap<>();
        buttonNames.put("en", new String[]{
                "Back", "Forward", "Page %currentPage% of %totalPages%", "Settings", "Close", "Page", // 0-5
                "Enable Discord Webhook", "Enable Bug Report Notifications", "Enable Category Selection", // 6-8
                "Set Max Reports Per Player", "Set Language", // 9-10
                "On", "Off", "Language", // 11-13
                "Cancelled", "Cancel" // 14-15
        });
        return buttonNames.get("en");
    }

    public static String getEnglishVersionFromLanguage(String displayName) {
        String[] allTitles = BugReportLanguage.getTitleFromLanguage();
        String customDisplayName = "";

        for (String title : allTitles) {
            String strippedTitle = ChatColor.stripColor(title);
            if (displayName.contains(strippedTitle)) {
                int index = Arrays.asList(allTitles).indexOf(title);
                String[] enTitles = BugReportLanguage.getEnglishTitles();
                if (enTitles != null && enTitles.length > index) {
                    customDisplayName = enTitles[index];
                    break;
                }
            }
        }
        return customDisplayName;
    }
}

