package com.leon.bugreport;

import com.leon.bugreport.discord.LinkDiscord;
import com.leon.bugreport.extensions.PlanHook;
import com.leon.bugreport.gui.BugReportConfirmationGUI;
import com.leon.bugreport.listeners.PluginMessageListener;
import com.leon.bugreport.listeners.ReportCreatedEvent;
import com.leon.bugreport.logging.ErrorMessages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.Serial;
import java.util.*;

import static com.leon.bugreport.API.DataSource.getPlayerHead;
import static com.leon.bugreport.API.ErrorClass.logErrorMessage;
import static com.leon.bugreport.BugReportDatabase.getStaticUUID;
import static com.leon.bugreport.BugReportLanguage.getKeyFromTranslation;
import static com.leon.bugreport.BugReportLanguage.getValueFromLanguageFile;
import static com.leon.bugreport.BugReportSettings.getSettingsGUI;
import static com.leon.bugreport.commands.BugReportCommand.getChatColorByCode;
import static com.leon.bugreport.commands.BugReportCommand.stringColorToColorCode;
import static com.leon.bugreport.gui.bugreportGUI.openBugReportDetailsGUI;
import static org.bukkit.Bukkit.getServer;

public class BugReportManager implements Listener {
	public static Map<UUID, List<String>> bugReports;
	public static boolean debugMode;
	public static Plugin plugin;
	public static FileConfiguration config;
	public static File configFile;
	public static String language;
	public static ChatColor endingPluginTitleColor;
	public static ChatColor pluginColor;
	public static String pluginTitle;
	public static int localCurrentPage = 1;
	private static BugReportDatabase database;
	private final List<Category> reportCategories;
	private final LinkDiscord discord;

	public BugReportManager(Plugin plugin) {
		BugReportManager.plugin = plugin;
		bugReports = new HashMap<>();
		database = new BugReportDatabase();

		loadBugReports();
		loadConfig();
		checkConfig();

		String webhookURL = config.getString("webhookURL", "");
		pluginTitle = Objects.requireNonNull(config.getString("pluginTitle", "[Bug Report]"));

		if (pluginTitle.contains("&")) {
			pluginTitle = pluginTitle.replace("&", "§");
			String[] parts = pluginTitle.split("§");
			for (int i = 1; i < parts.length; i++) {
				String colorCode = extractValidColorCode(parts[i]);

				if (colorCode != null) {
					ChatColor endingPluginTitleColorOther = getChatColorByCode("§" + colorCode);
					if (endingPluginTitleColorOther != null) {
						endingPluginTitleColor = endingPluginTitleColorOther;
					}
				}
			}
		}

		pluginColor = stringColorToColorCode(Objects.requireNonNull(config.getString("pluginColor", "Yellow").toUpperCase()));
		discord = new LinkDiscord(webhookURL);
		reportCategories = loadReportCategories();
	}

	public static void setDebugMode(int setDebugMode) {
		debugMode = setDebugMode == 1;
	}

	private static @Nullable String extractValidColorCode(String input) {
		input = input.replaceAll("[^0-9a-fA-FklmnorKLMNOR]", "").trim();
		if (input.length() < 1) {
			return null;
		}
		input = input.substring(0, 1);
		if (input.matches("[0-9a-fA-FklmnorKLMNOR]")) {
			return input;
		}
		return null;
	}

	@Contract(pure = true)
	public static @NotNull String returnStartingMessage(ChatColor defaultColor) {
		return pluginColor + pluginTitle + " " + Objects.requireNonNullElse(endingPluginTitleColor, defaultColor);
	}

