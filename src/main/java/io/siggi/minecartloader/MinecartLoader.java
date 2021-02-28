package io.siggi.minecartloader;

import io.siggi.minecartloader.controlsign.*;
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
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.RideableMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.*;

public class MinecartLoader extends JavaPlugin implements Listener {

	private final Map<String, ControlSignHandler> controlSignHandlers = new HashMap<>();
	private final Vector zeroVelocity = new Vector(0.0, 0.0, 0.0);
	private final Map<Minecart, MinecartMetadata> minecartMetadataMap = new HashMap<>();
	private final SingleFile singleFile = new SingleFile();
	private final List<BlockFace> allDirections = Collections.unmodifiableList(Arrays.asList(new BlockFace[]{
			BlockFace.NORTH,
			BlockFace.SOUTH,
			BlockFace.WEST,
			BlockFace.EAST
	}));
	private final Set<Chunk> currentlyKeptLoaded = new HashSet<>();
	private Block lastRailSearched = null;
	private LinkedList<Sign> lastSigns = null;

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		getServer().getScheduler().runTaskTimer(this, this::tick, 1L, 1L);

		getCommand("findminecart").setExecutor(new FindMinecartCommand(this));

		controlSignHandlers.put("setname", new SetNameSign());
		controlSignHandlers.put("setblock", new SetBlockSign());
		controlSignHandlers.put("dropcart", new DropCartSign());
		controlSignHandlers.put("queue", new QueueSign());
		controlSignHandlers.put("weight", new WeightSign());
		controlSignHandlers.put("boost", new BoostSign());
		controlSignHandlers.put("slowdown", new SlowdownSign());
		controlSignHandlers.put("oneway", new OneWaySign());
		controlSignHandlers.put("singlefile", new SingleFileSign(singleFile));
		controlSignHandlers.put("dropoff", new DropOffSign());
		controlSignHandlers.put("pickup", new PickupSign());
		controlSignHandlers.put("turn", new TurnSign());
		controlSignHandlers.put("turnif", new TurnConditionalSign(false));
		controlSignHandlers.put("turnifnot", new TurnConditionalSign(true));
	}

	LinkedList<Minecart> getAllMinecarts() {
		LinkedList<Minecart> minecarts = new LinkedList<>();
		for (World world : getServer().getWorlds()) {
			minecarts.addAll(world.getEntitiesByClass(Minecart.class));
		}
		return minecarts;
	}

	private void tick() {
		singleFile.tick();
		Set<Chunk> keepLoaded = new HashSet<>();
		LinkedList<Minecart> minecarts = getAllMinecarts();
		minecarts.removeIf(m -> minecartMetadataMap.containsKey(m));
		for (Minecart minecart : minecarts) {
			minecartMetadataMap.put(minecart, new MinecartMetadata(minecart));
		}
		for (Iterator<Map.Entry<Minecart, MinecartMetadata>> it = minecartMetadataMap.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Minecart, MinecartMetadata> set = it.next();
			Minecart minecart = set.getKey();
			MinecartMetadata metadata = set.getValue();
			if (!minecart.isValid()) {
				it.remove();
				continue;
			}
			metadata.preTick();
			Vector velocity = metadata.velocity = minecart.getVelocity();
			if (!Util.isZeroVelocity(velocity)) {
				metadata.savedVelocity = null;
			} else if (metadata.savedVelocity == null) {
				continue;
			} else {
				metadata.stopped = true;
				velocity = metadata.velocity = metadata.savedVelocity;
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
			List<Sign> controlSigns = getControlSigns(block);
			for (Sign sign : controlSigns) {
				String command = sign.getLine(0).substring(5);
				ControlSignHandler handler = controlSignHandlers.get(command);
				if (handler != null) {
					handler.handle(minecart, metadata, sign);
				}
			}
			if (metadata.queueing || metadata.queueThisTick || metadata.stopThisTick) {
				boolean queueBlocked = false;
				if (metadata.stopThisTick) {
					queueBlocked = true;
				} else {
					double searchX = 0.0;
					double searchZ = 0.0;
					double searchXDistance = 0.5;
					double searchZDistance = 0.5;
					if (velocity.getX() < 0.0) {
						searchX -= 2.0;
						searchXDistance = 2.0;
					} else if (velocity.getX() > 0.0) {
						searchX += 2.0;
						searchXDistance = 2.0;
					}
					if (velocity.getZ() < 0.0) {
						searchZ -= 2.0;
						searchZDistance = 2.0;
					} else if (velocity.getZ() > 0.0) {
						searchZ += 2.0;
						searchZDistance = 2.0;
					}
					Location searchLocation = new Location(world, location.getX() + searchX, location.getY(), location.getZ() + searchZ);
					Collection<Entity> nearby = world.getNearbyEntities(searchLocation,
							searchXDistance, 2.0, searchZDistance,
							e -> {
								if (e == minecart || !(e instanceof Minecart)) return false;
								Minecart otherMinecart = (Minecart) e;
								MinecartMetadata otherMetadata = minecartMetadataMap.get(otherMinecart);
								if (otherMetadata == null) return true;
								Vector otherVelocity = otherMetadata.savedVelocity == null
										? otherMinecart.getVelocity()
										: otherMetadata.savedVelocity;
								return !Util.areOppositeDirections(metadata.velocity, otherVelocity);
							}
					);
					if (!nearby.isEmpty()) {
						queueBlocked = true;
					}
				}
				if (metadata.stopped && !queueBlocked) {
					minecart.setVelocity(velocity.multiply(0.8));
					metadata.savedVelocity = null;
				} else if (!metadata.stopped && queueBlocked) {
					minecart.setVelocity(zeroVelocity);
					if (metadata.savedVelocity == null) {
						metadata.savedVelocity = velocity;
					}
				}
			} else if (metadata.stopped && velocity != null) {
				minecart.setVelocity(velocity);
			}
		}
		updateLoaded(keepLoaded);
	}

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

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void itemMovedEvent(InventoryMoveItemEvent event) {
		Inventory inventory = event.getDestination();
		InventoryHolder holder = inventory.getHolder();
		if (!(holder instanceof StorageMinecart || holder instanceof HopperMinecart)) {
			return;
		}
		Minecart minecart = (Minecart) holder;
		MinecartMetadata metadata = minecartMetadataMap.get(minecart);
		if (metadata == null) return;
		if (Util.haveSpaceToInsert(inventory, event.getItem())) {
			metadata.pickupTime = 20;
		}
	}

	@EventHandler
	public void redstoneChange(BlockRedstoneEvent event) {
		Block block = event.getBlock();
		if (block.getType() != Material.DETECTOR_RAIL
				|| event.getOldCurrent() > 0
				|| event.getNewCurrent() == 0) {
			return;
		}
		boolean invert = false;
		Sign onlySign = getControlSign(event.getBlock(), "only");
		if (onlySign == null) {
			onlySign = getControlSign(event.getBlock(), "not");
			invert = true;
		}
		if (onlySign == null) {
			return;
		}
		boolean foundMinecart = false;
		String mustMatch = onlySign.getLine(1).trim().replace(" ", "").toLowerCase();
		Collection<Entity> minecarts = block.getWorld().getNearbyEntities(block.getLocation(), 1, 1, 1, e -> e instanceof Minecart);

		Class<? extends Minecart> mustMatchType = null;
		switch (mustMatch) {
			case "passenger": {
				mustMatchType = RideableMinecart.class;
				break;
			}
			case "chest": {
				mustMatchType = StorageMinecart.class;
				break;
			}
			case "hopper": {
				mustMatchType = HopperMinecart.class;
				break;
			}
		}
		for (Entity entity : minecarts) {
			if (!(entity instanceof Minecart)) continue;
			Minecart minecart = (Minecart) entity;
			if (mustMatchType == null) {
				String name = minecart.getCustomName();
				if (name == null) continue;
				String lowerName = name.trim().replace(" ", "").toLowerCase();
				if (lowerName.equals(mustMatch)) {
					foundMinecart = true;
					break;
				}
			} else if (mustMatchType.isAssignableFrom(minecart.getClass())) {
				foundMinecart = true;
				break;
			}
		}
		if (invert)
			foundMinecart = !foundMinecart;
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
			if (Util.canonicalize(sign.getLine(0), false).equals("cart:" + firstLine)) {
				return sign;
			}
		}
		return null;
	}

	private List<Sign> getControlSigns(Block rail) {
		if (lastRailSearched != null && lastRailSearched.equals(rail)) {
			return lastSigns;
		}
		LinkedList<Sign> signs = lastSigns = new LinkedList<>();
		Block below = rail.getRelative(BlockFace.DOWN);
		addSign(signs, rail.getRelative(BlockFace.UP), null);
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
		if (!Util.canonicalize(sign.getLine(0), false).startsWith("cart:")) return;
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
