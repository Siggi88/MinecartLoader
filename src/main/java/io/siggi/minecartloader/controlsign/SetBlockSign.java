package io.siggi.minecartloader.controlsign;

import io.siggi.minecartloader.MinecartMetadata;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Minecart;

public class SetBlockSign extends ControlSignHandler {
	@Override
	public void handle(Minecart minecart, MinecartMetadata metadata, Sign sign) {
		String blockName = sign.getLine(1);
		if (blockName.isEmpty()) {
			minecart.setDisplayBlockData(null);
		} else {
			try {
				Material material = Material.valueOf(blockName.toUpperCase());
				BlockData blockData = material.createBlockData();
				minecart.setDisplayBlockData(blockData);
			} catch (Exception e) {
			}
		}
	}
}
