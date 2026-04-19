package me.viethoang.meowmanhunt.managers;

import me.viethoang.meowmanhunt.MeowManhunt;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final MeowManhunt plugin;

    public ConfigManager(MeowManhunt plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration config() {
        return plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
    }

    public String getLanguage() {
        return config().getString("language", "en");
    }

    public String getPrefix() {
        return config().getString("prefix", "&5&l[&d🐱 MeowManhunt&5&l]&r ");
    }

    public int getHunterLives() {
        return Math.max(1, Math.min(10, config().getInt("game.hunter-lives", 3)));
    }

    public void setHunterLives(int lives) {
        config().set("game.hunter-lives", lives);
        plugin.saveConfig();
    }

    public int getCountdownTime() {
        return Math.max(3, config().getInt("game.countdown-time", 10));
    }

    public void setCountdownTime(int seconds) {
        config().set("game.countdown-time", seconds);
        plugin.saveConfig();
    }

    public int getCompassUpdateInterval() {
        return config().getInt("game.compass-update-interval", 20);
    }

    public boolean isGlowEffect() {
        return config().getBoolean("game.glow-effect", true);
    }

    public void setGlowEffect(boolean glow) {
        config().set("game.glow-effect", glow);
        plugin.saveConfig();
    }

    public boolean isAllowHunterRespawn() {
        return config().getBoolean("game.allow-hunter-respawn", true);
    }

    public boolean isAutoGiveCompass() {
        return config().getBoolean("game.auto-give-compass", true);
    }

    public int getHeadStart() {
        return config().getInt("game.head-start", 30);
    }

    public int getTimerAnnounceInterval() {
        return config().getInt("game.timer-announce-interval", 300);
    }

    public int getAutoResetDelay() {
        return config().getInt("game.auto-reset-delay", 15);
    }

    public boolean isBossBarEnabled() {
        return config().getBoolean("bossbar.enabled", true);
    }

    public String getBossBarRunningColor() {
        return config().getString("bossbar.running-color", "RED");
    }

    public String getBossBarCountdownColor() {
        return config().getString("bossbar.countdown-color", "YELLOW");
    }

    public String getBossBarStyle() {
        return config().getString("bossbar.style", "SOLID");
    }

    public boolean isScoreboardEnabled() {
        return config().getBoolean("scoreboard.enabled", true);
    }

    public int getScoreboardUpdateInterval() {
        return config().getInt("scoreboard.update-interval", 10);
    }

    public boolean isFireworksEnabled() {
        return config().getBoolean("fireworks.enabled", true);
    }

    public int getFireworksCount() {
        return config().getInt("fireworks.count", 10);
    }

    public boolean isSoundsEnabled() {
        return config().getBoolean("sounds.enabled", true);
    }

    public String getSoundGameStart() {
        return config().getString("sounds.game-start", "ENTITY_ENDER_DRAGON_GROWL");
    }

    public String getSoundGameEnd() {
        return config().getString("sounds.game-end", "UI_TOAST_CHALLENGE_COMPLETE");
    }

    public String getSoundCountdownTick() {
        return config().getString("sounds.countdown-tick", "BLOCK_NOTE_BLOCK_PLING");
    }

    public String getSoundHunterDeath() {
        return config().getString("sounds.hunter-death", "ENTITY_WITHER_HURT");
    }

    public String getSoundSpeedrunnerDeath() {
        return config().getString("sounds.speedrunner-death", "ENTITY_LIGHTNING_BOLT_THUNDER");
    }

    public String getSoundJoin() {
        return config().getString("sounds.join", "ENTITY_PLAYER_LEVELUP");
    }
}
