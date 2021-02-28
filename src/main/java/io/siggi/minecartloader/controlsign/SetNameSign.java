package io.siggi.minecartloader.controlsign;

import io.siggi.minecartloader.MinecartMetadata;
import org.bukkit.block.Sign;
import org.bukkit.entity.Minecart;

public class SetNameSign extends ControlSignHandler {
	@Override
	public void handle(Minecart minecart, MinecartMetadata metadata, Sign sign) {
		minecart.setCustomName(sign.getLine(1));
	}
}
