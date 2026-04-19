package me.viethoang.meowmanhunt.gui;

import me.viethoang.meowmanhunt.MeowManhunt;
import me.viethoang.meowmanhunt.managers.GameManager;
import me.viethoang.meowmanhunt.managers.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MenuManager {

    // Menu ID strings (used to identify open menus)
    public static final String MAIN_MENU_ID = "MEOWMH_MAIN";
    public static final String TEAM_MENU_ID = "MEOWMH_TEAM";
    public static final String SETTINGS_MENU_ID = "MEOWMH_SETTINGS";
    public static final String LANG_MENU_ID = "MEOWMH_LANG";

    private final MeowManhunt plugin;

    public MenuManager(MeowManhunt plugin) {
        this.plugin = plugin;
    }

    // ===================== OPEN MENUS =====================

    public void openMainMenu(Player player) {
        LanguageManager lm = plugin.getLanguageManager();
        GameManager gm = plugin.getGameManager();
        String title = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "🐱 MeowManhunt" +
                ChatColor.RESET + ChatColor.GRAY + " - Main Menu";

        Inventory inv = Bukkit.createInventory(null, 45, title);

        // Fill borders with glass
        fillBorders(inv, 45, Material.GRAY_STAINED_GLASS_PANE, " ");

        // --- Row 1 (slots 0-8): top border already filled ---

        // Slot 4: Header / Plugin Info
        inv.setItem(4, makeItem(Material.NETHER_STAR, 1,
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "🐱 MeowManhunt",
                Arrays.asList(
                        ChatColor.GRAY + "Version: " + ChatColor.WHITE + "1.0.0",
                        ChatColor.GRAY + "Author: " + ChatColor.YELLOW + "Việt Hoàng",
                        ChatColor.GRAY + "Website: " + ChatColor.AQUA + "viethoangmc.page.gd",
                        "",
                        ChatColor.GRAY + "State: " + ChatColor.WHITE + getStateName(lm, player)
                )));

        // Slot 10: Start Game
        boolean canStart = gm.isWaiting();
        inv.setItem(10, makeItem(
                canStart ? Material.LIME_WOOL : Material.GRAY_WOOL, 1,
                (canStart ? ChatColor.GREEN : ChatColor.GRAY) + "" + ChatColor.BOLD + "▶ Start Game",
                Arrays.asList(
                        ChatColor.GRAY + "Start the Manhunt game.",
                        "",
                        canStart ? ChatColor.GREEN + "Click to start!" : ChatColor.RED + "Game already running!"
                )));

        // Slot 12: Stop Game
        boolean canStop = !gm.isWaiting();
        inv.setItem(12, makeItem(
                canStop ? Material.RED_WOOL : Material.GRAY_WOOL, 1,
                (canStop ? ChatColor.RED : ChatColor.GRAY) + "" + ChatColor.BOLD + "⏹ Stop Game",
                Arrays.asList(
                        ChatColor.GRAY + "Stop the current game.",
                        "",
                        canStop ? ChatColor.RED + "Click to stop!" : ChatColor.GRAY + "No game running."
                )));

        // Slot 14: Status
        inv.setItem(14, makeItem(Material.CLOCK, 1,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "📊 Game Status",
                Arrays.asList(
                        ChatColor.GRAY + "View current game info.",
                        "",
                        ChatColor.YELLOW + "🏃 Runners: " + ChatColor.WHITE + gm.getSpeedrunners().size(),
                        ChatColor.RED + "⚔ Hunters: " + ChatColor.WHITE + gm.getHunters().size(),
                        ChatColor.AQUA + "❤ Lives: " + ChatColor.WHITE + plugin.getConfigManager().getHunterLives()
                )));

        // Slot 16: Reset
        inv.setItem(16, makeItem(Material.TNT, 1,
                ChatColor.DARK_RED + "" + ChatColor.BOLD + "🔄 Reset Game",
                Arrays.asList(
                        ChatColor.GRAY + "Reset all game data.",
                        ChatColor.RED + "Warning: Clears all teams!"
                )));

        // Slot 28: Team Management
        inv.setItem(28, makeItem(Material.PLAYER_HEAD, 1,
                ChatColor.AQUA + "" + ChatColor.BOLD + "👥 Teams",
                Arrays.asList(
                        ChatColor.GRAY + "Manage player teams.",
                        "",
                        ChatColor.YELLOW + "🏃 Runners: " + ChatColor.WHITE + gm.getSpeedrunners().size(),
                        ChatColor.RED + "⚔ Hunters: " + ChatColor.WHITE + gm.getHunters().size()
                )));

        // Slot 30: Settings
        inv.setItem(30, makeItem(Material.COMPARATOR, 1,
                ChatColor.GOLD + "" + ChatColor.BOLD + "⚙ Settings",
                Arrays.asList(
                        ChatColor.GRAY + "Configure game settings.",
                        "",
                        ChatColor.YELLOW + "Lives: " + ChatColor.WHITE + plugin.getConfigManager().getHunterLives(),
                        ChatColor.YELLOW + "Countdown: " + ChatColor.WHITE + plugin.getConfigManager().getCountdownTime() + "s",
                        ChatColor.YELLOW + "Glow: " + (plugin.getConfigManager().isGlowEffect() ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF")
                )));

        // Slot 32: Language
        inv.setItem(32, makeItem(Material.BOOK, 1,
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "🌐 Language",
                Arrays.asList(
                        ChatColor.GRAY + "Current: " + ChatColor.WHITE + getLangDisplay(lm.getPlayerLanguage(player.getUniqueId())),
                        "",
                        ChatColor.LIGHT_PURPLE + "Click to change language!"
                )));

        // Slot 34: Reload Config
        inv.setItem(34, makeItem(Material.COMMAND_BLOCK, 1,
                ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "🔃 Reload Config",
                Arrays.asList(
                        ChatColor.GRAY + "Reload plugin configuration.",
                        ChatColor.YELLOW + "Admin only!"
                )));

        // Slot 40: Close
        inv.setItem(40, makeItem(Material.BARRIER, 1,
                ChatColor.RED + "" + ChatColor.BOLD + "✖ Close",
                Arrays.asList(ChatColor.GRAY + "Close this menu.")));

        // Tag the inventory title for identification
        player.openInventory(inv);
        player.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "open_menu"),
                org.bukkit.persistence.PersistentDataType.STRING,
                MAIN_MENU_ID
        );
    }

    public void openTeamMenu(Player player) {
        GameManager gm = plugin.getGameManager();

        String title = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "🐱 MeowManhunt" +
                ChatColor.RESET + ChatColor.GRAY + " - Teams";

        Inventory inv = Bukkit.createInventory(null, 45, title);
        fillBorders(inv, 45, Material.GRAY_STAINED_GLASS_PANE, " ");

        // Slot 4: Header
        inv.setItem(4, makeItem(Material.NETHER_STAR, 1,
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "👥 Team Selection",
                Arrays.asList(ChatColor.GRAY + "Choose your role in the game!")));

        // Slot 10: Join Speedrunner
        boolean isRunner = gm.isSpeedrunner(player.getUniqueId());
        inv.setItem(10, makeItem(Material.DIAMOND_BOOTS, 1,
                ChatColor.GREEN + "" + ChatColor.BOLD + "🏃 Speedrunner",
                Arrays.asList(
                        ChatColor.GRAY + "Run from hunters!",
                        ChatColor.GRAY + "Kill the Ender Dragon to win.",
                        "",
                        ChatColor.GREEN + "Players: " + ChatColor.WHITE + gm.getSpeedrunners().size(),
                        "",
                        isRunner ? ChatColor.YELLOW + "✔ You are already a Speedrunner!" : ChatColor.GREEN + "Click to join!"
                )));

        // Slot 14: Join Hunter
        boolean isHunter = gm.isHunter(player.getUniqueId());
        inv.setItem(14, makeItem(Material.IRON_SWORD, 1,
                ChatColor.RED + "" + ChatColor.BOLD + "⚔ Hunter",
                Arrays.asList(
                        ChatColor.GRAY + "Hunt down the Speedrunner!",
                        ChatColor.GRAY + "Kill them before they kill the dragon.",
                        "",
                        ChatColor.RED + "Players: " + ChatColor.WHITE + gm.getHunters().size(),
                        ChatColor.RED + "❤ Lives: " + ChatColor.WHITE + plugin.getConfigManager().getHunterLives(),
                        "",
                        isHunter ? ChatColor.YELLOW + "✔ You are already a Hunter!" : ChatColor.RED + "Click to join!"
                )));

        // Slot 16: Spectator
        boolean isSpectator = gm.isSpectator(player.getUniqueId());
        inv.setItem(16, makeItem(Material.ENDER_EYE, 1,
                ChatColor.GRAY + "" + ChatColor.BOLD + "👁 Spectator",
                Arrays.asList(
                        ChatColor.GRAY + "Watch the game without playing.",
                        "",
                        isSpectator ? ChatColor.YELLOW + "✔ You are already a Spectator!" : ChatColor.GRAY + "Click to spectate!"
                )));

        // Show current speedrunner list (slots 28-35)
        inv.setItem(28, makeItem(Material.GREEN_BANNER, 1,
                ChatColor.GREEN + "🏃 Speedrunners",
                buildPlayerList(gm.getSpeedrunners())));

        inv.setItem(30, makeItem(Material.RED_BANNER, 1,
                ChatColor.RED + "⚔ Hunters",
                buildPlayerList(gm.getHunters())));

        inv.setItem(32, makeItem(Material.WHITE_BANNER, 1,
                ChatColor.GRAY + "👁 Spectators",
                buildPlayerList(gm.getSpectators())));

        // Slot 40: Back
        inv.setItem(40, makeItem(Material.ARROW, 1,
                ChatColor.YELLOW + "◀ Back",
                Arrays.asList(ChatColor.GRAY + "Return to main menu.")));

        player.openInventory(inv);
        player.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "open_menu"),
                org.bukkit.persistence.PersistentDataType.STRING,
                TEAM_MENU_ID
        );
    }

    public void openSettingsMenu(Player player) {
        String title = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "🐱 MeowManhunt" +
                ChatColor.RESET + ChatColor.GRAY + " - Settings";

        Inventory inv = Bukkit.createInventory(null, 45, title);
        fillBorders(inv, 45, Material.GRAY_STAINED_GLASS_PANE, " ");

        int lives = plugin.getConfigManager().getHunterLives();
        int countdown = plugin.getConfigManager().getCountdownTime();
        boolean glow = plugin.getConfigManager().isGlowEffect();
        int headStart = plugin.getConfigManager().getHeadStart();

        // Header
        inv.setItem(4, makeItem(Material.COMPARATOR, 1,
                ChatColor.GOLD + "" + ChatColor.BOLD + "⚙ Game Settings",
                Arrays.asList(ChatColor.GRAY + "Adjust game configuration.")));

        // Hunter Lives section (slots 10, 11, 12)
        inv.setItem(10, makeItem(Material.RED_STAINED_GLASS_PANE, 1,
                ChatColor.RED + "◀ Less Lives", Arrays.asList(ChatColor.GRAY + "Decrease hunter lives")));

        inv.setItem(11, makeItem(Material.REDSTONE, lives,
                ChatColor.RED + "❤ Hunter Lives: " + ChatColor.WHITE + lives,
                Arrays.asList(
                        ChatColor.GRAY + "Lives each hunter starts with.",
                        ChatColor.YELLOW + "Current: " + ChatColor.WHITE + lives
                )));

        inv.setItem(12, makeItem(Material.GREEN_STAINED_GLASS_PANE, 1,
                ChatColor.GREEN + "More Lives ▶", Arrays.asList(ChatColor.GRAY + "Increase hunter lives")));

        // Countdown section (slots 14, 15, 16)
        inv.setItem(14, makeItem(Material.RED_STAINED_GLASS_PANE, 1,
                ChatColor.RED + "◀ Less Time", Arrays.asList(ChatColor.GRAY + "Decrease countdown")));

        inv.setItem(15, makeItem(Material.CLOCK, 1,
                ChatColor.YELLOW + "⏱ Countdown: " + ChatColor.WHITE + countdown + "s",
                Arrays.asList(
                        ChatColor.GRAY + "Seconds before game starts.",
                        ChatColor.YELLOW + "Current: " + ChatColor.WHITE + countdown + "s"
                )));

        inv.setItem(16, makeItem(Material.GREEN_STAINED_GLASS_PANE, 1,
                ChatColor.GREEN + "More Time ▶", Arrays.asList(ChatColor.GRAY + "Increase countdown")));

        // Glow Effect (slot 28)
        inv.setItem(28, makeItem(
                glow ? Material.GLOWSTONE : Material.GRAY_CONCRETE, 1,
                (glow ? ChatColor.YELLOW : ChatColor.GRAY) + "✨ Glow Effect: " +
                        (glow ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"),
                Arrays.asList(
                        ChatColor.GRAY + "Speedrunners glow for hunters.",
                        ChatColor.YELLOW + "Click to toggle!"
                )));

        // Head Start section (slots 30, 31, 32)
        inv.setItem(30, makeItem(Material.RED_STAINED_GLASS_PANE, 1,
                ChatColor.RED + "◀ Less", Arrays.asList(ChatColor.GRAY + "Decrease head start")));

        inv.setItem(31, makeItem(Material.FEATHER, 1,
                ChatColor.AQUA + "🚀 Head Start: " + ChatColor.WHITE + headStart + "s",
                Arrays.asList(
                        ChatColor.GRAY + "Seconds hunters are frozen at start.",
                        ChatColor.YELLOW + "Current: " + ChatColor.WHITE + headStart + "s"
                )));

        inv.setItem(32, makeItem(Material.GREEN_STAINED_GLASS_PANE, 1,
                ChatColor.GREEN + "More ▶", Arrays.asList(ChatColor.GRAY + "Increase head start")));

        // Respawn toggle (slot 34)
        boolean respawn = plugin.getConfigManager().isAllowHunterRespawn();
        inv.setItem(34, makeItem(
                respawn ? Material.RESPAWN_ANCHOR : Material.GRAY_CONCRETE, 1,
                (respawn ? ChatColor.GREEN : ChatColor.GRAY) + "🔄 Hunter Respawn: " +
                        (respawn ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"),
                Arrays.asList(
                        ChatColor.GRAY + "Allow hunters to respawn (costs a life).",
                        ChatColor.YELLOW + "Click to toggle!"
                )));

        // Back button (slot 40)
        inv.setItem(40, makeItem(Material.ARROW, 1,
                ChatColor.YELLOW + "◀ Back",
                Arrays.asList(ChatColor.GRAY + "Return to main menu.")));

        player.openInventory(inv);
        player.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "open_menu"),
                org.bukkit.persistence.PersistentDataType.STRING,
                SETTINGS_MENU_ID
        );
    }

    public void openLanguageMenu(Player player) {
        String title = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "🐱 MeowManhunt" +
                ChatColor.RESET + ChatColor.GRAY + " - Language";

        Inventory inv = Bukkit.createInventory(null, 27, title);
        fillBorders(inv, 27, Material.GRAY_STAINED_GLASS_PANE, " ");

        // Header
        inv.setItem(4, makeItem(Material.BOOK, 1,
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "🌐 Language Selection",
                Arrays.asList(ChatColor.GRAY + "Choose your preferred language.")));

        String currentLang = plugin.getLanguageManager().getPlayerLanguage(player.getUniqueId());

        // English
        inv.setItem(11, makeItem(Material.WHITE_BANNER, 1,
                (currentLang.equals("en") ? ChatColor.YELLOW + "★ " : "") + ChatColor.WHITE + "🇬🇧 English",
                Arrays.asList(
                        ChatColor.GRAY + "Switch to English messages.",
                        "",
                        currentLang.equals("en") ? ChatColor.YELLOW + "✔ Currently selected!" : ChatColor.GREEN + "Click to select!"
                )));

        // Vietnamese
        inv.setItem(15, makeItem(Material.RED_BANNER, 1,
                (currentLang.equals("vi") ? ChatColor.YELLOW + "★ " : "") + ChatColor.RED + "🇻🇳 Tiếng Việt",
                Arrays.asList(
                        ChatColor.GRAY + "Chuyển sang tiếng Việt.",
                        "",
                        currentLang.equals("vi") ? ChatColor.YELLOW + "✔ Đang được chọn!" : ChatColor.GREEN + "Nhấn để chọn!"
                )));

        // Back
        inv.setItem(22, makeItem(Material.ARROW, 1,
                ChatColor.YELLOW + "◀ Back",
                Arrays.asList(ChatColor.GRAY + "Return to main menu.")));

        player.openInventory(inv);
        player.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "open_menu"),
                org.bukkit.persistence.PersistentDataType.STRING,
                LANG_MENU_ID
        );
    }

    // ===================== HELPERS =====================

    public static ItemStack makeItem(Material mat, int amount, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack makeBorderItem(Material mat) {
        return makeItem(mat, 1, " ", new ArrayList<>());
    }

    private void fillBorders(Inventory inv, int size, Material mat, String name) {
        ItemStack border = makeItem(mat, 1, name, new ArrayList<>());
        int rows = size / 9;
        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(size - 9 + i, border);
        }
        // Left and right columns
        for (int r = 1; r < rows - 1; r++) {
            inv.setItem(r * 9, border);
            inv.setItem(r * 9 + 8, border);
        }
    }

    private List<String> buildPlayerList(java.util.Set<UUID> uuids) {
        List<String> list = new ArrayList<>();
        list.add("");
        if (uuids.isEmpty()) {
            list.add(ChatColor.GRAY + "(none)");
        } else {
            for (UUID uuid : uuids) {
                Player p = Bukkit.getPlayer(uuid);
                String name = p != null ? p.getName() : Bukkit.getOfflinePlayer(uuid).getName();
                list.add(ChatColor.WHITE + "• " + (name != null ? name : "Unknown"));
            }
        }
        return list;
    }

    private String getStateName(LanguageManager lm, Player player) {
        String key = switch (plugin.getGameManager().getGameState()) {
            case WAITING -> "state-waiting";
            case COUNTDOWN -> "state-countdown";
            case RUNNING -> "state-running";
            case ENDED -> "state-ended";
        };
        return lm.getMessage(key, player);
    }

    private String getLangDisplay(String lang) {
        return switch (lang) {
            case "vi" -> "🇻🇳 Tiếng Việt";
            default -> "🇬🇧 English";
        };
    }
}
