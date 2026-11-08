package dev.afk;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class RoomManager {

    private final AfkPlugin plugin;
    private Location pos1, pos2, spawn;
    private final Set<UUID> inRoom = new HashSet<>();

    public RoomManager(AfkPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("room.region");
        if (sec != null && sec.contains("pos1") && sec.contains("pos2")) {
            pos1 = deserialize(sec.getConfigurationSection("pos1"));
            pos2 = deserialize(sec.getConfigurationSection("pos2"));
        } else {
            pos1 = null;
            pos2 = null;
        }
        ConfigurationSection sp = plugin.getConfig().getConfigurationSection("room.spawn");
        spawn = sp != null ? deserialize(sp) : null;
    }

    private Location deserialize(ConfigurationSection s) {
        if (s == null) return null;
        World w = Bukkit.getWorld(s.getString("world", ""));
        if (w == null) return null;
        return new Location(w, s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
                (float) s.getDouble("yaw", 0), (float) s.getDouble("pitch", 0));
    }

    private void serialize(ConfigurationSection s, Location loc) {
        s.set("world", loc.getWorld().getName());
        s.set("x", loc.getX());
        s.set("y", loc.getY());
        s.set("z", loc.getZ());
        s.set("yaw", (double) loc.getYaw());
        s.set("pitch", (double) loc.getPitch());
    }

    public void setRegion(Location a, Location b) {
        this.pos1 = a;
        this.pos2 = b;
        ConfigurationSection sec = plugin.getConfig().createSection("room.region");
        serialize(sec.createSection("pos1"), a);
        serialize(sec.createSection("pos2"), b);
        plugin.saveConfig();
    }

    public void setSpawn(Location loc) {
        this.spawn = loc;
        ConfigurationSection sp = plugin.getConfig().createSection("room.spawn");
        serialize(sp, loc);
        plugin.saveConfig();
    }

    public boolean hasRegion() {
        return pos1 != null && pos2 != null;
    }

    public boolean hasSpawn() {
        return spawn != null;
    }

    public Location getSpawn() {
        return spawn;
    }

    public boolean isInside(Location loc) {
        if (!hasRegion() || loc == null || loc.getWorld() == null || !loc.getWorld().equals(pos1.getWorld())) return false;
        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());
        return loc.getX() >= minX && loc.getX() <= maxX
                && loc.getY() >= minY && loc.getY() <= maxY
                && loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }

    public void markInside(Player p) {
        inRoom.add(p.getUniqueId());
    }

    public void markOutside(Player p) {
        inRoom.remove(p.getUniqueId());
    }

    public boolean isMarkedInside(UUID id) {
        return inRoom.contains(id);
    }
}
