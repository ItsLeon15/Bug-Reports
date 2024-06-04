package com.leon.bugreport.gui;

import com.leon.bugreport.API.ErrorClass;
import com.leon.bugreport.keys.guiTextures;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.Serial;
import java.util.*;

import static com.leon.bugreport.API.DataSource.getPlayerHead;
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

	public static void generateNewYML() {
		if (debugMode) {
			ErrorClass.throwDebug("bugreportGUI: Starting generateNewYML", "debug");
		}
		File configFile = new File(plugin.getDataFolder(), "custom_bug_report_details_GUI.yml");
		if (!configFile.exists()) {
			if (debugMode) {
				ErrorClass.throwDebug("bugreportGUI: Created custom_bug_report_details_GUI.yml", "debug");
			}
			plugin.saveResource("custom_bug_report_details_GUI.yml", false);
		}
	}

	public static void updateBugReportItems() {
		if (debugMode) {
			ErrorClass.throwDebug("bugreportGUI: Starting updateBugReportItems", "debug");
		}
		bugReportItems.put("BugReportUnArchive", getValueFromLanguageFile("buttonNames.unarchive", "Unarchive"));
		bugReportItems.put("BugReportArchive", getValueFromLanguageFile("buttonNames.archive", "Archive"));
		bugReportItems.put("BugReportBack", getValueFromLanguageFile("buttonNames.back", "Back"));
		bugReportItems.put("BugReportDelete", getValueFromLanguageFile("buttonNames.delete", "Delete"));
	}

	private static @Nullable YamlConfiguration loadGUIConfig() {
		if (debugMode) {
			ErrorClass.throwDebug("bugreportGUI: Starting loadGUIConfig", "debug");
		}
		File configFile = new File(plugin.getDataFolder(), "custom_bug_report_details_GUI.yml");
		if (!configFile.exists()) {
			ErrorClass.throwDebug("custom_bug_report_details_GUI.yml not found. Falling back to default GUI settings.", "error");
			return null;
		}
		return YamlConfiguration.loadConfiguration(configFile);
	}

	public static void openBugReportDetailsGUI(Player player, String report, Integer reportIDGUI, Boolean isArchivedGUI) {
		if (debugMode) {
			ErrorClass.throwDebug("bugreportGUI: Starting openBugReportDetailsGUI", "debug");
		}
		updateBugReportItems();
		YamlConfiguration guiConfig = loadGUIConfig();
		String bugReportTitle = isArchivedGUI ? "Archived Bug Details - #" : "Bug Report Details - #";
		int guiSize;

		if (guiConfig != null && validateGUIConfig(guiConfig)) {
			guiSize = guiConfig.getInt("bug_report_details_GUI.guiSize");
			if (guiSize < 9 || guiSize > 54 || guiSize % 9 != 0) {
				guiSize = 45;
				ErrorClass.throwDebug("Invalid GUI size in customGUI.yml. Falling back to default size.", "warning");
			}
		} else {
			guiSize = 45;
		}

		Inventory gui = Bukkit.createInventory(player, guiSize, ChatColor.YELLOW + bugReportTitle + reportIDGUI);

		if (guiConfig == null || !validateGUIConfig(guiConfig)) {
			setupDefaultGUI(gui, player, report, reportIDGUI, isArchivedGUI);
		} else {
			setupGUIFromConfig(gui, player, guiConfig, report, reportIDGUI, isArchivedGUI);
		}

		player.openInventory(gui);
		Bukkit.getPluginManager().registerEvents(new BugReportDetailsListener(gui, reportIDGUI), plugin);
	}

	private static void setupGUIFromConfig(Inventory gui, Player player, @NotNull YamlConfiguration guiConfig, String report, Integer reportIDGUI, Boolean isArchivedGUI) {
		if (debugMode) {
			ErrorClass.throwDebug("bugreportGUI: Starting setupGUIFromConfig", "debug");
		}
		if (!validateGUIConfig(guiConfig)) {
			ErrorClass.throwDebug("The layout of the customGUI.yml file is incorrect. Falling back to the default layout.", "error");
			setupDefaultGUI(gui, player, report, reportIDGUI, isArchivedGUI);
			return;
		}

		List<?> itemsList = guiConfig.getList("bug_report_details_GUI.items");
		for (Object itemObj : Objects.requireNonNull(itemsList)) {
			if (itemObj instanceof Map<?, ?> itemMap) {
				try {
					int slot = Integer.parseInt(itemMap.get("slot").toString());
					String bugReportItem = itemMap.get("bugReportItem").toString();
					String materialKey = itemMap.get("material").toString();
					Material material;
					ItemStack itemStack;

					if ((isArchivedGUI && bugReportItem.equals("BugReportArchive")) || (!isArchivedGUI && bugReportItem.equals("BugReportUnArchive"))) {
						continue;
					}

					if (materialKey.contains("[") && materialKey.contains("]")) {
						String[] materials = materialKey.replaceAll("[\\[\\]]", "").split(",\\s*");
						material = Material.valueOf(isArchivedGUI ? materials[1].trim() : materials[0].trim());
					} else {
						material = Material.valueOf(materialKey);
					}

					Object textureObj = itemMap.get("texture");
					String texture = textureObj != null ? textureObj.toString() : "";

					Map<String, String> reportDetails = parseReportDetails(report);
					if (Objects.equals(bugReportItem, "BugReportMessage")) {
						ItemStack messageItem = createItemForReportDetail(bugReportItem, material, texture, reportDetails, isArchivedGUI);
						ItemMeta meta = messageItem.getItemMeta();
						Objects.requireNonNull(meta).setDisplayName(meta.getDisplayName());

						String fullMessage = reportDetails.getOrDefault("Full Message", "N/A");
						if (fullMessage.length() > 32) {
							List<String> lore = new ArrayList<>();
							String[] words = fullMessage.split(" ");
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
							meta.setLore(Collections.singletonList(ChatColor.WHITE + fullMessage));
						}

						messageItem.setItemMeta(meta);
						itemStack = messageItem;
						gui.setItem(slot, itemStack);
					} else {
						itemStack = createItemForReportDetail(bugReportItem, material, texture, reportDetails, isArchivedGUI);
						gui.setItem(slot, itemStack);
					}
				} catch (IllegalArgumentException e) {
					ErrorClass.throwDebug("Error parsing material or slot number: " + e.getMessage(), "error");
				}
			}
		}
	}

	/**
	 * Validates the custom GUI configuration.
	 *
	 * @param guiConfig The custom GUI configuration.
	 * @return true if the configuration is valid; false otherwise.
	 */
	private static boolean validateGUIConfig(@NotNull YamlConfiguration guiConfig) {
		if (debugMode) {
			ErrorClass.throwDebug("bugreportGUI: Starting validateGUIConfig", "debug");
		}
		int guiSize = guiConfig.getInt("bug_report_details_GUI.guiSize", -1);
		if (guiSize < 9 || guiSize > 54 || guiSize % 9 != 0) {
			ErrorClass.throwDebug("Invalid GUI size defined in customGUI.yml. Must be between 9 and 54 and a multiple of 9.", "error");
			return false;
		}

		List<?> itemsList = guiConfig.getList("bug_report_details_GUI.items");
		if (itemsList == null || itemsList.isEmpty()) {
			ErrorClass.throwDebug("No items defined in customGUI.yml or incorrect format.", "warning");
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
				Integer.parseInt(itemMap.get("slot").toString());
			} catch (NumberFormatException e) {
				return false;
			}

			if (Integer.parseInt(itemMap.get("slot").toString()) >= guiSize) {
				ErrorClass.throwDebug("Slot number in customGUI.yml is greater than the GUI size.", "error");
				return false;
			}

			if (!bugReportItems.containsKey(itemMap.get("bugReportItem").toString())) {
				ErrorClass.throwDebug("Invalid bugReportItem in customGUI.yml: " + itemMap.get("bugReportItem"), "warning");
				return false;
			}

			if ("BugReporter".equals(itemMap.get("bugReportItem").toString())) {
				if (!"%player_texture%".equals(itemMap.get("texture"))) {
					ErrorClass.throwDebug("Texture for BugReporter item in customGUI.yml must be %player_texture%.", "warning");
					return false;
				}
			}
		}

		return true;
	}

	private static @NotNull ItemStack createItemForReportDetail(String bugReportItemKey, Material defaultMaterial, @Nullable String textureBase64, @NotNull Map<String, String> reportDetails, Boolean isArchivedGUI) {
		if (debugMode) {
			ErrorClass.throwDebug("bugreportGUI: Starting createItemForReportDetail", "debug");
		}
		String reportDetailKey = deriveReportDetailKey(bugReportItemKey);
		var ref = new Object() {
			String detailValue = reportDetails.getOrDefault(reportDetailKey, "N/A");
		};
		ItemStack item;

		switch (bugReportItemKey) {
			case "BugReportTimestamp" -> ref.detailValue = translateTimestampToDate(Long.parseLong(ref.detailValue));
			case "BugReportCategory" -> {
				String categoryID = reportDetails.getOrDefault("Category ID", "N/A");
				if (!"N/A".equals(categoryID)) {
					List<Map<?, ?>> categoryList = config.getMapList("reportCategories");
					ref.detailValue = categoryList.stream().filter(categoryMap -> categoryID.equals(String.valueOf(categoryMap.get("id")))).map(categoryMap -> (String) categoryMap.get("name")).findFirst().orElse("Unknown Category");
				}
			}
			case "BugReporter" -> {
				String username = reportDetails.get("Username");

				if (config.getBoolean("enablePlayerHeads")) {
					item = getPlayerHead(username);
				} else {
					item = createInfoItem(Material.PLAYER_HEAD, ChatColor.GOLD + "Username", ChatColor.WHITE + username, false);
				}
				ItemMeta meta = item.getItemMeta();
				if (meta != null) {
					meta.setDisplayName(ChatColor.YELLOW + bugReportItems.getOrDefault(bugReportItemKey, "Unknown Item"));
					List<String> lore = new ArrayList<>();
					lore.add(ChatColor.WHITE + username);
					meta.setLore(lore);
					item.setItemMeta(meta);
				}
				return item;
			}
			case "BugReportStatus" -> ref.detailValue = isArchivedGUI ? "Archived" : "Active";
			case "BugReportBack", "BugReportDelete", "BugReportArchive", "BugReportUnArchive" -> ref.detailValue = null;
		}

		if (isItemSupportsTexture(bugReportItemKey) && textureBase64 != null && !textureBase64.trim().isEmpty()) {
			item = createCustomPlayerHead(textureBase64, bugReportItems.getOrDefault(bugReportItemKey, "Unknown Item"), 1);
		} else {
			ItemStack statusItem;
			String status = reportDetails.get("Status");

			if (status != null) {
				List<Map<?, ?>> statuses = config.getMapList("statuses");
				for (Map<?, ?> statusMap : statuses) {
					if (statusMap.get("id").toString().equals(status)) {
						String statusName = statusMap.get("name").toString();
						String statusDescription = statusMap.get("description").toString();

						ChatColor statusColor = ChatColor.valueOf(statusMap.get("color").toString().toUpperCase());
						Material statusIcon = Material.matchMaterial((String) statusMap.get("icon")) != null ? Material.matchMaterial((String) statusMap.get("icon")) : Material.BARRIER;
						statusItem = createInfoItem(statusIcon, statusColor + statusName + " (Click to change)", statusColor + statusDescription, false);

						if (bugReportItemKey.equals("BugReportStatus")) {
							item = statusItem;
							return item;
						}
					}
				}
			}

			item = new ItemStack(defaultMaterial, 1);
			ItemMeta meta = item.getItemMeta();

			if (meta != null) {
				meta.setDisplayName(ChatColor.YELLOW + bugReportItems.getOrDefault(bugReportItemKey, "Unknown Item"));
				List<String> lore = new ArrayList<>();
				if (ref.detailValue != null) {
					lore.add(ChatColor.WHITE + ref.detailValue);
				}
				meta.setLore(lore);
				item.setItemMeta(meta);
			}
		}
		return item;
	}

	/**
	 * Derives the report detail key from the bug report item key.
	 *
	 * @param bugReportItemKey The key of the bug report item.
	 * @return The derived report detail key.
	 */
	@Contract(pure = true)
	private static String deriveReportDetailKey(@NotNull String bugReportItemKey) {
		if (debugMode) {
			ErrorClass.throwDebug("bugreportGUI: Starting deriveReportDetailKey", "debug");
		}
		switch (bugReportItemKey) {
			case "BugReporter":
				return getValueFromLanguageFile("buttonNames.bugReportDetailsUsername", "Username");
			case "BugReportUUID":
				return getValueFromLanguageFile("buttonNames.bugReportDetailsUUID", "UUID");
			case "BugReportWorld":
				return getValueFromLanguageFile("buttonNames.bugReportDetailsWorld", "World");
			case "BugReportMessage":
				return getValueFromLanguageFile("buttonNames.bugReportDetailsFullMessage", "Full Message");
			case "BugReportCategory":
				return getValueFromLanguageFile("buttonNames.bugReportDetailsCategory", "Category ID");
			case "BugReportStatus":
				return getValueFromLanguageFile("buttonNames.bugReportDetailsStatus", "Status") + " (Click to change)";
			case "BugReportTimestamp":
				return getValueFromLanguageFile("buttonNames.bugReportDetailsTimestamp", "Timestamp");
			case "BugReportLocation":
				return getValueFromLanguageFile("buttonNames.bugReportDetailsLocation", "Location") + ChatColor.BOLD + " (Click to teleport)";
			case "BugReportGamemode":
				return getValueFromLanguageFile("buttonNames.bugReportDetailsGamemode", "Gamemode");
			case "BugReportServerName":
				return getValueFromLanguageFile("buttonNames.bugReportDetailsServerName", "Server Name");
			case "BugReportUnArchive":
				return getValueFromLanguageFile("buttonNames.unarchive", "Unarchive");
			case "BugReportArchive":
				return getValueFromLanguageFile("buttonNames.archive", "Archive");
			case "BugReportBack":
				return getValueFromLanguageFile("buttonNames.back", "Back");
			case "BugReportDelete":
				return getValueFromLanguageFile("buttonNames.delete", "Delete");
			default:
				return bugReportItemKey.replace("BugReport", "");
		}
	}

	/**
	 * Checks if the given item key supports custom textures.
	 *
	 * @param bugReportItemKey The key of the bug report item.
	 * @return true if the item supports custom textures; false otherwise.
	 */
	private static boolean isItemSupportsTexture(String bugReportItemKey) {
		if (debugMode) {
			ErrorClass.throwDebug("bugreportGUI: Starting isItemSupportsTexture", "debug");
		}
		return List.of("BugReportUUID", "BugReportWorld", "BugReportMessage", "BugReportCategory", "BugReportTimestamp", "BugReportLocation", "BugReportGamemode", "BugReportServerName", "BugReportUnArchive", "BugReportArchive", "BugReportDelete").contains(bugReportItemKey);
	}

	private static @NotNull Map<String, String> parseReportDetails(@NotNull String report) {
		if (debugMode) {
			ErrorClass.throwDebug("bugreportGUI: Starting parseReportDetails", "debug");
		}
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
		if (debugMode) {
			ErrorClass.throwDebug("bugreportGUI: Starting setupDefaultGUI", "debug");
		}
		if (report == null) {
			player.sendMessage(returnStartingMessage(ChatColor.RED) + " Error 101: Report is null. Please report this to the plugin developer.");
			ErrorClass.throwDebug("Error 101: Report is null.", "error");
			return;
		}

		String username = getReportByKey(report, "Username");
		String uuid = getReportByKey(report, "UUID");
		String world = getReportByKey(report, "World");
		String fullMessage = getReportByKey(report, "Full Message");
		String category = getReportByKey(report, "Category ID");
		String location = getReportByKey(report, "Location");
		String gamemode = getReportByKey(report, "Gamemode");
		String status = getReportByKey(report, "Status");
		String serverName = getReportByKey(report, "serverName");

		ItemStack emptyItem = createEmptyItem();
		String locationTitle;

		if (location == null || location.equals("null")) location = "Not found";

		if (location.length() - location.replace(",", "").length() != 3) {
			location = "Not found";
			locationTitle = "Location";
		} else {
			locationTitle = "Location " + ChatColor.BOLD + "(Click to teleport)";
		}

		if (gamemode == null || gamemode.equals("null")) gamemode = "Unknown";

		ItemStack usernameItem;
		if (config.getBoolean("enablePlayerHeads")) {
			usernameItem = getPlayerHead(username);
		} else {
			usernameItem = createInfoItem(Material.PLAYER_HEAD, ChatColor.GOLD + "Username", ChatColor.WHITE + username, false);
		}

		boolean isLongMessage = fullMessage.length() > 32;

		String timestampToDate = translateTimestampToDate(Long.parseLong(getReportByKey(report, "Timestamp")));
		ItemStack uuidItem = createInfoItem(Material.NAME_TAG, ChatColor.GOLD + "UUID", ChatColor.WHITE + uuid, false);
		ItemStack worldItem = createInfoItem(Material.GRASS_BLOCK, ChatColor.GOLD + "World", ChatColor.WHITE + world, false);
		ItemStack messageItem = createInfoItem(Material.PAPER, ChatColor.GOLD + "Full Message", ChatColor.WHITE + fullMessage, isLongMessage);
		ItemStack serverNameItem = createInfoItem(Material.COMPASS, ChatColor.GOLD + "Server Name", ChatColor.WHITE + serverName, false);
		ItemStack statusItem = null;

		if (status == null) {
			statusItem = createInfoItem(isArchivedGUI ? Material.RED_DYE : Material.LIME_DYE, ChatColor.GOLD + "Status (Click to change)", ChatColor.WHITE + (isArchivedGUI ? "Archived" : "Open"), false);
		} else {
			List<Map<?, ?>> statuses = config.getMapList("statuses");
			boolean statusFound = false;

			for (Map<?, ?> statusMap : statuses) {
				if (status.equals("0")) {
					status = isArchivedGUI ? "Archived" : "Active";
					Material stautsMaterial = isArchivedGUI ? Material.RED_DYE : Material.LIME_DYE;
					ChatColor statusChatColor = isArchivedGUI ? ChatColor.RED : ChatColor.GREEN;

					statusItem = createInfoItem(stautsMaterial, statusChatColor + "Status (Click to change)", statusChatColor + status, false);
					statusFound = true;
					break;
				}

				if (statusMap.get("id").toString().equals(status)) {
					String statusName = statusMap.get("name").toString();
					String statusColor = statusMap.get("color").toString();
					ChatColor statusChatColor = ChatColor.valueOf(statusColor.toUpperCase());
					String statusMaterial = statusMap.get("icon").toString().toUpperCase();

					if (statusMaterial.contains("MINECRAFT:")) {
						statusMaterial = statusMaterial.replace("MINECRAFT:", "");
					}

					statusItem = createInfoItem(Material.valueOf(statusMaterial), statusChatColor + "Status (Click to change)", statusChatColor + statusName, false);
					statusFound = true;
					break;
				}
			}

			if (!statusFound) {
				statusItem = createInfoItem(Material.BARRIER, ChatColor.GOLD + "Status (Click to change)", ChatColor.GOLD + "Unknown", false);
			}
		}

		ItemStack timestampItem = createInfoItem(Material.CLOCK, ChatColor.GOLD + "Timestamp", ChatColor.WHITE + timestampToDate, false);
		ItemStack locationItem = createInfoItem(Material.COMPASS, ChatColor.GOLD + locationTitle, ChatColor.WHITE + location, false);
		ItemStack gamemodeItem = createInfoItem(Material.DIAMOND_SWORD, ChatColor.GOLD + "Gamemode", ChatColor.WHITE + gamemode, false);
		ItemStack backButton = createButton(Material.BARRIER, ChatColor.RED + getValueFromLanguageFile("buttonNames.back", "Back"));
		ItemStack archiveButton = createCustomPlayerHead(guiTextures.archiveTexture, ChatColor.YELLOW + getValueFromLanguageFile("buttonNames.archive", "Archive"), 16);
		ItemStack unarchiveButton = createCustomPlayerHead(guiTextures.unarchiveTexture, ChatColor.YELLOW + getValueFromLanguageFile("buttonNames.unarchive", "Unarchive"), 17);
		ItemStack deleteButton = createCustomPlayerHead(guiTextures.deleteTexture, ChatColor.YELLOW + getValueFromLanguageFile("buttonNames.delete", "Delete"), 18);

		for (int i = 0; i < gui.getSize(); i++) {
			gui.setItem(i, emptyItem);
		}

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

		if (!"null".equals(category) && !"".equals(category)) {
			List<Map<?, ?>> categoryList = config.getMapList("reportCategories");

			Optional<String> categoryNameOptional = categoryList.stream().filter(categoryMap -> Integer.parseInt(categoryMap.get("id").toString()) == Integer.parseInt(category)).map(categoryMap -> categoryMap.get("name").toString()).findFirst();

			if (categoryNameOptional.isPresent()) {
				String categoryName = categoryNameOptional.get();
				ItemStack categoryItem = createInfoItem(Material.CHEST, ChatColor.GOLD + "Category Name", ChatColor.WHITE + categoryName, false);
				gui.setItem(18, categoryItem);
			}
		} else {
			ItemStack categoryItem = createInfoItem(Material.CHEST, ChatColor.GOLD + "Category Name", ChatColor.WHITE + "None", false);
			gui.setItem(18, categoryItem);
		}

		player.openInventory(gui);
		Bukkit.getPluginManager().registerEvents(new BugReportDetailsListener(gui, reportIDGUI), plugin);
	}
}
