package io.siggi.minecartloader.controlsign;

import io.siggi.minecartloader.MinecartMetadata;
import io.siggi.minecartloader.Util;
import org.bukkit.block.Sign;
import org.bukkit.entity.Minecart;

public class WeightSign extends ControlSignHandler {
	@Override
	public void handle(Minecart minecart, MinecartMetadata metadata, Sign sign) {
		setWeight(minecart, sign.getLine(1));
	}

	protected void setWeight(Minecart minecart, String weightLine) {
		switch (Util.canonicalize(weightLine, false)) {
			case "heavy":
				minecart.setSlowWhenEmpty(false);
				break;
			case "light":
				minecart.setSlowWhenEmpty(true);
				break;
		}
	}
}
