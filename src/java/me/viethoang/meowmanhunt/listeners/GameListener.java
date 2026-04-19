package me.viethoang.meowmanhunt.listeners;

import me.viethoang.meowmanhunt.MeowManhunt;
import me.viethoang.meowmanhunt.managers.GameManager;
import me.viethoang.meowmanhunt.managers.LanguageManager;
import org.bukkit.GameMode;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

public class GameListener implements Listener {

    private final MeowManhunt plugin;

    public GameListener(MeowManhunt plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        GameManager gm = plugin.getGameManager();
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();

        if (!gm.isRunning()) return;

        if (gm.isSpeedrunner(uuid)) {
            // Speedrunner died → hunters might win
            event.setDeathMessage(null); // Suppress default message
            gm.handleSpeedrunnerDeath(player);
            // Play sound to all
            if (plugin.getConfigManager().isSoundsEnabled()) {
                playGlobalSound(plugin.getConfigManager().getSoundSpeedrunnerDeath());
            }
        } else if (gm.isHunter(uuid)) {
            // Hunter died → lose a life
            event.setDeathMessage(null);
            gm.handleHunterDeath(player);
            if (plugin.getConfigManager().isSoundsEnabled()) {
                playGlobalSound(plugin.getConfigManager().getSoundHunterDeath());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        GameManager gm = plugin.getGameManager();
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!gm.isRunning()) return;

        // If player is now a spectator (eliminated or speedrunner died)
        if (gm.isSpectator(uuid)) {
            // Delay setting spectator mode to allow respawn to finish
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.setGameMode(GameMode.SPECTATOR);
                    plugin.getLanguageManager().send(player, "hunter-eliminated");
                }
            }, 5L);
        } else if (gm.isHunter(uuid)) {
            // Hunter respawned - give compass back
            if (plugin.getConfigManager().isAutoGiveCompass()) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.COMPASS));
                        int lives = gm.getHunterLivesFor(uuid);
                        plugin.getLanguageManager().send(player, "lives-remaining", "lives", String.valueOf(lives));
                    }
                }, 10L);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Detect Ender Dragon death
        if (event.getEntity() instanceof EnderDragon) {
            GameManager gm = plugin.getGameManager();
            if (gm.isRunning()) {
                gm.handleDragonDeath();
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        GameManager gm = plugin.getGameManager();

        if (!gm.isInGame(uuid)) return;

        gm.broadcastKey("player-left", "player", player.getName());
        gm.removePlayer(uuid);

        // Update scoreboard for remaining players
        plugin.getScoreboardManager().updateAll();

        // Check if game can continue
        if (gm.isRunning()) {
            if (gm.getSpeedrunners().isEmpty()) {
                // Speedrunner left - hunters win
                gm.broadcastKey("hunters-win");
                gm.resetGame();
            } else if (gm.getHunters().isEmpty()) {
                // All hunters left - no one to hunt
                gm.broadcastKey("speedrunners-win");
                gm.resetGame();
            }
        } else if (gm.isCountdown()) {
            // Check minimum players
            if (gm.getSpeedrunners().isEmpty() || gm.getHunters().isEmpty()) {
                gm.broadcastKey("need-players");
                gm.stopGame(null);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        GameManager gm = plugin.getGameManager();

        // Add boss bar if game running
        if (gm.isRunning() || gm.isCountdown()) {
            if (gm.getBossBar() != null && gm.isInGame(uuid)) {
                gm.getBossBar().addPlayer(player);
            }
        }
    }

    private void playGlobalSound(String soundName) {
        try {
            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName);
            plugin.getGameManager().getAllGamePlayers().forEach(uuid -> {
                Player p = org.bukkit.Bukkit.getPlayer(uuid);
                if (p != null) {
                    p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
                }
            });
        } catch (IllegalArgumentException ignored) {}
    }
}
