package io.siggi.minecartloader;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class Util {
	private Util() {
	}

	public static String canonicalize(String string, boolean name) {
		if (name) {
			return ChatColor.stripColor(string).toLowerCase();
		} else {
			return ChatColor.stripColor(string).toLowerCase().replaceAll("[^0-9a-z:]*", "");
		}
	}

	public static ItemStack getItem(Minecart minecart) {
		Material material;
		if (minecart instanceof CommandMinecart) {
			material = Material.COMMAND_BLOCK_MINECART;
		} else if (minecart instanceof StorageMinecart) {
			material = Material.CHEST_MINECART;
		} else if (minecart instanceof ExplosiveMinecart) {
			material = Material.TNT_MINECART;
		} else if (minecart instanceof HopperMinecart) {
			material = Material.HOPPER_MINECART;
		} else {
			material = Material.MINECART;
		}
		ItemStack stack = new ItemStack(material, 1);
		String name = minecart.getCustomName();
		if (name != null) {
			ItemMeta meta = stack.getItemMeta();
			meta.setDisplayName(name);
			stack.setItemMeta(meta);
		}
		return stack;
	}

	public static List<ItemStack> getInventoryItems(Minecart minecart) {
		List<ItemStack> items = new ArrayList<>(27);
		Inventory inventory = null;
		if (minecart instanceof StorageMinecart) {
			inventory = ((StorageMinecart) minecart).getInventory();
		} else if (minecart instanceof HopperMinecart) {
			inventory = ((HopperMinecart) minecart).getInventory();
		}
		if (inventory == null) return items;
		for (ItemStack stack : inventory.getContents()) {
			if (stack != null && stack.getType() != Material.AIR) {
				items.add(stack);
			}
		}
		return items;
	}

	public static double max(double value, double maxAmount) {
		if (value < 0) {
			return -maxAmount;
		} else if (value > 0) {
			return maxAmount;
		} else {
			return 0;
		}
	}

	public static double min(double value, double maxAmount) {
		if (value < 0) {
			return Math.max(value, -maxAmount);
		} else if (value > 0) {
			return Math.min(value, maxAmount);
		} else {
			return 0;
		}
	}

	public static boolean isZeroVelocity(Vector vector) {
		return vector.getX() == 0.0 && vector.getY() == 0.0 && vector.getZ() == 0.0;
	}
}