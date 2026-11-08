package dev.afk;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public final class RoomProtectionListener implements Listener {

    private final AfkPlugin plugin;
    private final RoomManager rooms;

    public RoomProtectionListener(AfkPlugin plugin, RoomManager rooms) {
        this.plugin = plugin;
        this.rooms = rooms;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!rooms.isInside(p.getLocation())) return;
        if (plugin.getConfig().getBoolean("room.protection.no-damage", true)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!rooms.isInside(p.getLocation())) return;
        if (plugin.getConfig().getBoolean("room.protection.no-hunger", true)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (!rooms.isInside(e.getBlock().getLocation())) return;
        if (plugin.getConfig().getBoolean("room.protection.no-block-break", true)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (!rooms.isInside(e.getBlock().getLocation())) return;
        if (plugin.getConfig().getBoolean("room.protection.no-block-place", true)) {
            e.setCancelled(true);
        }
    }
}
