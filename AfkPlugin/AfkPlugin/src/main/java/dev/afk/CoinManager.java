package dev.afk;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class CoinManager {

    private final AfkPlugin plugin;
    private final File file;
    private final FileConfiguration data;
    private final Map<UUID, Long> nextCoinAt = new HashMap<>();

    public CoinManager(AfkPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "coins.yml");
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try { file.createNewFile(); } catch (IOException ignored) { }
        }
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save coins.yml: " + e.getMessage());
        }
    }

    public long getCoins(UUID id) {
        return data.getLong("players." + id + ".coins", 0);
    }

    public void setCoins(UUID id, long amount) {
        data.set("players." + id + ".coins", Math.max(0, amount));
        save();
        plugin.getScoreboardManager().updatePlayer(id);
    }

    public void addCoins(UUID id, long amount) {
        setCoins(id, getCoins(id) + amount);
    }

    public boolean takeCoins(UUID id, long amount) {
        long bal = getCoins(id);
        if (bal < amount) return false;
        setCoins(id, bal - amount);
        return true;
    }

    public void startTimer(UUID id) {
        long intervalMs = plugin.getConfig().getLong("coins.interval-seconds", 120) * 1000L;
        nextCoinAt.put(id, System.currentTimeMillis() + intervalMs);
    }

    public void stopTimer(UUID id) {
        nextCoinAt.remove(id);
        Player p = Bukkit.getPlayer(id);
        if (p != null) p.sendActionBar(Component.empty());
    }

    public void tick() {
        long intervalMs = plugin.getConfig().getLong("coins.interval-seconds", 120) * 1000L;
        boolean actionbar = plugin.getConfig().getBoolean("coins.actionbar-countdown", true);
        boolean sound = plugin.getConfig().getBoolean("coins.sound", true);
        long now = System.currentTimeMillis();

        for (Map.Entry<UUID, Long> entry : new HashMap<>(nextCoinAt).entrySet()) {
            UUID id = entry.getKey();
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline()) {
                nextCoinAt.remove(id);
                continue;
            }

            long remaining = entry.getValue() - now;
            if (remaining <= 0) {
                addCoins(id, 1);
                p.sendMessage(Component.text("You got 1 Oskar coin!", NamedTextColor.GOLD));
                if (sound) {
                    try {
                        p.playSound(p.getLocation(), Sound.valueOf(
                                plugin.getConfig().getString("coins.sound-name", "ENTITY_EXPERIENCE_ORB_PICKUP")), 1f, 1f);
                    } catch (IllegalArgumentException ignored) { }
                }
                nextCoinAt.put(id, now + intervalMs);
                remaining = intervalMs;
            }
            if (actionbar) {
                long secs = Math.max(0, remaining / 1000);
                p.sendActionBar(Component.text("Next Oskar coin in " + secs + "s", NamedTextColor.YELLOW));
            }
        }
    }

    public Map<UUID, Long> topBalances(int limit) {
        Map<UUID, Long> all = new LinkedHashMap<>();
        if (data.getConfigurationSection("players") == null) return all;
        for (String key : data.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                all.put(id, data.getLong("players." + key + ".coins", 0));
            } catch (IllegalArgumentException ignored) { }
        }
        return all.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }
}
