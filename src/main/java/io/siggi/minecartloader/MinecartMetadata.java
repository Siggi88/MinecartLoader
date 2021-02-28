package io.siggi.minecartloader;

import org.bukkit.block.Block;
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

	public Block block = null;

	public Block pickupSign = null;
	public int pickupTime = 0;

	public MinecartMetadata(Minecart minecart) {
		this.minecart = minecart;
	}

	public void preTick() {
		stoppedLastTick = stopped;
		queueThisTick = false;
		stopThisTick = false;
		stopped = false;
		if (!minecart.isValid()) return;
		Block currentBlock = minecart.getLocation().getBlock();
		if (block == null || !currentBlock.equals(block)) {
			block = currentBlock;
			resetLocationBasedState();
		}
	}

	private void resetLocationBasedState() {
		this.pickupSign = null;
		this.pickupTime = 0;
	}

	public boolean isValid() {
		return minecart.isValid();
	}

}
