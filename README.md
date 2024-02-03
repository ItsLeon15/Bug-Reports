## What is BugReport?
BugReport is a tool that helps server owners manage any bug reports found by players that can be accessed by a GUI. This is especially helpful as information about where the player is located and their UUID is automatically stored and shown to admins.

## Commands
- bugreport <message> - **Adds a new bug report either via message or a GUI for the categories**.
- buglist - **This shows a GUI with all the reports made on the settings page**.
- buglistsettings - **This shows the Bug List Settings GUI**.
- buglistarchived - **See all of the bug reports that have been archived in a GUI**.
- buglinkdiscord <Webhook URL> - **Links a Discord Webhook to send all Bug Reports to**.

## Permissions
- bugreport.admin - **Allows a user to access the buglist command**.
- bugreport.notify - **Let a player be notified every time a bug report is sent**.

## Bug List GUI
![https://www.spigotmc.org/attachments/bug-report-list-gui-png.795626/](https://www.spigotmc.org/attachments/bug-report-list-gui-png.795626/)

## Bug Details GUI
![https://www.spigotmc.org/attachments/bug-report-details-png.795652/](https://www.spigotmc.org/attachments/bug-report-details-png.795652/)

## Plan Support
Bug Report now supports the Plan API. To use Bug Report with Plan, you will need to install Plan onto your Spigot Server and it should set up everything automatically.

![https://proxy.spigotmc.org/11743b792604519bd95c3ab037a541508e8a5712?url=https%3A%2F%2Fcdn.discordapp.com%2Fattachments%2F1126552957597519892%2F1197707477647638569%2Fimage.png](https://proxy.spigotmc.org/11743b792604519bd95c3ab037a541508e8a5712?url=https%3A%2F%2Fcdn.discordapp.com%2Fattachments%2F1126552957597519892%2F1197707477647638569%2Fimage.png)

## Placeholder API Support
Bug Report now supports Placeholder API, the keys for Bug Report is the following with more to come in the future.

- %bugreports_totalBugReports% - (**Returns the total amount of bug reports**)
- %bugreports_totalArchivedBugReports% - (**Returns the total amount of archived bug reports**)
- %bugreports_totalNonArchivedBugReports% - (**Returns the total amount of non-archived bug reports**)

## Discord Support
- If you need any support or have features you want to be added to Bug Report, you can join the Discord here - https://discord.gg/jXsV3xQJD4.

Example Config
```yaml
webhookURL: https://discord.com/api/webhooks/

enableDiscordWebhook: false
enablePluginReportCategories: false
enableBugReportNotifications: true

# # Available placeholders:
#
# Aqua, Black, Blue, Dark_Aqua, Dark_Blue
# Dark_Gray, Dark_Green, Dark_Purple, Dark_Red
# Gold, Gray, Green, Light_Purple, Red, White, Yellow
discordEmbedColor: Green
discordEmbedTitle: New Bug Report
discordEmbedFooter: Bug Report v0.8.0
discordEmbedThumbnail: https://www.spigotmc.org/data/resource_icons/110/110732.jpg
discordEnableThumbnail: true
discordEnableUserAuthor: true
discordIncludeDate: true

useTitleInsteadOfMessage: false
enablePlayerHeads: true
refreshPlayerHeadCache: 1d # Default, 1m, 1h, 1d, 1w, 1mo, 1y

language: en
max-reports-per-player: 50 # 0 = unlimited
report-confirmation-message: Thanks for submitting a report!

# # Available placeholders:
#
# Aqua, Black, Blue, Dark_Aqua, Dark_Blue
# Dark_Gray, Dark_Green, Dark_Purple, Dark_Red
# Gold, Gray, Green, Light_Purple, Red, White, Yellow
pluginColor: Yellow
pluginTitle: '[Bug Report]'

# This can either be "mysql" or "local".
# If you choose "mysql", you must fill out the database section below.
# If you choose "local", no configuration is required.
databaseType: local
database:
  host: localhost
  port: 3306
  database: database
  username: root
  password: password

reportCategories:
  - name: Plugin Bug
    id: 1
    description: Report a bug with a plugin
    item: minecraft:book
    color: red
  - name: Server Bug
    id: 2
    description: Report a bug with the server
    item: minecraft:emerald
    color: green
  - name: Resource Bug
    id: 3
    description: Report a bug with the resource pack
    item: minecraft:map
    color: blue
  - name: Other
    id: 4
    description: Report something else
    item: minecraft:paper
    color: yellow
```

## Metrics Collection
Bug Report uses bStats to collect anonymous statistics about servers. If you would like to disable metrics collection, you can do so by editing the ``plugins/bStats/config.yml`` file.
