package dev.afk;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class AfkCommand implements CommandExecutor, TabCompleter {

    private final AfkPlugin plugin;
    private final AfkManager manager;
    private final RoomManager rooms;
    private final WandListener wand;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public AfkCommand(AfkPlugin plugin, AfkManager manager, RoomManager rooms, WandListener wand) {
        this.plugin = plugin;
        this.manager = manager;
        this.rooms = rooms;
        this.wand = wand;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("Only players can toggle AFK.");
                return true;
            }
            if (!p.hasPermission("afk.use")) { sender.sendMessage("No permission."); return true; }
            manager.toggle(p);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> {
                if (!sender.hasPermission("afk.list")) { sender.sendMessage("No permission."); return true; }
                List<String> names = manager.afkNames();
                sender.sendMessage(names.isEmpty() ? "Nobody is AFK." : "AFK (" + names.size() + "): " + String.join(", ", names));
            }
            case "wand" -> {
                if (!(sender instanceof Player p) || !p.hasPermission("afk.admin")) { sender.sendMessage("No permission."); return true; }
                p.getInventory().addItem(WandListener.createWand());
                p.sendMessage(mm.deserialize("<yellow>Left-click a block = pos1, right-click a block = pos2.</yellow>"));
            }
            case "setroom" -> {
                if (!(sender instanceof Player p) || !p.hasPermission("afk.admin")) { sender.sendMessage("No permission."); return true; }
                Location a = wand.getPos1(p);
                Location b = wand.getPos2(p);
                if (a == null || b == null) { p.sendMessage(mm.deserialize("<red>Set pos1 and pos2 with the wand first.</red>")); return true; }
                rooms.setRegion(a, b);
                p.sendMessage(mm.deserialize("<green>AFK room saved.</green>"));
            }
            case "setspawn" -> {
                if (!(sender instanceof Player p) || !p.hasPermission("afk.admin")) { sender.sendMessage("No permission."); return true; }
                rooms.setSpawn(p.getLocation());
                p.sendMessage(mm.deserialize("<green>AFK spawn point set.</green>"));
            }
            case "setip" -> {
                if (!sender.hasPermission("afk.admin")) { sender.sendMessage("No permission."); return true; }
                if (args.length < 2) { sender.sendMessage("Usage: /afk setip <ip>"); return true; }
                plugin.getConfig().set("scoreboard.ip", args[1]);
                plugin.saveConfig();
                sender.sendMessage("Scoreboard IP updated to " + args[1] + ".");
            }
            case "reload" -> {
                if (!sender.hasPermission("afk.admin")) { sender.sendMessage("No permission."); return true; }
                plugin.reloadConfig();
                rooms.load();
                sender.sendMessage("Config reloaded.");
            }
            default -> sender.sendMessage("Usage: /afk [list|wand|setroom|setspawn|setip|reload]");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("list", "wand", "setroom", "setspawn", "setip", "reload").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        return List.of();
    }
}
