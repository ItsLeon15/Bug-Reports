package com.leon.bugreport.gui;

import com.leon.bugreport.keys.guiTextures;
import com.leon.bugreport.logging.ErrorMessages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.Serial;
import java.util.*;

import static com.leon.bugreport.API.DataSource.getPlayerHead;
import static com.leon.bugreport.API.ErrorClass.logErrorMessage;
import static com.leon.bugreport.BugReportLanguage.getValueFromLanguageFile;
import static com.leon.bugreport.BugReportManager.*;
import static com.leon.bugreport.BugReportSettings.createCustomPlayerHead;

public class bugreportGUI {
	private static final Map<String, String> bugReportItems = new HashMap<>() {
		@Serial
		private static final long serialVersionUID = 2870322851221649721L;

		{
			put("BugReporter", getValueFromLanguageFile("buttonNames.bugReportDetailsUsername", "Username"));
			put("BugReportUUID", getValueFromLanguageFile("buttonNames.bugReportDetailsUUID", "UUID"));
			put("BugReportWorld", getValueFromLanguageFile("buttonNames.bugReportDetailsWorld", "World"));
			put("BugReportMessage", getValueFromLanguageFile("buttonNames.bugReportDetailsFullMessage", "Full Message"));
			put("BugReportCategory", getValueFromLanguageFile("buttonNames.bugReportDetailsCategory", "Category ID"));
			put("BugReportStatus", getValueFromLanguageFile("buttonNames.bugReportDetailsStatus", "Status") + " (Click to change)");
			put("BugReportTimestamp", getValueFromLanguageFile("buttonNames.bugReportDetailsTimestamp", "Timestamp"));
			put("BugReportLocation", getValueFromLanguageFile("buttonNames.bugReportDetailsLocation", "Location") + ChatColor.BOLD + " (Click to teleport)");
			put("BugReportGamemode", getValueFromLanguageFile("buttonNames.bugReportDetailsGamemode", "Gamemode"));
			put("BugReportServerName", getValueFromLanguageFile("buttonNames.bugReportDetailsServerName", "Server Name"));
			put("BugReportUnArchive", getValueFromLanguageFile("buttonNames.unarchive", "Unarchive"));
			put("BugReportArchive", getValueFromLanguageFile("buttonNames.archive", "Archive"));
			put("BugReportBack", getValueFromLanguageFile("buttonNames.back", "Back"));
			put("BugReportDelete", getValueFromLanguageFile("buttonNames.delete", "Delete"));
		}
	};

	private static final List<String> TEXTURE_SUPPORTED_ITEMS = List.of(
			"BugReportUUID", "BugReportWorld", "BugReportMessage", "BugReportCategory",
			"BugReportTimestamp", "BugReportLocation", "BugReportGamemode", "BugReportServerName",
			"BugReportUnArchive", "BugReportArchive", "BugReportDelete"
	);

	public static void generateNewYML() {
		File configFile = new File(plugin.getDataFolder(), "custom_bug_report_details_GUI.yml");
		if (!configFile.exists()) {
			plugin.saveResource("custom_bug_report_details_GUI.yml", false);
		}
	}

	public static void updateBugReportItems() {
		bugReportItems.put("BugReportUnArchive", getValueFromLanguageFile("buttonNames.unarchive", "Unarchive"));
		bugReportItems.put("BugReportArchive", getValueFromLanguageFile("buttonNames.archive", "Archive"));
		bugReportItems.put("BugReportBack", getValueFromLanguageFile("buttonNames.back", "Back"));
		bugReportItems.put("BugReportDelete", getValueFromLanguageFile("buttonNames.delete", "Delete"));
	}

	private static @Nullable YamlConfiguration loadGUIConfig() {
		File configFile = new File(plugin.getDataFolder(), "custom_bug_report_details_GUI.yml");
		if (!configFile.exists()) {
			plugin.getLogger().info("custom_bug_report_details_GUI.yml not found. Falling back to default GUI settings.");
			return null;
		}
		return YamlConfiguration.loadConfiguration(configFile);
	}

