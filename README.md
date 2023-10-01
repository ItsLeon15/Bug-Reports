# Bug-Reports
A Spigot 1.17.1 Bug Report plugin

## What is BugReport?
BugReport is a tool that helps server owners manage any bug reports found by players which can be accessed by a GUI. This is especially helpful as information about where the player is located and their UUID is automatically stored and shown to admins.

## Commands
- **bugreport <message>** - Adds a new bug report.
- **buglist** - Shows a GUI with all the reports made.
- **buglinkdiscord <Webhook URL>** - Link a Discord Webhook to send all Bug Reports to.
## Permissions
- **bugreport.admin** - Allows a user to access the buglist command.
- **bugreport.notify** - Allows a player be notified every time a bug report is sent.

## Fixes for future releases
- [x] Allow more than 9 bug reports with multiple pages.
- [x] Add a command to link a Discord Webhook to send all Bug Reports to.
- [x] Add an option to edit the GUI options in the config.
- [x] Add support for multiple languages.

## Current working versions
- [x] 1.17.X
- [x] 1.18.X
- [x] 1.19.X
- [x] 1.20.X