package dev.afk;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class RankManager {

    private final AfkPlugin plugin;
    private final File file;
    private final FileConfiguration data;

    public RankManager(AfkPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "ranks.yml");
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try { file.createNewFile(); } catch (IOException ignored) { }
        }
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save ranks.yml: " + e.getMessage());
        }
    }

    public boolean hasActive(UUID id, String rankId) {
        long expiry = data.getLong("active." + id + "." + rankId, 0);
        return expiry > System.currentTimeMillis();
    }

    /** Records the purchase locally and runs LuckPerms' temp-group command from console. */
    public void grant(Player p, String rankId, String group, String duration) {
        long ms = TimeUtil.parseToMillis(duration);
        data.set("active." + p.getUniqueId() + "." + rankId, System.currentTimeMillis() + ms);
        save();
        String cmd = "lp user " + p.getName() + " parent addtemp " + group + " " + duration;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }
}
