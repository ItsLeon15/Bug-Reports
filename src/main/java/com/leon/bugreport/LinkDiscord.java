package com.leon.bugreport;

import java.util.UUID;

public class LinkDiscord {
    private static final String EMBED_TITLE = "New Bug Report";
    private static final String EMBED_FOOTER_TEXT = "Bug Report V0.4.0";

    private String webhookURL;

    public LinkDiscord(String webhookURL) {
        this.webhookURL = webhookURL;
    }

    public void setWebhookURL(String webhookURL) {
        this.webhookURL = webhookURL;
    }

    public void sendBugReport(String message, UUID UUID, String world, String username) {
        if (webhookURL.isEmpty()) {
            System.out.println("Webhook URL is not configured. Bug report not sent to Discord.");
            return;
        }

        DiscordWebhook webhook = new DiscordWebhook(webhookURL);
        webhook.setContent(EMBED_TITLE);
        webhook.addEmbed(new DiscordWebhook.EmbedObject()
                .addField("Username", username, false)
                .addField("UUID", String.valueOf(UUID), false)
                .addField("World", world, false)
                .addField("Full Message", message, false)
                .setFooter(EMBED_FOOTER_TEXT, null)
        );

        try {
            webhook.execute();
        } catch (Exception e) {
            System.out.println("Error sending bug report to Discord: " + e.getMessage());
        }
    }
}