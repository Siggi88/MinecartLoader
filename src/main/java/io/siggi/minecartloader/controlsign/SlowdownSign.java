package io.siggi.minecartloader.controlsign;

import io.siggi.minecartloader.MinecartMetadata;
import io.siggi.minecartloader.Util;
import org.bukkit.block.Sign;
import org.bukkit.entity.Minecart;
import org.bukkit.util.Vector;

public class SlowdownSign extends WeightSign {
	@Override
	public void handle(Minecart minecart, MinecartMetadata metadata, Sign sign) {
		Vector velocity = minecart.getVelocity();
		Vector newVelocity = new Vector(
				Util.min(velocity.getX(), 0.1),
				Util.min(velocity.getY(), 0.1),
				Util.min(velocity.getZ(), 0.1)
		);
		minecart.setVelocity(newVelocity);
		setWeight(minecart, sign.getLine(1));
	}
}
