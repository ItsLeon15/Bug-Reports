package com.leon.bugreport;

import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.HashMap;
import java.util.Map;

import static com.leon.bugreport.BugReportLanguage.getText;

public class DefaultLanguageSelector {
	public static final Map<String, String> defaultLanguagesLocal = new HashMap<>() {
		@Serial
		private static final long serialVersionUID = 8445192709181880960L;

		{
			put("back", "Back");
			put("forward", "Forward");
			put("pageInfo", "Page %currentPage% of %totalPages%");
			put("settings", "Settings");
			put("close", "Close");
			put("page", "Page");
			put("enableDiscordWebhook", "Enable Discord Webhook");
			put("enableBugReportNotifications", "Enable Bug Report Notifications");
			put("enableCategorySelection", "Enable Category Selection");
			put("setMaxReportsPerPlayer", "Set Max Reports Per Player");
			put("setLanguage", "Set Language");
			put("on", "On");
			put("off", "Off");
			put("language", "Language");
			put("cancelled", "Cancelled");
			put("cancel", "Cancel");
			put("archive", "Archive");
			put("delete", "Delete");
			put("otherSettings", "Other Settings");
			put("enableTitleMessage", "Enable Title Message");
			put("enablePlayerHeads", "Enable Player Heads");
			put("enableReportBook", "Enable Report Book");
			put("viewStatus", "View Status");
			put("editStatus", "Edit Status");
			put("renameStatus", "Rename Status");
			put("removeStatus", "Remove Status");
			put("statusName", "Status Name");
			put("statusMaterial", "Status Material");
			put("statusColor", "Status Color");
			put("statusDescription", "Status Description");
			put("statusSelection", "Status Selection");
			put("confirmationDelete", "Delete Bug Report %bugReportID%?");
			put("confirmationArchive", "Archive Bug Report %bugReportID%?");
			put("missingValueMessage", "Missing '%key%' in reportCategories in config.yml");
			put("missingReportCategoryMessage", "Missing reportCategories in config.yml");
			put("wentWrongLoadingCategoriesMessage", "Something went wrong while loading the report categories");
			put("bugReportNotificationMessage", "A new bug report has been submitted by %player%!");
			put("missingDiscordWebhookURLMessage", "Missing webhookURL in config.yml");
			put("bugReportConfirmationMessage", "Bug report submitted successfully!");
			put("enterBugReportMessageCategory", "Please enter your bug report in chat. Type 'cancel' to cancel");
			put("cancelledBugReportMessage", "Bug report cancelled");
			put("maxReportsPerPlayerMessage", "You have reached the maximum amount of reports you can submit");
			put("bugReportCategoriesNotConfiguredMessage", "Bug report categories are not configured");
			put("enterValidNumber", "Please enter a valid number");
			put("reportCooldownSuccessMessage", "Report cooldown has been set to %time% seconds");
			put("maxReportsPerPlayerSuccessMessage", "Max reports per player has been set to %amount%");
			put("languageSetTo", "Language set to %language%");
			put("enterMaxReportsPerPlayer", "Enter the max reports a player can submit. Or type 'cancel' to cancel");
			put("newReportsMessage", "You have %numReports% new reports");
			put("noNewReportsMessage", "You have no new reports");
		}
	};

	public static @NotNull String getTextElseDefault(String language, String key) {
		String text = getText(language, key);
		if (text != null) {
			return text;
		}
		return DefaultLanguageSelector.defaultLanguagesLocal.get(key);
	}
}
