package com.leon.bugreport.API;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.leon.bugreport.logging.ErrorMessages;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import static com.leon.bugreport.API.ErrorClass.logErrorMessage;
import static com.leon.bugreport.BugReportManager.config;
import static com.leon.bugreport.BugReportManager.plugin;

public class DataSource {
	private static final File CACHE_DIR = new File("plugins/BugReport/cache");
	private static final File CACHE_FILE = new File(CACHE_DIR, "playerData.json");
	private static final Gson GSON = new Gson();
	private static long CACHE_EXPIRY_DURATION = 24 * 60 * 60 * 1000; // 24 hours

	public static long convertTimeToMillis(@NotNull String timeString) {
		Map<String, Integer> timeUnits = Map.of("m", 60, "h", 3600, "d", 86400, "w", 604800, "mo", 2592000, "y", 31536000);
		String[] parts = timeString.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
		int number = Integer.parseInt(parts[0]);
		String unit = parts[1].toLowerCase();

		int seconds = timeUnits.getOrDefault(unit, 86400);
		CACHE_EXPIRY_DURATION = (long) number * seconds * 1000;
		return CACHE_EXPIRY_DURATION;
	}

	private static Map<String, CacheEntry> loadCache() {
		ensureCacheDirectoryExists();
		if (!CACHE_FILE.exists()) {
			return new HashMap<>();
		}
		try (BufferedReader reader = new BufferedReader(new FileReader(CACHE_FILE))) {
			Type type = new TypeToken<Map<String, CacheEntry>>() {
			}.getType();
			try {
				return GSON.fromJson(reader, type);
			} catch (Exception e) {
				return new HashMap<>();
			}
		} catch (IOException e) {
			String errorMessage = ErrorMessages.getErrorMessage(3);
			plugin.getLogger().warning(errorMessage);
			logErrorMessage(errorMessage);
			return new HashMap<>();
		}
	}

