package com.leon.bugreport.discord;

import com.leon.bugreport.BugReportPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.*;

import static com.leon.bugreport.API.ErrorClass.logErrorMessage;
import static com.leon.bugreport.BugReportManager.config;
import static com.leon.bugreport.BugReportManager.plugin;
import static com.leon.bugreport.commands.BugReportCommand.chatColorToColor;
import static com.leon.bugreport.commands.BugReportCommand.stringColorToColorCode;

public class LinkDiscord {
	private static final String EMBED_TITLE = "New Bug Report";
	private static final String EMBED_FOOTER_TEXT = "Bug Report V0.12.4";
	private static final String EMBED_THUMBNAIL = "https://www.spigotmc.org/data/resource_icons/110/110732.jpg";
	private static final Color EMBED_COLOR = Color.YELLOW;
	private static boolean errorLogged = false;
	private String webhookURL;

	public LinkDiscord(String webhookURL) {
		this.webhookURL = webhookURL;
	}

	public void setWebhookURL(String webhookURL) {
		this.webhookURL = webhookURL;
	}

	private DiscordWebhook.EmbedObject generateDefaultEmbed() {
		String discordEmbedTitle = config.getString("discordEmbedTitle");
		String discordEmbedFooter = config.getString("discordEmbedFooter");
		String discordEmbedThumbnail = config.getString("discordEmbedThumbnail");
		Color discordEmbedColor = chatColorToColor(stringColorToColorCode(config.getString("discordEmbedColor")));

		discordEmbedFooter = (discordEmbedFooter == null || discordEmbedFooter.isEmpty()) ? EMBED_FOOTER_TEXT : discordEmbedFooter;
		discordEmbedColor = (discordEmbedColor == null) ? EMBED_COLOR : discordEmbedColor;
		discordEmbedTitle = (discordEmbedTitle == null || discordEmbedTitle.isEmpty()) ? EMBED_TITLE : discordEmbedTitle;
		discordEmbedThumbnail = (discordEmbedThumbnail == null || discordEmbedThumbnail.isEmpty()) ? EMBED_THUMBNAIL : discordEmbedThumbnail;

		return new DiscordWebhook.EmbedObject().setTitle(discordEmbedTitle).setFooter(discordEmbedFooter, null).setColor(discordEmbedColor).setThumbnail(discordEmbedThumbnail);
	}

	private void sendEmptyEmbedOrDefault(String username, DiscordWebhook.EmbedObject @NotNull ... existingEmbedObject) {
		DiscordWebhook.EmbedObject embedObject = existingEmbedObject.length > 0 ? existingEmbedObject[0] : generateDefaultEmbed();

		String discordEnableUserAuthor = config.getString("discordEnableUserAuthor");
		String discordIncludeDate = config.getString("discordIncludeDate");
		String discordEnableThumbnail = config.getString("discordEnableThumbnail");
		String discordEmbedThumbnail = config.getString("discordEmbedThumbnail");
		String userAuthorURL = "https://crafatar.com/avatars/" + getUserIDFromAPI(username);

		if (Objects.equals(discordEnableUserAuthor, "true")) {
			embedObject.setAuthor(username, userAuthorURL, userAuthorURL);
		}

		if (Objects.equals(discordIncludeDate, "true")) {
			embedObject.setTimestamp();
		}

		if (Objects.equals(discordEnableThumbnail, "true")) {
			embedObject.setThumbnail(discordEmbedThumbnail);
		}

		sendEmbed(embedObject);
	}

	public void sendBugReport(String message, String world, String username, String location, String gamemode, Integer category, String serverName) {
		if (webhookURL == null || webhookURL.isEmpty()) {
			plugin.getLogger().info("Webhook URL is not configured. Bug report not sent to Discord.");
			return;
		}

		if (!config.contains("discordEmbedFields")) {
			plugin.getLogger().warning("discordEmbedFields key is not present in the config. Sending an empty embed.");
			logErrorMessage("discordEmbedFields key is not present in the config. Sending an empty embed.");
		}

		List<Map<?, ?>> discordEmbedFields = config.getMapList("discordEmbedFields");
		if (discordEmbedFields.isEmpty()) {
			plugin.getLogger().warning("discordEmbedFields is empty in the config. Bug report not sent to Discord.");
			logErrorMessage("discordEmbedFields is empty in the config. Bug report not sent to Discord.");
			sendEmptyEmbedOrDefault(username);
			return;
		}

		sendDiscordMessageEmbedFull(message, world, username, location, gamemode, category, serverName, discordEmbedFields);
	}

	private void sendDiscordMessageEmbedFull(
			String message,
			String world,
			String username,
			String location,
			String gamemode,
			Integer category,
			String serverName,
			@NotNull List<Map<?, ?>> discordEmbedFields
	) {
		List<DiscordEmbedDetails> discordDetails = new ArrayList<>();

		for (Map<?, ?> field : discordEmbedFields) {
			String name = (String) field.get("name");
			int id = (int) field.get("id");
			String value = (String) field.get("value");
			boolean inline = (boolean) field.get("inline");
			discordDetails.add(new DiscordEmbedDetails(name, id, value, inline));
		}

		discordDetails.sort(Comparator.comparingInt(DiscordEmbedDetails::getId));

		DiscordWebhook.EmbedObject embedObject = generateDefaultEmbed();

		for (DiscordEmbedDetails detail : discordDetails) {
			String name = detail.getName();
			String detailValue = detail.getValue();

			String value = getValueForField(detailValue, username, world, location, gamemode, category, message, serverName);

			Boolean inline = detail.getInline();
			embedObject.addField(name, value, inline);
		}

		sendEmptyEmbedOrDefault(username, embedObject);
	}

