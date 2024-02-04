package com.leon.bugreport.gui;

import com.leon.bugreport.BugReportLanguage;
import com.leon.bugreport.BugReportManager;
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
import java.util.*;

import static com.leon.bugreport.API.DataSource.getPlayerHead;
import static com.leon.bugreport.BugReportManager.*;
import static com.leon.bugreport.BugReportSettings.createCustomPlayerHead;

public class bugreportGUI {
	public static void generateNewYML() {
		File configFile = new File(plugin.getDataFolder(), "custom_bug_report_details_GUI.yml");
		if (!configFile.exists()) {
			plugin.saveResource("custom_bug_report_details_GUI.yml", false);
		}
	}

	private static final Map<String, String> bugReportItems = new HashMap<>() {{
		put("BugReporter", "Username");
		put("BugReportUUID", "UUID");
		put("BugReportWorld", "World");
		put("BugReportMessage", "Full Message");
		put("BugReportCategory", "Category ID");
		put("BugReportStatus", "Status");
		put("BugReportTimestamp", "Timestamp");
		put("BugReportLocation", "Location " + ChatColor.BOLD + "(Click to teleport)");
		put("BugReportGamemode", "Gamemode");
		put("BugReportUnArchive", BugReportLanguage.getTitleFromLanguage("unarchive"));
		put("BugReportArchive", BugReportLanguage.getTitleFromLanguage("archive"));
		put("BugReportBack", BugReportLanguage.getTitleFromLanguage("back"));
		put("BugReportDelete", BugReportLanguage.getTitleFromLanguage("delete"));
	}};


	private static @Nullable YamlConfiguration loadGUIConfig() {
		File configFile = new File(plugin.getDataFolder(), "custom_bug_report_details_GUI.yml");
		if (!configFile.exists()) {
			plugin.getLogger().info("custom_bug_report_details_GUI.yml not found. Falling back to default GUI settings.");
			return null;
		}
		return YamlConfiguration.loadConfiguration(configFile);
	}

