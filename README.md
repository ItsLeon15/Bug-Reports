## What is BugReport?

Bug Report is a tool that helps server owners manage any bugs found by players which can then be accessed by a GUI. This
is especially helpful as information about the player is stored as soon as they submit a bug report. Information
collected when a bug report is submitted is as follows:

- Server Name
- Player Name
- Player's UUID
- Current World Name
- Full Message
- Category ID (If the option is turned on)
- Status (Can be customized using a GUI)
- Date & Time
- Player's Location
- Player's Game Mode

You can archive/unarchive/delete bug reports, customize the "Bug Report Details" GUI, customize all of the statuses,
language selector and more!

## Commands

- bugreport <message> - **Adds a new bug report either via message or a GUI for the categories**.
- buglist - **This shows a GUI with all the reports made on the settings page**.
- buglistsettings - **This shows the Bug List Settings GUI**.
- buglistarchived - **See all the bug reports that have been archived in a GUI**.
- buglinkdiscord <Webhook URL> - **Links a Discord Webhook to send all Bug Reports to**.

## Command Aliases

- **/br <message>** (/bugreport <Message>)
- **/bl** (/buglist)
- **/bla** (/buglistarchived)
- **/bls** (/buglistsettings)
- **/bld <Webhook URL>** (/buglinkdiscord <Webhook URL>)

## Permissions

- bugreport.admin - **Allows a user to access the buglist command**.
- bugreport.notify - **Let a player be notified every time a bug report is sent**.
- bugreport.archived - **Allows the player to access the archived bug reports**.
- bugreport.settings - **Allows the player to access and change the settings of the plugin**.
- bugreport.list - **Allows the player to access the list of bug reports**.
- bugreport.use - **Allows the player to use the bug report command**.
- bugreport.archive - **Allows the player to archive bug reports**.
- bugreport.delete - **Allows the player to delete bug reports**.

## Bug List GUI

