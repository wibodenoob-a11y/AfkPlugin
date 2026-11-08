package dev.afk;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ShopManager implements Listener {

    private final AfkPlugin plugin;
    private final CoinManager coins;
    private final RankManager ranks;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<UUID, Pending> pending = new HashMap<>();

    public ShopManager(AfkPlugin plugin, CoinManager coins, RankManager ranks) {
        this.plugin = plugin;
        this.coins = coins;
        this.ranks = ranks;
    }

    private record Pending(boolean isRank, String id, String display, String group,
                            String duration, String kit, long price) { }

    private static final class ShopHolder implements InventoryHolder {
        private Inventory inv;
        @Override public Inventory getInventory() { return inv; }
    }

    private static final class ConfirmHolder implements InventoryHolder {
        private Inventory inv;
        @Override public Inventory getInventory() { return inv; }
    }

    public void open(Player p) {
        ShopHolder holder = new ShopHolder();
        Inventory inv = Bukkit.createInventory(holder, 36, mm.deserialize("<gold><bold>Oskar Coin Shop</bold></gold>"));
        holder.inv = inv;

        int[] rankSlots = {10, 12, 14, 16};
        int[] kitSlots = {19, 21, 23, 25};

        ConfigurationSection rankSec = plugin.getConfig().getConfigurationSection("shop.ranks");
        ConfigurationSection kitSec = plugin.getConfig().getConfigurationSection("shop.kits");

        int i = 0;
        if (rankSec != null) {
            for (String id : rankSec.getKeys(false)) {
                if (i >= rankSlots.length) break;
                long price = rankSec.getLong(id + ".price", 0);
                String duration = rankSec.getString(id + ".duration", "3d");
                inv.setItem(rankSlots[i], makeItem(Material.DIAMOND, id,
                        List.of("<gray>Rank for " + duration + "</gray>", "<yellow>Price: " + price + " coins</yellow>"),
                        "rank:" + id));
                i++;
            }
        }

        i = 0;
        if (kitSec != null) {
            for (String id : kitSec.getKeys(false)) {
                if (i >= kitSlots.length) break;
                long price = kitSec.getLong(id + ".price", 0);
                inv.setItem(kitSlots[i], makeItem(Material.CHEST, id,
                        List.of("<gray>One-time kit</gray>", "<yellow>Price: " + price + " coins</yellow>"),
                        "kit:" + id));
                i++;
            }
        }

        p.openInventory(inv);
    }

    private ItemStack makeItem(Material mat, String display, List<String> loreLines, String tag) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mm.deserialize("<white>" + display + "</white>"));
        List<Component> lore = new ArrayList<>();
        for (String l : loreLines) lore.add(mm.deserialize(l));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(plugin.getShopKey(), PersistentDataType.STRING, tag);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        InventoryHolder holder = e.getInventory().getHolder();

        if (holder instanceof ShopHolder) {
            e.setCancelled(true);
            ItemStack item = e.getCurrentItem();
            if (item == null || !item.hasItemMeta()) return;
            String tag = item.getItemMeta().getPersistentDataContainer()
                    .get(plugin.getShopKey(), PersistentDataType.STRING);
            if (tag == null) return;

            boolean isRank = tag.startsWith("rank:");
            String id = tag.substring(tag.indexOf(':') + 1);
            ConfigurationSection sec = plugin.getConfig().getConfigurationSection(
                    isRank ? "shop.ranks." + id : "shop.kits." + id);
            if (sec == null) return;

            long price = sec.getLong("price", 0);

            if (isRank) {
                String group = sec.getString("group", id);
                String duration = sec.getString("duration", "3d");
                if (ranks.hasActive(p.getUniqueId(), id)) {
                    p.sendMessage(mm.deserialize("<red>You already have an active " + id + " rank.</red>"));
                    p.closeInventory();
                    return;
                }
                pending.put(p.getUniqueId(), new Pending(true, id, id, group, duration, null, price));
            } else {
                String kit = sec.getString("kit", id);
                pending.put(p.getUniqueId(), new Pending(false, id, id, null, null, kit, price));
            }
            openConfirm(p, id, price);

        } else if (holder instanceof ConfirmHolder) {
            e.setCancelled(true);
            ItemStack item = e.getCurrentItem();
            if (item == null) return;
            Pending pend = pending.get(p.getUniqueId());
            if (pend == null) { p.closeInventory(); return; }

            if (item.getType() == Material.LIME_WOOL) {
                executePurchase(p, pend);
                p.closeInventory();
            } else if (item.getType() == Material.RED_WOOL) {
                pending.remove(p.getUniqueId());
                p.closeInventory();
            }
        }
    }

    private void openConfirm(Player p, String display, long price) {
        ConfirmHolder holder = new ConfirmHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, mm.deserialize("<gold>Confirm Purchase</gold>"));
        holder.inv = inv;

        inv.setItem(11, makeSimple(Material.LIME_WOOL, "<green>Confirm: " + display + " (" + price + " coins)</green>"));
        inv.setItem(15, makeSimple(Material.RED_WOOL, "<red>Cancel</red>"));
        p.openInventory(inv);
    }

    private ItemStack makeSimple(Material mat, String display) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mm.deserialize(display));
        item.setItemMeta(meta);
        return item;
    }

    private void executePurchase(Player p, Pending pend) {
        if (!coins.takeCoins(p.getUniqueId(), pend.price())) {
            p.sendMessage(mm.deserialize("<red>You don't have enough coins.</red>"));
            pending.remove(p.getUniqueId());
            return;
        }
        if (pend.isRank()) {
            ranks.grant(p, pend.id(), pend.group(), pend.duration());
            p.sendMessage(mm.deserialize("<green>You bought the " + pend.display() + " rank for " + pend.duration() + "!</green>"));
        } else {
            String cmdTemplate = plugin.getConfig().getString("kits.give-command", "kit give {kit} {player}");
            String cmd = cmdTemplate.replace("{kit}", pend.kit()).replace("{player}", p.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            p.sendMessage(mm.deserialize("<green>You bought the " + pend.display() + " kit!</green>"));
        }
        pending.remove(p.getUniqueId());
    }
}
