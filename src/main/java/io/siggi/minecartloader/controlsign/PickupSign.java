package io.siggi.minecartloader.controlsign;

import io.siggi.minecartloader.MinecartMetadata;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.StorageMinecart;

public class PickupSign extends ControlSignHandler {
	@Override
	public void handle(Minecart minecart, MinecartMetadata metadata, Sign sign) {
		if (!(minecart instanceof StorageMinecart || minecart instanceof HopperMinecart))
			return;
		Block block = minecart.getLocation().getBlock();
		Block signBlock = sign.getBlock();
		if (metadata.pickupSign == null || !metadata.pickupSign.equals(signBlock)) {
			metadata.pickupSign = signBlock;
			metadata.pickupTime = 20;
		}
		if (metadata.pickupTime > 0) {
			metadata.pickupTime -= 1;
			metadata.stopThisTick = true;
		}
	}
}
