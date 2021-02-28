package io.siggi.minecartloader.controlsign;

import io.siggi.minecartloader.MinecartMetadata;
import org.bukkit.block.Sign;
import org.bukkit.entity.Minecart;

public class QueueSign extends ControlSignHandler {
	@Override
	public void handle(Minecart minecart, MinecartMetadata metadata, Sign sign) {
		String line = sign.getLine(1);
		if (line.isEmpty()) {
			metadata.queueThisTick = true;
		} else {
			boolean enable = line.equals("1") || line.equals("on") || line.equals("yes");
			if (enable) {
				metadata.queueing = true;
			} else {
				metadata.queueing = false;
			}
		}
	}
}
