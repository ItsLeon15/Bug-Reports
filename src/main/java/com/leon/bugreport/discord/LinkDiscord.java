package com.leon.bugreport.discord;

import com.leon.bugreport.logging.ErrorMessages;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;

import static com.leon.bugreport.API.ErrorClass.logErrorMessage;
import static com.leon.bugreport.BugReportManager.config;
import static com.leon.bugreport.BugReportManager.plugin;
import static com.leon.bugreport.commands.BugReportCommand.chatColorToColor;
import static com.leon.bugreport.commands.BugReportCommand.stringColorToColorCode;

public class LinkDiscord {
	private static final String EMBED_TITLE = "New Bug Report";
	private static final String EMBED_FOOTER_TEXT = "Bug Report V0.14.0";
	private static final String EMBED_THUMBNAIL = "https://www.spigotmc.org/data/resource_icons/110/110732.jpg";
	private static final Color EMBED_COLOR = Color.YELLOW;
	private static boolean errorLogged = false;
	private static String webhookURL;

	public LinkDiscord(String webhookURL) {
		LinkDiscord.webhookURL = webhookURL;
	}

	private static void modifyDiscordNotification(
			String playerUsername,
			String playerUUID,
			String playerWorld,
			String playerLocation,
			String playerGamemode,
			String playerStatus,
			Integer category,
			String serverName,
			String fullMessage,
			String notificationMessage,
			Color color,
			String bugReportDiscordWebhookID
	) {
		String newCategory;

		playerUsername = validateParam("playerUsername", playerUsername, "Unknown Player");
		playerUUID = validateParam("playerUUID", playerUUID, "");
		playerWorld = validateParam("playerWorld", playerWorld, "Unknown World");
		playerLocation = validateParam("playerLocation", playerLocation, "Unknown Location");
		playerGamemode = validateParam("playerGamemode", playerGamemode, "Unknown Gamemode");
		playerStatus = validateParam("playerStatus", playerStatus, "0");
		newCategory = validateParam("category", getCategoryName(category), "Unknown Category");
		serverName = validateParam("serverName", serverName, "Unknown Server");
		fullMessage = validateParam("fullMessage", fullMessage, "");
		notificationMessage = validateParam("notificationMessage", notificationMessage, "Bug Report");

		if (color == null) {
			String errorMessage = "Error: color is null";
			plugin.getLogger().warning(errorMessage);
			logErrorMessage(errorMessage);
			color = Color.RED;
		}

		bugReportDiscordWebhookID = validateParam("bugReportDiscordWebhookID", bugReportDiscordWebhookID, "");

		String webhookURL = config.getString("webhookURL", "");
		if (webhookURL.isEmpty()) {
			String errorMessage = ErrorMessages.getErrorMessage(24);
			plugin.getLogger().warning(errorMessage);
			logErrorMessage(errorMessage);
			return;
		}

		URL url;
		try {
			url = new URL(webhookURL + "/messages/" + bugReportDiscordWebhookID);
		} catch (MalformedURLException e) {
			String errorMessage = ErrorMessages.getErrorMessageWithAdditionalMessage(25, "Error editing Discord message: " + e.getMessage());
			plugin.getLogger().warning(errorMessage);
			logErrorMessage(errorMessage);
			return;
		}

		String host = url.getHost();
		String path = url.getPath() + (url.getQuery() != null ? "?" + url.getQuery() : "");
		int port = url.getPort() == -1 ? 443 : url.getPort();

		try {
			String discordEmbedFooter = config.getString("discordEmbedFooter", EMBED_FOOTER_TEXT);
			String discordEmbedThumbnail = config.getString("discordEmbedThumbnail", EMBED_THUMBNAIL);

			StringBuilder jsonBuilder = new StringBuilder();
			jsonBuilder.append("{\"embeds\":[{");
			jsonBuilder.append("\"title\":\"").append(escapeJson(notificationMessage)).append("\",");
			jsonBuilder.append("\"color\":").append(color.getRGB() & 0xFFFFFF).append(",");
			jsonBuilder.append("\"footer\":{\"text\":\"").append(escapeJson(discordEmbedFooter)).append("\"},");
			jsonBuilder.append("\"thumbnail\":{\"url\":\"").append(escapeJson(discordEmbedThumbnail)).append("\"}");

			String discordEnableUserAuthor = config.getString("discordEnableUserAuthor");
			if (Objects.equals(discordEnableUserAuthor, "true")) {
				String userAuthorURL = "https://crafatar.com/avatars/" + playerUUID;
				jsonBuilder.append(",\"author\":{\"name\":\"").append(escapeJson(playerUsername)).append("\",");
				jsonBuilder.append("\"url\":\"").append(escapeJson(userAuthorURL)).append("\",");
				jsonBuilder.append("\"icon_url\":\"").append(escapeJson(userAuthorURL)).append("\"}");
			}

			String discordIncludeDate = config.getString("discordIncludeDate");
			if (Objects.equals(discordIncludeDate, "true")) {
				jsonBuilder.append(",\"timestamp\":\"").append(new Date().toInstant().toString()).append("\"");
			}

			List<Map<?, ?>> discordEmbedFields = config.getMapList("discordEmbedFields");
			if (!discordEmbedFields.isEmpty()) {
				jsonBuilder.append(",\"fields\":[");

				List<DiscordEmbedDetails> discordDetails = getDiscordEmbedDetails(discordEmbedFields);

				boolean firstField = true;
				for (DiscordEmbedDetails detail : discordDetails) {
					if (!firstField) {
						jsonBuilder.append(",");
					}
					firstField = false;

					String name = detail.getName();
					String detailValue = detail.getValue();
					String value = getValueForField(
							detailValue,
							playerUsername,
							playerWorld,
							playerLocation,
							playerGamemode,
							getStatusNameFromID(Integer.valueOf(playerStatus)),
							newCategory,
							fullMessage,
							serverName
					);
					Boolean inline = detail.getInline();

					jsonBuilder.append("{");
					jsonBuilder.append("\"name\":\"").append(escapeJson(name)).append("\",");
					jsonBuilder.append("\"value\":\"").append(escapeJson(value)).append("\",");
					jsonBuilder.append("\"inline\":").append(inline);
					jsonBuilder.append("}");
				}

				jsonBuilder.append("]");
			}

			jsonBuilder.append("}]}");

			String jsonPayload = jsonBuilder.toString();
			byte[] payloadBytes = jsonPayload.getBytes(StandardCharsets.UTF_8);

			SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(host, port);

			String requestBuilder = "PATCH " + path + " HTTP/1.1\r\n" +
					"Host: " + host + "\r\n" +
					"Content-Type: application/json\r\n" +
					"User-Agent: BugReport/0.14.0\r\n" +
					"Content-Length: " + payloadBytes.length + "\r\n" +
					"Connection: close\r\n" +
					"\r\n";

			OutputStream out = socket.getOutputStream();
			out.write(requestBuilder.getBytes(StandardCharsets.UTF_8));
			out.write(payloadBytes);
			out.flush();

			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String inputLine;
			StringBuilder response = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine).append("\n");
			}

