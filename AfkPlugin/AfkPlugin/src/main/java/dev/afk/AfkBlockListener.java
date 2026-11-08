package dev.afk;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AfkBlockListener implements Listener {

    private final AfkPlugin plugin;
    private final RoomManager rooms;
    private final AfkManager afk;
    private final CoinManager coins;
    private final Map<UUID, Location> cameFrom = new HashMap<>();
    private final Map<UUID, Long> cooldown = new HashMap<>();

    public AfkBlockListener(AfkPlugin plugin, RoomManager rooms, AfkManager afk, CoinManager coins) {
        this.plugin = plugin;
        this.rooms = rooms;
        this.afk = afk;
        this.coins = coins;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getTo() == null) return;
        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;

        Player p = e.getPlayer();
        Material blockMat = Material.matchMaterial(
                plugin.getConfig().getString("room.block-material", "RED_STAINED_GLASS"));
        if (blockMat == null) blockMat = Material.RED_STAINED_GLASS;

        Location under = e.getTo().clone().subtract(0, 1, 0);
        if (under.getBlock().getType() != blockMat) return;

        UUID id = p.getUniqueId();
        long now = System.currentTimeMillis();
        long cdMs = plugin.getConfig().getLong("room.teleport-cooldown-seconds", 3) * 1000L;
        Long last = cooldown.get(id);
        if (last != null && now - last < cdMs) return;

        boolean insideRoom = rooms.isInside(e.getTo());

        if (!insideRoom) {
            if (!rooms.hasSpawn()) return;
            cameFrom.put(id, p.getLocation().clone());
            cooldown.put(id, now);
            p.teleportAsync(rooms.getSpawn());
            rooms.markInside(p);
            afk.setAfk(p, true);
            coins.startTimer(id);
        } else {
            cooldown.put(id, now);
            Location back = cameFrom.remove(id);
            rooms.markOutside(p);
            afk.setAfk(p, false);
            coins.stopTimer(id);
            p.teleportAsync(back != null ? back : p.getWorld().getSpawnLocation());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        cameFrom.remove(id);
        cooldown.remove(id);
        rooms.markOutside(e.getPlayer());
        coins.stopTimer(id);
    }
}
