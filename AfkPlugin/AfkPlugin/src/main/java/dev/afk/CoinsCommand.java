package dev.afk;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class CoinsCommand implements CommandExecutor, TabCompleter {

    private final CoinManager coins;

    public CoinsCommand(CoinManager coins) {
        this.coins = coins;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
            sender.sendMessage("You have " + coins.getCoins(p.getUniqueId()) + " Oskar coins.");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give" -> {
                if (!sender.hasPermission("coins.admin")) { sender.sendMessage("No permission."); return true; }
                if (args.length < 3) { sender.sendMessage("Usage: /coins give <player> <amount>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage("Player not found."); return true; }
                long amount = parseLong(args[2]);
                if (amount <= 0) { sender.sendMessage("Invalid amount."); return true; }
                coins.addCoins(target.getUniqueId(), amount);
                sender.sendMessage("Gave " + amount + " coins to " + target.getName() + ".");
                target.sendMessage("You received " + amount + " Oskar coins.");
            }
            case "giveall" -> {
                if (!sender.hasPermission("coins.admin")) { sender.sendMessage("No permission."); return true; }
                if (args.length < 2) { sender.sendMessage("Usage: /coins giveall <amount>"); return true; }
                long amount = parseLong(args[1]);
                if (amount <= 0) { sender.sendMessage("Invalid amount."); return true; }
                for (Player online : Bukkit.getOnlinePlayers()) {
                    coins.addCoins(online.getUniqueId(), amount);
                    online.sendMessage("You received " + amount + " Oskar coins.");
                }
                sender.sendMessage("Gave " + amount + " coins to all online players.");
            }
            case "take" -> {
                if (!sender.hasPermission("coins.admin")) { sender.sendMessage("No permission."); return true; }
                if (args.length < 3) { sender.sendMessage("Usage: /coins take <player> <amount>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage("Player not found."); return true; }
                long amount = parseLong(args[2]);
                if (amount <= 0) { sender.sendMessage("Invalid amount."); return true; }
                long bal = coins.getCoins(target.getUniqueId());
                long newBal = Math.max(0, bal - amount);
                coins.setCoins(target.getUniqueId(), newBal);
                sender.sendMessage("Took " + (bal - newBal) + " coins from " + target.getName() + ".");
            }
            default -> sender.sendMessage("Usage: /coins [give|giveall|take]");
        }
        return true;
    }

    private long parseLong(String s) {
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return -1; }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.addAll(List.of("give", "giveall", "take"));
            return out.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take"))) {
            for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
            return out.stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).toList();
        }
        if ((args.length == 3 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take")))
                || (args.length == 2 && args[0].equalsIgnoreCase("giveall"))) {
            return List.of("10", "50", "100", "500", "1000");
        }
        return List.of();
    }
}
