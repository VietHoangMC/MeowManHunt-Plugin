package me.viethoang.meowmanhunt;

import me.viethoang.meowmanhunt.commands.ManhuntCommand;
import me.viethoang.meowmanhunt.listeners.CompassListener;
import me.viethoang.meowmanhunt.listeners.GameListener;
import me.viethoang.meowmanhunt.listeners.GuiListener;
import me.viethoang.meowmanhunt.managers.ConfigManager;
import me.viethoang.meowmanhunt.managers.GameManager;
import me.viethoang.meowmanhunt.managers.LanguageManager;
import me.viethoang.meowmanhunt.managers.ScoreboardManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MeowManhunt extends JavaPlugin {

    private static MeowManhunt instance;

    private ConfigManager configManager;
    private LanguageManager languageManager;
    private GameManager gameManager;
    private ScoreboardManager scoreboardManager;

    @Override
    public void onEnable() {
        instance = this;

        // Save default configs
        saveDefaultConfig();

        // Initialize managers in order
        configManager = new ConfigManager(this);
        languageManager = new LanguageManager(this);
        gameManager = new GameManager(this);
        scoreboardManager = new ScoreboardManager(this);

        // Register command
        ManhuntCommand manhuntCmd = new ManhuntCommand(this);
        if (getCommand("manhunt") != null) {
            getCommand("manhunt").setExecutor(manhuntCmd);
            getCommand("manhunt").setTabCompleter(manhuntCmd);
        }

        // Register listeners
        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        getServer().getPluginManager().registerEvents(new CompassListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);

        getLogger().info("  ");
        getLogger().info("  §d🐱 MeowManhunt §av1.0.0 §aenabled!");
        getLogger().info("  §7Author: §eViệt Hoàng");
        getLogger().info("  §7Website: §bhttps://viethoangmc.page.gd");
        getLogger().info("  ");
    }

    @Override
    public void onDisable() {
        if (gameManager != null && gameManager.getGameState() == GameState.RUNNING) {
            gameManager.forceStop();
        }
        getLogger().info("§d🐱 MeowManhunt §cdisabled!");
    }

    public static MeowManhunt getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
}
