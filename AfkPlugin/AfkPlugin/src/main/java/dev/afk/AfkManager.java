package dev.afk;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AfkManager {

    private final AfkPlugin plugin;
    private final RoomManager rooms;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<UUID, Long> lastActive = new ConcurrentHashMap<>();
    private final Set<UUID> afk = ConcurrentHashMap.newKeySet();
    private BukkitTask task;

    public AfkManager(AfkPlugin plugin, RoomManager rooms) {
        this.plugin = plugin;
        this.rooms = rooms;
    }

    public void start() {
        for (Player p : Bukkit.getOnlinePlayers()) track(p);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) task.cancel();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (afk.contains(p.getUniqueId())) applyState(p, false);
        }
        afk.clear();
        lastActive.clear();
    }

    public void track(Player p) {
        lastActive.put(p.getUniqueId(), System.currentTimeMillis());
    }

    public void untrack(Player p) {
        lastActive.remove(p.getUniqueId());
        afk.remove(p.getUniqueId());
    }

    public boolean isAfk(UUID id) {
        return afk.contains(id);
    }

    /** Call whenever a player does something. Ignored while they're parked in the AFK room. */
    public void activity(Player p) {
        if (rooms.isMarkedInside(p.getUniqueId())) return;
        lastActive.put(p.getUniqueId(), System.currentTimeMillis());
        if (afk.contains(p.getUniqueId())) setAfk(p, false);
    }

    public void toggle(Player p) {
        setAfk(p, !afk.contains(p.getUniqueId()));
    }

    public void setAfk(Player p, boolean state) {
        UUID id = p.getUniqueId();
        if (state == afk.contains(id)) return;
        if (state) afk.add(id); else afk.remove(id);
        applyState(p, state);
        if (plugin.getConfig().getBoolean("afk.announce", true)) {
            Bukkit.broadcast(message(state ? "now-afk" : "no-longer-afk", p));
        }
        if (!state) lastActive.put(id, System.currentTimeMillis());
    }

    private void applyState(Player p, boolean state) {
        if (state) {
            Component prefix = mm.deserialize(plugin.getConfig().getString("afk.tab-prefix", "<gray>[AFK]</gray> "));
            p.playerListName(prefix.append(Component.text(p.getName())));
        } else {
            p.playerListName(null);
        }
    }

    private void tick() {
        long autoMs = plugin.getConfig().getLong("afk.auto-afk-seconds", 300) * 1000L;
        long now = System.currentTimeMillis();
        if (autoMs <= 0) return;

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (rooms.isMarkedInside(p.getUniqueId())) continue; // room owns this player's AFK state
            Long last = lastActive.get(p.getUniqueId());
            if (last == null) { track(p); continue; }
            if (now - last >= autoMs && !afk.contains(p.getUniqueId())) {
                setAfk(p, true);
            }
        }
    }

    public List<String> afkNames() {
        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> afk.contains(p.getUniqueId()))
                .map(Player::getName)
                .toList();
    }

    public Component message(String key, Player p) {
        String raw = plugin.getConfig().getString("messages." + key, key);
        return mm.deserialize(raw, Placeholder.unparsed("player", p.getName()));
    }
}
