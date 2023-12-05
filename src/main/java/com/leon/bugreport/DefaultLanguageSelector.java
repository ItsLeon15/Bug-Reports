package com.leon.bugreport;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static com.leon.bugreport.BugReportLanguage.getText;
import static com.leon.bugreport.BugReportManager.plugin;

public class DefaultLanguageSelector {
	public static final Map<String, String> defaultLanguagesLocal = new HashMap<String, String> () {{
			put("back"										,"Back");
			put("forward"									,"Forward");
			put("pageInfo"									,"Page %currentPage% of %totalPages%");
			put("settings"									,"Settings");
			put("close"										,"Close");
			put("page"										,"Page");
			put("enableDiscordWebhook"						,"Enable Discord Webhook");
			put("enableBugReportNotifications"				,"Enable Bug Report Notifications");
			put("enableCategorySelection"					,"Enable Category Selection");
			put("setMaxReportsPerPlayer"					,"Set Max Reports Per Player");
			put("setLanguage"								,"Set Language");
			put("on"										,"On");
			put("off"										,"Off");
			put("language"									,"Language");
			put("cancelled"									,"Cancelled");
			put("cancel"									,"Cancel");
			put("archive"									,"Archive");
			put("delete"									,"Delete");
			put("otherSettings"								,"Other Settings");
			put("enableTitleMessage"						,"Enable Title Message");
			put("enablePlayerHeads"							,"Enable Player Heads");
			put("missingValueMessage"						,"Missing '%key%' in reportCategories in config.yml");
			put("missingReportCategoryMessage"				,"Missing reportCategories in config.yml");
			put("wentWrongLoadingCategoriesMessage"			,"Something went wrong while loading the report categories");
			put("bugReportNotificationMessage"				,"A new bug report has been submitted by %player%!");
			put("missingDiscordWebhookURLMessage"			,"Missing webhookURL in config.yml");
			put("bugReportConfirmationMessage"				,"Bug report submitted successfully!");
			put("enterBugReportMessageCategory"				,"Please enter your bug report in chat. Type 'cancel' to cancel");
			put("cancelledBugReportMessage"					,"Bug report cancelled");
			put("maxReportsPerPlayerMessage"				,"You have reached the maximum amount of reports you can submit");
			put("bugReportCategoriesNotConfiguredMessage"	,"Bug report categories are not configured");
			put("enterValidNumber"							,"Please enter a valid number");
			put("reportCooldownSuccessMessage"				,"Report cooldown has been set to %time% seconds");
			put("maxReportsPerPlayerSuccessMessage"			,"Max reports per player has been set to %amount%");
			put("languageSetTo"								,"Language set to %language%");
			put("enterMaxReportsPerPlayer"					,"Enter the max reports a player can submit. Or type 'cancel' to cancel");
			put("newReportsMessage"							,"You have %numReports% new reports");
			put("noNewReportsMessage"						,"You have no new reports");
	}};

	public static @NotNull String getTextElseDefault(String language, String key) {
		String text = getText(language, key);
		plugin.getLogger().info("getTextElseDefault: " + language + " " + key + " " + text);
		if (text != null) {
			return text;
		}
		return DefaultLanguageSelector.defaultLanguagesLocal.get(key);
	}
}