	public static void openBugReportDetailsGUI(
			Player player,
			String report,
			Integer reportIDGUI,
			Boolean isArchivedGUI
	) {
		YamlConfiguration guiConfig = loadGUIConfig();
		String bugReportTitle = isArchivedGUI ? "Archived Bug Details - #" : "Bug Report Details - #";
		int guiSize;

		if (guiConfig != null && validateGUIConfig(guiConfig)) {
			guiSize = guiConfig.getInt("bug_report_details_GUI.guiSize");
			if (guiSize < 9 || guiSize > 54 || guiSize % 9 != 0) {
				guiSize = 45;
				Bukkit.getLogger().warning("Invalid GUI size in customGUI.yml. Falling back to default size.");
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

	private static void setupGUIFromConfig(
			Inventory gui,
			Player player,
			@NotNull YamlConfiguration guiConfig,
			String report,
			Integer reportIDGUI,
			Boolean isArchivedGUI
	) {
		if (!validateGUIConfig(guiConfig)) {
			Bukkit.getLogger().severe("The layout of the customGUI.yml file is incorrect. Falling back to the default layout.");
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

					if ((isArchivedGUI && bugReportItem.equals("BugReportArchive")) ||
							(!isArchivedGUI && bugReportItem.equals("BugReportUnArchive"))) {
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

					ItemStack itemStack = createItemForReportDetail(bugReportItem, material, texture, reportDetails, reportIDGUI, isArchivedGUI);
					gui.setItem(slot, itemStack);
				} catch (IllegalArgumentException e) {
					Bukkit.getLogger().warning("Error parsing material or slot number: " + e.getMessage());
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
				Integer.parseInt(itemMap.get("slot").toString());
			} catch (NumberFormatException e) {
				return false;
			}

			if (Integer.parseInt(itemMap.get("slot").toString()) >= guiSize) {
				Bukkit.getLogger().warning("Slot number in customGUI.yml is greater than the GUI size.");
				return false;
			}

			if ("BugReporter".equals(itemMap.get("bugReportItem").toString())) {
				if (!"%player_texture%".equals(itemMap.get("texture"))) {
					return false;
				}
			}
		}

		return true;
	}

	private static @NotNull ItemStack createItemForReportDetail(
			String bugReportItemKey,
			Material defaultMaterial,
			@Nullable String textureBase64,
			@NotNull Map<String, String> reportDetails,
			Integer reportIDGUI,
			Boolean isArchivedGUI
	) {
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
					ref.detailValue = categoryList.stream()
							.filter(categoryMap -> categoryID.equals(String.valueOf(categoryMap.get("id"))))
							.map(categoryMap -> (String) categoryMap.get("name"))
							.findFirst()
							.orElse("Unknown Category");
				}
			}
			case "BugReporter" -> {
				String username = reportDetails.get("Username");
				item = getPlayerHead(username);
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
		return switch (bugReportItemKey) {
			case "BugReporter" -> "Username";
			case "BugReportUUID" -> "UUID";
			case "BugReportWorld" -> "World";
			case "BugReportMessage" -> "Full Message";
			case "BugReportCategory" -> "Category ID";
			case "BugReportStatus" -> "Status";
			case "BugReportTimestamp" -> "Timestamp";
			case "BugReportLocation" -> "Location";
			case "BugReportGamemode" -> "Gamemode";
			case "BugReportUnArchive" -> BugReportLanguage.getTitleFromLanguage("unarchive");
			case "BugReportArchive" -> BugReportLanguage.getTitleFromLanguage("archive");
			case "BugReportBack" -> BugReportLanguage.getTitleFromLanguage("back");
			case "BugReportDelete" -> BugReportLanguage.getTitleFromLanguage("delete");
			default -> bugReportItemKey.replace("BugReport", "");
		};
	}

	/**
	 * Checks if the given item key supports custom textures.
	 *
	 * @param bugReportItemKey The key of the bug report item.
	 * @return true if the item supports custom textures; false otherwise.
	 */
	private static boolean isItemSupportsTexture(String bugReportItemKey) {
		return List.of("BugReportUUID", "BugReportWorld", "BugReportMessage",
			"BugReportCategory", "BugReportTimestamp", "BugReportLocation",
			"BugReportGamemode", "BugReportUnArchive", "BugReportArchive",
			"BugReportDelete").contains(bugReportItemKey);
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
			player.sendMessage(pluginColor + pluginTitle + ChatColor.RED + " Error 101: Report is null. Please report this to the plugin developer.");
			return;
		}

		String[] reportLines = report.split("\n");
		Map<String, String> reportData = new HashMap<>();

		for (String line : reportLines) {
			int colonIndex = line.indexOf(":");
			if (colonIndex >= 0) {
				String key = line.substring(0, colonIndex).trim();
				String value = line.substring(colonIndex + 1).trim();
				reportData.put(key, value);
			}
		}

		String username = reportData.get("Username");
		String uuid = reportData.get("UUID");
		String world = reportData.get("World");
		String fullMessage = reportData.get("Full Message");
		String category = reportData.get("Category ID");
		ItemStack emptyItem = createEmptyItem();
		String location = reportData.get("Location");
		String gamemode = reportData.get("Gamemode");
		String locationTitle;

		if (location == null || location.equals("null")) {
			location = "Not found";
		}

		if (location.length() - location.replace(",", "").length() != 3) {
			location = "Not found";
			locationTitle = "Location";
		} else {
			locationTitle = "Location " + ChatColor.BOLD + "(Click to teleport)";
		}

		if (gamemode == null || gamemode.equals("null")) {
			gamemode = "Unknown";
		}

		ItemStack usernameItem = getPlayerHead(username);
		String timestampToDate = translateTimestampToDate(Long.parseLong(reportData.get("Timestamp")));

		ItemStack uuidItem = createInfoItem(Material.NAME_TAG, ChatColor.GOLD + "UUID", ChatColor.WHITE + uuid);
		ItemStack worldItem = createInfoItem(Material.GRASS_BLOCK, ChatColor.GOLD + "World", ChatColor.WHITE + world);
		ItemStack messageItem = createInfoItem(Material.PAPER, ChatColor.GOLD + "Full Message", ChatColor.WHITE + fullMessage, fullMessage.length() > 32);
		ItemStack statusItem = createInfoItem((isArchivedGUI ? Material.RED_DYE : Material.LIME_DYE), ChatColor.GOLD + "Status", ChatColor.WHITE + (isArchivedGUI ? "Archived" : "Open"), false);
		ItemStack timestampItem = createInfoItem(Material.CLOCK, ChatColor.GOLD + "Timestamp", ChatColor.WHITE + timestampToDate, false);
		ItemStack locationItem = createInfoItem(Material.COMPASS, ChatColor.GOLD + locationTitle, ChatColor.WHITE + location, false);
		ItemStack gamemodeItem = createInfoItem(Material.DIAMOND_SWORD, ChatColor.GOLD + "Gamemode", ChatColor.WHITE + gamemode, false);

		ItemStack backButton = createButton(Material.BARRIER, ChatColor.RED  + BugReportLanguage.getTitleFromLanguage("back"));

		ItemStack archiveButton = createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2Y5YjY3YmI5Y2MxYzg4NDg2NzYwYjE3MjY1MDU0MzEyZDY1OWRmMmNjNjc1NTc1MDA0NWJkNzFjZmZiNGU2MCJ9fX0=", ChatColor.YELLOW + BugReportLanguage.getTitleFromLanguage("archive"), 16);
		ItemStack unarchiveButton = createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDVjNTg4YjllYzBhMDhhMzdlMDFhODA5ZWQwOTAzY2MzNGMzZTNmMTc2ZGM5MjIzMDQxN2RhOTNiOTQ4ZjE0OCJ9fX0=", ChatColor.YELLOW + BugReportLanguage.getTitleFromLanguage("unarchive"), 17);
		ItemStack deleteButton = createCustomPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmUwZmQxMDE5OWU4ZTRmY2RhYmNhZTRmODVjODU5MTgxMjdhN2M1NTUzYWQyMzVmMDFjNTZkMThiYjk0NzBkMyJ9fX0=", ChatColor.YELLOW + BugReportLanguage.getTitleFromLanguage("delete"), 18);

		for (int i = 0; i < gui.getSize(); i++) {
			gui.setItem(i, emptyItem);
		}

		gui.setItem(1, usernameItem);
		gui.setItem(3, uuidItem);
		gui.setItem(5, worldItem);
		gui.setItem(7, messageItem);

		gui.setItem(20, statusItem);
		gui.setItem(22, timestampItem);
		gui.setItem(24, locationItem);
		gui.setItem(26, gamemodeItem);

		gui.setItem(38, !isArchivedGUI ? archiveButton : unarchiveButton);
		gui.setItem(40, backButton);
		gui.setItem(42, deleteButton);

		if (!"null".equals(category) && !"".equals(category)) {
			List<Map<?, ?>> categoryList = config.getMapList("reportCategories");

			Optional<String> categoryNameOptional = categoryList.stream()
					.filter(categoryMap -> Integer.parseInt(categoryMap.get("id").toString()) == Integer.parseInt(category))
					.map(categoryMap -> categoryMap.get("name").toString())
					.findFirst();

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
		Bukkit.getPluginManager().registerEvents(new BugReportManager.BugReportDetailsListener(gui, reportIDGUI), plugin);
	}
}