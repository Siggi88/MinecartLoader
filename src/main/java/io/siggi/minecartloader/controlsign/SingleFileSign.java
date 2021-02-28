package io.siggi.minecartloader.controlsign;

import io.siggi.minecartloader.MinecartMetadata;
import io.siggi.minecartloader.SingleFile;
import org.bukkit.block.Sign;
import org.bukkit.entity.Minecart;

public class SingleFileSign extends ControlSignHandler {
	private final SingleFile singleFile;

	public SingleFileSign(SingleFile singleFile) {
		this.singleFile = singleFile;
	}

	@Override
	public void handle(Minecart minecart, MinecartMetadata metadata, Sign sign) {
		String trackName = sign.getLine(1).toLowerCase().trim().replace(" ", "");
		if (trackName.equals("release")) {
			singleFile.releaseMinecart(minecart);
		} else if (!singleFile.trackMinecart(minecart, sign, trackName)) {
			metadata.stopThisTick = true;
		}
	}
}