	public static void openBugReportDetailsGUI(Player player, String report, Integer reportIDGUI, Boolean isArchivedGUI) {
		for (HandlerList handlerList : HandlerList.getHandlerLists()) {
			RegisteredListener[] registeredListeners = handlerList.getRegisteredListeners();
			for (RegisteredListener registeredListener : registeredListeners) {
				if (registeredListener.getListener() instanceof BugReportDetailsListener) {
					handlerList.unregister(registeredListener);
				}
			}
		}

		updateBugReportItems();
		YamlConfiguration guiConfig = loadGUIConfig();
		String bugReportTitle = isArchivedGUI ? "Archived Bug Details - #" : "Bug Report Details - #";
		int guiSize = determineGUISize(guiConfig);

		Inventory gui = Bukkit.createInventory(player, guiSize, ChatColor.YELLOW + bugReportTitle + reportIDGUI);

		if (guiConfig == null || !validateGUIConfig(guiConfig)) {
			setupDefaultGUI(gui, player, report, reportIDGUI, isArchivedGUI);
		} else {
			setupGUIFromConfig(gui, player, guiConfig, report, reportIDGUI, isArchivedGUI);
		}

		player.openInventory(gui);
		Bukkit.getPluginManager().registerEvents(new BugReportDetailsListener(gui, reportIDGUI, report, isArchivedGUI), plugin);
	}

	private static int determineGUISize(YamlConfiguration guiConfig) {
		if (guiConfig != null && validateGUIConfig(guiConfig)) {
			int size = guiConfig.getInt("bug_report_details_GUI.guiSize");
			if (size >= 9 && size <= 54 && size % 9 == 0) {
				return size;
			}
			Bukkit.getLogger().warning("Invalid GUI size in customGUI.yml. Falling back to default size.");
		}
		return 45;
	}

	private static void setupGUIFromConfig(Inventory gui, Player player, @NotNull YamlConfiguration guiConfig,
										   String report, Integer reportIDGUI, Boolean isArchivedGUI) {
		if (!validateGUIConfig(guiConfig)) {
			String errorMessage = ErrorMessages.getErrorMessage(44);
			plugin.getLogger().severe(errorMessage);
			logErrorMessage(errorMessage);
			setupDefaultGUI(gui, player, report, reportIDGUI, isArchivedGUI);
			return;
		}

		List<?> itemsList = guiConfig.getList("bug_report_details_GUI.items");
		Map<String, String> reportDetails = parseReportDetails(report);

		for (Object itemObj : Objects.requireNonNull(itemsList)) {
			if (!(itemObj instanceof Map<?, ?> itemMap)) continue;

			try {
				int slot = Integer.parseInt(itemMap.get("slot").toString());
				String bugReportItem = itemMap.get("bugReportItem").toString();

				if ((isArchivedGUI && bugReportItem.equals("BugReportArchive")) ||
						(!isArchivedGUI && bugReportItem.equals("BugReportUnArchive"))) {
					continue;
				}

				String materialKey = itemMap.get("material").toString();
				Material material = resolveMaterial(materialKey, isArchivedGUI);

				Object textureObj = itemMap.get("texture");
				String texture = textureObj != null ? textureObj.toString() : "";

				ItemStack itemStack;
				if (bugReportItem.equals("BugReportMessage")) {
					itemStack = createMessageItem(bugReportItem, material, texture, reportDetails, isArchivedGUI);
				} else {
					itemStack = createItemForReportDetail(bugReportItem, material, texture, reportDetails, isArchivedGUI);
				}

				gui.setItem(slot, itemStack);
			} catch (IllegalArgumentException e) {
				Bukkit.getLogger().warning("Error parsing material or slot number: " + e.getMessage());
			}
		}
	}

	private static Material resolveMaterial(@NotNull String materialKey, boolean isArchivedGUI) {
		if (materialKey.contains("[") && materialKey.contains("]")) {
			String[] materials = materialKey.replaceAll("[\\[\\]]", "").split(",\\s*");
			return Material.valueOf(isArchivedGUI ? materials[1].trim() : materials[0].trim());
		}
		return Material.valueOf(materialKey);
	}

	private static @NotNull ItemStack createMessageItem(String bugReportItem, Material material, String texture,
														Map<String, String> reportDetails, boolean isArchivedGUI) {
		ItemStack messageItem = createItemForReportDetail(bugReportItem, material, texture, reportDetails, isArchivedGUI);
		ItemMeta meta = messageItem.getItemMeta();
		if (meta == null) return messageItem;

		String fullMessage = reportDetails.getOrDefault("Full Message", "N/A");
		if (fullMessage.length() > 32) {
			List<String> lore = formatLongMessage(fullMessage);
			meta.setLore(lore);
		} else {
			meta.setLore(Collections.singletonList(ChatColor.WHITE + fullMessage));
		}

		messageItem.setItemMeta(meta);
		return messageItem;
	}

