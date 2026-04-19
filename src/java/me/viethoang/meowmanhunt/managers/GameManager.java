package me.viethoang.meowmanhunt.managers;

import me.viethoang.meowmanhunt.GameState;
import me.viethoang.meowmanhunt.MeowManhunt;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class GameManager {

    private final MeowManhunt plugin;
    private GameState gameState = GameState.WAITING;

    private final Set<UUID> speedrunners = new HashSet<>();
    private final Set<UUID> hunters = new HashSet<>();
    private final Set<UUID> spectators = new HashSet<>();
    private final Map<UUID, Integer> hunterLives = new HashMap<>();

    private BukkitTask compassTask;
    private BukkitTask countdownTask;
    private BukkitTask timerTask;
    private BukkitTask headStartTask;
    private BukkitTask scoreboardTask;

    private BossBar bossBar;
    private long gameStartTime;
    private int headStartRemaining;
    private boolean headStartActive = false;

    public GameManager(MeowManhunt plugin) {
        this.plugin = plugin;
    }

    // ===================== TEAM MANAGEMENT =====================

    public boolean addSpeedrunner(Player player) {
        UUID uuid = player.getUniqueId();
        if (speedrunners.contains(uuid) || hunters.contains(uuid) || spectators.contains(uuid)) {
            return false;
        }
        speedrunners.add(uuid);
        return true;
    }

    public boolean addHunter(Player player) {
        UUID uuid = player.getUniqueId();
        if (speedrunners.contains(uuid) || hunters.contains(uuid) || spectators.contains(uuid)) {
            return false;
        }
        hunters.add(uuid);
        return true;
    }

    public boolean addSpectator(Player player) {
        UUID uuid = player.getUniqueId();
        if (speedrunners.contains(uuid) || hunters.contains(uuid) || spectators.contains(uuid)) {
            return false;
        }
        spectators.add(uuid);
        return true;
    }

    public void forceAddHunter(Player player) {
        hunters.add(player.getUniqueId());
        spectators.remove(player.getUniqueId());
    }

    public void removePlayer(UUID uuid) {
        speedrunners.remove(uuid);
        hunters.remove(uuid);
        spectators.remove(uuid);
        hunterLives.remove(uuid);
    }

    public boolean isSpeedrunner(UUID uuid) { return speedrunners.contains(uuid); }
    public boolean isHunter(UUID uuid) { return hunters.contains(uuid); }
    public boolean isSpectator(UUID uuid) { return spectators.contains(uuid); }
    public boolean isInGame(UUID uuid) {
        return speedrunners.contains(uuid) || hunters.contains(uuid) || spectators.contains(uuid);
    }

    public Set<UUID> getSpeedrunners() { return Collections.unmodifiableSet(speedrunners); }
    public Set<UUID> getHunters() { return Collections.unmodifiableSet(hunters); }
    public Set<UUID> getSpectators() { return Collections.unmodifiableSet(spectators); }
    public Map<UUID, Integer> getHunterLives() { return Collections.unmodifiableMap(hunterLives); }

    public int getHunterLivesFor(UUID uuid) {
        return hunterLives.getOrDefault(uuid, plugin.getConfigManager().getHunterLives());
    }

    // ===================== GAME FLOW =====================

    public boolean startGame(Player initiator) {
        if (gameState != GameState.WAITING) return false;
        if (speedrunners.isEmpty() || hunters.isEmpty()) {
            if (initiator != null) {
                plugin.getLanguageManager().send(initiator, "need-players");
            }
            return false;
        }

        gameState = GameState.COUNTDOWN;
        int countdownTime = plugin.getConfigManager().getCountdownTime();

        // Init boss bar
        if (plugin.getConfigManager().isBossBarEnabled()) {
            String style = plugin.getConfigManager().getBossBarStyle();
            String color = plugin.getConfigManager().getBossBarCountdownColor();
            bossBar = Bukkit.createBossBar(
                    "🐱 Game Starting...",
                    parseBarColor(color),
                    parseBarStyle(style)
            );
            getAllGamePlayers().stream()
                    .map(Bukkit::getPlayer)
                    .filter(Objects::nonNull)
                    .forEach(p -> bossBar.addPlayer(p));
        }

        // Countdown task
        countdownTask = new BukkitRunnable() {
            int remaining = countdownTime;

            @Override
            public void run() {
                if (remaining <= 0) {
                    cancel();
                    beginGame(initiator);
                    return;
                }

                String msg;
                if (remaining <= 3) {
                    msg = "countdown-" + remaining;
                } else {
                    msg = "countdown-start";
                }

                // Broadcast countdown
                for (UUID uuid : getAllGamePlayers()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null) continue;
                    if (remaining <= 3) {
                        plugin.getLanguageManager().send(p, msg);
                        // Play tick sound
                        if (plugin.getConfigManager().isSoundsEnabled()) {
                            playSound(p, plugin.getConfigManager().getSoundCountdownTick());
                        }
                    } else if (remaining == countdownTime || remaining % 5 == 0) {
                        plugin.getLanguageManager().send(p, msg, "count", String.valueOf(remaining));
                    }
                    // Titles for last 3
                    if (remaining <= 3) {
                        p.sendTitle(
                                ChatColor.RED + "" + remaining,
                                "",
                                2, 16, 2
                        );
                    }
                }

                // Update boss bar
                if (bossBar != null) {
                    String txt = plugin.getLanguageManager().getMessageByLang("bossbar-countdown",
                            "time", String.valueOf(remaining));
                    bossBar.setTitle(txt);
                    bossBar.setProgress((double) remaining / countdownTime);
                }

                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        return true;
    }

    private void beginGame(Player initiator) {
        gameState = GameState.RUNNING;
        gameStartTime = System.currentTimeMillis();
        int defaultLives = plugin.getConfigManager().getHunterLives();

        // Set hunter lives
        for (UUID uuid : hunters) {
            hunterLives.put(uuid, defaultLives);
        }

        // Build player name lists for broadcast
        StringBuilder runnerNames = new StringBuilder();
        for (UUID uuid : speedrunners) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                if (runnerNames.length() > 0) runnerNames.append(", ");
                runnerNames.append(p.getName());
            }
        }
        StringBuilder hunterNames = new StringBuilder();
        for (UUID uuid : hunters) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                if (hunterNames.length() > 0) hunterNames.append(", ");
                hunterNames.append(p.getName());
            }
        }

        // Broadcast start message
        broadcastRaw("game-start-broadcast",
                "runners", runnerNames.toString(),
                "hunters", hunterNames.toString(),
                "lives", String.valueOf(defaultLives));

        // Give compass & apply glow
        for (UUID uuid : hunters) {
            Player hunter = Bukkit.getPlayer(uuid);
            if (hunter == null) continue;
            if (plugin.getConfigManager().isAutoGiveCompass()) {
                hunter.getInventory().addItem(new ItemStack(Material.COMPASS, 1));
                plugin.getLanguageManager().send(hunter, "give-compass");
            }
        }
        if (plugin.getConfigManager().isGlowEffect()) {
            for (UUID uuid : speedrunners) {
                Player runner = Bukkit.getPlayer(uuid);
                if (runner != null) runner.setGlowing(true);
            }
        }

        // Show title to all
        String goMsg = ChatColor.GREEN + "" + ChatColor.BOLD + "GO!";
        for (UUID uuid : getAllGamePlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            p.sendTitle(goMsg, ChatColor.YELLOW + "The hunt begins!", 5, 30, 10);
            if (plugin.getConfigManager().isSoundsEnabled()) {
                playSound(p, plugin.getConfigManager().getSoundGameStart());
            }
        }

        // Head start
        int headStartSecs = plugin.getConfigManager().getHeadStart();
        if (headStartSecs > 0) {
            headStartActive = true;
            headStartRemaining = headStartSecs;
            // Freeze hunters
            for (UUID uuid : hunters) {
                Player hunter = Bukkit.getPlayer(uuid);
                if (hunter != null) {
                    hunter.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.SLOWNESS, headStartSecs * 20, 6, false, false));
                    hunter.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.JUMP_BOOST, headStartSecs * 20, 128, false, false));
                }
            }
            headStartTask = new BukkitRunnable() {
                int remaining = headStartSecs;

                @Override
                public void run() {
                    if (remaining <= 0) {
                        headStartActive = false;
                        cancel();
                        for (UUID uuid : hunters) {
                            Player hunter = Bukkit.getPlayer(uuid);
                            if (hunter != null) {
                                hunter.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS);
                                hunter.removePotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST);
                                plugin.getLanguageManager().send(hunter, "head-start-end");
                            }
                        }
                        broadcastKey("head-start-end");
                        return;
                    }
                    if (remaining == headStartSecs || remaining % 10 == 0 || remaining <= 5) {
                        broadcastKey("head-start-warning", "time", String.valueOf(remaining));
                    }
                    if (bossBar != null) {
                        String txt = plugin.getLanguageManager().getMessageByLang("bossbar-headstart",
                                "time", String.valueOf(remaining));
                        bossBar.setTitle(txt);
                        bossBar.setProgress((double) remaining / headStartSecs);
                    }
                    remaining--;
                }
            }.runTaskTimer(plugin, 20L, 20L);
        }

        // Update boss bar to running color
        if (bossBar != null) {
            bossBar.setColor(parseBarColor(plugin.getConfigManager().getBossBarRunningColor()));
            bossBar.setProgress(1.0);
        }

        // Compass update task
        compassTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (gameState != GameState.RUNNING) {
                    cancel();
                    return;
                }
                updateAllCompasses();
            }
        }.runTaskTimer(plugin, 0L, plugin.getConfigManager().getCompassUpdateInterval());

        // Timer / boss bar update task
        timerTask = new BukkitRunnable() {
            int announceCounter = 0;

            @Override
            public void run() {
                if (gameState != GameState.RUNNING) {
                    cancel();
                    return;
                }
                String timeStr = formatTime((System.currentTimeMillis() - gameStartTime) / 1000);

                // Update bossbar
                if (bossBar != null) {
                    String txt = plugin.getLanguageManager().getMessageByLang("bossbar-running",
                            "time", timeStr,
                            "runners", String.valueOf(speedrunners.size()),
                            "hunters", String.valueOf(hunters.size()));
                    bossBar.setTitle(txt);
                }

                // Timer announce
                int interval = plugin.getConfigManager().getTimerAnnounceInterval();
                if (interval > 0) {
                    announceCounter++;
                    if (announceCounter >= interval) {
                        announceCounter = 0;
                        broadcastKey("status-time", "time", timeStr);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);

        // Scoreboard update task
        if (plugin.getConfigManager().isScoreboardEnabled()) {
            scoreboardTask = new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getScoreboardManager().updateAll();
                }
            }.runTaskTimer(plugin, 0L, plugin.getConfigManager().getScoreboardUpdateInterval());
        }

        // Notify initiator
        if (initiator != null) {
            plugin.getLanguageManager().send(initiator, "game-started", "player", initiator.getName());
        }
    }

    public void stopGame(Player initiator) {
        if (gameState == GameState.WAITING) return;
        if (initiator != null) {
            broadcastKey("game-stopped", "player", initiator.getName());
        } else {
            broadcastKey("game-stopped-no-one");
        }
        resetGame();
    }

    public void forceStop() {
        cancelTasks();
        cleanupBossBar();
        clearPlayers();
        gameState = GameState.WAITING;
    }

    public void handleSpeedrunnerDeath(Player player) {
        speedrunners.remove(player.getUniqueId());
        spectators.add(player.getUniqueId());
        if (plugin.getConfigManager().isGlowEffect()) {
            player.setGlowing(false);
        }
        broadcastKey("speedrunner-died", "player", player.getName());

        // Check win
        if (speedrunners.isEmpty()) {
            triggerHuntersWin();
        }
    }

    public void handleHunterDeath(Player player) {
        UUID uuid = player.getUniqueId();
        int lives = hunterLives.getOrDefault(uuid, 1) - 1;

        if (lives <= 0) {
            // Eliminated
            hunterLives.remove(uuid);
            hunters.remove(uuid);
            spectators.add(uuid);
            broadcastKey("hunter-eliminated", "player", player.getName());

            // Set to spectator mode after respawn
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    p.setGameMode(GameMode.SPECTATOR);
                }
            }, 5L);

            checkAllHuntersDead();
        } else {
            hunterLives.put(uuid, lives);
            broadcastKey("hunter-died", "player", player.getName(), "lives", String.valueOf(lives));
        }
    }

    private void checkAllHuntersDead() {
        if (hunters.isEmpty()) {
            // Check if speedrunner won differently (dragon)
            // Here, all hunters dead doesn't mean speedrunner wins
            // Just broadcast (optional game logic: speedrunner wins if no hunters)
            // We'll keep hunters win on speedrunner death, speedrunner wins on dragon death
        }
    }

    public void handleDragonDeath() {
        if (gameState != GameState.RUNNING) return;
        triggerSpeedrunnersWin();
    }

    private void triggerHuntersWin() {
        gameState = GameState.ENDED;
        cancelTasks();
        if (bossBar != null) {
            bossBar.setTitle(plugin.getLanguageManager().getMessage("bossbar-ended"));
            bossBar.setColor(BarColor.RED);
        }

        // Broadcast win message
        for (UUID uuid : getAllGamePlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            String msg = plugin.getLanguageManager().getMessage("hunters-win", p);
            for (String line : msg.split("\n")) {
                p.sendMessage(LanguageManager.colorize(line));
            }
            p.sendTitle(
                    ChatColor.RED + "" + ChatColor.BOLD + "⚔ HUNTERS WIN! ⚔",
                    ChatColor.YELLOW + "The Speedrunner has fallen!",
                    10, 60, 20
            );
            if (plugin.getConfigManager().isSoundsEnabled()) {
                playSound(p, plugin.getConfigManager().getSoundGameEnd());
            }
        }
        // Fireworks for hunters
        if (plugin.getConfigManager().isFireworksEnabled()) {
            spawnWinFireworks(hunters, ChatColor.RED);
        }
        scheduleAutoReset();
    }

    private void triggerSpeedrunnersWin() {
        gameState = GameState.ENDED;
        cancelTasks();
        if (bossBar != null) {
            bossBar.setTitle(plugin.getLanguageManager().getMessage("bossbar-ended"));
            bossBar.setColor(BarColor.GREEN);
        }

        for (UUID uuid : getAllGamePlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            String msg = plugin.getLanguageManager().getMessage("speedrunners-win", p);
            for (String line : msg.split("\n")) {
                p.sendMessage(LanguageManager.colorize(line));
            }
            p.sendTitle(
                    ChatColor.GREEN + "" + ChatColor.BOLD + "🏆 SPEEDRUNNERS WIN!",
                    ChatColor.YELLOW + "The Ender Dragon has been slain!",
                    10, 60, 20
            );
            if (plugin.getConfigManager().isSoundsEnabled()) {
                playSound(p, plugin.getConfigManager().getSoundGameEnd());
            }
        }
        if (plugin.getConfigManager().isFireworksEnabled()) {
            spawnWinFireworks(speedrunners, ChatColor.GREEN);
        }
        scheduleAutoReset();
    }

    private void scheduleAutoReset() {
        int delay = plugin.getConfigManager().getAutoResetDelay();
        if (delay <= 0) return;

        broadcastKey("auto-reset", "time", String.valueOf(delay));
        plugin.getServer().getScheduler().runTaskLater(plugin, this::resetGame, delay * 20L);
    }

    public void resetGame() {
        cancelTasks();
        cleanupBossBar();
        // Remove scoreboards
        plugin.getScoreboardManager().removeAll();
        // Reset glow for runners
        for (UUID uuid : speedrunners) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.setGlowing(false);
        }
        clearPlayers();
        gameState = GameState.WAITING;
    }

    // ===================== COMPASS =====================

    public void updateAllCompasses() {
        for (UUID hunterUUID : hunters) {
            Player hunter = Bukkit.getPlayer(hunterUUID);
            if (hunter == null) continue;
            updateCompassForHunter(hunter);
        }
    }

    public void updateCompassForHunter(Player hunter) {
        Player closest = getClosestSpeedrunner(hunter);
        if (closest == null) {
            plugin.getLanguageManager().send(hunter, "compass-no-target");
            return;
        }
        hunter.setCompassTarget(closest.getLocation());
        int dist = (int) hunter.getLocation().distance(closest.getLocation());
        plugin.getLanguageManager().send(hunter, "compass-update",
                "target", closest.getName(),
                "distance", String.valueOf(dist));
    }

    private Player getClosestSpeedrunner(Player hunter) {
        Player closest = null;
        double closestDist = Double.MAX_VALUE;
        for (UUID uuid : speedrunners) {
            Player runner = Bukkit.getPlayer(uuid);
            if (runner == null || !runner.getWorld().equals(hunter.getWorld())) continue;
            double dist = runner.getLocation().distanceSquared(hunter.getLocation());
            if (dist < closestDist) {
                closestDist = dist;
                closest = runner;
            }
        }
        return closest;
    }

    // ===================== BROADCAST HELPERS =====================

    public void broadcastKey(String key, String... replacements) {
        for (UUID uuid : getAllGamePlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                plugin.getLanguageManager().send(p, key, replacements);
            }
        }
    }

    public void broadcastRaw(String key, String... replacements) {
        for (UUID uuid : getAllGamePlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            String msg = plugin.getLanguageManager().getMessage(key, p, replacements);
            for (String line : msg.split("\n")) {
                p.sendMessage(LanguageManager.colorize(line));
            }
        }
    }

    // ===================== UTILITY =====================

    public Set<UUID> getAllGamePlayers() {
        Set<UUID> all = new HashSet<>();
        all.addAll(speedrunners);
        all.addAll(hunters);
        all.addAll(spectators);
        return all;
    }

    private void cancelTasks() {
        if (compassTask != null) { compassTask.cancel(); compassTask = null; }
        if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
        if (timerTask != null) { timerTask.cancel(); timerTask = null; }
        if (headStartTask != null) { headStartTask.cancel(); headStartTask = null; }
        if (scoreboardTask != null) { scoreboardTask.cancel(); scoreboardTask = null; }
        headStartActive = false;
    }

    private void cleanupBossBar() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
    }

    private void clearPlayers() {
        speedrunners.clear();
        hunters.clear();
        spectators.clear();
        hunterLives.clear();
    }

    private void spawnWinFireworks(Set<UUID> winners, ChatColor color) {
        int count = plugin.getConfigManager().getFireworksCount();
        for (UUID uuid : winners) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            for (int i = 0; i < count; i++) {
                final int delay = i * 4;
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (!p.isOnline()) return;
                    Firework fw = p.getWorld().spawn(p.getLocation().add(
                            (Math.random() - 0.5) * 3, 0, (Math.random() - 0.5) * 3),
                            Firework.class);
                    FireworkMeta meta = fw.getFireworkMeta();
                    FireworkEffect.Builder builder = FireworkEffect.builder()
                            .with(FireworkEffect.Type.BALL_LARGE)
                            .withColor(toFireworkColor(color))
                            .withFade(Color.WHITE)
                            .withTrail()
                            .withFlicker();
                    meta.addEffect(builder.build());
                    meta.setPower(1);
                    fw.setFireworkMeta(meta);
                }, delay);
            }
        }
    }

    private Color toFireworkColor(ChatColor cc) {
        if (cc == ChatColor.RED) return Color.RED;
        if (cc == ChatColor.GREEN) return Color.LIME;
        if (cc == ChatColor.BLUE) return Color.BLUE;
        return Color.WHITE;
    }

    private void playSound(Player player, String soundName) {
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException ignored) {}
    }

    public static String formatTime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    private BarColor parseBarColor(String name) {
        try { return BarColor.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) { return BarColor.RED; }
    }

    private BarStyle parseBarStyle(String name) {
        try { return BarStyle.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) { return BarStyle.SOLID; }
    }

    // ===================== GETTERS =====================

    public GameState getGameState() { return gameState; }
    public boolean isWaiting() { return gameState == GameState.WAITING; }
    public boolean isRunning() { return gameState == GameState.RUNNING; }
    public boolean isCountdown() { return gameState == GameState.COUNTDOWN; }
    public BossBar getBossBar() { return bossBar; }
    public long getGameStartTime() { return gameStartTime; }
    public boolean isHeadStartActive() { return headStartActive; }
}
