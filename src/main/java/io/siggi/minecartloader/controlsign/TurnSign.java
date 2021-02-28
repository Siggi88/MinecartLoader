package io.siggi.minecartloader.controlsign;

import io.siggi.minecartloader.MinecartMetadata;
import io.siggi.minecartloader.Util;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Rail;
import org.bukkit.entity.Minecart;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TurnSign extends ControlSignHandler {
	@Override
	public void handle(Minecart minecart, MinecartMetadata metadata, Sign sign) {
		Block block = minecart.getLocation().getBlock();
		BlockFace direction = Util.getDirection(minecart.getVelocity());
		Block blockAhead = block.getRelative(direction);
		Block trackLeft, trackRight;
		Set<Rail.Shape> leftRequiredShape, rightRequiredShape;
		Rail.Shape leftShape;
		Rail.Shape rightShape;
		Rail.Shape straightShape;
		switch (direction) {
			case NORTH:
				trackLeft = blockAhead.getRelative(BlockFace.WEST);
				trackRight = blockAhead.getRelative(BlockFace.EAST);
				leftRequiredShape = CONNECTS_EAST;
				rightRequiredShape = CONNECTS_WEST;
				leftShape = Rail.Shape.SOUTH_WEST;
				rightShape = Rail.Shape.SOUTH_EAST;
				straightShape = Rail.Shape.NORTH_SOUTH;
				break;
			case SOUTH:
				trackLeft = blockAhead.getRelative(BlockFace.EAST);
				trackRight = blockAhead.getRelative(BlockFace.WEST);
				leftRequiredShape = CONNECTS_WEST;
				rightRequiredShape = CONNECTS_EAST;
				leftShape = Rail.Shape.NORTH_EAST;
				rightShape = Rail.Shape.NORTH_WEST;
				straightShape = Rail.Shape.NORTH_SOUTH;
				break;
			case EAST:
				trackLeft = blockAhead.getRelative(BlockFace.NORTH);
				trackRight = blockAhead.getRelative(BlockFace.SOUTH);
				leftRequiredShape = CONNECTS_SOUTH;
				rightRequiredShape = CONNECTS_NORTH;
				leftShape = Rail.Shape.NORTH_WEST;
				rightShape = Rail.Shape.SOUTH_WEST;
				straightShape = Rail.Shape.EAST_WEST;
				break;
			case WEST:
				trackLeft = blockAhead.getRelative(BlockFace.SOUTH);
				trackRight = blockAhead.getRelative(BlockFace.NORTH);
				leftRequiredShape = CONNECTS_NORTH;
				rightRequiredShape = CONNECTS_SOUTH;
				leftShape = Rail.Shape.SOUTH_EAST;
				rightShape = Rail.Shape.NORTH_EAST;
				straightShape = Rail.Shape.EAST_WEST;
				break;
			default:
				return;
		}
		if (!RAILS.contains(blockAhead.getType())) {
			blockAhead = blockAhead.getRelative(BlockFace.UP);
			if (!RAILS.contains(blockAhead.getType())) return;
			trackLeft = trackLeft.getRelative(BlockFace.UP);
			trackRight = trackRight.getRelative(BlockFace.UP);
		}
		trackLeft = findTrack(trackLeft, leftRequiredShape);
		trackRight = findTrack(trackRight, rightRequiredShape);
		String desiredDirection = Util.canonicalize(sign.getLine(1), false);
		switch (desiredDirection) {
			case "straight":
				setShape(blockAhead, straightShape);
				break;
			case "left":
				if (trackLeft != null) {
					setShape(blockAhead, leftShape);
				}
				break;
			case "right":
				if (trackRight != null) {
					setShape(blockAhead, rightShape);
				}
				break;
			default:
				if (trackLeft != null) {
					if (trackRight == null) {
						setShape(blockAhead, leftShape);
					}
				} else if (trackRight != null) {
					setShape(blockAhead, rightShape);
				}
				break;
		}
	}

	public static final Set<Material> RAILS;
	public static final Set<Rail.Shape> CONNECTS_NORTH;
	public static final Set<Rail.Shape> CONNECTS_SOUTH;
	public static final Set<Rail.Shape> CONNECTS_EAST;
	public static final Set<Rail.Shape> CONNECTS_WEST;

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

	public static Block findTrack(Block block, Set<Rail.Shape> allowedShapes) {
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

	public static Rail.Shape getShape(Block block) {
		try {
			return ((Rail) block.getBlockData()).getShape();
		} catch (Exception e) {
			return null;
		}
	}

	public static void setShape(Block block, Rail.Shape shape) {
		try {
			Rail rail = (Rail) block.getBlockData();
			rail.setShape(shape);
			block.setBlockData(rail);
		} catch (Exception e) {
		}
	}
}
