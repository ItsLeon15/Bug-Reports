package com.bugreportmc.bugreport.api

import com.bugreportmc.bugreport.BugReportManager.Companion.config
import com.bugreportmc.bugreport.BugReportPlugin.Companion.plugin
import com.bugreportmc.bugreport.api.ErrorClass.logErrorMessage
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import org.bukkit.Bukkit.createPlayerProfile
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.profile.PlayerProfile
import org.bukkit.profile.PlayerTextures
import java.io.*
import java.lang.reflect.Field
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

object DataSource {
	private val CACHE_DIR: File = File("plugins/BugReport/cache")
	private val CACHE_FILE: File = File(CACHE_DIR, "playerData.json")
	private val GSON: Gson = Gson()
	private var CACHE_EXPIRY_DURATION = (24 * 60 * 60 * 1000 // 24 hours
			).toLong()

	private fun convertTimeToMillis(timeString: String): Long {
		val timeUnits = java.util.Map.of("m", 60, "h", 3600, "d", 86400, "w", 604800, "mo", 2592000, "y", 31536000)
		val parts =
			timeString.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		val number = parts[0].toInt()
		val unit = parts[1].lowercase(Locale.getDefault())

		val seconds = timeUnits.getOrDefault(unit, 86400)
		CACHE_EXPIRY_DURATION = number.toLong() * seconds * 1000
		return CACHE_EXPIRY_DURATION
	}

	private fun loadCache(): MutableMap<String, CacheEntry?> {
		ensureCacheDirectoryExists()
		if (!CACHE_FILE.exists()) {
			return HashMap()
		}
		try {
			BufferedReader(FileReader(CACHE_FILE)).use { reader ->
				val type = object : TypeToken<Map<String?, CacheEntry?>?>() {}.type
				return try {
					GSON.fromJson(reader, type)
				} catch (e: Exception) {
					HashMap()
				}
			}
		} catch (e: IOException) {
			plugin.logger.warning("Failed to load cache")
			logErrorMessage("Failed to load cache")
			return HashMap()
		}
	}

	private fun saveCache(cache: Map<String, CacheEntry?>) {
		ensureCacheDirectoryExists()
		try {
			BufferedWriter(FileWriter(CACHE_FILE)).use { writer ->
				val jsonString: String = Gson().toJson(cache)
				writer.write(jsonString)
			}
		} catch (e: IOException) {
			plugin.logger.warning("Failed to save cache")
			logErrorMessage("Failed to save cache")
		}
	}

	private fun isCacheValid(timestamp: Long): Boolean {
		val configKeyExists: Boolean = config.contains("refreshPlayerHeadCache")
		val configDate = if (configKeyExists) convertTimeToMillis(
			Objects.requireNonNull<String>(
				config.getString("refreshPlayerHeadCache")
			)
		) else CACHE_EXPIRY_DURATION
		return System.currentTimeMillis() - timestamp < configDate
	}

	private fun ensureCacheDirectoryExists() {
		if (!CACHE_DIR.exists() && !CACHE_DIR.mkdirs()) {
			plugin.logger.warning("Failed to create cache directory")
			logErrorMessage("Failed to create cache directory")
		}
	}

	@JvmStatic
	fun cleanOutdatedCache(listAllNewReports: Boolean) {
		val cache = loadCache()

		if (cache.isEmpty()) {
			saveCache(HashMap())
			return
		}

		if (listAllNewReports) return

		cache.entries.removeIf { entry: Map.Entry<String, CacheEntry?> ->
			val cacheInvalid = !isCacheValid(
				entry.value!!.timestamp
			)
			val nestedDataInvalid = entry.value!!.nestedData == null || !isCacheValid(
				entry.value!!.nestedData!!.timestamp
			)
			cacheInvalid && nestedDataInvalid
		}
		saveCache(cache)
	}

	@Throws(IOException::class)
	private fun fetchFromURL(urlString: String): String? {
		val url = URL(urlString)
		val connection = url.openConnection() as HttpURLConnection
		try {
			InputStreamReader(connection.inputStream).use { reader ->
				val result = StringBuilder()
				var ch: Int
				while ((reader.read().also { ch = it }) != -1) {
					result.append(ch.toChar())
				}
				return result.toString()
			}
		} catch (e: IOException) {
			return null
		}
	}

	@Throws(Exception::class)
	private fun getUUIDFromUsername(username: String, cache: MutableMap<String, CacheEntry?>): UUID {
		if (cache.containsKey(username) && isCacheValid(cache[username]!!.timestamp)) {
			return UUID.fromString(cache[username]!!.data)
		}

		val response = fetchFromURL("https://api.mojang.com/users/profiles/minecraft/$username")
		var existingEntry = cache[username]

		if (response != null) {
			val uuid: UUID = extractUUIDFromResponse(response)

			if (existingEntry == null) {
				existingEntry = CacheEntry(uuid.toString(), System.currentTimeMillis())
			} else {
				existingEntry.data = uuid.toString()
			}

			cache[username] = existingEntry
			saveCache(cache)
			return uuid
		} else {
			if (existingEntry == null) {
				existingEntry = CacheEntry("$username-0000-0000-0000-000000000000", System.currentTimeMillis())
			} else {
				existingEntry.data = "$username-0000-0000-0000-000000000000"
			}

			cache[username] = existingEntry
			saveCache(cache)
			return UUID.fromString("00000000-0000-0000-0000-000000000000")
		}
	}

