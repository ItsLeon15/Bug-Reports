package com.leon.bugreport.API;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.leon.bugreport.BugReportManager.config;
import static com.leon.bugreport.BugReportManager.plugin;

public class DataSource {
	private static final File CACHE_DIR = new File("plugins/BugReport/cache");
	private static final File CACHE_FILE = new File(CACHE_DIR, "playerData.json");
	private static final Gson GSON = new Gson();
	private static long CACHE_EXPIRY_DURATION = 24 * 60 * 60 * 1000; // Default = 24 hours

	public static long convertTimeToString(@NotNull String date) {
		String[] splitDate = date.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
		String number = splitDate[0];
		String letter = splitDate[1];
		int numberInt = Integer.parseInt(number);
		int letterInt = switch(letter.toLowerCase()) {
			case "m" -> 60;
			case "h" -> 3600;
			case "d", "default" -> 86400;
			case "w" -> 604800;
			case "mo" -> 2592000;
			case "y" -> 31536000;
			default -> {
				plugin.getLogger().warning("Invalid time format. Defaulting to 24 hours.");
				yield 86400;
			}
		};
		int totalSeconds = numberInt * letterInt;
		CACHE_EXPIRY_DURATION = totalSeconds * 1000L;
		return CACHE_EXPIRY_DURATION;
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

	private static Map<String, CacheEntry> loadCache() {
		ensureCacheDirectoryExists();
		if (!CACHE_FILE.exists()) {
			return new HashMap<>();
		}
		try (BufferedReader reader = new BufferedReader(new FileReader(CACHE_FILE))) {
			Type type = new TypeToken<Map<String, CacheEntry>>() {}.getType();
			return GSON.fromJson(reader, type);
		} catch (IOException e) {
			plugin.getLogger().warning("Failed to load cache");
			return new HashMap<>();
		}
	}

	private static void saveCache(Map<String, CacheEntry> cache) {
		ensureCacheDirectoryExists();
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(CACHE_FILE))) {
			String jsonString = new Gson().toJson(cache);
			writer.write(jsonString);
		} catch (IOException e) {
			plugin.getLogger().warning("Failed to save cache");
		}
	}

	private static boolean isCacheValid(long timestamp) {
		boolean configKeyExists = config.contains("refreshPlayerHeadCache");
		long configDate = configKeyExists ? convertTimeToString(Objects.requireNonNull(config.getString("refreshPlayerHeadCache"))) : CACHE_EXPIRY_DURATION;
		return System.currentTimeMillis() - timestamp < configDate;
	}

	private static void ensureCacheDirectoryExists() {
		if (!CACHE_DIR.exists() && !CACHE_DIR.mkdirs()) {
			plugin.getLogger().warning("Failed to create cache directory");
		}
	}

	public static void cleanOutdatedCache() {
		Map<String, CacheEntry> cache = loadCache();
		if (cache == null || cache.isEmpty()) {
			saveCache(new HashMap<>());
			return;
		}
		cache.entrySet().removeIf(entry -> {
			boolean mainInvalid = !isCacheValid(entry.getValue().timestamp);
			boolean nestedInvalid = entry.getValue().nestedData == null || !isCacheValid(entry.getValue().nestedData.timestamp);
			return mainInvalid && nestedInvalid;
		});
		saveCache(cache);
	}

	private static @NotNull String fetchFromURL(String urlString) throws IOException {
		URL url = new URL(urlString);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
			StringBuilder result = new StringBuilder();
			int ch;
			while ((ch = reader.read()) != -1) {
				result.append((char) ch);
			}
			return result.toString();
		}
	}

	private static @NotNull UUID getUUIDFromUsername(String username, @NotNull Map<String, CacheEntry> cache) throws Exception {
		if (cache.containsKey(username) && isCacheValid(cache.get(username).timestamp)) {
			return UUID.fromString(cache.get(username).data);
		}
		String response = fetchFromURL("https://api.mojang.com/users/profiles/minecraft/" + username);
		UUID uuid = extractUUIDFromResponse(response);
		CacheEntry existingEntry = cache.get(username);
		if (existingEntry == null) {
			existingEntry = new CacheEntry(uuid.toString(), System.currentTimeMillis());
		} else {
			existingEntry.data = uuid.toString();
		}
		cache.put(username, existingEntry);
		saveCache(cache);
		return uuid;
	}

	private static @NotNull UUID extractUUIDFromResponse(String response) {
		JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
		String uuidString = jsonResponse.get("id").getAsString();
		return UUID.fromString(
				uuidString.substring(0, 8) + "-" +
					uuidString.substring(8, 12) + "-" +
					uuidString.substring(12, 16) + "-" +
					uuidString.substring(16, 20) + "-" +
					uuidString.substring(20, 32));
	}

	public static @NotNull ItemStack getPlayerHead(String playerName) {
		try {
			Map<String, CacheEntry> cache = loadCache();
			if (checkIfPlayerHeadIsCached(playerName, cache)) {
				return getCachedPlayerHead(playerName, cache);
			}
			UUID uuid = getUUIDFromUsername(playerName, cache);
			String base64 = getBase64FromUUID(uuid, cache);
			ItemStack head = new ItemStack(Material.PLAYER_HEAD);
			SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
			Objects.requireNonNull(skullMeta).setDisplayName(playerName);
			setSkullWithBase64(skullMeta, base64);
			head.setItemMeta(skullMeta);
			return head;
		} catch (Exception e) {
			if (playerName != null && !"00000000-0000-0000-0000-000000000000".equals(playerName)) {
				plugin.getLogger().warning("Failed to get player head for " + playerName);
				Map<String, CacheEntry> cache = loadCache();
				long currentTime = System.currentTimeMillis();
				cache.put(playerName, new CacheEntry("00000000-0000-0000-0000-000000000000", currentTime));
				if (cache.get("00000000-0000-0000-0000-000000000000") == null) {
					cache.put("00000000-0000-0000-0000-000000000000", new CacheEntry("00000000-0000-0000-0000-000000000000", currentTime));
				}
				saveCache(cache);
			}
			ItemStack head = new ItemStack(Material.PLAYER_HEAD);
			SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
			Objects.requireNonNull(skullMeta).setDisplayName(playerName);
			head.setItemMeta(skullMeta);
			return head;
		}
	}

	private static boolean checkIfPlayerHeadIsCached(String playerName, @NotNull Map<String, CacheEntry> cache) {
		return cache.containsKey(playerName) && isCacheValid(cache.get(playerName).timestamp);
	}

	private static @NotNull ItemStack getCachedPlayerHead(String playerName, @NotNull Map<String, CacheEntry> cache) {
		CacheEntry mainEntry = cache.get(playerName);
		String uuidString = mainEntry.data;
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
			plugin.getLogger().warning("Failed to get cached player head for " + playerName);
		}
		head.setItemMeta(skullMeta);
		return head;
	}

	private static @NotNull String getBase64FromUUID(@NotNull UUID uuid, @NotNull Map<String, CacheEntry> cache) throws Exception {
		if ("00000000-0000-0000-0000-000000000000".equals(uuid.toString())) {
			return "";
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
		GameProfile profile = new GameProfile(UUID.randomUUID(), null);
		profile.getProperties().put("textures", new Property("textures", base64));
		try {
			Field profileField = skullMeta.getClass().getDeclaredField("profile");
			profileField.setAccessible(true);
			profileField.set(skullMeta, profile);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
