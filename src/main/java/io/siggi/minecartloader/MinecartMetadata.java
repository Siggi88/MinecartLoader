package io.siggi.minecartloader;

import org.bukkit.entity.Minecart;
import org.bukkit.util.Vector;

public class MinecartMetadata {
	private final Minecart minecart;

	public boolean queueThisTick = false;
	public boolean stopThisTick = false;
	public boolean stopped;
	public boolean stoppedLastTick = false;
	public boolean queueing;
	public Vector savedVelocity = null;
	public Vector velocity = null;

	public MinecartMetadata(Minecart minecart) {
		this.minecart = minecart;
	}

	public void preTick() {
		stoppedLastTick = stopped;
		queueThisTick = false;
		stopThisTick = false;
		stopped = false;
	}

	public boolean isValid() {
		return minecart.isValid();
	}

}
