package io.siggi.minecartloader;

import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Minecart;

public class FindMinecartCommand implements CommandExecutor {

	private final MinecartLoader plugin;

	FindMinecartCommand(MinecartLoader plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		String minecartName = args[0];
		for (Minecart minecart : plugin.getAllMinecarts()) {
			String name = minecart.getName();
			if (name != null && name.equals(minecartName)) {
				Block block = minecart.getLocation().getBlock();
				String uuid = minecart.getUniqueId().toString().substring(0, 8);
				sender.sendMessage(uuid + ": " + block.getX() + "," + block.getY() + "," + block.getZ() + "," + Util.getDirection(minecart.getVelocity()).toString());
			}
		}
		return true;
	}
}
