package com.leon.bugreport.discord;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.leon.bugreport.BugReportManager.config;
import static com.leon.bugreport.commands.BugReportCommand.chatColorToColor;
import static com.leon.bugreport.commands.BugReportCommand.stringColorToColorCode;

public class LinkDiscord {
    private static final String EMBED_TITLE = "New Bug Report";
    private static final String EMBED_FOOTER_TEXT = "Bug Report V0.8.0";
    private static final String EMBED_THUMBNAIL = "https://www.spigotmc.org/data/resource_icons/110/110732.jpg";
    private static final String EMBED_AUTHOR = "true";
    private static final String EMBED_DATE = "true";
    private static final String EMBED_THUMBNAIL_ENABLED = "true";
    private static final Color EMBED_COLOR = Color.RED;
    private String webhookURL;

    public LinkDiscord(String webhookURL) {
        this.webhookURL = webhookURL;
    }

    public void setWebhookURL(String webhookURL) {
        this.webhookURL = webhookURL;
    }

    public void sendBugReport(String message, String world, String username, String location, String gamemode) {
        if (webhookURL.isEmpty()) {
            System.out.println("Webhook URL is not configured. Bug report not sent to Discord.");
            return;
        }

        String discordEmbedTitle = config.getString("discordEmbedTitle");
        String discordEmbedFooter = config.getString("discordEmbedFooter");
        String discordEmbedThumbnail = config.getString("discordEmbedThumbnail");
        String discordEnableUserAuthor = config.getString("discordEnableUserAuthor");
        String discordIncludeDate = config.getString("discordIncludeDate");
        String discordEnableThumbnail = config.getString("discordEnableThumbnail");
        Color discordEmbedColor = chatColorToColor(stringColorToColorCode(config.getString("discordEmbedColor")));

        discordEmbedFooter = (discordEmbedFooter == null || discordEmbedFooter.isEmpty()) ? EMBED_FOOTER_TEXT : discordEmbedFooter;
        discordEmbedColor = (discordEmbedColor == null) ? EMBED_COLOR : discordEmbedColor;
        discordEmbedThumbnail = (discordEmbedThumbnail == null || discordEmbedThumbnail.isEmpty()) ? EMBED_THUMBNAIL : discordEmbedThumbnail;
        discordEmbedTitle = (discordEmbedTitle == null || discordEmbedTitle.isEmpty()) ? EMBED_TITLE : discordEmbedTitle;
        discordEnableUserAuthor = (discordEnableUserAuthor == null) ? EMBED_AUTHOR : discordEnableUserAuthor;
        discordIncludeDate = (discordIncludeDate == null) ? EMBED_DATE : discordIncludeDate;
        discordEnableThumbnail = (discordEnableThumbnail == null) ? EMBED_THUMBNAIL_ENABLED : discordEnableThumbnail;
        String newUUID = discordEnableUserAuthor.equals ("true") ? getUserIDFromAPI (username) : "Not Available";

        String userAuthorURL = "https://crafatar.com/avatars/" + newUUID;
        String userAuthorIconURL = "https://crafatar.com/avatars/" + newUUID;

        DiscordWebhook webhook = new DiscordWebhook(webhookURL);
        DiscordWebhook.EmbedObject embedObject = new DiscordWebhook.EmbedObject()
            .setTitle(discordEmbedTitle)
            .addField("Username", username, true)
            .addField("UUID", newUUID, true)
            .addField("World", world, true)
            .addField("Location (X, Y, Z)", location, true)
            .addField("Gamemode", gamemode, true)
            .addField("Full Message", message, false)
            .setFooter(discordEmbedFooter, null)
            .setColor(discordEmbedColor);

        if (discordEnableUserAuthor.equals("true")) embedObject.setAuthor(username, userAuthorURL, userAuthorIconURL);
        if (discordIncludeDate.equals("true")) embedObject.setTimestamp();
        if (discordEnableThumbnail.equals("true")) embedObject.setThumbnail (discordEmbedThumbnail);

        webhook.addEmbed(embedObject);

        try {
            webhook.execute();
        } catch (Exception e) {
            System.out.println("Error sending bug report to Discord: " + e.getMessage());
        }
    }

    private @NotNull String getUserIDFromAPI(String username) {
        String url = "https://playerdb.co/api/player/minecraft/" + username;
        StringBuilder content = new StringBuilder();
        try {
            URL playerdb = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) playerdb.openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "BugReport/0.8.0");
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
        } catch (Exception e) {
            System.out.println("Error getting UUID from API: " + e.getMessage());
        }
        return content.toString().split("\"raw_id\":\"")[1].split("\"")[0];
    }
}