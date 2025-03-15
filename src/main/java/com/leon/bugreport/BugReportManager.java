package com.leon.bugreport;

import com.leon.bugreport.API.DataSource;
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

import java.awt.*;
import java.io.File;
import java.io.Serial;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

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
	private static final int ITEMS_PER_PAGE = 27;
	private static final int NAVIGATION_ROW = 36;

	public static Map<UUID, List<String>> bugReports;
	public static BugReportDatabase database;
	public static boolean debugMode;
	public static Plugin plugin;
	public static FileConfiguration config;
	public static File configFile;
	public static String language;
	public static ChatColor endingPluginTitleColor;
	public static ChatColor pluginColor;
	public static String pluginTitle;
	public static int localCurrentPage = 1;
	public static LinkDiscord discord;
	private static List<Category> reportCategories;

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
					ChatColor color = getChatColorByCode("§" + colorCode);
					if (color != null) {
						endingPluginTitleColor = color;
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
		if (input.isEmpty()) {
			return null;
		}
		input = input.substring(0, 1);
		return input.matches("[0-9a-fA-FklmnorKLMNOR]") ? input : null;
	}

	@Contract(pure = true)
	public static @NotNull String returnStartingMessage(ChatColor defaultColor) {
		return pluginColor + pluginTitle + " " + Objects.requireNonNullElse(endingPluginTitleColor, defaultColor);
	}

	public static boolean isCategoryConfigInvalid() {
		if (!config.contains("reportCategories")) {
			String errorMessage = ErrorMessages.getErrorMessage(20);
			plugin.getLogger().warning(errorMessage);
			logErrorMessage(errorMessage);
			return true;
		}

		List<Map<?, ?>> categoryList = config.getMapList("reportCategories");
		for (Map<?, ?> categoryMap : categoryList) {
			Object[] keys = categoryMap.keySet().toArray();
			Object[] values = categoryMap.values().toArray();

			for (int i = 0; i < keys.length; i++) {
				if (values[i] == null) {
					String errorMessage = ErrorMessages.getErrorMessage(21).replaceAll("%key%", keys[i].toString());
					plugin.getLogger().warning(errorMessage);
					logErrorMessage(errorMessage);
					return true;
				}
			}
		}

		return false;
	}

	public static void reloadConfig() {
		loadConfig();
		language = config.getString("language", "en_US");
		reloadAllCategories();
		checkConfig();

		BugReportPlugin pluginInstance = BugReportPlugin.getPlugin();
		UniversalTabCompleter universalTabCompleter = new UniversalTabCompleter(pluginInstance.reportManager, config);

		Objects.requireNonNull(pluginInstance.getCommand("bugreport")).setTabCompleter(universalTabCompleter);
		Objects.requireNonNull(pluginInstance.getCommand("buglist")).setTabCompleter(universalTabCompleter);
	}

	public static void reloadAllCategories() {
		reportCategories = loadReportCategories();
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
		Map<String, Object> defaultValues = createDefaultConfigValues();

		defaultValues.forEach((key, value) -> {
			if (!config.contains(key)) {
				config.set(key, value);
			}
		});

		saveConfig();
	}

	private static @NotNull Map<String, Object> createDefaultConfigValues() {
		Map<String, Object> defaults = new HashMap<>() {
			@Serial
			private static final long serialVersionUID = -2578293471267967277L;
		};

		defaults.put("webhookURL", "https://discord.com/api/webhooks/");
		defaults.put("enableDiscordWebhook", true);
		defaults.put("enablePluginReportCategoriesGUI", false);
		defaults.put("enablePluginReportCategoriesTabComplete", false);
		defaults.put("enablePluginReportBook", false);
		defaults.put("enableBugReportNotifications", true);
		defaults.put("bug-category-tab-complete", true);
		defaults.put("language", "en_US");
		defaults.put("update-checker", true);
		defaults.put("update-checker-join", true);
		defaults.put("discordEmbedColor", "Yellow");
		defaults.put("discordEmbedTitle", "New Bug Report");
		defaults.put("discordEmbedFooter", "Bug Report V0.13.0");
		defaults.put("discordEmbedThumbnail", "https://www.spigotmc.org/data/resource_icons/110/110732.jpg");
		defaults.put("discordEnableThumbnail", true);
		defaults.put("discordEnableUserAuthor", true);
		defaults.put("discordIncludeDate", true);
		defaults.put("enableBungeeCordSendMessage", true);
		defaults.put("enableBungeeCordReceiveMessage", true);
		defaults.put("useTitleInsteadOfMessage", false);
		defaults.put("enablePlayerHeads", true);
		defaults.put("refreshPlayerHeadCache", "1d");
		defaults.put("metrics", true);
		defaults.put("serverName", "My Server");
		defaults.put("max-reports-per-player", 50);
		defaults.put("report-confirmation-message", "Thanks for submitting a report!");
		defaults.put("bug-report-cooldown", 0);
		defaults.put("pluginColor", "Yellow");
		defaults.put("pluginTitle", "[Bug Report]");

		return defaults;
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

	public static @NotNull Inventory generateBugReportGUI(int testCurrentPage, boolean showArchived) {
		loadBugReports();

		List<String> reports = bugReports.getOrDefault(getStaticUUID(), new ArrayList<>(Collections.singletonList("DUMMY")));

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> reports.stream()
				.map(report -> getReportByKey(report, "Username"))
				.forEach(DataSource::getPlayerHead));

		List<String> filteredReports = getFilteredReports(showArchived, reports);
		int totalPages = Math.max(1, (int) Math.ceil((double) filteredReports.size() / ITEMS_PER_PAGE));
		int currentPage = Math.max(1, Math.min(testCurrentPage, totalPages));

		String title = ChatColor.YELLOW + (showArchived ? "Archived Bugs" : "Bug Report") + " - " +
				Objects.requireNonNull(getValueFromLanguageFile("buttonNames.pageInfo", "Page %currentPage% of %totalPages%"))
						.replace("%currentPage%", String.valueOf(currentPage))
						.replace("%totalPages%", String.valueOf(totalPages));

		Inventory gui = Bukkit.createInventory(null, 45, title);

		addReportItemsToGui(filteredReports, currentPage, gui);
		addNavigationButtonsToGui(gui, currentPage, totalPages);

		return gui;
	}

	private static void addReportItemsToGui(@NotNull List<String> filteredReports, int currentPage, Inventory gui) {
		int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
		int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredReports.size());

		for (int i = startIndex, slotIndex = 0; i < endIndex; i++, slotIndex++) {
			String report = filteredReports.get(i);
			String reportID = getReportByKey(report, "Report ID");
			String firstLine = report.split("\n")[0];
			String username = firstLine.split(": ")[1];

			ItemStack playerHead = config.getBoolean("enablePlayerHeads")
					? getPlayerHead(username)
					: createInfoItem(Material.ENCHANTED_BOOK, ChatColor.GOLD + "Username", ChatColor.WHITE + username, false);

			ItemMeta itemMeta = playerHead.getItemMeta();
			Objects.requireNonNull(itemMeta).setDisplayName(ChatColor.YELLOW + "Bug Report #" + reportID);
			itemMeta.setLore(Collections.singletonList(ChatColor.GRAY + firstLine));
			playerHead.setItemMeta(itemMeta);

			gui.setItem(slotIndex, playerHead);
		}
	}

	private static void addNavigationButtonsToGui(Inventory gui, int currentPage, int totalPages) {
		ItemStack settingsButton = createButton(Material.CHEST, ChatColor.YELLOW + getValueFromLanguageFile("buttonNames.settings", "Settings"));
		ItemStack closeButton = createButton(Material.BARRIER, ChatColor.RED + getValueFromLanguageFile("buttonNames.close", "Close"));
		String pageInfoText = Objects.requireNonNull(getValueFromLanguageFile("buttonNames.pageInfo", "Page %currentPage% of %totalPages%"))
				.replace("%currentPage%", String.valueOf(currentPage))
				.replace("%totalPages%", String.valueOf(totalPages));
		ItemStack pageIndicator = createButton(Material.PAPER, ChatColor.YELLOW + pageInfoText);

		if (currentPage > 1) {
			createNavigationButtons("back", gui, NAVIGATION_ROW);
		} else {
			gui.setItem(NAVIGATION_ROW, new ItemStack(Material.AIR));
		}

		if (currentPage < totalPages) {
			createNavigationButtons("forward", gui, 44);
		} else {
			gui.setItem(44, new ItemStack(Material.AIR));
		}

		gui.setItem(NAVIGATION_ROW + 2, settingsButton);
		gui.setItem(NAVIGATION_ROW + 4, pageIndicator);
		gui.setItem(NAVIGATION_ROW + 6, closeButton);
	}

	public static String getReportByKey(@NotNull String currentReport, String keyName) {
		return Arrays.stream(currentReport.split("\n"))
				.filter(line -> line.contains(":"))
				.map(line -> {
					int colonIndex = line.indexOf(":");
					if (colonIndex >= 0) {
						String key = line.substring(0, colonIndex).trim();
						String value = line.substring(colonIndex + 1).trim();
						return Map.entry(key, value);
					}
					return null;
				})
				.filter(Objects::nonNull)
				.filter(entry -> entry.getKey().equals(keyName))
				.map(Map.Entry::getValue)
				.findFirst()
				.orElse(null);
	}

	public static void playButtonClickSound(@NotNull Player player) {
		player.playSound(player.getLocation(), "ui.button.click", 0.6F, 1.0F);
	}

	public static void createNavigationButtons(@NotNull String direction, @NotNull Inventory bugReportGUI, int index) {
		String displayName = getValueFromLanguageFile("buttonNames." + direction, direction.substring(0, 1).toUpperCase() + direction.substring(1));
		ItemStack button = new ItemStack(Material.ARROW);
		ItemMeta meta = button.getItemMeta();

		Objects.requireNonNull(meta).setDisplayName(ChatColor.GREEN + displayName);
		button.setItemMeta(meta);
		bugReportGUI.setItem(index, button);
	}

	@NotNull
	private static List<String> getFilteredReports(boolean showArchived, @NotNull List<String> reports) {
		return reports.stream()
				.filter(report -> {
					boolean isArchived = report.contains("Archived: 1");
					boolean isDummy = report.contains("DUMMY");
					return (showArchived && isArchived) || (!showArchived && !isDummy && !isArchived);
				})
				.sorted((r1, r2) -> {
					int id1 = Integer.parseInt(extractReportIDFromReport(r1));
					int id2 = Integer.parseInt(extractReportIDFromReport(r2));
					return Integer.compare(id1, id2);
				})
				.collect(Collectors.toList());
	}

	private static @NotNull String extractReportIDFromReport(@NotNull String report) {
		return Arrays.stream(report.split("\n"))
				.filter(line -> line.contains(":"))
				.map(line -> {
					int colonIndex = line.indexOf(":");
					if (colonIndex >= 0) {
						String key = line.substring(0, colonIndex).trim();
						String value = line.substring(colonIndex + 1).trim();
						if ("Report ID".equals(key)) {
							return value;
						}
					}
					return null;
				})
				.filter(Objects::nonNull)
				.findFirst()
				.orElse("0");
	}

	public static @NotNull Inventory getArchivedBugReportsGUI(int testCurrentPage) {
		return generateBugReportGUI(testCurrentPage, true);
	}

	public static @NotNull Inventory getBugReportGUI(int testCurrentPage) {
		return generateBugReportGUI(testCurrentPage, false);
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
		return (int) Math.ceil((double) reports.size() / ITEMS_PER_PAGE);
	}

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
		String hourString = hour < 10 ? "0" + hour : String.valueOf(hour);
		String minuteString = minute < 10 ? "0" + minute : String.valueOf(minute);

		return new StringJoiner(" ")
				.add(calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH))
				.add(day + daySuffix + ",")
				.add(String.valueOf(calendar.get(Calendar.YEAR)))
				.add("at")
				.add(hourString + ":" + minuteString)
				.add(amPm)
				.toString();
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

			if (!currentLine.isEmpty()) {
				lore.add(currentLine.toString());
			}
			meta.setLore(lore);
		} else {
			meta.setLore(Collections.singletonList(value));
		}

		item.setItemMeta(meta);
		return item;
	}

	private static @Nullable List<Category> loadReportCategories() {
		if (isCategoryConfigInvalid()) {
			String errorMessage = ErrorMessages.getErrorMessage(23);
			plugin.getLogger().warning(errorMessage);
			logErrorMessage(errorMessage);
			return null;
		}

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
		String location = String.format("%s, %d, %d, %d",
				player.getWorld().getName(),
				player.getLocation().getBlockX(),
				player.getLocation().getBlockY(),
				player.getLocation().getBlockZ());

		String lastReportId = reports.stream()
				.filter(report -> report.contains("Report ID: "))
				.reduce((first, second) -> second)
				.map(report -> getReportByKey(report, "Report ID"))
				.orElse("0");
		String reportIDInt = String.valueOf(Integer.parseInt(lastReportId) + 1);

		String header = createReportHeader(playerName, playerUUID, worldName, categoryId, message,
				reportIDInt, location, gamemode);

		reports.add(header);
		bugReports.put(playerId, reports);

		updatePlanHook(playerId, playerName);

		String discordWebhookMessageID = sendDiscordWebhook(message, worldName, playerName, location, gamemode, categoryId, serverName);

		if (debugMode) {
			plugin.getLogger().info("Adding bug report to database...");
		}
		BugReportDatabase.addBugReport(playerName, playerId, worldName, header, message, location, gamemode, serverName, discordWebhookMessageID);

		sendBugReportNotifications(playerName);

		if (getServer().getMessenger().isIncomingChannelRegistered(BugReportPlugin.getPlugin(), "BungeeCord")) {
			PluginMessageListener.sendPluginMessage(player);
		}

		Bukkit.getScheduler().runTask(plugin, () -> {
			ReportCreatedEvent reportEvent = new ReportCreatedEvent(header);
			getServer().getPluginManager().callEvent(reportEvent);
		});
	}

	private @NotNull String createReportHeader(String playerName, String playerUUID, String worldName,
											   Integer categoryId, String message, String reportId,
											   String location, String gamemode) {
		return "Username: " + playerName + "\n" +
				"UUID: " + playerUUID + "\n" +
				"World: " + worldName + "\n" +
				"hasBeenRead: 0" + "\n" +
				"Category ID: " + categoryId + "\n" +
				"Full Message: " + message + "\n" +
				"Archived: 0" + "\n" +
				"Report ID: " + reportId + "\n" +
				"Timestamp: " + System.currentTimeMillis() + "\n" +
				"Location: " + location + "\n" +
				"Gamemode: " + gamemode;
	}

	private void updatePlanHook(UUID playerId, String playerName) {
		if (Bukkit.getPluginManager().isPluginEnabled("Plan")) {
			if (debugMode) {
				plugin.getLogger().info("Updating Plan hook for " + playerName + "...");
			}
			try {
				PlanHook.getInstance().updateHook(playerId, playerName);
			} catch (Exception e) {
				String errorMessage = ErrorMessages.getErrorMessage(47);
				plugin.getLogger().warning(errorMessage);
				logErrorMessage(errorMessage);
			}
		}
	}

	private @Nullable String sendDiscordWebhook(String message, String worldName, String playerName,
												String location, String gamemode, Integer categoryId,
												String serverName) {
		if (!config.getBoolean("enableDiscordWebhook", true)) {
			return null;
		}

		if (debugMode) {
			plugin.getLogger().info("Sending bug report to Discord...");
		}

		String webhookURL = config.getString("webhookURL", "");
		if (webhookURL.isEmpty()) {
			String errorMessage = ErrorMessages.getErrorMessage(24);
			plugin.getLogger().warning(errorMessage);
			logErrorMessage(errorMessage);
			return null;
		}

		try {
			String messageId = discord.sendBugReport(message, worldName, playerName,
					location, gamemode, categoryId, serverName);
			if (debugMode) {
				plugin.getLogger().info("Bug report sent to Discord.");
			}
			return messageId;
		} catch (Exception e) {
			String errorMessage = ErrorMessages.getErrorMessageWithAdditionalMessage(25, e.getMessage());
			plugin.getLogger().warning(errorMessage);
			logErrorMessage(errorMessage);
			return null;
		}
	}

	private void sendBugReportNotifications(String playerName) {
		if (!config.getBoolean("enableBugReportNotifications", true)) {
			return;
		}

		if (debugMode) {
			plugin.getLogger().info("Sending bug report notification to online players...");
		}

		String defaultMessage = returnStartingMessage(ChatColor.GRAY) +
				getValueFromLanguageFile("bugReportNotificationMessage",
						"A new bug report has been submitted by %player%!")
						.replace("%player%", ChatColor.AQUA + playerName + ChatColor.GRAY);

		for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
			if (onlinePlayer.hasPermission("bugreport.notify")) {
				onlinePlayer.sendMessage(defaultMessage);
			}
		}
	}

	public static class BugReportListener implements Listener {
		private final Map<UUID, Boolean> closingInventoryMap;

		public BugReportListener() {
			this.closingInventoryMap = new HashMap<>();
		}

		@EventHandler(priority = EventPriority.NORMAL)
		public void onInventoryClick(@NotNull InventoryClickEvent event) {
			String titleText = ChatColor.stripColor(event.getView().getTitle());

			if (debugMode) {
				plugin.getLogger().info("Clicked inventory: " + titleText);
			}

			boolean isArchivedGUI = titleText.startsWith("Archived Bugs");
			if (!titleText.startsWith("Bug Report") && !isArchivedGUI) {
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
				List<String> reports = bugReports.getOrDefault(getStaticUUID(),
						new ArrayList<>(Collections.singletonList("DUMMY")));
				String report = reports.stream()
						.filter(r -> r.contains("Report ID: " + reportID))
						.findFirst()
						.orElse(null);

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
						setCurrentPage(player, currentPage - 1);
						playButtonClickSound(player);
						localCurrentPage = currentPage - 1;
						player.openInventory(isArchivedGUI ?
								getArchivedBugReportsGUI(localCurrentPage) :
								getBugReportGUI(localCurrentPage));
					}
				}
				case "buttonNames.forward" -> {
					int currentPage = getCurrentPage(player);
					if (currentPage < getTotalPages()) {
						setCurrentPage(player, currentPage + 1);
						playButtonClickSound(player);
						localCurrentPage = currentPage + 1;
						player.openInventory(isArchivedGUI ?
								getArchivedBugReportsGUI(localCurrentPage) :
								getBugReportGUI(localCurrentPage));
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

	public record BugReportDetailsListener(
			Inventory gui,
			Integer reportIDGUI,
			String report,
			Boolean isArchivedGUI
	) implements Listener {
		@EventHandler(priority = EventPriority.NORMAL)
		public void onInventoryClick(@NotNull InventoryClickEvent event) {
			String title = event.getView().getTitle();
			boolean isArchivedDetails = title.startsWith(ChatColor.YELLOW + "Archived Bug Details");

			if (!title.startsWith(ChatColor.YELLOW + "Bug Report Details - #") && !isArchivedDetails) {
				return;
			}

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
				player.openInventory(BugReportSettings.getStatusSelectionGUI(reportIDGUI, isArchivedGUI));
				return;
			}

			if (itemName.contains("(Click to teleport)")) {
				handleTeleportAction(player, reportIDGUI);
				return;
			}

			String customDisplayName = getKeyFromTranslation(itemName);
			if (customDisplayName == null || customDisplayName.equals(" ")) {
				return;
			}

			switch (customDisplayName) {
				case "buttonNames.back" -> {
					playButtonClickSound(player);
					player.openInventory(isArchivedDetails ?
							getArchivedBugReportsGUI(localCurrentPage) :
							getBugReportGUI(localCurrentPage));
				}
				case "buttonNames.unarchive" -> handleUnarchiveAction(player, reportIDGUI);
				case "buttonNames.archive" -> handleArchiveAction(player, reportIDGUI, bugReportID, isArchivedDetails);
				case "buttonNames.delete" -> handleDeleteAction(player, reportIDGUI, bugReportID, isArchivedDetails);
			}

			HandlerList.unregisterAll(this);
		}

		private void handleTeleportAction(Player player, Integer reportID) {
			playButtonClickSound(player);

			if (debugMode) {
				plugin.getLogger().info("Teleporting to the location of bug report #" + reportID + "...");
			}

			String teleportMessage = returnStartingMessage(ChatColor.GREEN) + " Teleporting to the location of Bug Report #" + reportID + ".";

			if (checkForKey("useTitleInsteadOfMessage", true)) {
				player.sendTitle(teleportMessage + "." + ".", " ", 10, 70, 20);
			} else {
				player.sendMessage(teleportMessage);
			}

			Location teleportLocation = BugReportDatabase.getBugReportLocation(reportID);
			if (teleportLocation != null) {
				player.teleport(teleportLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
			} else {
				player.sendMessage(returnStartingMessage(ChatColor.RED) + " The location of Bug Report #" + reportID + " is not available.");
				player.closeInventory();
			}

			HandlerList.unregisterAll(this);
		}

		private void handleUnarchiveAction(Player player, Integer reportID) {
			playButtonClickSound(player);
			BugReportDatabase.updateBugReportArchive(reportID, 0);

			if (debugMode) {
				plugin.getLogger().info("Unarchiving bug report #" + reportID + "...");
			}

			if (config.getBoolean("enableDiscordWebhook", true)) {
				if (debugMode) {
					plugin.getLogger().info("Sending unarchive notification to Discord...");
				}

				String bugReportDiscordWebhookID = BugReportDatabase.getBugReportDiscordWebhookMessageID(reportID);
				if (bugReportDiscordWebhookID != null) {
					Map<String, String> fullBugReportSplit = BugReportDatabase.getBugReportById(reportIDGUI);

					String Username = fullBugReportSplit.get("Username");
					String UUID = fullBugReportSplit.get("UUID");
					String World = fullBugReportSplit.get("World");
					String FullMessage = fullBugReportSplit.get("FullMessage");
					String CategoryID = fullBugReportSplit.get("CategoryID");
					String Location = fullBugReportSplit.get("Location");
					String Gamemode = fullBugReportSplit.get("Gamemode");
					String Status = fullBugReportSplit.get("Status");
					String ServerName = fullBugReportSplit.get("ServerName");

					LinkDiscord.modifyNotification(
							Username,
							UUID,
							World,
							Location,
							Gamemode,
							Status,
							CategoryID,
							ServerName,
							FullMessage,
							bugReportDiscordWebhookID,
							Color.GREEN,
							"Bug Report #" + reportID + " has been unarchived."
					);
				} else {
					String errorMessage = ErrorMessages.getErrorMessage(25);
					plugin.getLogger().warning(errorMessage);
					logErrorMessage(errorMessage);
				}
			}

			player.openInventory(getBugReportGUI(localCurrentPage));
			player.sendMessage(returnStartingMessage(ChatColor.YELLOW) + " Bug Report #" + reportID + " has been unarchived.");

			HandlerList.unregisterAll(this);
		}

		private void handleArchiveAction(@NotNull Player player, Integer reportID, String bugReportID, boolean isArchivedDetails) {
			if (player.hasPermission("bugreport.archive") || player.hasPermission("bugreport.admin")) {
				playButtonClickSound(player);

				if (debugMode) {
					plugin.getLogger().info("Archiving bug report #" + reportID + "...");
				}

				Bukkit.getPluginManager().registerEvents(
						new BugReportConfirmationGUI.BugReportConfirmationListener(gui, reportID, isArchivedDetails),
						plugin
				);

				BugReportConfirmationGUI.openConfirmationGUI(player, true);
			} else {
				player.closeInventory();
				player.sendMessage(returnStartingMessage(ChatColor.RED) + " You don't have permission to archive bug reports!");
			}
		}

		private void handleDeleteAction(@NotNull Player player, Integer reportID, String bugReportID,
										boolean isArchivedDetails) {
			if (player.hasPermission("bugreport.delete") || player.hasPermission("bugreport.admin")) {
				playButtonClickSound(player);

				if (debugMode) {
					plugin.getLogger().info("Opening confirmation GUI for deletion on Bug Report #" + reportID + "...");
				}

				Bukkit.getPluginManager().registerEvents(
						new BugReportConfirmationGUI.BugReportConfirmationListener(gui, reportID, isArchivedDetails),
						plugin
				);

				BugReportConfirmationGUI.openConfirmationGUI(player, false);
			} else {
				player.closeInventory();
				player.sendMessage(returnStartingMessage(ChatColor.RED) + " You don't have permission to delete bug reports!");
			}
		}
	}
}