	public static boolean checkCategoryConfig() {
		if (!config.contains("reportCategories")) {
			String errorMessage = ErrorMessages.getErrorMessage(20);
			plugin.getLogger().warning(errorMessage);
			logErrorMessage(errorMessage);
			return false;
		}

		List<Map<?, ?>> categoryList = config.getMapList("reportCategories");
		for (Map<?, ?> categoryMap : categoryList) {
			Object[] keys = categoryMap.keySet().toArray();
			Object[] values = categoryMap.values().toArray();

			for (int i = 0; i < keys.length; i++) {
				if (values[i] == null) {
					String errorMessage = ErrorMessages.getErrorMessage(21);
					String finalErrorMessage = errorMessage.replaceAll("%key%", keys[i].toString());

					plugin.getLogger().warning(finalErrorMessage);
					logErrorMessage(finalErrorMessage);
					return false;
				}
			}
		}
		return true;
	}

	public static void reloadConfig() {
		language = config.getString("language", "en_US");
		loadConfig();
		checkConfig();
	}

	public static void loadConfig() {
		configFile = new File(plugin.getDataFolder(), "config.yml");

		if (!configFile.exists()) {
			plugin.saveResource("config.yml", false);
		}

		config = YamlConfiguration.loadConfiguration(configFile);

		language = config.getString("language", "en_US");
	}

	public static void checkConfig() {
		Map<String, ?> newValues = new HashMap<>() {
			@Serial
			private static final long serialVersionUID = -2578293471267967277L;

			{
				put("webhookURL", "https://discord.com/api/webhooks/");
				put("enableDiscordWebhook", true);
				put("enablePluginReportCategoriesGUI", false);
				put("enablePluginReportCategoriesTabComplete", false);
				put("enablePluginReportBook", false);
				put("enableBugReportNotifications", true);
				put("bug-category-tab-complete", true);
				put("language", "en_US");
				put("update-checker", true);
				put("update-checker-join", true);
				put("discordEmbedColor", "Yellow");
				put("discordEmbedTitle", "New Bug Report");
				put("discordEmbedFooter", "Bug Report V0.12.5");
				put("discordEmbedThumbnail", "https://www.spigotmc.org/data/resource_icons/110/110732.jpg");
				put("discordEnableThumbnail", true);
				put("discordEnableUserAuthor", true);
				put("discordIncludeDate", true);
				put("enableBungeeCordSendMessage", true);
				put("enableBungeeCordReceiveMessage", true);
				put("useTitleInsteadOfMessage", false);
				put("enablePlayerHeads", true);
				put("refreshPlayerHeadCache", "1d");
				put("metrics", true);
				put("serverName", "My Server");
				put("max-reports-per-player", 50);
				put("report-confirmation-message", "Thanks for submitting a report!");
				put("bug-report-cooldown", 0);
				put("pluginColor", "Yellow");
				put("pluginTitle", "[Bug Report]");
			}
		};

		for (Map.Entry<String, ?> entry : newValues.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (!config.contains(key)) {
				config.set(key, value);
			}
		}
		saveConfig();
	}

	public static void saveConfig() {
		if (debugMode) {
			plugin.getLogger().info("Saving config.yml...");
		}

		try {
			config.save(configFile);
		} catch (Exception e) {
			String errorMessage = ErrorMessages.getErrorMessageWithAdditionalMessage(22, e.getMessage());
			plugin.getLogger().warning(errorMessage);
			logErrorMessage(errorMessage);
		}
	}

