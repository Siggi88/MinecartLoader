package io.siggi.minecartloader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileReader;
import java.util.*;

public class MinecartLoader extends JavaPlugin implements Listener {

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		getServer().getScheduler().runTaskTimer(this, this::tick, 1L, 1L);
	}

	private final Vector zeroVelocity = new Vector(0.0, 0.0, 0.0);
	private final Map<Minecart, Vector> savedVelocity = new HashMap<>();
	private final Set<Minecart> queuingEnabled = new HashSet<>();
	private final List<BlockFace> allDirections = Collections.unmodifiableList(Arrays.asList(new BlockFace[]{
			BlockFace.NORTH,
			BlockFace.SOUTH,
			BlockFace.WEST,
			BlockFace.EAST
	}));

	private LinkedList<Minecart> getAllMinecarts() {
		LinkedList<Minecart> minecarts = new LinkedList<>();
		for (World world : getServer().getWorlds()) {
			minecarts.addAll(world.getEntitiesByClass(Minecart.class));
		}
		return minecarts;
	}

	private void tick() {
		Set<Chunk> keepLoaded = new HashSet<>();
		savedVelocity.keySet().removeIf(m -> !m.isValid());
		queuingEnabled.removeIf(m -> !m.isValid());
		LinkedList<Minecart> minecarts = getAllMinecarts();
		for (Minecart minecart : minecarts) {
			boolean currentlyStopped = false;
			Vector velocity = minecart.getVelocity();
			if (!isZeroVelocity(velocity)) {
				savedVelocity.remove(minecart);
			} else if (!savedVelocity.containsKey(minecart)) {
				continue;
			} else {
				currentlyStopped = true;
				velocity = savedVelocity.get(minecart);
			}
			Location location = minecart.getLocation();
			Chunk chunk = location.getChunk();
			World world = chunk.getWorld();
			int x = chunk.getX();
			int z = chunk.getZ();
			for (int dz = -2; dz <= 2; dz++) {
				for (int dx = -2; dx <= 2; dx++) {
					keepLoaded.add(world.getChunkAt(x + dx, z + dz));
				}
			}
			Block block = location.getBlock();
			Sign queueControlSign = getControlSign(block, "queue");
			if (queueControlSign != null) {
				String line = queueControlSign.getLine(1);
				boolean enable = line.equals("1") || line.equals("on") || line.equals("yes");
				if (enable) {
					queuingEnabled.add(minecart);
				} else {
					queuingEnabled.remove(minecart);
				}
			}
			if (queuingEnabled.contains(minecart)) {
				double searchX = 0.0;
				double searchZ = 0.0;
				if (velocity.getX() < 0.0) {
					searchX -= 2.0;
				} else if (velocity.getX() > 0.0) {
					searchX += 2.0;
				}
				if (velocity.getZ() < 0.0) {
					searchZ -= 2.0;
				} else if (velocity.getZ() > 0.0) {
					searchZ += 2.0;
				}
				Location searchLocation = new Location(world, location.getX() + searchX, location.getY(), location.getZ() + searchZ);
				Collection<Entity> nearby = world.getNearbyEntities(searchLocation, 2.0, 2.0, 2.0, e -> e != minecart && e instanceof Minecart);
				if (currentlyStopped && nearby.isEmpty()) {
					minecart.setVelocity(velocity.multiply(0.8));
					savedVelocity.remove(minecart);
				} else if (!currentlyStopped && !nearby.isEmpty()) {
					minecart.setVelocity(zeroVelocity);
					savedVelocity.put(minecart, velocity);
				}
			} else if (currentlyStopped && velocity != null) {
				minecart.setVelocity(velocity);
			}
		}
		updateLoaded(keepLoaded);
	}

	private final Set<Chunk> currentlyKeptLoaded = new HashSet<>();

	private void updateLoaded(Set<Chunk> newSet) {
		Set<Chunk> toRemove = new HashSet<>();
		toRemove.addAll(currentlyKeptLoaded);
		toRemove.removeAll(newSet);
		Set<Chunk> newChunks = new HashSet<>();
		newChunks.addAll(newSet);
		newChunks.removeAll(currentlyKeptLoaded);
		currentlyKeptLoaded.removeAll(toRemove);
		currentlyKeptLoaded.addAll(newChunks);
		for (Chunk chunk : toRemove) {
			chunk.removePluginChunkTicket(this);
		}
		for (Chunk chunk : newChunks) {
			chunk.addPluginChunkTicket(this);
		}
	}

	private boolean isZeroVelocity(Vector vector) {
		return vector.getX() == 0.0 && vector.getY() == 0.0 && vector.getZ() == 0.0;
	}

	@EventHandler
	public void redstoneChange(BlockRedstoneEvent event) {
		Block block = event.getBlock();
		if (block.getType() != Material.DETECTOR_RAIL
				|| event.getOldCurrent() > 0
				|| event.getNewCurrent() == 0) {
			return;
		}
		Sign onlySign = getControlSign(event.getBlock(), "only");
		if (onlySign == null) {
			return;
		}
		boolean foundMinecart = false;
		String mustMatch = onlySign.getLine(1).trim().replace(" ", "").toLowerCase();
		Collection<Entity> minecarts = block.getWorld().getNearbyEntities(block.getLocation(), 1, 1, 1, e -> e instanceof Minecart);
		for (Entity entity : minecarts) {
			if (!(entity instanceof Minecart)) continue;
			Minecart minecart = (Minecart) entity;
			String name = minecart.getCustomName();
			if (name == null) continue;
			String lowerName = name.trim().replace(" ", "").toLowerCase();
			if (lowerName.equals(mustMatch)) {
				foundMinecart = true;
				break;
			}
		}
		if (!foundMinecart) {
			event.setNewCurrent(0);
		}
	}

	private Sign getControlSign(Block rail, String firstLine) {
		List<Sign> signs = getControlSigns(rail);
		if (firstLine == null) {
			if (signs.isEmpty()) return null;
			return signs.get(0);
		}
		for (Sign sign : getControlSigns(rail)) {
			if (sign.getLine(0).toLowerCase().equals(firstLine)) {
				return sign;
			}
		}
		return null;
	}

	private List<Sign> getControlSigns(Block rail) {
		LinkedList<Sign> signs = new LinkedList<>();
		Block below = rail.getRelative(BlockFace.DOWN);
		for (BlockFace direction : allDirections) {
			addSign(signs, rail.getRelative(direction), null);
		}
		for (BlockFace direction : allDirections) {
			addSign(signs, below.getRelative(direction), direction);
		}
		return signs;
	}

	private void addSign(LinkedList<Sign> signs, Block block, BlockFace attached) {
		BlockState state = block.getState();
		if (!(state instanceof Sign)) return;
		Sign sign = (Sign) state;
		if (attached != null) {
			BlockData blockData = sign.getBlockData();
			if (blockData instanceof WallSign) {
				WallSign signData = (WallSign) blockData;
				if (attached != null && signData.getFacing() != attached) {
					return;
				}
			}
		}
		signs.add(sign);
	}
}
