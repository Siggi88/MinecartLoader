package io.siggi.minecartloader;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;

import java.util.ArrayList;
import java.util.List;

public class SignFinder {
	private SignFinder() {
	}

	private static BlockFace getPerpendicularDirection(BlockFace face) {
		switch (face) {
			case NORTH:
			case SOUTH:
				return BlockFace.WEST;
			case WEST:
			case EAST:
				return BlockFace.NORTH;
			default:
				// unreachable, not a valid wall sign direction
				// unless we were passed a BlockFace that didn't come from a wall sign
				return null;
		}
	}

	public static List<Sign> findOtherSigns(Sign sign, boolean findMain) {
		List<Sign> list = new ArrayList<>();
		Block block = sign.getBlock();
		BlockData blockData = sign.getBlockData();
		if (!(blockData instanceof WallSign)) {
			return list;
		}
		WallSign wallSign = (WallSign) blockData;
		BlockFace facing = wallSign.getFacing();
		BlockFace direction = getPerpendicularDirection(facing);
		addSigns(list, block, facing, direction, findMain);
		addSigns(list, block, facing, direction.getOppositeFace(), findMain);
		return list;
	}

	private static void addSigns(List<Sign> list, Block block, BlockFace facing, BlockFace direction, boolean findMain) {
		while (true) {
			block = block.getRelative(direction);
			BlockState state = block.getState();
			if (!(state instanceof Sign))
				return;
			Sign sign = (Sign) state;
			String firstLine = Util.canonicalize(sign.getLine(0), false);
			boolean mainSign = findMain && firstLine.startsWith("cart:");
			if (!mainSign && !firstLine.equals("more"))
				return;
			BlockData blockData = sign.getBlockData();
			if (!(blockData instanceof WallSign))
				return;
			WallSign wallSign = (WallSign) blockData;
			if (!wallSign.getFacing().equals(facing))
				return;
			if (!findMain || mainSign)
				list.add(sign);
		}
	}
}
