package io.siggi.minecartloader.controlsign;

import io.siggi.minecartloader.MinecartMetadata;
import io.siggi.minecartloader.Util;
import org.bukkit.block.Sign;
import org.bukkit.entity.Minecart;
import org.bukkit.util.Vector;

public class BoostSign extends WeightSign {
	@Override
	public void handle(Minecart minecart, MinecartMetadata metadata, Sign sign) {
		if (!metadata.stopped) {
			Vector velocity = minecart.getVelocity();
			Vector newVelocity = new Vector(
					Util.max(velocity.getX(), 0.7),
					Util.max(velocity.getY(), 0.7),
					Util.max(velocity.getZ(), 0.7)
			);
			minecart.setVelocity(newVelocity);
		}
		setWeight(minecart, sign.getLine(1));
	}
}