	private static @NotNull List<String> formatLongMessage(@NotNull String message) {
		List<String> lore = new ArrayList<>();
		String[] words = message.split(" ");
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

		return lore;
	}

	private static boolean validateGUIConfig(@NotNull YamlConfiguration guiConfig) {
		int guiSize = guiConfig.getInt("bug_report_details_GUI.guiSize", -1);
		if (guiSize < 9 || guiSize > 54 || guiSize % 9 != 0) {
			Bukkit.getLogger().warning("Invalid GUI size defined in customGUI.yml. Must be between 9 and 54 and a multiple of 9.");
			return false;
		}

		List<?> itemsList = guiConfig.getList("bug_report_details_GUI.items");
		if (itemsList == null || itemsList.isEmpty()) {
			Bukkit.getLogger().warning("No items defined in customGUI.yml or incorrect format.");
			return false;
		}

		for (Object itemObj : itemsList) {
			if (!(itemObj instanceof Map<?, ?> itemMap)) {
				return false;
			}

			if (!itemMap.containsKey("slot") || !itemMap.containsKey("bugReportItem") || !itemMap.containsKey("material")) {
				return false;
			}

			try {
				int slot = Integer.parseInt(itemMap.get("slot").toString());
				if (slot >= guiSize) {
					Bukkit.getLogger().warning("Slot number in customGUI.yml is greater than the GUI size.");
					return false;
				}
			} catch (NumberFormatException e) {
				return false;
			}

			String itemKey = itemMap.get("bugReportItem").toString();
			if (!bugReportItems.containsKey(itemKey)) {
				Bukkit.getLogger().warning("Invalid bugReportItem in customGUI.yml: " + itemKey);
				return false;
			}

			if ("BugReporter".equals(itemKey) && !"%player_texture%".equals(itemMap.get("texture"))) {
				Bukkit.getLogger().warning("Texture for BugReporter item in customGUI.yml must be %player_texture%.");
				return false;
			}
		}

		return true;
	}

	private static @NotNull ItemStack createItemForReportDetail(
			@NotNull String bugReportItemKey, Material defaultMaterial,
			@Nullable String textureBase64,
			@NotNull Map<String, String> reportDetails,
			Boolean isArchivedGUI
	) {
		if (bugReportItemKey.equals("BugReporter")) {
			return createBugReporterItem(reportDetails);
		}

		String detailValue = getDetailValue(bugReportItemKey, reportDetails, isArchivedGUI);

		if (bugReportItemKey.equals("BugReportStatus")) {
			ItemStack statusItem = createStatusItem(reportDetails, isArchivedGUI);
			if (statusItem != null) return statusItem;
		}

		boolean useTexture = TEXTURE_SUPPORTED_ITEMS.contains(bugReportItemKey) &&
				textureBase64 != null && !textureBase64.trim().isEmpty();

		if (useTexture) {
			return createCustomPlayerHead(textureBase64,
					bugReportItems.getOrDefault(bugReportItemKey, "Unknown Item"),
					1);
		} else {
			return createBasicItem(defaultMaterial, bugReportItemKey, detailValue);
		}
	}

	private static @NotNull ItemStack createBugReporterItem(@NotNull Map<String, String> reportDetails) {
		String username = reportDetails.get("Username");
		ItemStack item;

		if (config.getBoolean("enablePlayerHeads")) {
			item = getPlayerHead(username);
		} else {
			item = createInfoItem(Material.PLAYER_HEAD,
					ChatColor.GOLD + "Username",
					ChatColor.WHITE + username,
					false);
		}

		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(ChatColor.YELLOW + bugReportItems.getOrDefault("BugReporter", "Unknown Item"));
			meta.setLore(Collections.singletonList(ChatColor.WHITE + username));
			item.setItemMeta(meta);
		}

