package com.bugreportmc.bugreport.discord

import java.awt.Color
import java.io.IOException
import java.net.URL
import java.sql.Timestamp
import java.time.Instant
import java.time.ZoneId
import javax.net.ssl.HttpsURLConnection

class DiscordWebhook(private val url: String) {
	private val embeds: MutableList<EmbedObject> = mutableListOf()
	private var content: String? = null
	private var username: String? = null
	private var avatarUrl: String? = null

	fun setContent(content: String) {
		this.content = content
	}

	fun setUsername(username: String) {
		this.username = username
	}

	fun setAvatarUrl(avatarUrl: String) {
		this.avatarUrl = avatarUrl
	}

	fun addEmbed(embed: EmbedObject) {
		this.embeds.add(embed)
	}

	@Throws(IOException::class)
	fun execute() {
		if (this.content == null && this.embeds.isEmpty()) {
			throw IllegalArgumentException("Set content or add at least one EmbedObject")
		}

		val json = JSONObject()

		json.put("content", this.content)
		json.put("username", this.username)
		json.put("avatar_url", this.avatarUrl)
		json.put("tts", false)

		if (this.embeds.isNotEmpty()) {
			val embedObjects = mutableListOf<JSONObject>()

			for (embed in this.embeds) {
				val jsonEmbed = JSONObject()

				jsonEmbed.put("title", embed.title)
				jsonEmbed.put("description", embed.description)
				jsonEmbed.put("url", embed.url)

				embed.color?.let {
					val rgb = it.red shl 16 or (it.green shl 8) or it.blue
					jsonEmbed.put("color", rgb)
				}

				embed.timestamp?.let {
					jsonEmbed.put("timestamp", it.toString())
				}

				embed.footer?.let {
					val jsonFooter = JSONObject()
					jsonFooter.put("text", it.text)
					jsonFooter.put("icon_url", it.iconUrl)
					jsonEmbed.put("footer", jsonFooter)
				}

				embed.image?.let {
					val jsonImage = JSONObject()
					jsonImage.put("url", it.url)
					jsonEmbed.put("image", jsonImage)
				}

				embed.thumbnail?.let {
					val jsonThumbnail = JSONObject()
					jsonThumbnail.put("url", it.url)
					jsonEmbed.put("thumbnail", jsonThumbnail)
				}

				embed.author?.let {
					val jsonAuthor = JSONObject()
					jsonAuthor.put("name", it.name)
					jsonAuthor.put("url", it.url)
					jsonAuthor.put("icon_url", it.iconUrl)
					jsonEmbed.put("author", jsonAuthor)
				}

				val jsonFields = embed.fields.map { field ->
					val jsonField = JSONObject()
					jsonField.put("name", field.name)
					jsonField.put("value", field.value)
					jsonField.put("inline", field.inline)
					jsonField
				}

				jsonEmbed.put("fields", jsonFields.toTypedArray())
				embedObjects.add(jsonEmbed)
			}

			json.put("embeds", embedObjects.toTypedArray())
		}

		val url = URL(this.url)
		val connection = url.openConnection() as HttpsURLConnection
		connection.addRequestProperty("Content-Type", "application/json")
		connection.addRequestProperty("User-Agent", "BugReport-Webhook")
		connection.doOutput = true
		connection.requestMethod = "POST"

		connection.outputStream.use { stream ->
			stream.write(json.toString().toByteArray())
			stream.flush()
		}

		connection.inputStream.close()
		connection.disconnect()
	}

	class EmbedObject {
		val fields: MutableList<Field> = mutableListOf()
		var title: String? = null
		var description: String? = null
		var url: String? = null
		var color: Color? = null
		var timestamp: Timestamp? = null
		var footer: Footer? = null
		var thumbnail: Thumbnail? = null
		var image: Image? = null
		var author: Author? = null

		fun setTitle(title: String): EmbedObject {
			this.title = title
			return this
		}

		fun setDescription(description: String): EmbedObject {
			this.description = description
			return this
		}

		fun setUrl(url: String): EmbedObject {
			this.url = url
			return this
		}

		fun setColor(color: Color): EmbedObject {
			this.color = color
			return this
		}

		fun setTimestamp(): EmbedObject {
			val zonedDateTime = Instant.now().atZone(ZoneId.of("UTC"))
			this.timestamp = Timestamp.valueOf(zonedDateTime.toLocalDateTime())
			return this
		}

		fun setFooter(text: String, icon: String): EmbedObject {
			this.footer = Footer(text, icon)
			return this
		}

		fun setThumbnail(url: String): EmbedObject {
			this.thumbnail = Thumbnail(url)
			return this
		}

		fun setImage(url: String): EmbedObject {
			this.image = Image(url)
			return this
		}

		fun setAuthor(name: String, url: String, icon: String): EmbedObject {
			this.author = Author(name, url, icon)
			return this
		}

		fun addField(name: String, value: String, inline: Boolean): EmbedObject {
			this.fields.add(Field(name, value, inline))
			return this
		}

		data class Footer(val text: String, val iconUrl: String?)

		data class Thumbnail(val url: String)

		data class Image(val url: String)

		data class Author(val name: String, val url: String?, val iconUrl: String?)

		data class Field(val name: String, val value: String, val inline: Boolean)
	}

	class JSONObject {
		private val map = hashMapOf<String, Any?>()

		fun put(key: String, value: Any?) {
			if (value != null) {
				map[key] = value
			}
		}

		override fun toString(): String {
			val builder = StringBuilder()
			val entrySet = map.entries
			builder.append("{")

			var i = 0
			for ((key, value) in entrySet) {
				builder.append(quote(key)).append(":")
				when (value) {
					is String -> builder.append(quote(value))
					is Int -> builder.append(value)
					is Boolean -> builder.append(value)
					is JSONObject -> builder.append(value)
					is List<*> -> {
						builder.append("[")
						value.forEachIndexed { index, element ->
							builder.append(element.toString())
							if (index != value.size - 1) builder.append(",")
						}
						builder.append("]")
					}

					else -> value?.let { builder.append(it) }
				}
				builder.append(if (++i == entrySet.size) "}" else ",")
			}

			return builder.toString()
		}

		private fun quote(string: String): String {
			return "\"$string\""
		}
	}
}