	public static @NotNull Inventory generateBugReportGUI(int testCurrentPage, @NotNull Player player, boolean showArchived) {
		loadBugReports();

		int itemsPerPage = 27;
		int navigationRow = 36;

		List<String> reports = bugReports.getOrDefault(getStaticUUID(), new ArrayList<>(Collections.singletonList("DUMMY")));

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			for (String report : reports) {
				String username = getReportByKey(report, "Username");
				getPlayerHead(username);
			}
		});

		List<String> filteredReports = getFilteredReports(showArchived, reports);

		int totalPages = Math.max(1, (int) Math.ceil((double) filteredReports.size() / itemsPerPage));
		int currentPage = Math.max(1, Math.min(testCurrentPage, totalPages));

		Inventory gui = Bukkit.createInventory(null, 45, ChatColor.YELLOW + (showArchived ? "Archived Bugs" : "Bug " + "Report") + " - " + Objects.requireNonNull(getValueFromLanguageFile("buttonNames.pageInfo", "Page %currentPage% of %totalPages%")).replace("%currentPage%", String.valueOf(currentPage)).replace("%totalPages%", String.valueOf(totalPages)));

		int startIndex = (currentPage - 1) * itemsPerPage;
		int endIndex = Math.min(startIndex + itemsPerPage, filteredReports.size());
		int slotIndex = 0;

		for (int i = startIndex; i < endIndex; i++) {
			String report = filteredReports.get(i);
			String reportID = getReportByKey(report, "Report ID");
			String firstLine = report.split("\n")[0];
			String username = firstLine.split(": ")[1];

			ItemStack playerHead = config.getBoolean("enablePlayerHeads") ? getPlayerHead(username) : createInfoItem(Material.ENCHANTED_BOOK, ChatColor.GOLD + "Username", ChatColor.WHITE + username, false);

			ItemStack reportItem = new ItemStack(playerHead);

			ItemMeta itemMeta = reportItem.getItemMeta();
			Objects.requireNonNull(itemMeta).setDisplayName(ChatColor.YELLOW + "Bug Report #" + reportID);
			itemMeta.setLore(Collections.singletonList(ChatColor.GRAY + firstLine));

			reportItem.setItemMeta(itemMeta);

			gui.setItem(slotIndex, reportItem);
			slotIndex++;
		}

		ItemStack settingsButton = createButton(Material.CHEST, ChatColor.YELLOW + getValueFromLanguageFile("buttonNames.settings", "Settings"));
		ItemStack closeButton = createButton(Material.BARRIER, ChatColor.RED + getValueFromLanguageFile("buttonNames.close", "Close"));
		ItemStack pageIndicator = createButton(Material.PAPER, ChatColor.YELLOW + Objects.requireNonNull(getValueFromLanguageFile("buttonNames.pageInfo", "Page %currentPage% of %totalPages%")).replace("%currentPage%", String.valueOf(currentPage)).replace("%totalPages%", String.valueOf(totalPages)));

		if (getCurrentPage(player) == 1) {
			gui.setItem(36, new ItemStack(Material.AIR));
		} else {
			createNavigationButtons("back", gui, 36);
		}

		if (getCurrentPage(player) == getTotalPages()) {
			gui.setItem(44, new ItemStack(Material.AIR));
		} else {
			createNavigationButtons("forward", gui, 44);
		}

		gui.setItem(navigationRow + 2, settingsButton);
		gui.setItem(navigationRow + 4, pageIndicator);
		gui.setItem(navigationRow + 6, closeButton);

		return gui;
	}

	public static String getReportByKey(@NotNull String currentReport, String keyName) {
		String[] reportLines = currentReport.split("\n");
		Map<String, String> reportData = new HashMap<>();

		for (String line : reportLines) {
			int colonIndex = line.indexOf(":");
			if (colonIndex >= 0) {
				String key = line.substring(0, colonIndex).trim();
				String value = line.substring(colonIndex + 1).trim();
				reportData.put(key, value);
			}
		}

		return reportData.get(keyName);
	}

	public static void playButtonClickSound(@NotNull Player player) {
		player.playSound(player.getLocation(), "ui.button.click", 0.6F, 1.0F);
	}

	public static void createNavigationButtons(String forward, @NotNull Inventory bugReportGUI, int index) {
		ItemStack forwardButton = new ItemStack(Material.ARROW);
		ItemMeta forwardMeta = forwardButton.getItemMeta();
		Objects.requireNonNull(forwardMeta).setDisplayName(ChatColor.GREEN + getValueFromLanguageFile("buttonNames." + forward, forward.substring(0, 1).toUpperCase() + forward.substring(1)));
		forwardButton.setItemMeta(forwardMeta);
		bugReportGUI.setItem(index, forwardButton);
	}

	@NotNull
	private static List<String> getFilteredReports(boolean showArchived, @NotNull List<String> reports) {
		List<String> filteredReports = new ArrayList<>();
		for (String report : reports) {
			if (showArchived && report.contains("Archived: 1") || (!showArchived && !report.contains("DUMMY") && !report.contains("Archived: 1"))) {
				filteredReports.add(report);
			}
		}

		filteredReports.sort((r1, r2) -> {
			int id1 = Integer.parseInt(extractReportIDFromReport(r1));
			int id2 = Integer.parseInt(extractReportIDFromReport(r2));
			return Integer.compare(id1, id2);
		});
		return filteredReports;
	}

	private static @NotNull String extractReportIDFromReport(@NotNull String report) {
		String[] reportLines = report.split("\n");
		for (String line : reportLines) {
			int colonIndex = line.indexOf(":");
			if (colonIndex >= 0) {
				String key = line.substring(0, colonIndex).trim();
				String value = line.substring(colonIndex + 1).trim();
				if ("Report ID".equals(key)) {
					return value;
				}
			}
		}
		return "0";
	}

	public static @NotNull Inventory getArchivedBugReportsGUI(int testCurrentPage, Player player) {
		return generateBugReportGUI(testCurrentPage, player, true);
	}

	public static @NotNull Inventory getBugReportGUI(int testCurrentPage, Player player) {
		return generateBugReportGUI(testCurrentPage, player, false);
	}

	public static @NotNull ItemStack createButton(Material material, String name) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		Objects.requireNonNull(meta).setDisplayName(name);
		item.setItemMeta(meta);
		return item;
	}

	private static void loadBugReports() {
		bugReports = BugReportDatabase.loadBugReports();
	}

	public static int getCurrentPage(@NotNull Player player) {
		List<MetadataValue> metadata = player.getMetadata("currentPage");
		if (debugMode) {
			plugin.getLogger().info("Current page for " + player.getName() + " is " + (!metadata.isEmpty() ? metadata.get(0).asInt() : 0));
		}
		return !metadata.isEmpty() ? metadata.get(0).asInt() : 0;
	}

	public static void setCurrentPage(@NotNull Player player, int page) {
		if (debugMode) {
			plugin.getLogger().info("Setting current page to " + page + " for " + player.getName());
		}
		player.setMetadata("currentPage", new FixedMetadataValue(plugin, page));
	}

	public static int getTotalPages() {
		List<String> reports = bugReports.getOrDefault(getStaticUUID(), new ArrayList<>(Collections.singletonList("DUMMY")));
		return (int) Math.ceil((double) reports.size() / 27);
	}

	/**
	 * Check if a key exists in the config, and if it does, return the value.
	 * If checkForBoolean is true, return the value.
	 * If checkForBoolean is false, return if the key exists.
	 */
	public static boolean checkForKey(String key, boolean checkForBoolean) {
		if (!config.contains(key) || config.get(key) == null) {
			return false;
		}

		return !checkForBoolean || config.getBoolean(key);
	}

	public static @NotNull String translateTimestampToDate(long timestamp) {
		Date date = new Date(timestamp);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);

		int day = calendar.get(Calendar.DAY_OF_MONTH);
		int hour = calendar.get(Calendar.HOUR);
		int minute = calendar.get(Calendar.MINUTE);

		String daySuffix = getDayOfMonthSuffix(day);
		String amPm = calendar.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";
		String hourString = String.valueOf(hour);
		String minuteString = String.valueOf(minute);

		if (hour < 10) hourString = "0" + hourString;
		if (minute < 10) minuteString = "0" + minuteString;

		return new StringJoiner(" ").add(calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH)).add(day + daySuffix + ",").add(String.valueOf(calendar.get(Calendar.YEAR))).add("at").add(hourString + ":" + minuteString).add(amPm).toString();
	}

	@Contract(pure = true)
	private static @NotNull String getDayOfMonthSuffix(int day) {
		return switch (day) {
			case 1, 21, 31 -> "st";
			case 2, 22 -> "nd";
			case 3, 23 -> "rd";
			default -> "th";
		};
	}

	public static @NotNull ItemStack createEmptyItem() {
		ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
		ItemMeta meta = item.getItemMeta();
		Objects.requireNonNull(meta).setDisplayName(" ");
		item.setItemMeta(meta);

		return item;
	}

	public static @NotNull ItemStack createInfoItem(Material material, String name, String value, @NotNull Boolean longMessage) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		Objects.requireNonNull(meta).setDisplayName(name);

		if (longMessage) {
			List<String> lore = new ArrayList<>();
			String[] words = value.split(" ");
			StringBuilder currentLine = new StringBuilder();
			for (String word : words) {
				if (currentLine.length() + word.length() > 30) {
					lore.add(currentLine.toString());
					currentLine = new StringBuilder();
				}

				currentLine.append(word).append(" ");
			}

			if (!currentLine.isEmpty()) lore.add(currentLine.toString());
			meta.setLore(lore);
		} else {
			meta.setLore(Collections.singletonList(value));
		}

		item.setItemMeta(meta);
		return item;
	}

	private @Nullable List<Category> loadReportCategories() {
		if (checkCategoryConfig()) {
			List<Category> categories = new ArrayList<>();
			List<Map<?, ?>> categoryList = config.getMapList("reportCategories");

			for (Map<?, ?> categoryMap : categoryList) {
				String name = categoryMap.get("name").toString();
				String description = categoryMap.get("description").toString();
				String itemString = categoryMap.get("item").toString();
				String color = categoryMap.get("color").toString().toUpperCase();
				int id = Integer.parseInt(categoryMap.get("id").toString());

				Material itemMaterial = Material.matchMaterial(itemString);
				if (itemMaterial == null) {
					continue;
				}

				ItemStack itemStack = new ItemStack(itemMaterial);
				ItemMeta itemMeta = itemStack.getItemMeta();
				Objects.requireNonNull(itemMeta).setDisplayName(ChatColor.YELLOW + name);
				itemMeta.setLore(Collections.singletonList(ChatColor.GRAY + description));
				itemStack.setItemMeta(itemMeta);
				categories.add(new Category(id, name, color, itemStack));
			}

			return categories;
		} else {
			String errorMessage = ErrorMessages.getErrorMessage(23);
			plugin.getLogger().warning(errorMessage);
			logErrorMessage(errorMessage);
			return null;
		}
	}

	public List<Category> getReportCategories() {
		return reportCategories;
	}

	public void setWebhookURL(String webhookURL) {
		if (debugMode) {
			plugin.getLogger().info("Setting Discord Webhook URL to " + webhookURL);
		}
		config.set("webhookURL", webhookURL);
		saveConfig();
		discord.setWebhookURL(webhookURL);
	}

	public void submitBugReport(@NotNull Player player, String message, Integer categoryId) {
		if (debugMode) {
			plugin.getLogger().info("Submitting bug report for " + player.getName() + "...");
		}
		List<String> reports = bugReports.getOrDefault(getStaticUUID(), new ArrayList<>(Collections.singletonList("DUMMY")));
		UUID playerId = player.getUniqueId();

		String playerName = player.getName();
		String playerUUID = playerId.toString();
		String worldName = player.getWorld().getName();
		String gamemode = player.getGameMode().toString();
		String serverName = config.getString("serverName", "Unknown Server");
		String location = player.getWorld().getName() + ", " + player.getLocation().getBlockX() + ", " + player.getLocation().getBlockY() + ", " + player.getLocation().getBlockZ();
		String reportID = reports.stream().filter(report -> report.contains("Report ID: ")).reduce((first, second) -> second).map(report -> Arrays.stream(report.split("\n")).filter(line -> line.contains("Report ID:")).findFirst().orElse("Report ID: 0")).map(reportIDLine -> reportIDLine.split(": ")[1].trim()).orElse("0");
		String reportIDInt = String.valueOf(Integer.parseInt(reportID) + 1);
		String header = "Username: " + playerName + "\n" + "UUID: " + playerUUID + "\n" + "World: " + worldName + "\n" + "hasBeenRead: 0" + "\n" + "Category ID: " + categoryId + "\n" + "Full Message: " + message + "\n" + "Archived: 0" + "\n" + "Report ID: " + reportIDInt + "\n" + "Timestamp: " + System.currentTimeMillis() + "\n" + "Location: " + location + "\n" + "Gamemode: " + gamemode;

		reports.add(header);
		bugReports.put(playerId, reports);

		if (Bukkit.getPluginManager().isPluginEnabled("Plan")) {
			if (debugMode) {
				plugin.getLogger().info("Updating Plan hook for " + playerName + "...");
			}
			PlanHook.getInstance().updateHook(playerId, playerName);
		}

		if (debugMode) {
			plugin.getLogger().info("Adding bug report to database...");
		}
		BugReportDatabase.addBugReport(playerName, playerId, worldName, header, message, location, gamemode, serverName);

		if (config.getBoolean("enableBugReportNotifications", true)) {
			if (debugMode) {
				plugin.getLogger().info("Sending bug report notification to online players...");
			}
			String defaultMessage = returnStartingMessage(ChatColor.GRAY) + getValueFromLanguageFile("bugReportNotificationMessage", "A new bug report has been submitted by %player%!").replace("%player%", ChatColor.AQUA + playerName + ChatColor.GRAY);

			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				if (onlinePlayer.hasPermission("bugreport.notify")) {
					onlinePlayer.sendMessage(defaultMessage);
				}
			}
		}

		if (config.getBoolean("enableDiscordWebhook", true)) {
			if (debugMode) {
				plugin.getLogger().info("Sending bug report to Discord...");
			}
			String webhookURL = config.getString("webhookURL", "");
			if (webhookURL.isEmpty()) {
				String errorMessage = ErrorMessages.getErrorMessage(24);

				plugin.getLogger().warning(errorMessage);
				logErrorMessage(errorMessage);
			}

			try {
				discord.sendBugReport(message, worldName, playerName, location, gamemode, categoryId, serverName);
				if (debugMode) {
					plugin.getLogger().info("Bug report sent to Discord.");
				}
			} catch (Exception e) {
				String errorMessage = ErrorMessages.getErrorMessageWithAdditionalMessage(25, e.getMessage());

				plugin.getLogger().warning(errorMessage);
				logErrorMessage(errorMessage);
			}
		}

		if (getServer().getMessenger().isIncomingChannelRegistered(BugReportPlugin.getPlugin(), "BungeeCord")) {
			PluginMessageListener.sendPluginMessage(player);
		}

		Bukkit.getScheduler().runTask(plugin, () -> {
			ReportCreatedEvent reportEvent = new ReportCreatedEvent(header);
			getServer().getPluginManager().callEvent(reportEvent);
		});
	}

	public static class BugReportListener implements Listener {
		private final Map<UUID, Boolean> closingInventoryMap;

		public BugReportListener() {
			this.closingInventoryMap = new HashMap<>();
		}

		@EventHandler(priority = EventPriority.NORMAL)
		public void onInventoryClick(@NotNull InventoryClickEvent event) {
			String TitleText = ChatColor.stripColor(event.getView().getTitle());

			if (debugMode) {
				plugin.getLogger().info("Clicked inventory: " + TitleText);
			}

			boolean isArchivedGUI = TitleText.startsWith("Archived Bugs");

			if (!TitleText.startsWith("Bug Report") && !isArchivedGUI) {
				return;
			}

			event.setCancelled(true);

			Player player = (Player) event.getWhoClicked();
			Inventory clickedInventory = event.getClickedInventory();
			if (clickedInventory == null) {
				return;
			}

			ItemStack clickedItem = event.getCurrentItem();
			if (clickedItem == null || clickedItem.getType() == Material.AIR) {
				return;
			}

			ItemMeta itemMeta = clickedItem.getItemMeta();
			if (itemMeta == null || !itemMeta.hasDisplayName()) {
				return;
			}

			String displayName = itemMeta.getDisplayName();
			String cleanedDisplayName = ChatColor.stripColor(displayName);

			if (debugMode) {
				plugin.getLogger().info("Clicked item: " + cleanedDisplayName);
			}

			if (cleanedDisplayName.startsWith("Bug Report #")) {
				int reportID = Integer.parseInt(displayName.substring(14));
				List<String> reports = bugReports.getOrDefault(getStaticUUID(), new ArrayList<>(Collections.singletonList("DUMMY")));
				String report = reports.stream().filter(reportString -> reportString.contains("Report ID: " + reportID)).findFirst().orElse(null);

				if (debugMode) {
					plugin.getLogger().info("Opening bug report details GUI for report ID " + reportID);
				}

				playButtonClickSound(player);
				openBugReportDetailsGUI(player, report, reportID, isArchivedGUI);
				return;
			}

			String customDisplayName = getKeyFromTranslation(displayName);
			if (customDisplayName == null || customDisplayName.equals(" ")) {
				return;
			}

			switch (customDisplayName) {
				case "buttonNames.back" -> {
					int currentPage = getCurrentPage(player);
					if (currentPage > 1) {
						if (TitleText.startsWith("Bug Report Details - ")) {
							playButtonClickSound(player);
							player.openInventory(isArchivedGUI ? getArchivedBugReportsGUI(currentPage, player) : getBugReportGUI(currentPage, player));
						} else {
							setCurrentPage(player, currentPage - 1);
							playButtonClickSound(player);
							localCurrentPage = currentPage - 1;
							player.openInventory(isArchivedGUI ? getArchivedBugReportsGUI(localCurrentPage, player) : getBugReportGUI(localCurrentPage, player));
						}
					}
				}
				case "buttonNames.forward" -> {
					int currentPage = getCurrentPage(player);
					if (currentPage < getTotalPages()) {
						setCurrentPage(player, currentPage + 1);
						playButtonClickSound(player);
						localCurrentPage = currentPage + 1;
						player.openInventory(isArchivedGUI ? getArchivedBugReportsGUI(localCurrentPage, player) : getBugReportGUI(localCurrentPage, player));
					}
				}
				case "buttonNames.settings" -> {
					player.openInventory(getSettingsGUI());
					playButtonClickSound(player);
				}
				case "buttonNames.close" -> {
					closingInventoryMap.put(player.getUniqueId(), true);
					playButtonClickSound(player);
					player.closeInventory();
				}
			}
		}

		@EventHandler(priority = EventPriority.NORMAL)
		public void onInventoryClose(@NotNull InventoryCloseEvent event) {
			if (event.getView().getTitle().startsWith(ChatColor.YELLOW + "Bug Report")) {
				Player player = (Player) event.getPlayer();
				UUID playerId = player.getUniqueId();

				if (closingInventoryMap.getOrDefault(playerId, false)) {
					closingInventoryMap.put(playerId, false);
					return;
				}

				closingInventoryMap.remove(playerId);
			}
		}
	}

	public record BugReportDetailsListener(Inventory gui, Integer reportIDGUI) implements Listener {
		@EventHandler(priority = EventPriority.NORMAL)
		public void onInventoryClick(@NotNull InventoryClickEvent event) {
			String title = event.getView().getTitle();
			boolean isArchivedDetails = title.startsWith(ChatColor.YELLOW + "Archived Bug Details");

			if (!title.startsWith(ChatColor.YELLOW + "Bug Report Details - #") && !title.startsWith(ChatColor.YELLOW + "Archived Bug Details")) {
				return;
			}

			// The ID is the number after the # in the title
			String bugReportID = title.split("#")[1].split(" ")[0];

			event.setCancelled(true);

			Player player = (Player) event.getWhoClicked();

			Inventory clickedInventory = event.getClickedInventory();
			ItemStack clickedItem = event.getCurrentItem();

			if (clickedInventory == null || clickedItem == null || clickedItem.getType() == Material.AIR) {
				return;
			}

			ItemMeta itemMeta = clickedItem.getItemMeta();

			if (itemMeta == null || !itemMeta.hasDisplayName() || itemMeta.getDisplayName().equals(" ")) {
				return;
			}

			String itemName = itemMeta.getDisplayName();

			if (debugMode) {
				plugin.getLogger().info("Clicked item: " + ChatColor.stripColor(itemName));
			}

			if (itemName.contains("(Click to change)")) {
				playButtonClickSound(player);
				player.openInventory(BugReportSettings.getStatusSelectionGUI(reportIDGUI));
			}

			if (itemName.contains("(Click to teleport)")) {
				playButtonClickSound(player);
				if (debugMode) {
					plugin.getLogger().info("Teleporting to the location of bug report #" + reportIDGUI + "...");
				}

				if (checkForKey("useTitleInsteadOfMessage", true)) {
					player.sendTitle(returnStartingMessage(ChatColor.GREEN) + " Teleporting to the location of Bug Report #" + reportIDGUI + "." + "." + ".", " ", 10, 70, 20);
				} else {
					player.sendMessage(returnStartingMessage(ChatColor.GREEN) + " Teleporting to the location of Bug Report #" + reportIDGUI + ".");
				}

				Location teleportLocation = BugReportDatabase.getBugReportLocation(reportIDGUI);
				if (teleportLocation != null) {
					player.teleport(teleportLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
				} else {
					player.sendMessage(returnStartingMessage(ChatColor.RED) + " The location of Bug Report #" + reportIDGUI + " is not available.");
					player.closeInventory();
				}
			}

			String customDisplayName = getKeyFromTranslation(itemName);
			if (customDisplayName == null || customDisplayName.equals(" ")) {
				return;
			}

			switch (customDisplayName) {
				case "buttonNames.back" -> {
					playButtonClickSound(player);
					player.openInventory(isArchivedDetails ? getArchivedBugReportsGUI(localCurrentPage, player) : getBugReportGUI(localCurrentPage, player));
				}
				case "buttonNames.unarchive" -> {
					playButtonClickSound(player);
					BugReportDatabase.updateBugReportArchive(reportIDGUI, 0);

					if (debugMode) {
						plugin.getLogger().info("Unarchiving bug report #" + reportIDGUI + "...");
					}
					player.openInventory(isArchivedDetails ? getArchivedBugReportsGUI(localCurrentPage, player) : getBugReportGUI(localCurrentPage, player));
					player.sendMessage(returnStartingMessage(ChatColor.YELLOW) + " Bug Report #" + reportIDGUI + " has been unarchived.");

					HandlerList.unregisterAll(this);
				}
				case "buttonNames.archive" -> {
					if (player.hasPermission("bugreport.archive") || player.hasPermission("bugreport.admin")) {
						playButtonClickSound(player);

						if (debugMode) {
							plugin.getLogger().info("Archiving bug report #" + reportIDGUI + "...");
						}
						Bukkit.getPluginManager().registerEvents(new BugReportConfirmationGUI.BugReportConfirmationListener(gui, reportIDGUI, isArchivedDetails), plugin);
						BugReportConfirmationGUI.openConfirmationGUI(player, true, bugReportID);
					} else {
						player.closeInventory();
						player.sendMessage(returnStartingMessage(ChatColor.RED) + " You don't have permission to archive bug reports!");
					}
				}
				case "buttonNames.delete" -> {
					if (player.hasPermission("bugreport.delete") || player.hasPermission("bugreport.admin")) {
						playButtonClickSound(player);

						if (debugMode) {
							plugin.getLogger().info("Opening confirmation GUI for deletion on Bug Report #" + reportIDGUI + "...");
						}
						Bukkit.getPluginManager().registerEvents(new BugReportConfirmationGUI.BugReportConfirmationListener(gui, reportIDGUI, isArchivedDetails), plugin);
						BugReportConfirmationGUI.openConfirmationGUI(player, false, bugReportID);
					} else {
						player.closeInventory();
						player.sendMessage(returnStartingMessage(ChatColor.RED) + " You don't have permission to delete bug reports!");
					}
				}
			}

			HandlerList.unregisterAll(this);
		}
	}
}