	private fun extractUUIDFromResponse(response: String): UUID {
		val jsonResponse: JsonObject = JsonParser.parseString(response).getAsJsonObject()
		val uuidString = jsonResponse["id"].asString
		return UUID.fromString(
			uuidString.substring(0, 8) + "-" + uuidString.substring(
				8, 12
			) + "-" + uuidString.substring(12, 16) + "-" + uuidString.substring(16, 20) + "-" + uuidString.substring(
				20, 32
			)
		)
	}

	private fun returnFalseIfCacheIsInvalid(cache: Map<String, CacheEntry?>): String? {
		for ((_, cacheEntry) in cache) {
			if (cacheEntry?.data == null) return "Cache entry data is null"
			if (cacheEntry.data.isEmpty()) return "Cache entry data is empty"
			if (cacheEntry.data.isBlank()) return "Cache entry data is blank"
			if (cacheEntry.timestamp <= 0) return "Cache entry timestamp is 0"
			if (cacheEntry.nestedData == null) return "Cache entry nested data is null"
			if (cacheEntry.nestedData!!.data.isEmpty()) return "Cache entry nested data is empty"
			if (cacheEntry.nestedData!!.data.isBlank()) return "Cache entry nested data is blank"
			if (cacheEntry.nestedData!!.timestamp <= 0) return "Cache entry nested data timestamp is 0"

			if (Date.from(Date().toInstant()).time < cacheEntry.timestamp) return "Cache entry timestamp is not a date"
			if (Date.from(Date().toInstant()).time < cacheEntry.nestedData!!.timestamp) return "Cache entry nested data timestamp is not a date"

			try {
				UUID.fromString(cacheEntry.data)
			} catch (ex: IllegalArgumentException) {
				return "Data is not a valid UUID"
			}
		}
		return null
	}

	fun getPlayerHead(playerName: String?): ItemStack {
		var cache = loadCache()

		cleanOutdatedCache(true)

		if (playerName == null || playerName.trim { it <= ' ' }.isEmpty()) {
			return defaultPlayerHead
		}

		try {
			val base64: String

			val cacheInvalidReason = returnFalseIfCacheIsInvalid(cache)
			if (cacheInvalidReason != null) {
				logErrorMessage("Cache is invalid: $cacheInvalidReason")
				saveCache(HashMap())
				cache = loadCache()
			}

			if (checkIfPlayerHeadIsCached(playerName, cache)) {
				base64 = cache[playerName]!!.nestedData!!.data
			} else {
				val uuid: UUID = getUUIDFromUsername(playerName, cache)
				base64 = getBase64FromUUID(uuid, cache)
				updatePlayerHeadCache(playerName, uuid.toString(), base64, cache)
			}

			return if (base64.isNotEmpty()) createSkullItem(base64, playerName) else defaultPlayerHead
		} catch (e: Exception) {
			plugin.logger.warning("Failed to get player head for " + playerName + ": " + e.message)
			logErrorMessage("Failed to get player head for " + playerName + ": " + e.message)
			return defaultPlayerHead
		}
	}

	private val defaultPlayerHead: ItemStack
		get() {
			val defaultHead = ItemStack(Material.PLAYER_HEAD)
			val meta: SkullMeta = defaultHead.itemMeta as SkullMeta
			meta.setDisplayName("Default Player")
			defaultHead.setItemMeta(meta)
			return defaultHead
		}

	private fun updatePlayerHeadCache(
		playerName: String,
		uuid: String,
		base64: String,
		cache: MutableMap<String, CacheEntry?>,
	) {
		val currentTime = System.currentTimeMillis()
		val playerCacheEntry = CacheEntry(uuid, currentTime)
		playerCacheEntry.nestedData = CacheEntry(base64, currentTime)

		cache[playerName] = playerCacheEntry
		saveCache(cache)
	}

