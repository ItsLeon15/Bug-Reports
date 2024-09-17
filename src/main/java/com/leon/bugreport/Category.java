package com.leon.bugreport;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;

public class Category {
	private final int id;
	private final String color;
	private final String name;
	private final ItemStack itemStack;

	Category(int id, String name, String color, ItemStack itemStack) {
		this.id = id;
		this.name = name;
		this.color = color;
		this.itemStack = itemStack;
	}

	public int getId() {
		return id;
	}

	public String getColor() {
		return color;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		ItemMeta itemMeta = itemStack.getItemMeta();
		if (itemMeta == null || !itemMeta.hasLore()) {
			return "";
		}
		return Objects.requireNonNull(itemMeta.getLore()).get(0);
	}

	public Material getItem() {
		return itemStack.getType();
	}
}
