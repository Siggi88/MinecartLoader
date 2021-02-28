package io.siggi.minecartloader.controlsign;

import io.siggi.minecartloader.MinecartMetadata;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.block.Sign;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.inventory.Inventory;

public class PickupSign extends ControlSignHandler {
	@Override
	public void handle(Minecart minecart, MinecartMetadata metadata, Sign sign) {
		Block block = minecart.getLocation().getBlock();
		Block above = block.getRelative(BlockFace.UP);
		if (above.getType() == Material.HOPPER) {
			Inventory inventory = null;
			if (minecart instanceof StorageMinecart) {
				inventory = ((StorageMinecart) minecart).getInventory();
			} else if (minecart instanceof HopperMinecart) {
				inventory = ((HopperMinecart) minecart).getInventory();
			}
			if (inventory != null) {
				Inventory aboveInventory = ((Hopper) above.getState()).getInventory();
				if (!aboveInventory.isEmpty() && inventory.firstEmpty() != -1) {
					metadata.stopThisTick = true;
				}
			}
		}
	}
}
