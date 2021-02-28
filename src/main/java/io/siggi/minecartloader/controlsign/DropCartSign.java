package io.siggi.minecartloader.controlsign;

import io.siggi.minecartloader.MinecartMetadata;
import io.siggi.minecartloader.Util;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Minecart;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class DropCartSign extends ControlSignHandler {
	@Override
	public void handle(Minecart minecart, MinecartMetadata metadata, Sign sign) {
		Location location = minecart.getLocation();
		Block block = location.getBlock();
		Location dropLocation = block.getLocation().add(0.5, 0.0, 0.5);
		ItemStack minecartItem = Util.getItem(minecart);
		List<ItemStack> inventoryItems = Util.getInventoryItems(minecart);
		minecart.remove();
		location.getWorld().dropItem(dropLocation, minecartItem);
		for (ItemStack stack : inventoryItems) {
			location.getWorld().dropItem(dropLocation, stack);
		}
	}
}
