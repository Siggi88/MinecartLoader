package io.siggi.minecartloader;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.minecart.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.*;

public class MinecartLoader extends JavaPlugin implements Listener {

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		getServer().getScheduler().runTaskTimer(this, this::tick, 1L, 1L);
		getCommand("findminecart").setExecutor(new FindMinecartCommand(this));
	}

	private final Vector zeroVelocity = new Vector(0.0, 0.0, 0.0);
	private final Map<Minecart, Vector> savedVelocity = new HashMap<>();
	private final Set<Minecart> queuingEnabled = new HashSet<>();

	private final SingleFile singleFile = new SingleFile();

	private final List<BlockFace> allDirections = Collections.unmodifiableList(Arrays.asList(new BlockFace[]{
			BlockFace.NORTH,
			BlockFace.SOUTH,
			BlockFace.WEST,
			BlockFace.EAST
	}));

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
			boolean queueOnce = false;
			if (queueControlSign != null) {
				String line = queueControlSign.getLine(1);
				if (line.isEmpty()) {
					queueOnce = true;
				} else {
					boolean enable = line.equals("1") || line.equals("on") || line.equals("yes");
					if (enable) {
						queuingEnabled.add(minecart);
					} else {
						queuingEnabled.remove(minecart);
					}
				}
			}
			Sign dropMinecartSign = getControlSign(block, "dropcart");
			if (dropMinecartSign != null) {
				ItemStack minecartItem = getItem(minecart);
				List<ItemStack> inventoryItems = getInventoryItems(minecart);
				minecart.remove();
				location.getWorld().dropItem(location, minecartItem);
				for (ItemStack stack : inventoryItems) {
					location.getWorld().dropItem(location, stack);
				}
				continue;
			}
			Sign setNameSign = getControlSign(block, "setname");
			if (setNameSign != null) {
				String name = setNameSign.getLine(1);
				if (name.isEmpty()) {
					minecart.setCustomName(null);
				} else {
					minecart.setCustomName(name);
				}
			}
			Sign setBlockSign = getControlSign(block, "setblock");
			if (setBlockSign != null) {
				String blockName = setBlockSign.getLine(1);
				if (blockName.isEmpty()) {
					minecart.setDisplayBlockData(null);
				} else {
					try {
						Material material = Material.valueOf(blockName.toUpperCase());
						BlockData blockData = material.createBlockData();
						minecart.setDisplayBlockData(blockData);
					} catch (Exception e) {
					}
				}
			}
			Sign boosterSign = getControlSign(block, "boost");
			if (boosterSign != null && !currentlyStopped) {
				Vector newVelocity = new Vector(
						max(velocity.getX(), 0.7),
						max(velocity.getY(), 0.7),
						max(velocity.getZ(), 0.7)
				);
				minecart.setVelocity(newVelocity);
				velocity = newVelocity;
				String line = boosterSign.getLine(1).trim().toLowerCase().replace(" ", "");
				switch (line) {
					case "heavy":
						minecart.setSlowWhenEmpty(false);
						break;
					case "light":
						minecart.setSlowWhenEmpty(true);
						break;
				}
			}
			Sign slowDownSign = getControlSign(block, "slowdown");
			if (slowDownSign != null && !currentlyStopped) {
				Vector newVelocity = new Vector(
						min(velocity.getX(), 0.1),
						min(velocity.getY(), 0.1),
						min(velocity.getZ(), 0.1)
				);
				minecart.setVelocity(newVelocity);
				velocity = newVelocity;
				String line = slowDownSign.getLine(1).trim().toLowerCase().replace(" ", "");
				switch (line) {
					case "heavy":
						minecart.setSlowWhenEmpty(false);
						break;
					case "light":
						minecart.setSlowWhenEmpty(true);
						break;
				}
			}
			Sign oneWaySign = getControlSign(block, "oneway");
			if (oneWaySign != null) {
				boolean bounce = false;
				String line = oneWaySign.getLine(1).trim().toLowerCase().replace(" ", "");
				switch (line) {
					case "north":
						if (velocity.getZ() > 0)
							bounce = true;
						break;
					case "south":
						if (velocity.getZ() < 0)
							bounce = true;
						break;
					case "west":
						if (velocity.getX() > 0)
							bounce = true;
						break;
					case "east":
						if (velocity.getX() < 0)
							bounce = true;
						break;
				}
				if (bounce) {
					velocity = velocity.multiply(-1.0);
					minecart.setVelocity(velocity);
				}
			}
			boolean forceStop = false;
			Sign singleFileSign = getControlSign(block, "singlefile");
			if (singleFileSign != null) {
				String trackName = singleFileSign.getLine(1).toLowerCase().trim().replace(" ", "");
				if (trackName.equals("release")) {
					singleFile.releaseMinecart(minecart);
				} else if (!singleFile.trackMinecart(minecart, singleFileSign, trackName)) {
					forceStop = true;
				}
			}
			Sign dropOffSign = getControlSign(block, "dropoff");
			if (dropOffSign != null && block.getRelative(BlockFace.DOWN).getType() == Material.HOPPER) {
				Inventory inventory = null;
				if (minecart instanceof StorageMinecart) {
					inventory = ((StorageMinecart) minecart).getInventory();
				} else if (minecart instanceof HopperMinecart) {
					inventory = ((HopperMinecart) minecart).getInventory();
				}
				if (inventory != null && !inventory.isEmpty()) {
					forceStop = true;
				}
			}
			Sign pickUpSign = getControlSign(block, "pickup");
			if (pickUpSign != null) {
				Block above = block.getRelative(BlockFace.UP);
				if (above.getType() == Material.HOPPER) {
					Inventory inventory = null;
					if (minecart instanceof StorageMinecart) {
						inventory = ((StorageMinecart) minecart).getInventory();
					} else if (minecart instanceof HopperMinecart) {
						inventory = ((HopperMinecart) minecart).getInventory();
					}
					if (inventory != null) {
						Inventory aboveInventory = ((Hopper) above.getState()).getInventory();
						if (!aboveInventory.isEmpty() && inventory.firstEmpty() != -1) {
							forceStop = true;
						}
					}
				}
			}
			if (queuingEnabled.contains(minecart) || queueOnce || forceStop) {
				boolean queueBlocked = false;
				if (forceStop) {
					queueBlocked = true;
				} else {
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
					if (!nearby.isEmpty()) {
						queueBlocked = true;
					}
				}
				if (currentlyStopped && !queueBlocked) {
					minecart.setVelocity(velocity.multiply(0.8));
					savedVelocity.remove(minecart);
				} else if (!currentlyStopped && queueBlocked) {
					minecart.setVelocity(zeroVelocity);
					savedVelocity.put(minecart, velocity);
				}
			} else if (currentlyStopped && velocity != null) {
				minecart.setVelocity(velocity);
			}
		}
		updateLoaded(keepLoaded);
	}

	private ItemStack getItem(Minecart minecart) {
		Material material;
		if (minecart instanceof CommandMinecart) {
			material = Material.COMMAND_BLOCK_MINECART;
		} else if (minecart instanceof StorageMinecart) {
			material = Material.CHEST_MINECART;
		} else if (minecart instanceof ExplosiveMinecart) {
			material = Material.TNT_MINECART;
		} else if (minecart instanceof HopperMinecart) {
			material = Material.HOPPER_MINECART;
		} else {
			material = Material.MINECART;
		}
		ItemStack stack = new ItemStack(material, 1);
		String name = minecart.getCustomName();
		if (name != null) {
			ItemMeta meta = stack.getItemMeta();
			meta.setDisplayName(name);
			stack.setItemMeta(meta);
		}
		return stack;
	}

	private List<ItemStack> getInventoryItems(Minecart minecart) {
		List<ItemStack> items = new ArrayList<>(27);
		Inventory inventory = null;
		if (minecart instanceof StorageMinecart) {
			inventory = ((StorageMinecart) minecart).getInventory();
		} else if (minecart instanceof HopperMinecart) {
			inventory = ((HopperMinecart) minecart).getInventory();
		}
		if (inventory == null) return items;
		for (ItemStack stack : inventory.getContents()) {
			if (stack != null && stack.getType() != Material.AIR) {
				items.add(stack);
			}
		}
		return items;
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

	private double max(double value, double maxAmount) {
		if (value < 0) {
			return -maxAmount;
		} else if (value > 0) {
			return maxAmount;
		} else {
			return 0;
		}
	}

	private double min(double value, double maxAmount) {
		if (value < 0) {
			return Math.max(value, -maxAmount);
		} else if (value > 0) {
			return Math.min(value, maxAmount);
		} else {
			return 0;
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
			if (sign.getLine(0).toLowerCase().equals(firstLine)) {
				return sign;
			}
		}
		return null;
	}

	private Block lastRailSearched = null;
	private LinkedList<Sign> lastSigns = null;

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
