package dev.afk;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.UUID;

public final class BaltopCommand implements CommandExecutor {

    private final CoinManager coins;

    public BaltopCommand(CoinManager coins) {
        this.coins = coins;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Map<UUID, Long> top = coins.topBalances(10);
        if (top.isEmpty()) { sender.sendMessage("No coin data yet."); return true; }

        sender.sendMessage("--- Oskar Coin Leaderboard ---");
        int rank = 1;
        for (Map.Entry<UUID, Long> e : top.entrySet()) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(e.getKey());
            String name = op.getName() != null ? op.getName() : e.getKey().toString().substring(0, 8);
            sender.sendMessage(rank + ". " + name + " - " + e.getValue() + " coins");
            rank++;
        }
        return true;
    }
}