	private @NotNull String getValueForField(
			@NotNull String fieldValue,
			String username,
			String world,
			String location,
			String gamemode,
			Integer category,
			String message,
			String serverName
	) {
		Player player = Bukkit.getPlayer(username);

		if (player != null && PlaceholderAPI.containsPlaceholders(fieldValue)) {
			fieldValue = PlaceholderAPI.setPlaceholders(player, fieldValue);
		}

		Map<String, String> replacements = new HashMap<>();
		replacements.put("%report_username%", username);
		replacements.put("%report_uuid%", getUserIDFromAPI(username));
		replacements.put("%report_world%", world);
		replacements.put("%report_location%", location);
		replacements.put("%report_status%", "Active");
		replacements.put("%report_gamemode%", gamemode);
		replacements.put("%report_category%", getCategoryName(category));
		replacements.put("%report_server_name%", serverName);
		replacements.put("%report_full_message%", message);

		for (Map.Entry<String, String> entry : replacements.entrySet()) {
			fieldValue = fieldValue.replace(entry.getKey(), entry.getValue());
		}

		return fieldValue;
	}

	private void sendEmbed(DiscordWebhook.EmbedObject embedObject) {
		DiscordWebhook webhook = new DiscordWebhook(webhookURL);
		webhook.addEmbed(embedObject);

		if (config.getBoolean("discordEnablePing")) {
			try {
				List<String> discordPingMembers = config.getStringList("discordPingMembers");
				List<String> discordPingRoles = config.getStringList("discordPingRoles");

				StringBuilder membersToPing = new StringBuilder();
				StringBuilder rolesToPing = new StringBuilder();

				if (!discordPingMembers.isEmpty()) {
					for (String member : discordPingMembers) {
						String trimmedMember = member.trim();
						if (!trimmedMember.isEmpty() && !trimmedMember.equals("<@>") && !trimmedMember.equals("@")) {
							if (!trimmedMember.startsWith("<@")) {
								membersToPing.append("<@").append(trimmedMember).append(">");
							} else {
								membersToPing.append(trimmedMember);
							}
							membersToPing.append(" ");
						}
					}
				}

				if (!discordPingRoles.isEmpty()) {
					for (String role : discordPingRoles) {
						String trimmedRole = role.trim();
						if (!trimmedRole.isEmpty() && !trimmedRole.equals("<@&>") && !trimmedRole.equals("&")) {
							if (!trimmedRole.startsWith("<@&")) {
								rolesToPing.append("<@&").append(trimmedRole).append(">");
							} else {
								rolesToPing.append(trimmedRole);
							}
							rolesToPing.append(" ");
						}
					}
				}

				StringBuilder content = new StringBuilder();
				if (!rolesToPing.isEmpty() && rolesToPing.toString().contains("<@&") && rolesToPing.toString().contains(">")) {
					content.append(rolesToPing.toString().trim()).append(" ");
				}
				if (!membersToPing.isEmpty() && membersToPing.toString().contains("<@") && membersToPing.toString().contains(">")) {
					content.append(membersToPing.toString().trim()).append(" ");
				}

				if (!content.isEmpty()) {
					if (config.getString("discordPingMessage") != null && !config.getString("discordPingMessage").isEmpty()) {
						content.insert(0, config.getString("discordPingMessage") + " ");
					}

					webhook.setContent(content.toString().trim());
				}
			} catch (Exception e) {
				throwException("Error sending additional pings to Discord: " + e.getMessage());
			} finally {
				try {
					webhook.execute();
				} catch (IOException e) {
					throwException("Error sending bug report to Discord: " + e.getMessage());
				}
			}
		} else {
			try {
				webhook.execute();
			} catch (IOException e) {
				throwException("Error sending bug report to Discord: " + e.getMessage());
			}
		}
	}

	private void throwException(String message) {
		plugin.getLogger().warning(message);
		logErrorMessage(message);
	}

	private String getCategoryName(Integer category) {
		List<Map<?, ?>> categoryList = config.getMapList("reportCategories");
		for (Map<?, ?> categoryMap : categoryList) {
			if (categoryMap.get("id").equals(category)) {
				return (String) categoryMap.get("name");
			}
		}
		return "Unknown Category";
	}

	private @NotNull String getUserIDFromAPI(String username) {
		String url = "https://playerdb.co/api/player/minecraft/" + username;
		StringBuilder content = new StringBuilder();

		try {
			URL playerdb = new URL(url);
			HttpURLConnection connection = (HttpURLConnection) playerdb.openConnection();

			connection.setRequestMethod("GET");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("User-Agent", "BugReport/0.12.4");
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);
			connection.setDoOutput(true);

			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				content.append(inputLine);
			}
			in.close();
			connection.disconnect();
			errorLogged = false;
		} catch (Exception e) {
			if (!errorLogged) {
				BugReportPlugin.getPlugin().getLogger().warning("Error getting UUID from API: " + e.getMessage());
				logErrorMessage("Error getting UUID from API: " + e.getMessage());
				errorLogged = true;
			}
			return "Unknown UUID";
		}

		String[] splitContent = content.toString().split("\"raw_id\":\"");

		if (splitContent.length > 1) {
			return splitContent[1].split("\"")[0];
		} else {
			return "Unknown UUID";
		}
	}
}
