package com.bugreportmc.bugreport.listeners

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.inventory.meta.BookMeta

class ItemDropEvent : Listener {
	@EventHandler
	fun onDropItem(event: PlayerDropItemEvent) {
		val droppedItemType = event.itemDrop.itemStack.type
		if (droppedItemType == Material.WRITTEN_BOOK || droppedItemType == Material.WRITABLE_BOOK) {
			val bookMeta = event.itemDrop.itemStack.itemMeta as BookMeta?
			if (bookMeta != null && bookMeta.hasCustomModelData() && bookMeta.customModelData == 1889234213) {
				event.isCancelled = true
			}
		}
	}
}
