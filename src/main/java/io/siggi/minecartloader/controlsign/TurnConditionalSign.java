package io.siggi.minecartloader.controlsign;

import io.siggi.minecartloader.MinecartMetadata;
import io.siggi.minecartloader.Util;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rail;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.RideableMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.inventory.Inventory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TurnConditionalSign extends ControlSignHandler {
	private static final Set<Material> RAILS;
	private static final Set<Rail.Shape> CONNECTS_NORTH;
	private static final Set<Rail.Shape> CONNECTS_SOUTH;
	private static final Set<Rail.Shape> CONNECTS_EAST;
	private static final Set<Rail.Shape> CONNECTS_WEST;

	static {
		Set<Material> rails = new HashSet<>();
		rails.addAll(Arrays.asList(new Material[]{
				Material.RAIL,
				Material.POWERED_RAIL,
				Material.DETECTOR_RAIL,
				Material.ACTIVATOR_RAIL
		}));
		RAILS = Collections.unmodifiableSet(rails);
		Set<Rail.Shape> connectsNorth = new HashSet<>();
		connectsNorth.addAll(Arrays.asList(new Rail.Shape[]{
				Rail.Shape.NORTH_SOUTH,
				Rail.Shape.NORTH_EAST,
				Rail.Shape.NORTH_WEST,
				Rail.Shape.ASCENDING_NORTH,
				Rail.Shape.ASCENDING_SOUTH
		}));
		CONNECTS_NORTH = Collections.unmodifiableSet(connectsNorth);
		Set<Rail.Shape> connectsSouth = new HashSet<>();
		connectsSouth.addAll(Arrays.asList(new Rail.Shape[]{
				Rail.Shape.NORTH_SOUTH,
				Rail.Shape.SOUTH_EAST,
				Rail.Shape.SOUTH_WEST,
				Rail.Shape.ASCENDING_NORTH,
				Rail.Shape.ASCENDING_SOUTH
		}));
		CONNECTS_SOUTH = Collections.unmodifiableSet(connectsSouth);
		Set<Rail.Shape> connectsEast = new HashSet<>();
		connectsEast.addAll(Arrays.asList(new Rail.Shape[]{
				Rail.Shape.EAST_WEST,
				Rail.Shape.NORTH_EAST,
				Rail.Shape.SOUTH_EAST,
				Rail.Shape.ASCENDING_EAST,
				Rail.Shape.ASCENDING_WEST
		}));
		CONNECTS_EAST = Collections.unmodifiableSet(connectsEast);
		Set<Rail.Shape> connectsWest = new HashSet<>();
		connectsWest.addAll(Arrays.asList(new Rail.Shape[]{
				Rail.Shape.EAST_WEST,
				Rail.Shape.NORTH_WEST,
				Rail.Shape.SOUTH_WEST,
				Rail.Shape.ASCENDING_EAST,
				Rail.Shape.ASCENDING_WEST
		}));
		CONNECTS_WEST = Collections.unmodifiableSet(connectsWest);
	}

	private final boolean not;

	public TurnConditionalSign(boolean not) {
		this.not = not;
	}

	@Override
	public void handle(Minecart minecart, MinecartMetadata metadata, Sign sign) {
		Block block = minecart.getLocation().getBlock();
		BlockState state = block.getState();
		BlockData blockData = state.getBlockData();
		if (!(blockData instanceof Rail)) return;
		Rail rail = (Rail) blockData;

		Block trackAhead;
		Rail.Shape shapeOnConditionPass = null;
		Rail.Shape shapeOnConditionFail = null;
		{
			Rail.Shape shape = rail.getShape();
			Block trackLeft, trackRight;
			Set<Rail.Shape> requiredMatchingTrackLeft, requiredMatchingTrackRight;
			Rail.Shape leftTurn, rightTurn;
			switch (shape) {
				case NORTH_SOUTH:
				case ASCENDING_NORTH:
				case ASCENDING_SOUTH:
					shapeOnConditionFail = Rail.Shape.NORTH_SOUTH;
					if (metadata.velocity.getZ() < 0) { // Northbound
						trackAhead = block.getRelative(BlockFace.NORTH);
						trackLeft = trackAhead.getRelative(BlockFace.WEST);
						trackRight = trackAhead.getRelative(BlockFace.EAST);
						requiredMatchingTrackLeft = CONNECTS_EAST;
						requiredMatchingTrackRight = CONNECTS_WEST;
						leftTurn = Rail.Shape.SOUTH_WEST;
						rightTurn = Rail.Shape.SOUTH_EAST;
					} else if (metadata.velocity.getZ() > 0) { // Southbound
						trackAhead = block.getRelative(BlockFace.SOUTH);
						trackLeft = trackAhead.getRelative(BlockFace.EAST);
						trackRight = trackAhead.getRelative(BlockFace.WEST);
						requiredMatchingTrackLeft = CONNECTS_WEST;
						requiredMatchingTrackRight = CONNECTS_EAST;
						leftTurn = Rail.Shape.NORTH_EAST;
						rightTurn = Rail.Shape.NORTH_WEST;
					} else return;
					break;
				case EAST_WEST:
				case ASCENDING_EAST:
				case ASCENDING_WEST:
					shapeOnConditionFail = Rail.Shape.EAST_WEST;
					if (metadata.velocity.getX() < 0) { // Westbound
						trackAhead = block.getRelative(BlockFace.WEST);
						trackLeft = trackAhead.getRelative(BlockFace.SOUTH);
						trackRight = trackAhead.getRelative(BlockFace.NORTH);
						requiredMatchingTrackLeft = CONNECTS_NORTH;
						requiredMatchingTrackRight = CONNECTS_SOUTH;
						leftTurn = Rail.Shape.SOUTH_EAST;
						rightTurn = Rail.Shape.NORTH_EAST;
					} else if (metadata.velocity.getX() > 0) { // Eastbound
						trackAhead = block.getRelative(BlockFace.EAST);
						trackLeft = trackAhead.getRelative(BlockFace.NORTH);
						trackRight = trackAhead.getRelative(BlockFace.SOUTH);
						requiredMatchingTrackLeft = CONNECTS_SOUTH;
						requiredMatchingTrackRight = CONNECTS_NORTH;
						leftTurn = Rail.Shape.NORTH_WEST;
						rightTurn = Rail.Shape.SOUTH_WEST;
					} else return;
					break;
				default:
					return;
			}
			if (trackAhead.getType() != Material.RAIL) {
				trackAhead = trackAhead.getRelative(BlockFace.UP);
				if (trackAhead.getType() != Material.RAIL) {
					return;
				}
				trackLeft = trackLeft.getRelative(BlockFace.UP);
				trackRight = trackRight.getRelative(BlockFace.UP);
			}
			trackLeft = findTrack(trackLeft, requiredMatchingTrackLeft);
			trackRight = findTrack(trackRight, requiredMatchingTrackRight);
			if (trackLeft != null) {
				if (trackRight == null) {
					shapeOnConditionPass = leftTurn;
				}
			} else if (trackRight != null) {
				shapeOnConditionPass = rightTurn;
			}
		}
		if (shapeOnConditionPass == null)
			return;
		boolean conditionPasses = false;
		boolean matchEmpty = false;
		String mustMatch = Util.canonicalize(sign.getLine(1), true);
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
			case "empty": {
				matchEmpty = true;
				break;
			}
		}
		if (matchEmpty) {
			if (minecart instanceof RideableMinecart) {
				if (minecart.getPassengers().isEmpty()) {
					conditionPasses = true;
				}
			} else {
				Inventory inventory = null;
				if (minecart instanceof HopperMinecart) {
					inventory = ((HopperMinecart) minecart).getInventory();
				}
				if (minecart instanceof StorageMinecart) {
					inventory = ((StorageMinecart) minecart).getInventory();
				}
				if (inventory != null && inventory.isEmpty()) {
					conditionPasses = true;
				}
			}
		} else if (mustMatchType == null) {
			String name = minecart.getCustomName();
			if (name == null) return;
			String lowerName = Util.canonicalize(name, true);
			if (lowerName.equals(mustMatch)) {
				conditionPasses = true;
			}
		} else if (mustMatchType.isAssignableFrom(minecart.getClass())) {
			conditionPasses = true;
		}
		if (not) conditionPasses = !conditionPasses;
		setShape(trackAhead, conditionPasses ? shapeOnConditionPass : shapeOnConditionFail);
	}

	private Block findTrack(Block block, Set<Rail.Shape> allowedShapes) {
		if (!RAILS.contains(block.getType())) {
			block = block.getRelative(BlockFace.DOWN);
			if (!RAILS.contains(block.getType())) {
				block = null;
			}
		}
		if (block != null && !allowedShapes.contains(getShape(block))) {
			block = null;
		}
		return block;
	}

	private Rail.Shape getShape(Block block) {
		try {
			return ((Rail) block.getBlockData()).getShape();
		} catch (Exception e) {
			return null;
		}
	}

	private void setShape(Block block, Rail.Shape shape) {
		try {
			Rail rail = (Rail) block.getBlockData();
			rail.setShape(shape);
			block.setBlockData(rail);
		} catch (Exception e) {
		}
	}
}
