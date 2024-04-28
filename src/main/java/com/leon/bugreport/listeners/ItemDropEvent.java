package com.leon.bugreport.listeners;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;

public class ItemDropEvent implements Listener {
	@EventHandler
	public void onDropItem(@NotNull PlayerDropItemEvent event) {
		Material droppedItemType = event.getItemDrop().getItemStack().getType();
		if (droppedItemType == Material.WRITTEN_BOOK || droppedItemType == Material.WRITABLE_BOOK) {
			BookMeta bookMeta = (BookMeta) event.getItemDrop().getItemStack().getItemMeta();
			if (bookMeta != null && bookMeta.hasCustomModelData() && bookMeta.getCustomModelData() == 1889234213) {
				event.setCancelled(true);
			}
		}
	}
}