		return item;
	}

	private static @Nullable String getDetailValue(String bugReportItemKey, @NotNull Map<String, String> reportDetails, boolean isArchivedGUI) {
		String reportDetailKey = deriveReportDetailKey(bugReportItemKey);
		String detailValue = reportDetails.getOrDefault(reportDetailKey, "N/A");

		switch (bugReportItemKey) {
			case "BugReportTimestamp" -> {
				return translateTimestampToDate(Long.parseLong(detailValue));
			}
			case "BugReportCategory" -> {
				String categoryID = reportDetails.getOrDefault("Category ID", "N/A");
				if (!"N/A".equals(categoryID)) {
					List<Map<?, ?>> categoryList = config.getMapList("reportCategories");
					return categoryList.stream()
							.filter(categoryMap -> categoryID.equals(String.valueOf(categoryMap.get("id"))))
							.map(categoryMap -> (String) categoryMap.get("name"))
							.findFirst()
							.orElse("Unknown Category");
				}
				return detailValue;
			}
			case "BugReportStatus" -> {
				return isArchivedGUI ? "Archived" : "Active";
			}
			case "BugReportBack", "BugReportDelete", "BugReportArchive", "BugReportUnArchive" -> {
				return null;
			}
			default -> {
				return detailValue;
			}
		}
	}

	private static @Nullable ItemStack createStatusItem(@NotNull Map<String, String> reportDetails, boolean isArchivedGUI) {
		String status = reportDetails.get("Status");
		if (status == null) {
			return createInfoItem(
					isArchivedGUI ? Material.RED_DYE : Material.LIME_DYE,
					ChatColor.GOLD + "Status (Click to change)",
					ChatColor.WHITE + (isArchivedGUI ? "Archived" : "Open"),
					false
			);
		}

		List<Map<?, ?>> statuses = config.getMapList("statuses");

		if (status.equals("0")) {
			Material statusMaterial = isArchivedGUI ? Material.RED_DYE : Material.LIME_DYE;
			ChatColor statusColor = isArchivedGUI ? ChatColor.RED : ChatColor.GREEN;
			String statusText = isArchivedGUI ? "Archived" : "Active";

			return createInfoItem(statusMaterial,
					statusColor + "Status (Click to change)",
					statusColor + statusText,
					false);
		}

		for (Map<?, ?> statusMap : statuses) {
			if (statusMap.get("id").toString().equals(status)) {
				String statusName = statusMap.get("name").toString();
				String statusDesc = statusMap.get("description").toString();
				ChatColor statusColor = ChatColor.valueOf(statusMap.get("color").toString().toUpperCase());

				String iconStr = statusMap.get("icon").toString();
				Material statusIcon = Material.matchMaterial(iconStr) != null ?
						Material.matchMaterial(iconStr) : Material.BARRIER;

				return createInfoItem(statusIcon,
						statusColor + statusName + " (Click to change)",
						statusColor + statusDesc,
						false);
			}
		}

		return null;
	}

	private static @NotNull ItemStack createBasicItem(Material material, String itemKey, String detailValue) {
		ItemStack item = new ItemStack(material, 1);
		ItemMeta meta = item.getItemMeta();

		if (meta != null) {
			meta.setDisplayName(ChatColor.YELLOW + bugReportItems.getOrDefault(itemKey, "Unknown Item"));
			if (detailValue != null) {
				meta.setLore(Collections.singletonList(ChatColor.WHITE + detailValue));
			}
			item.setItemMeta(meta);
		}

		return item;
	}

	@Contract(pure = true)
	private static String deriveReportDetailKey(@NotNull String bugReportItemKey) {
		return switch (bugReportItemKey) {
			case "BugReporter" -> getValueFromLanguageFile("buttonNames.bugReportDetailsUsername", "Username");
			case "BugReportUUID" -> getValueFromLanguageFile("buttonNames.bugReportDetailsUUID", "UUID");
			case "BugReportWorld" -> getValueFromLanguageFile("buttonNames.bugReportDetailsWorld", "World");
			case "BugReportMessage" -> getValueFromLanguageFile("buttonNames.bugReportDetailsFullMessage", "Full Message");
			case "BugReportCategory" -> getValueFromLanguageFile("buttonNames.bugReportDetailsCategory", "Category ID");
			case "BugReportStatus" -> getValueFromLanguageFile("buttonNames.bugReportDetailsStatus", "Status") + " (Click to change)";
			case "BugReportTimestamp" -> getValueFromLanguageFile("buttonNames.bugReportDetailsTimestamp", "Timestamp");
			case "BugReportLocation" -> getValueFromLanguageFile("buttonNames.bugReportDetailsLocation", "Location") + ChatColor.BOLD + " (Click to teleport)";
			case "BugReportGamemode" -> getValueFromLanguageFile("buttonNames.bugReportDetailsGamemode", "Gamemode");
			case "BugReportServerName" -> getValueFromLanguageFile("buttonNames.bugReportDetailsServerName", "Server Name");
			case "BugReportUnArchive" -> getValueFromLanguageFile("buttonNames.unarchive", "Unarchive");
			case "BugReportArchive" -> getValueFromLanguageFile("buttonNames.archive", "Archive");
			case "BugReportBack" -> getValueFromLanguageFile("buttonNames.back", "Back");
			case "BugReportDelete" -> getValueFromLanguageFile("buttonNames.delete", "Delete");
			default -> bugReportItemKey.replace("BugReport", "");
		};
	}

	private static @NotNull Map<String, String> parseReportDetails(@NotNull String report) {
		Map<String, String> details = new HashMap<>();
		String[] lines = report.split("\n");

		for (String line : lines) {
			String[] parts = line.split(":", 2);
			if (parts.length == 2) {
				String key = parts[0].trim();
				String value = parts[1].trim();
				details.put(key, value);
			}
		}

		return details;
	}

	public static void setupDefaultGUI(Inventory gui, Player player, String report, Integer reportIDGUI, Boolean isArchivedGUI) {
		if (report == null) {
			player.sendMessage(returnStartingMessage(ChatColor.RED) + " Error 101: Report is null. Please report this to the plugin developer.");
			return;
		}

		Map<String, String> reportInfo = extractReportInfo(report);
		setupCommonGuiItems(gui, reportInfo, isArchivedGUI);
		addCategoryItem(gui, reportInfo.get("category"));

		player.openInventory(gui);
		Bukkit.getPluginManager().registerEvents(new BugReportDetailsListener(gui, reportIDGUI, report, isArchivedGUI), plugin);
	}

	private static @NotNull Map<String, String> extractReportInfo(String report) {
		Map<String, String> info = new HashMap<>();

		info.put("username", getReportByKey(report, "Username"));
		info.put("uuid", getReportByKey(report, "UUID"));
		info.put("world", getReportByKey(report, "World"));
		info.put("fullMessage", getReportByKey(report, "Full Message"));
		info.put("category", getReportByKey(report, "Category ID"));
		info.put("location", getReportByKey(report, "Location"));
		info.put("gamemode", getReportByKey(report, "Gamemode"));
		info.put("status", getReportByKey(report, "Status"));
		info.put("serverName", getReportByKey(report, "serverName"));
		info.put("timestamp", getReportByKey(report, "Timestamp"));

		if (info.get("location") == null || info.get("location").equals("null")) {
			info.put("location", "Not found");
		}

		if (info.get("location").length() - info.get("location").replace(",", "").length() != 3) {
			info.put("location", "Not found");
			info.put("locationTitle", "Location");
		} else {
			info.put("locationTitle", "Location " + ChatColor.BOLD + "(Click to teleport)");
		}

		if (info.get("gamemode") == null || info.get("gamemode").equals("null")) {
			info.put("gamemode", "Unknown");
		}

		return info;
	}

	private static void setupCommonGuiItems(@NotNull Inventory gui, Map<String, String> info, Boolean isArchivedGUI) {
		ItemStack emptyItem = createEmptyItem();
		for (int i = 0; i < gui.getSize(); i++) {
			gui.setItem(i, emptyItem);
		}

		ItemStack usernameItem;
		if (config.getBoolean("enablePlayerHeads")) {
			usernameItem = getPlayerHead(info.get("username"));
		} else {
			usernameItem = createInfoItem(Material.PLAYER_HEAD, ChatColor.GOLD + "Username", ChatColor.WHITE + info.get("username"), false);
		}

		boolean isLongMessage = info.get("fullMessage").length() > 32;
		String timestampToDate = translateTimestampToDate(Long.parseLong(info.get("timestamp")));

		ItemStack uuidItem = createInfoItem(Material.NAME_TAG, ChatColor.GOLD + "UUID", ChatColor.WHITE + info.get("uuid"), false);
		ItemStack worldItem = createInfoItem(Material.GRASS_BLOCK, ChatColor.GOLD + "World", ChatColor.WHITE + info.get("world"), false);
		ItemStack messageItem = createInfoItem(Material.PAPER, ChatColor.GOLD + "Full Message", ChatColor.WHITE + info.get("fullMessage"), isLongMessage);
		ItemStack serverNameItem = createInfoItem(Material.COMPASS, ChatColor.GOLD + "Server Name", ChatColor.WHITE + info.get("serverName"), false);
		ItemStack statusItem = createStatusItemForDefaultGUI(info.get("status"), isArchivedGUI);
		ItemStack timestampItem = createInfoItem(Material.CLOCK, ChatColor.GOLD + "Timestamp", ChatColor.WHITE + timestampToDate, false);
		ItemStack locationItem = createInfoItem(Material.COMPASS, ChatColor.GOLD + info.get("locationTitle"), ChatColor.WHITE + info.get("location"), false);
		ItemStack gamemodeItem = createInfoItem(Material.DIAMOND_SWORD, ChatColor.GOLD + "Gamemode", ChatColor.WHITE + info.get("gamemode"), false);

		ItemStack backButton = createButton(Material.BARRIER, ChatColor.RED + getValueFromLanguageFile("buttonNames.back", "Back"));
		ItemStack archiveButton = createCustomPlayerHead(guiTextures.archiveTexture, ChatColor.YELLOW + getValueFromLanguageFile("buttonNames.archive", "Archive"), 16);
		ItemStack unarchiveButton = createCustomPlayerHead(guiTextures.unarchiveTexture, ChatColor.YELLOW + getValueFromLanguageFile("buttonNames.unarchive", "Unarchive"), 17);
		ItemStack deleteButton = createCustomPlayerHead(guiTextures.deleteTexture, ChatColor.YELLOW + getValueFromLanguageFile("buttonNames.delete", "Delete"), 18);

		gui.setItem(0, usernameItem);
		gui.setItem(2, uuidItem);
		gui.setItem(4, worldItem);
		gui.setItem(6, messageItem);
		gui.setItem(8, serverNameItem);
		gui.setItem(20, statusItem);
		gui.setItem(22, timestampItem);
		gui.setItem(24, locationItem);
		gui.setItem(26, gamemodeItem);
		gui.setItem(38, !isArchivedGUI ? archiveButton : unarchiveButton);
		gui.setItem(40, backButton);
		gui.setItem(42, deleteButton);
	}

	private static @NotNull ItemStack createStatusItemForDefaultGUI(String status, boolean isArchivedGUI) {
		if (status == null) {
			return createInfoItem(
					isArchivedGUI ? Material.RED_DYE : Material.LIME_DYE,
					ChatColor.GOLD + "Status (Click to change)",
					ChatColor.WHITE + (isArchivedGUI ? "Archived" : "Open"),
					false
			);
		}

		List<Map<?, ?>> statuses = config.getMapList("statuses");

		if (status.equals("0")) {
			Material statusMaterial = isArchivedGUI ? Material.RED_DYE : Material.LIME_DYE;
			ChatColor statusColor = isArchivedGUI ? ChatColor.RED : ChatColor.GREEN;
			String statusText = isArchivedGUI ? "Archived" : "Active";

			return createInfoItem(statusMaterial,
					statusColor + "Status (Click to change)",
					statusColor + statusText,
					false);
		}

		for (Map<?, ?> statusMap : statuses) {
			if (statusMap.get("id").toString().equals(status)) {
				String statusName = statusMap.get("name").toString();
				String statusColor = statusMap.get("color").toString();
				ChatColor statusChatColor = ChatColor.valueOf(statusColor.toUpperCase());
				String statusMaterial = statusMap.get("icon").toString().toUpperCase()
						.replace("MINECRAFT:", "");

				return createInfoItem(Material.valueOf(statusMaterial),
						statusChatColor + "Status (Click to change)",
						statusChatColor + statusName,
						false);
			}
		}

		return createInfoItem(Material.BARRIER,
				ChatColor.GOLD + "Status (Click to change)",
				ChatColor.GOLD + "Unknown",
				false);
	}

	private static void addCategoryItem(Inventory gui, String category) {
		if (category != null && !"null".equals(category) && !category.isEmpty()) {
			List<Map<?, ?>> categoryList = config.getMapList("reportCategories");

			Optional<String> categoryNameOptional = categoryList.stream()
					.filter(categoryMap -> {
						try {
							return Integer.parseInt(categoryMap.get("id").toString()) == Integer.parseInt(category);
						} catch (NumberFormatException e) {
							return false;
						}
					})
					.map(categoryMap -> categoryMap.get("name").toString())
					.findFirst();

			String categoryName = categoryNameOptional.orElse("None");
			ItemStack categoryItem = createInfoItem(Material.CHEST,
					ChatColor.GOLD + "Category Name",
					ChatColor.WHITE + categoryName,
					false);
			gui.setItem(18, categoryItem);
		} else {
			ItemStack categoryItem = createInfoItem(Material.CHEST,
					ChatColor.GOLD + "Category Name",
					ChatColor.WHITE + "None",
					false);
			gui.setItem(18, categoryItem);
		}
	}
}
