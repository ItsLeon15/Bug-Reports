package com.bugreportmc.bugreport

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.*

class Category internal constructor(
	@JvmField val id: Int,
	@JvmField val name: String,
	@JvmField val color: String,
	private val itemStack: ItemStack
) {
	fun getName(): String {
		return name
	}

	val description: String
		get() {
			val itemMeta = itemStack.itemMeta
			if (itemMeta == null || !itemMeta.hasLore()) {
				return ""
			}
			return Objects.requireNonNull(itemMeta.lore)[0]
		}

	val item: Material
		get() = itemStack.type
}
