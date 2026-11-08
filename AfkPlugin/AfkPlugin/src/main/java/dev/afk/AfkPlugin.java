package dev.afk;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class AfkPlugin extends JavaPlugin {

    private AfkManager afkManager;
    private RoomManager roomManager;
    private CoinManager coinManager;
    private RankManager rankManager;
    private ScoreboardManager scoreboardManager;
    private WandListener wandListener;
    private NamespacedKey shopKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        shopKey = new NamespacedKey(this, "shop-item");

        roomManager = new RoomManager(this);
        afkManager = new AfkManager(this, roomManager);
        coinManager = new CoinManager(this);
        rankManager = new RankManager(this);
        scoreboardManager = new ScoreboardManager(this);
        wandListener = new WandListener(this, roomManager);

        AfkListener afkListener = new AfkListener(this, afkManager);
        AfkBlockListener blockListener = new AfkBlockListener(this, roomManager, afkManager, coinManager);
        RoomProtectionListener protectionListener = new RoomProtectionListener(this, roomManager);
        ShopManager shopManager = new ShopManager(this, coinManager, rankManager);

        getServer().getPluginManager().registerEvents(afkListener, this);
        getServer().getPluginManager().registerEvents(blockListener, this);
        getServer().getPluginManager().registerEvents(protectionListener, this);
        getServer().getPluginManager().registerEvents(wandListener, this);
        getServer().getPluginManager().registerEvents(shopManager, this);
        getServer().getPluginManager().registerEvents(scoreboardManager, this);

        AfkCommand afkCommand = new AfkCommand(this, afkManager, roomManager, wandListener);
        getCommand("afk").setExecutor(afkCommand);
        getCommand("afk").setTabCompleter(afkCommand);

        CoinsCommand coinsCommand = new CoinsCommand(coinManager);
        getCommand("coins").setExecutor(coinsCommand);
        getCommand("coins").setTabCompleter(coinsCommand);

        getCommand("coinshop").setExecutor((sender, cmd, label, args) -> {
            if (sender instanceof Player p) {
                if (!p.hasPermission("coins.shop")) {
                    p.sendMessage("No permission.");
                    return true;
                }
                shopManager.open(p);
            } else {
                sender.sendMessage("Players only.");
            }
            return true;
        });

        getCommand("baltop").setExecutor(new BaltopCommand(coinManager));

        afkManager.start();
        getServer().getScheduler().runTaskTimer(this, coinManager::tick, 20L, 20L);
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player p : getServer().getOnlinePlayers()) scoreboardManager.update(p);
        }, 100L, 100L);
    }

    @Override
    public void onDisable() {
        if (afkManager != null) afkManager.stop();
        if (coinManager != null) coinManager.save();
    }

    public AfkManager getAfkManager() { return afkManager; }
    public RoomManager getRoomManager() { return roomManager; }
    public CoinManager getCoinManager() { return coinManager; }
    public RankManager getRankManager() { return rankManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public NamespacedKey getShopKey() { return shopKey; }
}
