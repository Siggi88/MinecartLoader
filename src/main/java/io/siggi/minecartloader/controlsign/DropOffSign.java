package io.siggi.minecartloader.controlsign;

import io.siggi.minecartloader.MinecartMetadata;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.inventory.Inventory;

public class DropOffSign extends ControlSignHandler {
	@Override
	public void handle(Minecart minecart, MinecartMetadata metadata, Sign sign) {
		Block block = minecart.getLocation().getBlock();
		if (block.getRelative(BlockFace.DOWN).getType() == Material.HOPPER) {
			Inventory inventory = null;
			if (minecart instanceof StorageMinecart) {
				inventory = ((StorageMinecart) minecart).getInventory();
			} else if (minecart instanceof HopperMinecart) {
				inventory = ((HopperMinecart) minecart).getInventory();
			}
			if (inventory != null && !inventory.isEmpty()) {
				metadata.stopThisTick = true;
			}
		}
	}
}