![https://www.spigotmc.org/attachments/bug-report-list-gui-png.795626/](https://www.spigotmc.org/attachments/bug-report-list-gui-png.795626/)

## Bug Details GUI

![https://www.spigotmc.org/attachments/bug-report-details-png.795652/](https://www.spigotmc.org/attachments/bug-report-details-png.795652/)

## Plan Support

Bug Report now supports the Plan API. To use Bug Report with Plan, you will need to install Plan onto your Spigot Server
and it should set up everything automatically.

![https://proxy.spigotmc.org/11743b792604519bd95c3ab037a541508e8a5712?url=https%3A%2F%2Fcdn.discordapp.com%2Fattachments%2F1126552957597519892%2F1197707477647638569%2Fimage.png](https://proxy.spigotmc.org/11743b792604519bd95c3ab037a541508e8a5712?url=https%3A%2F%2Fcdn.discordapp.com%2Fattachments%2F1126552957597519892%2F1197707477647638569%2Fimage.png)

## Placeholder API Support

Bug Report now supports Placeholder API, the keys for Bug Report is the following with more to come in the future.

- %bugreports_totalBugReports% - (**Returns the total amount of bug reports**)
- %bugreports_totalArchivedBugReports% - (**Returns the total amount of archived bug reports**)
- %bugreports_totalNonArchivedBugReports% - (**Returns the total amount of non-archived bug reports**)
- %bugreports_totalDeletedBugReports% - (**Returns the total amount of deleted bug reports**)

External Placeholders via PlaceholderAPI are also supported in the DiscordEmbedFields.

## Discord Support

- If you need any support or have features you want to be added to Bug Report, you can join the Discord
  here - https://discord.gg/jXsV3xQJD4.

Example Config

```yaml
webhookURL: https://discord.com/api/webhooks/

enableDiscordWebhook: false
enablePluginReportBook: false
enableBugReportNotifications: true

# Either one of them has to be true, they both can not be true at the same time
enablePluginReportCategoriesGUI: false # Set to true if you want a category selection GUI after doing /bugreport
enablePluginReportCategoriesTabComplete: false # Set to true if you want a category tab completion after doing /bugreport. No GUI will open

language: en_US

update-checker: true
update-checker-join: true

# # Available placeholders:
#
# Aqua, Black, Blue, Dark_Aqua, Dark_Blue
# Dark_Gray, Dark_Green, Dark_Purple, Dark_Red
# Gold, Gray, Green, Light_Purple, Red, White, Yellow
discordEmbedColor: Green
discordEmbedTitle: New Bug Report
discordEmbedFooter: Bug Report v0.12.4
discordEmbedThumbnail: https://www.spigotmc.org/data/resource_icons/110/110732.jpg
discordEnableThumbnail: true
discordEnableUserAuthor: true
discordIncludeDate: true

# If you want to ping a role or a member, you can add them here. You can add multiple roles and members.
# The role or member will be pinged in the message you set in discordPingMessage.
# You don't need to add the "<@" or "<@&" characters to the role or member. They will be added automatically.
discordPingMessage: A new Bug Report has been submitted
discordEnablePing: true

discordPingMembers:
  - ''
discordPingRoles:
  - ''

enableBungeeCordSendMessage: true
enableBungeeCordReceiveMessage: true

useTitleInsteadOfMessage: false
enablePlayerHeads: true
refreshPlayerHeadCache: 1d # Default, 1m, 1h, 1d, 1w, 1mo, 1y

metrics: true # Enables metrics

serverName: "My Server"

max-reports-per-player: 50 # 0 = unlimited
report-confirmation-message: Thanks for submitting a report!
bug-report-cooldown: 0 # 0 = disabled

# # Available placeholders:
#
# Aqua, Black, Blue, Dark_Aqua, Dark_Blue
# Dark_Gray, Dark_Green, Dark_Purple, Dark_Red
# Gold, Gray, Green, Light_Purple, Red, White, Yellow
pluginColor: Yellow

# You are able to use & for color codes such as "&8[&6Bug Report&8]&9".
# For a list of color codes, visit https://htmlcolorcodes.com/minecraft-color-codes (Format Codes will NOT work!)
# This will override the pluginColor option.
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

# Placeholders via PlaceholderAPI are allowed to be used in the discordEmbedFields.
# E.g: You can create a field with the biome of the player, where the bugreport was created.
# Choose for the value: "%player_biome%" (without ""). This works with any other Placeholder supported by PlaceholderAPI.
discordEmbedFields:
  - name: "Username"
    id: 1
    value: "%report_username%"
    inline: true
  - name: "UUID"
    id: 2
    value: "%report_uuid%"
    inline: true
  - name: "World"
    id: 3
    value: "%report_world%"
    inline: true
  - name: "Location (X, Y, Z)"
    id: 4
    value: "%report_location%"
    inline: true
  - name: "Gamemode"
    id: 5
    value: "%report_gamemode%"
    inline: true
  - name: "Status"
    id: 6
    value: "%report_status%"
    inline: true
  - name: "Category"
    id: 7
    value: "%report_category%"
    inline: true
  - name: "Sever Name"
    id: 8
    value: "%report_server_name%"
    inline: true
  - name: "Full Message"
    id: 9
    value: "%report_full_message%"
    inline: false

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

statuses:
  - name: Open
    description: Report is open
    icon: minecraft:green_dye
    color: GREEN
    id: 1
  - name: Archived
    description: Report is archived
    icon: minecraft:red_dye
    color: RED
    id: 2
  - name: Pending
    description: Report is pending
    icon: minecraft:yellow_dye
    color: YELLOW
    id: 3
  - name: Ignored
    description: Report is ignored
    icon: minecraft:white_dye
    color: WHITE
    id: 4
```

## Metrics Collection

Bug Report uses bStats to collect anonymous statistics about servers. If you would like to disable metrics collection,
you can do so by editing the ``plugins/bStats/config.yml`` file.
