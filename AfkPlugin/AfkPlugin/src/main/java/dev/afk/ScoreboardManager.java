package dev.afk;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.UUID;

public final class ScoreboardManager implements Listener {

    private static final String[] LINE_KEYS = {"name", "kills", "coins", "spacer", "ip"};

    private final AfkPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ScoreboardManager(AfkPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        setup(e.getPlayer());
    }

    public void setup(Player p) {
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) return;

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("afkplugin", Criteria.DUMMY,
                mm.deserialize(plugin.getConfig().getString("scoreboard.title", "<gold><bold>OskarSMP</bold></gold>")));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int score = LINE_KEYS.length;
        for (String key : LINE_KEYS) {
            String entry = ChatColor.values()[score % 15].toString();
            Team team = board.registerNewTeam(key);
            team.addEntry(entry);
            obj.getScore(entry).setScore(score);
            score--;
        }

        p.setScoreboard(board);
        update(p);
    }

    public void update(Player p) {
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) return;
        Scoreboard board = p.getScoreboard();
        if (board == null || board.getObjective(DisplaySlot.SIDEBAR) == null) return;

        int kills = p.getStatistic(Statistic.PLAYER_KILLS);
        long coins = plugin.getCoinManager().getCoins(p.getUniqueId());
        String ip = plugin.getConfig().getString("scoreboard.ip", "");

        setLine(board, "name", Component.text("Player: ", NamedTextColor.GRAY).append(Component.text(p.getName(), NamedTextColor.WHITE)));
        setLine(board, "kills", Component.text("Kills: ", NamedTextColor.GRAY).append(Component.text(kills, NamedTextColor.GREEN)));
        setLine(board, "coins", Component.text("Coins: ", NamedTextColor.GRAY).append(Component.text(coins, NamedTextColor.GOLD)));
        setLine(board, "spacer", Component.empty());
        setLine(board, "ip", Component.text(ip, NamedTextColor.AQUA));
    }

    private void setLine(Scoreboard board, String key, Component text) {
        Team team = board.getTeam(key);
        if (team == null) return;
        team.prefix(text);
    }

    public void updatePlayer(UUID id) {
        Player p = Bukkit.getPlayer(id);
        if (p != null) update(p);
    }
}
