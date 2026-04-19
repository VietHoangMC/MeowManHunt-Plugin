package me.viethoang.meowmanhunt.listeners;

import me.viethoang.meowmanhunt.MeowManhunt;
import me.viethoang.meowmanhunt.gui.MenuManager;
import me.viethoang.meowmanhunt.managers.ConfigManager;
import me.viethoang.meowmanhunt.managers.GameManager;
import me.viethoang.meowmanhunt.managers.LanguageManager;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.persistence.PersistentDataType;

public class GuiListener implements Listener {

    private final MeowManhunt plugin;
    private final MenuManager menuManager;
    private final NamespacedKey menuKey;

    public GuiListener(MeowManhunt plugin) {
        this.plugin = plugin;
        this.menuManager = new MenuManager(plugin);
        this.menuKey = new NamespacedKey(plugin, "open_menu");
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        String menuId = player.getPersistentDataContainer().get(menuKey, PersistentDataType.STRING);
        if (menuId == null) return;

        event.setCancelled(true);

        switch (menuId) {
            case MenuManager.MAIN_MENU_ID -> handleMainMenu(player, event.getSlot());
            case MenuManager.TEAM_MENU_ID -> handleTeamMenu(player, event.getSlot());
            case MenuManager.SETTINGS_MENU_ID -> handleSettingsMenu(player, event.getSlot());
            case MenuManager.LANG_MENU_ID -> handleLangMenu(player, event.getSlot());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        player.getPersistentDataContainer().remove(menuKey);
    }

    // ===================== MAIN MENU =====================

    private void handleMainMenu(Player player, int slot) {
        GameManager gm = plugin.getGameManager();
        LanguageManager lm = plugin.getLanguageManager();

        switch (slot) {
            case 10 -> { // Start Game
                player.closeInventory();
                if (!player.hasPermission("meowmanhunt.admin")) {
                    lm.send(player, "no-permission"); return;
                }
                gm.startGame(player);
            }
            case 12 -> { // Stop Game
                player.closeInventory();
                if (!player.hasPermission("meowmanhunt.admin")) {
                    lm.send(player, "no-permission"); return;
                }
                gm.stopGame(player);
            }
            case 14 -> { // Status - close and show in chat
                player.closeInventory();
                showStatus(player);
            }
            case 16 -> { // Reset
                if (!player.hasPermission("meowmanhunt.admin")) {
                    lm.send(player, "no-permission"); return;
                }
                gm.resetGame();
                player.closeInventory();
                lm.send(player, "reloaded");
            }
            case 28 -> { // Teams
                menuManager.openTeamMenu(player);
            }
            case 30 -> { // Settings
                if (!player.hasPermission("meowmanhunt.admin")) {
                    lm.send(player, "no-permission"); return;
                }
                menuManager.openSettingsMenu(player);
            }
            case 32 -> { // Language
                menuManager.openLanguageMenu(player);
            }
            case 34 -> { // Reload
                if (!player.hasPermission("meowmanhunt.admin")) {
                    lm.send(player, "no-permission"); return;
                }
                plugin.getConfigManager().reload();
                plugin.getLanguageManager().loadMessages();
                player.closeInventory();
                lm.send(player, "reloaded");
            }
            case 40 -> player.closeInventory(); // Close
        }
    }

    // ===================== TEAM MENU =====================

    private void handleTeamMenu(Player player, int slot) {
        GameManager gm = plugin.getGameManager();
        LanguageManager lm = plugin.getLanguageManager();

        switch (slot) {
            case 10 -> { // Join Speedrunner
                if (gm.isRunning() || gm.isCountdown()) {
                    lm.send(player, "game-already-running"); return;
                }
                if (gm.addSpeedrunner(player)) {
                    lm.send(player, "joined-speedrunner");
                    plugin.getServer().getOnlinePlayers().forEach(p -> {
                        if (plugin.getGameManager().isInGame(p.getUniqueId())) {
                            lm.send(p, "player-joined-speedrunner", "player", player.getName());
                        }
                    });
                    menuManager.openTeamMenu(player); // Refresh
                } else {
                    if (gm.isSpeedrunner(player.getUniqueId())) lm.send(player, "already-speedrunner");
                    else lm.send(player, "already-in-team", "player", player.getName());
                }
            }
            case 14 -> { // Join Hunter
                if (gm.isRunning() || gm.isCountdown()) {
                    lm.send(player, "game-already-running"); return;
                }
                if (gm.addHunter(player)) {
                    lm.send(player, "joined-hunter");
                    plugin.getServer().getOnlinePlayers().forEach(p -> {
                        if (plugin.getGameManager().isInGame(p.getUniqueId())) {
                            lm.send(p, "player-joined-hunter", "player", player.getName());
                        }
                    });
                    menuManager.openTeamMenu(player); // Refresh
                } else {
                    if (gm.isHunter(player.getUniqueId())) lm.send(player, "already-hunter");
                    else lm.send(player, "already-in-team", "player", player.getName());
                }
            }
            case 16 -> { // Spectate
                if (gm.addSpectator(player)) {
                    lm.send(player, "joined-spectator");
                    menuManager.openTeamMenu(player);
                } else {
                    lm.send(player, "already-spectator");
                }
            }
            case 40 -> menuManager.openMainMenu(player); // Back
        }
    }

    // ===================== SETTINGS MENU =====================

    private void handleSettingsMenu(Player player, int slot) {
        LanguageManager lm = plugin.getLanguageManager();
        ConfigManager cm = plugin.getConfigManager();

        switch (slot) {
            case 10 -> { // Lives -
                int lives = cm.getHunterLives();
                if (lives <= 1) { lm.send(player, "lives-min"); }
                else {
                    cm.setHunterLives(lives - 1);
                    lm.send(player, "lives-set", "lives", String.valueOf(lives - 1));
                }
                menuManager.openSettingsMenu(player);
            }
            case 12 -> { // Lives +
                int lives = cm.getHunterLives();
                if (lives >= 10) { lm.send(player, "lives-max"); }
                else {
                    cm.setHunterLives(lives + 1);
                    lm.send(player, "lives-set", "lives", String.valueOf(lives + 1));
                }
                menuManager.openSettingsMenu(player);
            }
            case 14 -> { // Countdown -
                int cd = cm.getCountdownTime();
                if (cd > 3) {
                    cm.setCountdownTime(cd - 5);
                    player.sendMessage(ChatColor.GREEN + "Countdown set to " + (cd - 5) + "s");
                }
                menuManager.openSettingsMenu(player);
            }
            case 16 -> { // Countdown +
                int cd = cm.getCountdownTime();
                cm.setCountdownTime(cd + 5);
                player.sendMessage(ChatColor.GREEN + "Countdown set to " + (cd + 5) + "s");
                menuManager.openSettingsMenu(player);
            }
            case 28 -> { // Glow toggle
                boolean glow = !cm.isGlowEffect();
                cm.setGlowEffect(glow);
                player.sendMessage(ChatColor.GREEN + "Glow effect: " + (glow ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
                menuManager.openSettingsMenu(player);
            }
            case 30 -> { // Head start -
                int hs = cm.getHeadStart();
                if (hs > 0) {
                    plugin.getConfig().set("game.head-start", Math.max(0, hs - 10));
                    plugin.saveConfig();
                    player.sendMessage(ChatColor.GREEN + "Head start set to " + Math.max(0, hs - 10) + "s");
                }
                menuManager.openSettingsMenu(player);
            }
            case 32 -> { // Head start +
                int hs = cm.getHeadStart();
                plugin.getConfig().set("game.head-start", hs + 10);
                plugin.saveConfig();
                player.sendMessage(ChatColor.GREEN + "Head start set to " + (hs + 10) + "s");
                menuManager.openSettingsMenu(player);
            }
            case 34 -> { // Respawn toggle
                boolean respawn = !cm.isAllowHunterRespawn();
                plugin.getConfig().set("game.allow-hunter-respawn", respawn);
                plugin.saveConfig();
                player.sendMessage(ChatColor.GREEN + "Hunter respawn: " + (respawn ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
                menuManager.openSettingsMenu(player);
            }
            case 40 -> menuManager.openMainMenu(player); // Back
        }
    }

    // ===================== LANGUAGE MENU =====================

    private void handleLangMenu(Player player, int slot) {
        LanguageManager lm = plugin.getLanguageManager();

        switch (slot) {
            case 11 -> { // English
                lm.setPlayerLanguage(player.getUniqueId(), "en");
                lm.send(player, "language-set");
                menuManager.openLanguageMenu(player); // Refresh
            }
            case 15 -> { // Vietnamese
                lm.setPlayerLanguage(player.getUniqueId(), "vi");
                lm.send(player, "language-set");
                menuManager.openLanguageMenu(player); // Refresh
            }
            case 22 -> menuManager.openMainMenu(player); // Back
        }
    }

    // ===================== STATUS HELPER =====================

    private void showStatus(Player player) {
        LanguageManager lm = plugin.getLanguageManager();
        GameManager gm = plugin.getGameManager();

        player.sendMessage(lm.getMessage("status-header", player));

        String stateKey = switch (gm.getGameState()) {
            case WAITING -> "state-waiting";
            case COUNTDOWN -> "state-countdown";
            case RUNNING -> "state-running";
            case ENDED -> "state-ended";
        };
        player.sendMessage(lm.getMessage("status-state", player, "state", lm.getMessage(stateKey, player)));

        // Runners
        StringBuilder runners = new StringBuilder();
        for (java.util.UUID uuid : gm.getSpeedrunners()) {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (runners.length() > 0) runners.append(", ");
            runners.append(p != null ? p.getName() : "?");
        }
        player.sendMessage(lm.getMessage("status-runners", player,
                "count", String.valueOf(gm.getSpeedrunners().size()),
                "list", runners.length() > 0 ? runners.toString() : "-"));

        // Hunters
        StringBuilder hunters = new StringBuilder();
        for (java.util.UUID uuid : gm.getHunters()) {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (hunters.length() > 0) hunters.append(", ");
            hunters.append(p != null ? p.getName() : "?");
        }
        player.sendMessage(lm.getMessage("status-hunters", player,
                "count", String.valueOf(gm.getHunters().size()),
                "list", hunters.length() > 0 ? hunters.toString() : "-"));

        // Spectators
        player.sendMessage(lm.getMessage("status-spectators", player,
                "count", String.valueOf(gm.getSpectators().size()),
                "list", "-"));

        player.sendMessage(lm.getMessage("status-lives", player,
                "lives", String.valueOf(plugin.getConfigManager().getHunterLives())));

        if (gm.isRunning()) {
            long elapsed = (System.currentTimeMillis() - gm.getGameStartTime()) / 1000;
            player.sendMessage(lm.getMessage("status-time", player,
                    "time", GameManager.formatTime(elapsed)));
        }

        player.sendMessage(lm.getMessage("status-footer", player));
    }
}
