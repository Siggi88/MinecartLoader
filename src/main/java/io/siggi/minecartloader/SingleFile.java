package io.siggi.minecartloader;

import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Minecart;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SingleFile {
	SingleFile() {
	}

	private final Map<Minecart, String> singleFileMinecarts = new HashMap<>();
	private final Map<String, Set<Minecart>> singleFileTracks = new HashMap<>();
	private final Map<String, Block> singleFileSigns = new HashMap<>();

	public void tick() {
		Set<Minecart> toRemove = new HashSet<>();
		toRemove.addAll(singleFileMinecarts.keySet());
		toRemove.removeIf(m -> m.isValid());
		for (Minecart minecart : toRemove) {
			releaseMinecart(minecart);
		}
	}

	public boolean trackMinecart(Minecart minecart, Sign sign, String track) {
		String currentTrack = singleFileMinecarts.get(minecart);
		if (currentTrack != null) {
			if (currentTrack.equals(track)) {
				return true;
			} else {
				releaseMinecart(minecart);
			}
		}
		Block block = sign.getBlock();
		Block currentBlock = singleFileSigns.get(track);
		if (currentBlock != null && !currentBlock.equals(block)) {
			return false;
		}
		Set<Minecart> minecarts = singleFileTracks.get(track);
		if (minecarts == null) {
			singleFileTracks.put(track, minecarts = new HashSet<>());
		}
		minecarts.add(minecart);
		singleFileSigns.put(track, block);
		singleFileMinecarts.put(minecart, track);
		System.out.println("Singlefile " + track + " has " + minecarts.size() + " carts.");
		return true;
	}

	public void releaseMinecart(Minecart minecart) {
		String track = singleFileMinecarts.get(minecart);
		if (track == null) return;
		Set<Minecart> singleFileTrack = singleFileTracks.get(track);
		if (singleFileTrack != null) {
			singleFileTrack.remove(minecart);
		}
		if (singleFileTrack == null || singleFileTrack.isEmpty()) {
			singleFileTracks.remove(track);
			singleFileSigns.remove(track);
		}
		singleFileMinecarts.remove(minecart);
	}
}
