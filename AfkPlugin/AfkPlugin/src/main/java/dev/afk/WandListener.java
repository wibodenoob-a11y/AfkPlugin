package dev.afk;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class WandListener implements Listener {

    private static final Component WAND_NAME = Component.text("AFK Wand", NamedTextColor.GOLD);

    private final AfkPlugin plugin;
    private final RoomManager rooms;
    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();

    public WandListener(AfkPlugin plugin, RoomManager rooms) {
        this.plugin = plugin;
        this.rooms = rooms;
    }

    public static ItemStack createWand() {
        ItemStack item = new ItemStack(Material.GOLDEN_AXE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(WAND_NAME);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isWand(ItemStack item) {
        if (item == null || item.getType() != Material.GOLDEN_AXE || !item.hasItemMeta()) return false;
        return WAND_NAME.equals(item.getItemMeta().displayName());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isWand(e.getItem())) return;
        if (e.getClickedBlock() == null) return;
        if (!p.hasPermission("afk.admin")) return;

        if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            e.setCancelled(true);
            pos1.put(p.getUniqueId(), e.getClickedBlock().getLocation());
            p.sendMessage(Component.text("Position 1 set.", NamedTextColor.YELLOW));
        } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            pos2.put(p.getUniqueId(), e.getClickedBlock().getLocation());
            p.sendMessage(Component.text("Position 2 set.", NamedTextColor.YELLOW));
        }
    }

    public Location getPos1(Player p) { return pos1.get(p.getUniqueId()); }
    public Location getPos2(Player p) { return pos2.get(p.getUniqueId()); }
}