			in.close();
			out.close();
			socket.close();

			if (!response.toString().contains("HTTP/1.1 2")) {
				String errorMessage = ErrorMessages.getErrorMessageWithAdditionalMessage(25, "Discord API error: " + response);
				plugin.getLogger().warning(errorMessage);
				logErrorMessage(errorMessage);
			}
		} catch (Exception e) {
			String errorMessage = ErrorMessages.getErrorMessageWithAdditionalMessage(25, "Error sending Discord Edit message: " + e.getMessage());
			plugin.getLogger().warning(errorMessage);
			logErrorMessage(errorMessage);
		}
	}

	private static List<DiscordEmbedDetails> getDiscordEmbedDetails(List<Map<?, ?>> discordEmbedFields) {
		List<DiscordEmbedDetails> discordDetails = new ArrayList<>();
		for (Map<?, ?> field : discordEmbedFields) {
			String name = (String) field.get("name");
			int id = (int) field.get("id");
			String value = (String) field.get("value");
			boolean inline = (boolean) field.get("inline");
			discordDetails.add(new DiscordEmbedDetails(name, id, value, inline));
		}

		discordDetails.sort(Comparator.comparingInt(DiscordEmbedDetails::getId));
		return discordDetails;
	}

	private static String validateParam(String paramName, String value, String defaultValue) {
		if (value == null) {
			String errorMessage = "Error: " + paramName + " is null";
			plugin.getLogger().warning(errorMessage);
			logErrorMessage(errorMessage);
			return defaultValue;
		}
		return value;
	}

	private static String getStatusNameFromID(Integer statusID) {
		if (statusID == 0) {
			return "Active";
		}

		List<Map<?, ?>> statuses = config.getMapList("statuses");
		for (Map<?, ?> statusMap : statuses) {
			if (statusMap.get("id").equals(statusID)) {
				return (String) statusMap.get("name");
			}
		}
		return "Unknown Status";
	}

	private static @NotNull String escapeJson(String text) {
		if (text == null) return "";
		return text.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\r", "\\r")
				.replace("\t", "\\t");
	}

	public static void modifyNotification(
			String playerUsername,
			String playerUUID,
			String playerWorld,
			String playerLocation,
			String playerGamemode,
			String playerStatus,
			Integer category,
			String serverName,
			String fullMessage,
			String bugReportDiscordWebhookID,
			Color color,
			String notificationMessage
	) {
		if (webhookURL == null || webhookURL.isEmpty()) {
			plugin.getLogger().info("Webhook URL is not configured. Notification not sent to Discord.");
			return;
		}

		modifyDiscordNotification(
				playerUsername,
				playerUUID,
				playerWorld,
				playerLocation,
				playerGamemode,
				playerStatus,
				category,
				serverName,
				fullMessage,
				notificationMessage,
				color,
				bugReportDiscordWebhookID
		);
	}

	private static @NotNull String getValueForField(
			@NotNull String fieldValue,
			String username,
			String world,
			String location,
			String gamemode,
			String status,
			String category,
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
		replacements.put("%report_status%", status);
		replacements.put("%report_gamemode%", gamemode);
		replacements.put("%report_category%", category);
		replacements.put("%report_server_name%", serverName);
		replacements.put("%report_full_message%", message);

		for (Map.Entry<String, String> entry : replacements.entrySet()) {
			fieldValue = fieldValue.replace(entry.getKey(), entry.getValue());
		}

		return fieldValue;
	}

	private static String getCategoryName(Integer category) {
		if (category == null) {
			return "Unknown Category";
		}

		List<Map<?, ?>> categoryList = config.getMapList("reportCategories");
		for (Map<?, ?> categoryMap : categoryList) {
			if (categoryMap.get("id").equals(category)) {
				return (String) categoryMap.get("name");
			}
		}

		return "Unknown Category";
	}

	private static @NotNull String getUserIDFromAPI(String username) {
		String url = "https://playerdb.co/api/player/minecraft/" + username;
		StringBuilder content = new StringBuilder();

		try {
			URL playerdb = new URL(url);
			HttpURLConnection connection = (HttpURLConnection) playerdb.openConnection();

			connection.setRequestMethod("GET");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("User-Agent", "BugReport/0.14.0");
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
				String errorMessage = ErrorMessages.getErrorMessageWithAdditionalMessage(42, e.getMessage());

				plugin.getLogger().warning(errorMessage);
				logErrorMessage(errorMessage);

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

	public void setWebhookURL(String webhookURL) {
		LinkDiscord.webhookURL = webhookURL;
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

	private String sendEmptyEmbedOrDefault(String username, DiscordWebhook.EmbedObject @NotNull ... existingEmbedObject) {
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

		return sendEmbed(embedObject);
	}

	public String sendBugReport(String message, String world, String username, String location, String gamemode, Integer category, String serverName) {
		if (webhookURL == null || webhookURL.isEmpty()) {
			plugin.getLogger().info("Webhook URL is not configured. Bug report not sent to Discord.");
			return message;
		}

		if (!config.contains("discordEmbedFields")) {
			String errorMessage = ErrorMessages.getErrorMessage(1);
			plugin.getLogger().warning(errorMessage);
			logErrorMessage(errorMessage);
		}

		List<Map<?, ?>> discordEmbedFields = config.getMapList("discordEmbedFields");
		if (discordEmbedFields.isEmpty()) {
			String errorMessage = ErrorMessages.getErrorMessage(2);
			plugin.getLogger().warning(errorMessage);
			logErrorMessage(errorMessage);
			sendEmptyEmbedOrDefault(username);
			return message;
		}

		return sendDiscordMessageEmbedFull(message, world, username, location, gamemode, category, serverName, discordEmbedFields);
	}

	private String sendDiscordMessageEmbedFull(
			String message,
			String world,
			String username,
			String location,
			String gamemode,
			Integer category,
			String serverName,
			@NotNull List<Map<?, ?>> discordEmbedFields
	) {
		List<DiscordEmbedDetails> discordDetails = getDiscordEmbedDetails(discordEmbedFields);
		DiscordWebhook.EmbedObject embedObject = generateDefaultEmbed();

		for (DiscordEmbedDetails detail : discordDetails) {
			String name = detail.getName();
			String detailValue = detail.getValue();

			String newCategory = getCategoryName(category);

			String value = getValueForField(detailValue, username, world, location, gamemode, "Active", newCategory, message, serverName);

			Boolean inline = detail.getInline();
			embedObject.addField(name, value, inline);
		}

		return sendEmptyEmbedOrDefault(username, embedObject);
	}

	private String sendEmbed(DiscordWebhook.EmbedObject embedObject) {
		DiscordWebhook webhook = new DiscordWebhook(webhookURL);
		webhook.addEmbed(embedObject);
		String discordWebhookResult = "";

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
					if (config.getString("discordPingMessage") != null && !Objects.requireNonNull(config.getString("discordPingMessage")).isEmpty()) {
						content.insert(0, config.getString("discordPingMessage") + " ");
					}

					webhook.setContent(content.toString().trim());
				}
			} catch (Exception e) {
				String errorMessage = ErrorMessages.getErrorMessageWithAdditionalMessage(45, e.getMessage());

				plugin.getLogger().warning(errorMessage);
				logErrorMessage(errorMessage);
			} finally {
				try {
					discordWebhookResult = webhook.execute();
				} catch (IOException e) {
					String errorMessage = ErrorMessages.getErrorMessageWithAdditionalMessage(25, e.getMessage());

					plugin.getLogger().warning(errorMessage);
					logErrorMessage(errorMessage);
				}
			}
		} else {
			try {
				discordWebhookResult = webhook.execute();
			} catch (IOException e) {
				String errorMessage = ErrorMessages.getErrorMessageWithAdditionalMessage(25, e.getMessage());

				plugin.getLogger().warning(errorMessage);
				logErrorMessage(errorMessage);
			}
		}

		return discordWebhookResult;
	}
}
