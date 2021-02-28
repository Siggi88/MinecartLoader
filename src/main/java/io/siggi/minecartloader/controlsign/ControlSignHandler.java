package io.siggi.minecartloader.controlsign;

import io.siggi.minecartloader.MinecartMetadata;
import org.bukkit.block.Sign;
import org.bukkit.entity.Minecart;

public abstract class ControlSignHandler {
	public abstract void handle(Minecart minecart, MinecartMetadata metadata, Sign sign);
}
