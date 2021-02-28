package io.siggi.minecartloader.controlsign;

import io.siggi.minecartloader.MinecartMetadata;
import org.bukkit.block.Sign;
import org.bukkit.entity.Minecart;
import org.bukkit.util.Vector;

public class OneWaySign extends ControlSignHandler {
	@Override
	public void handle(Minecart minecart, MinecartMetadata metadata, Sign sign) {
		Vector velocity = minecart.getVelocity();
		boolean bounce = false;
		String line = sign.getLine(1).trim().toLowerCase().replace(" ", "");
		switch (line) {
			case "north":
				if (velocity.getZ() > 0)
					bounce = true;
				break;
			case "south":
				if (velocity.getZ() < 0)
					bounce = true;
				break;
			case "west":
				if (velocity.getX() > 0)
					bounce = true;
				break;
			case "east":
				if (velocity.getX() < 0)
					bounce = true;
				break;
		}
		if (bounce) {
			velocity = velocity.multiply(-1.0);
			minecart.setVelocity(velocity);
		}
	}
}