	private static void saveCache(Map<String, CacheEntry> cache) {
		ensureCacheDirectoryExists();
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(CACHE_FILE))) {
			String jsonString = new Gson().toJson(cache);
			writer.write(jsonString);
		} catch (IOException e) {
			String errorMessage = ErrorMessages.getErrorMessage(4);
			plugin.getLogger().warning(errorMessage);
			logErrorMessage(errorMessage);
		}
	}

	private static boolean isCacheValid(long timestamp) {
		boolean configKeyExists = config.contains("refreshPlayerHeadCache");
		long configDate = configKeyExists ? convertTimeToMillis(Objects.requireNonNull(config.getString("refreshPlayerHeadCache"))) : CACHE_EXPIRY_DURATION;
		return System.currentTimeMillis() - timestamp < configDate;
	}

	private static void ensureCacheDirectoryExists() {
		if (!CACHE_DIR.exists() && !CACHE_DIR.mkdirs()) {
			String errorMessage = ErrorMessages.getErrorMessage(5);
			plugin.getLogger().warning(errorMessage);
			logErrorMessage(errorMessage);
		}
	}

	public static void cleanOutdatedCache(Boolean listAllNewReports) {
		Map<String, CacheEntry> cache = loadCache();

		if (cache == null || cache.isEmpty()) {
			saveCache(new HashMap<>());
			return;
		}

		if (listAllNewReports) return;

		cache.entrySet().removeIf(entry -> {
			boolean cacheInvalid = !isCacheValid(entry.getValue().timestamp);
			boolean nestedDataInvalid = entry.getValue().nestedData == null || !isCacheValid(entry.getValue().nestedData.timestamp);
			return cacheInvalid && nestedDataInvalid;
		});
		saveCache(cache);
	}

	private static @Nullable String fetchFromURL(String urlString) throws IOException {
		URL url = new URL(urlString);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
			StringBuilder result = new StringBuilder();
			int ch;
			while ((ch = reader.read()) != -1) {
				result.append((char) ch);
			}
			return result.toString();
		} catch (IOException e) {
			return null;
		}
	}

	private static @NotNull UUID getUUIDFromUsername(String username, @NotNull Map<String, CacheEntry> cache) throws Exception {
		if (cache.containsKey(username) && isCacheValid(cache.get(username).timestamp)) {
			return UUID.fromString(cache.get(username).data);
		}

		String response = fetchFromURL("https://api.mojang.com/users/profiles/minecraft/" + username);
		CacheEntry existingEntry = cache.get(username);

		if (response != null) {
			UUID uuid = extractUUIDFromResponse(response);

			if (existingEntry == null) {
				existingEntry = new CacheEntry(uuid.toString(), System.currentTimeMillis());
			} else {
				existingEntry.data = uuid.toString();
			}

			cache.put(username, existingEntry);
			saveCache(cache);
			return uuid;
		} else {
			if (existingEntry == null) {
				existingEntry = new CacheEntry(username + "-0000-0000-0000-000000000000", System.currentTimeMillis());
			} else {
				existingEntry.data = username + "-0000-0000-0000-000000000000";
			}

			cache.put(username, existingEntry);
			saveCache(cache);
			return UUID.fromString("00000000-0000-0000-0000-000000000000");
		}
	}

	private static @NotNull UUID extractUUIDFromResponse(String response) {
		JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
		String uuidString = jsonResponse.get("id").getAsString();
		return UUID.fromString(uuidString.substring(0, 8) + "-" + uuidString.substring(8, 12) + "-" + uuidString.substring(12, 16) + "-" + uuidString.substring(16, 20) + "-" + uuidString.substring(20, 32));
	}

	private static @Nullable String returnFalseIfCacheIsInvalid(@NotNull Map<String, CacheEntry> cache) {
		for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
			CacheEntry cacheEntry = entry.getValue();

			if (cacheEntry.data == null) return "Cache entry data is null";
			if (cacheEntry.data.isEmpty()) return "Cache entry data is empty";
			if (cacheEntry.data.isBlank()) return "Cache entry data is blank";
			if (cacheEntry.timestamp <= 0) return "Cache entry timestamp is 0";
			if (cacheEntry.nestedData == null) return "Cache entry nested data is null";
			if (cacheEntry.nestedData.data.isEmpty()) return "Cache entry nested data is empty";
			if (cacheEntry.nestedData.data.isBlank()) return "Cache entry nested data is blank";
			if (cacheEntry.nestedData.timestamp <= 0) return "Cache entry nested data timestamp is 0";

			if (Date.from(new Date().toInstant()).getTime() < cacheEntry.timestamp)
				return "Cache entry timestamp is not a date";
			if (Date.from(new Date().toInstant()).getTime() < cacheEntry.nestedData.timestamp)
				return "Cache entry nested data timestamp is not a date";

			try {
				UUID.fromString(cacheEntry.data);
			} catch (IllegalArgumentException ex) {
				return "Data is not a valid UUID";
			}
		}
		return null;
	}

	public static @NotNull ItemStack getPlayerHead(String playerName) {
		Map<String, CacheEntry> cache = loadCache();
		cleanOutdatedCache(true);

		if (playerName == null || playerName.trim().isEmpty()) {
			return getDefaultPlayerHead();
		}

		try {
			String base64;

			String cacheInvalidReason = returnFalseIfCacheIsInvalid(cache);
			if (cacheInvalidReason != null) {
				String errorMessage = ErrorMessages.getErrorMessageWithAdditionalMessage(34, cacheInvalidReason);

				plugin.getLogger().warning(errorMessage);
				logErrorMessage(errorMessage);

				saveCache(new HashMap<>());
				cache = loadCache(); // Reset cache if invalid
			}

			if (checkIfPlayerHeadIsCached(playerName, cache)) {
				base64 = cache.get(playerName).nestedData.data;
			} else {
				UUID uuid = getUUIDFromUsername(playerName, cache);
				base64 = getBase64FromUUID(uuid, cache);
				updatePlayerHeadCache(playerName, uuid.toString(), base64, cache);
			}

			return base64 != null && !base64.isEmpty() ? createSkullItem(base64, playerName) : getDefaultPlayerHead();
		} catch (Exception e) {
			String errorMessage = ErrorMessages.getErrorMessageWithAdditionalMessage(6, e.getMessage());
			String finalErrorMessage = errorMessage.replaceAll("%playerName%", playerName);

			plugin.getLogger().warning(finalErrorMessage);
			logErrorMessage(finalErrorMessage);
			return getDefaultPlayerHead();
		}
	}

	private static @NotNull ItemStack getDefaultPlayerHead() {
		ItemStack defaultHead = new ItemStack(Material.PLAYER_HEAD);
		SkullMeta meta = (SkullMeta) defaultHead.getItemMeta();
		if (meta != null) {
			meta.setDisplayName("Default Player");
			defaultHead.setItemMeta(meta);
		}
		return defaultHead;
	}

	private static void updatePlayerHeadCache(String playerName, String uuid, String base64, @NotNull Map<String, CacheEntry> cache) {
		long currentTime = System.currentTimeMillis();
		CacheEntry playerCacheEntry = new CacheEntry(uuid, currentTime);
		playerCacheEntry.nestedData = new CacheEntry(base64, currentTime);

		cache.put(playerName, playerCacheEntry);
		saveCache(cache);
	}

	private static @NotNull ItemStack createSkullItem(String textureValue, String displayName) {
		ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
		SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();

		if (skullMeta != null && textureValue != null && !textureValue.isEmpty()) {
			if (textureValue.equals("00000000-0000-0000-0000-000000000000")) {
				return playerHead;
			}
			try {
				String decodedValue = new String(Base64.getDecoder().decode(textureValue));
				JsonObject textureJson = JsonParser.parseString(decodedValue).getAsJsonObject();
				String textureUrl = textureJson.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();

				PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
				PlayerTextures textures = profile.getTextures();

				textures.setSkin(new URL(textureUrl));
				profile.setTextures(textures);
				skullMeta.setOwnerProfile(profile);

				skullMeta.setDisplayName(displayName);
				playerHead.setItemMeta(skullMeta);
			} catch (Exception e) {
				String errorMessage = ErrorMessages.getErrorMessageWithAdditionalMessage(7, e.getMessage());
				plugin.getLogger().warning(errorMessage);
				logErrorMessage(errorMessage);
				return new ItemStack(Material.PLAYER_HEAD); // Fallback to default head on failure
			}
		}

		return playerHead;
	}

	private static boolean checkIfPlayerHeadIsCached(String playerName, @NotNull Map<String, CacheEntry> cache) {
		return cache.containsKey(playerName) && isCacheValid(cache.get(playerName).timestamp);
	}

	private static @NotNull ItemStack getCachedPlayerHead(String playerName, @NotNull Map<String, CacheEntry> cache) {
		CacheEntry mainEntry = cache.get(playerName);
		String base64 = null;
		if ("00000000-0000-0000-0000-000000000000".equals(mainEntry.data)) {
			return new ItemStack(Material.PLAYER_HEAD);
		}
		if (mainEntry.nestedData != null) {
			base64 = mainEntry.nestedData.data;
		}
		ItemStack head = new ItemStack(Material.PLAYER_HEAD);
		SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
		Objects.requireNonNull(skullMeta).setDisplayName(playerName);
		try {
			if (base64 != null) {
				setSkullWithBase64(skullMeta, base64);
			} else {
				throw new IllegalArgumentException("Base64 data for player " + playerName + " not found in cache.");
			}
		} catch (Exception e) {
			String errorMessage = ErrorMessages.getErrorMessage(8);
			String finalErrorMessage = errorMessage.replaceAll("%playerName%", playerName);
			plugin.getLogger().warning(finalErrorMessage);
			logErrorMessage(finalErrorMessage);
		}
		head.setItemMeta(skullMeta);
		return head;
	}

	private static @NotNull String getBase64FromUUID(@NotNull UUID uuid, @NotNull Map<String, CacheEntry> cache) throws Exception {
		if (uuid.toString().equals("00000000-0000-0000-0000-000000000000")) {
			return "00000000-0000-0000-0000-000000000000";
		}
		String cacheKey = uuid.toString();
		CacheEntry mainEntry = cache.values().stream().filter(entry -> cacheKey.equals(entry.data)).findFirst().orElse(null);
		if (mainEntry != null && mainEntry.nestedData != null && isCacheValid(mainEntry.nestedData.timestamp)) {
			return mainEntry.nestedData.data;
		}
		String response = fetchFromURL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", ""));
		JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
		JsonArray properties = jsonResponse.getAsJsonArray("properties");
		for (int i = 0; i < properties.size(); i++) {
			JsonObject property = properties.get(i).getAsJsonObject();
			if ("textures".equals(property.get("name").getAsString())) {
				String base64 = property.get("value").getAsString();
				if (mainEntry != null) {
					mainEntry.nestedData = new CacheEntry(base64, System.currentTimeMillis());
				}
				saveCache(cache);
				return base64;
			}
		}
		throw new IllegalArgumentException("Couldn't find textures property for UUID " + uuid);
	}

	private static void setSkullWithBase64(@NotNull SkullMeta skullMeta, String base64) {
		if (base64 == null || base64.isEmpty()) {
			String errorMessage = ErrorMessages.getErrorMessage(9);
			plugin.getLogger().warning(errorMessage);
			logErrorMessage(errorMessage);
			return;
		}

		try {
			java.lang.reflect.Field profileField = skullMeta.getClass().getDeclaredField("profile");
			profileField.setAccessible(true);
			profileField.set(skullMeta, createGameProfile(base64));
		} catch (NoSuchFieldException | IllegalAccessException e) {
			String errorMessage = ErrorMessages.getErrorMessageWithAdditionalMessage(10, e.getMessage());

			plugin.getLogger().warning(errorMessage);
			logErrorMessage(errorMessage);
		}
	}

	private static @NotNull GameProfile createGameProfile(String textureValue) {
		GameProfile profile = new GameProfile(UUID.randomUUID(), "GameProfile");
		profile.getProperties().put("textures", new Property("textures", textureValue));
		return profile;
	}

	private static class CacheEntry {
		String data;
		long timestamp;
		CacheEntry nestedData;

		CacheEntry(String data, long timestamp) {
			this.data = data;
			this.timestamp = timestamp;
		}

		CacheEntry(String data, long timestamp, CacheEntry nestedData) {
			this.data = data;
			this.timestamp = timestamp;
			this.nestedData = nestedData;
		}
	}
}