	private fun createSkullItem(textureValue: String?, displayName: String): ItemStack {
		val playerHead = ItemStack(Material.PLAYER_HEAD)
		val skullMeta: SkullMeta = playerHead.itemMeta as SkullMeta

		if (!textureValue.isNullOrEmpty()) {
			if (textureValue == "00000000-0000-0000-0000-000000000000") {
				return playerHead
			}
			try {
				val decodedValue = String(Base64.getDecoder().decode(textureValue))
				val textureJson: JsonObject = JsonParser.parseString(decodedValue).getAsJsonObject()
				val textureUrl = textureJson.getAsJsonObject("textures").getAsJsonObject("SKIN")["url"].asString

				val profile: PlayerProfile = createPlayerProfile(UUID.randomUUID())
				val textures: PlayerTextures = profile.textures

				textures.skin = URL(textureUrl)
				profile.setTextures(textures)
				skullMeta.ownerProfile = profile

				skullMeta.setDisplayName(displayName)
				playerHead.setItemMeta(skullMeta)
			} catch (e: Exception) {
				plugin.logger.warning("Failed to set custom player head texture: " + e.message)
				logErrorMessage("Failed to set custom player head texture: " + e.message)
				return ItemStack(Material.PLAYER_HEAD)
			}
		}

		return playerHead
	}

	private fun checkIfPlayerHeadIsCached(playerName: String, cache: Map<String, CacheEntry?>): Boolean {
		return cache.containsKey(playerName) && isCacheValid(cache[playerName]!!.timestamp)
	}

	private fun getCachedPlayerHead(playerName: String, cache: Map<String, CacheEntry>): ItemStack {
		val mainEntry = cache[playerName]
		var base64: String? = null
		if ("00000000-0000-0000-0000-000000000000" == mainEntry!!.data) {
			return ItemStack(Material.PLAYER_HEAD)
		}
		if (mainEntry.nestedData != null) {
			base64 = mainEntry.nestedData!!.data
		}
		val head = ItemStack(Material.PLAYER_HEAD)
		val skullMeta: SkullMeta = head.itemMeta as SkullMeta
		Objects.requireNonNull(skullMeta).setDisplayName(playerName)
		try {
			if (base64 != null) {
				setSkullWithBase64(skullMeta, base64)
			} else {
				throw IllegalArgumentException("Base64 data for player $playerName not found in cache.")
			}
		} catch (e: Exception) {
			plugin.logger.warning("Failed to get cached player head for $playerName")
			logErrorMessage("Failed to get cached player head for $playerName")
		}
		head.setItemMeta(skullMeta)
		return head
	}

	@Throws(Exception::class)
	private fun getBase64FromUUID(uuid: UUID, cache: Map<String, CacheEntry?>): String {
		if (uuid.toString() == "00000000-0000-0000-0000-000000000000") {
			return "00000000-0000-0000-0000-000000000000"
		}
		val cacheKey: String = uuid.toString()
		val mainEntry =
			cache.values.stream().filter { entry: CacheEntry? -> cacheKey == entry!!.data }.findFirst().orElse(null)
		if (mainEntry?.nestedData != null && isCacheValid(
				mainEntry.nestedData!!.timestamp
			)
		) {
			return mainEntry.nestedData!!.data
		}
		val response = fetchFromURL(
			"https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", "")
		)
		val jsonResponse: JsonObject = JsonParser.parseString(response).getAsJsonObject()
		val properties: JsonArray = jsonResponse.getAsJsonArray("properties")
		for (i in 0 until properties.size()) {
			val property: JsonObject = properties.get(i).getAsJsonObject()
			if ("textures" == property["name"].asString) {
				val base64 = property["value"].asString
				if (mainEntry != null) {
					mainEntry.nestedData = CacheEntry(base64, System.currentTimeMillis())
				}
				saveCache(cache)
				return base64
			}
		}
		throw IllegalArgumentException("Couldn't find textures property for UUID $uuid")
	}

	private fun setSkullWithBase64(skullMeta: SkullMeta, base64: String?) {
		if (base64.isNullOrEmpty()) {
			plugin.logger.warning("Base64 string is empty. Cannot set custom player head texture.")
			logErrorMessage("Base64 string is empty. Cannot set custom player head texture.")
			return
		}

		try {
			val profileField: Field = skullMeta.javaClass.getDeclaredField("profile")
			profileField.isAccessible = true
			profileField[skullMeta] = createGameProfile(base64)
		} catch (e: NoSuchFieldException) {
			plugin.logger.warning("Failed to set custom player head texture: " + e.message)
			logErrorMessage("Failed to set custom player head texture: " + e.message)
		} catch (e: IllegalAccessException) {
			plugin.logger.warning("Failed to set custom player head texture: " + e.message)
			logErrorMessage("Failed to set custom player head texture: " + e.message)
		}
	}

	private fun createGameProfile(textureValue: String): GameProfile {
		val profile = GameProfile(UUID.randomUUID(), "GameProfile").also {
			it.properties.put("textures", Property("textures", textureValue))
		}
		return profile
	}

	private class CacheEntry {
		var data: String
		var timestamp: Long
		var nestedData: CacheEntry? = null

		constructor(data: String, timestamp: Long) {
			this.data = data
			this.timestamp = timestamp
		}

		constructor(data: String, timestamp: Long, nestedData: CacheEntry?) {
			this.data = data
			this.timestamp = timestamp
			this.nestedData = nestedData
		}
	}
}
