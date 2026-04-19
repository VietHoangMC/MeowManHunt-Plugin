package me.viethoang.meowmanhunt.commands;

import me.viethoang.meowmanhunt.MeowManhunt;
import me.viethoang.meowmanhunt.gui.MenuManager;
import me.viethoang.meowmanhunt.managers.GameManager;
import me.viethoang.meowmanhunt.managers.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class ManhuntCommand implements CommandExecutor, TabCompleter {

    private final MeowManhunt plugin;
    private final MenuManager menuManager;

    public ManhuntCommand(MeowManhunt plugin) {
        this.plugin = plugin;
        this.menuManager = new MenuManager(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LanguageManager lm = plugin.getLanguageManager();
        GameManager gm = plugin.getGameManager();

        // Player for lang lookup
        Player playerSender = (sender instanceof Player p) ? p : null;

        if (args.length == 0) {
            // Open menu or show help
            if (playerSender != null) {
                menuManager.openMainMenu(playerSender);
            } else {
                showHelp(sender, lm);
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {

            // ─── MENU ───────────────────────────────────────────────────────
            case "menu", "gui", "open" -> {
                if (!requirePlayer(sender, lm)) return true;
                menuManager.openMainMenu(playerSender);
            }

            // ─── START ──────────────────────────────────────────────────────
            case "start" -> {
                if (!requirePermission(sender, "meowmanhunt.admin", lm)) return true;
                if (!gm.isWaiting()) {
                    send(sender, playerSender, lm, "game-already-running");
                    return true;
                }
                gm.startGame(playerSender);
            }

            // ─── STOP ───────────────────────────────────────────────────────
            case "stop", "end" -> {
                if (!requirePermission(sender, "meowmanhunt.admin", lm)) return true;
                if (gm.isWaiting()) {
                    send(sender, playerSender, lm, "game-not-running");
                    return true;
                }
                gm.stopGame(playerSender);
            }

            // ─── RESET ──────────────────────────────────────────────────────
            case "reset" -> {
                if (!requirePermission(sender, "meowmanhunt.admin", lm)) return true;
                gm.resetGame();
                send(sender, playerSender, lm, "reloaded");
            }

            // ─── JOIN ───────────────────────────────────────────────────────
            case "join" -> {
                if (!requirePlayer(sender, lm)) return true;
                if (gm.isRunning() || gm.isCountdown()) {
                    lm.send(playerSender, "game-already-running");
                    return true;
                }
                if (args.length < 2) {
                    menuManager.openTeamMenu(playerSender);
                    return true;
                }
                String role = args[1].toLowerCase();
                switch (role) {
                    case "runner", "speedrunner", "run" -> {
                        if (gm.addSpeedrunner(playerSender)) {
                            lm.send(playerSender, "joined-speedrunner");
                            gm.broadcastKey("player-joined-speedrunner", "player", playerSender.getName());
                        } else {
                            lm.send(playerSender, gm.isSpeedrunner(playerSender.getUniqueId())
                                    ? "already-speedrunner" : "already-in-team", "player", playerSender.getName());
                        }
                    }
                    case "hunter", "hunt" -> {
                        if (gm.addHunter(playerSender)) {
                            lm.send(playerSender, "joined-hunter");
                            gm.broadcastKey("player-joined-hunter", "player", playerSender.getName());
                        } else {
                            lm.send(playerSender, gm.isHunter(playerSender.getUniqueId())
                                    ? "already-hunter" : "already-in-team", "player", playerSender.getName());
                        }
                    }
                    case "spectator", "spec", "spectate" -> {
                        if (gm.addSpectator(playerSender)) {
                            lm.send(playerSender, "joined-spectator");
                        } else {
                            lm.send(playerSender, "already-spectator");
                        }
                    }
                    default -> {
                        sender.sendMessage(LanguageManager.colorize("&cUsage: /manhunt join <runner|hunter|spectator>"));
                    }
                }
            }

            // ─── LEAVE ──────────────────────────────────────────────────────
            case "leave" -> {
                if (!requirePlayer(sender, lm)) return true;
                if (!gm.isInGame(playerSender.getUniqueId())) {
                    lm.send(playerSender, "not-in-game");
                    return true;
                }
                gm.removePlayer(playerSender.getUniqueId());
                lm.send(playerSender, "left-game");
                gm.broadcastKey("player-left", "player", playerSender.getName());
            }

            // ─── SETRUNNER / ADDRUNNER ───────────────────────────────────────
            case "setrunner", "addrunner", "runner" -> {
                if (!requirePermission(sender, "meowmanhunt.admin", lm)) return true;
                if (args.length < 2) {
                    sender.sendMessage(LanguageManager.colorize("&cUsage: /manhunt setrunner <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    send(sender, playerSender, lm, "player-not-found", "player", args[1]);
                    return true;
                }
                if (gm.isRunning() || gm.isCountdown()) {
                    gm.forceAddHunter(target); // Can add mid-game if admin forces
                }
                if (gm.addSpeedrunner(target)) {
                    send(sender, playerSender, lm, "set-speedrunner", "player", target.getName());
                    lm.send(target, "joined-speedrunner");
                } else {
                    send(sender, playerSender, lm, "already-in-team", "player", target.getName());
                }
            }

            // ─── SETHUNTER / ADDHUNTER ──────────────────────────────────────
            case "sethunter", "addhunter", "hunter" -> {
                if (!requirePermission(sender, "meowmanhunt.admin", lm)) return true;
                if (args.length < 2) {
                    sender.sendMessage(LanguageManager.colorize("&cUsage: /manhunt sethunter <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    send(sender, playerSender, lm, "player-not-found", "player", args[1]);
                    return true;
                }
                if (gm.addHunter(target)) {
                    send(sender, playerSender, lm, "set-hunter", "player", target.getName());
                    lm.send(target, "joined-hunter");
                } else {
                    send(sender, playerSender, lm, "already-in-team", "player", target.getName());
                }
            }

            // ─── REMOVERUNNER ────────────────────────────────────────────────
            case "removerunner", "kickrunner" -> {
                if (!requirePermission(sender, "meowmanhunt.admin", lm)) return true;
                if (args.length < 2) {
                    sender.sendMessage(LanguageManager.colorize("&cUsage: /manhunt removerunner <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    send(sender, playerSender, lm, "player-not-found", "player", args[1]);
                    return true;
                }
                gm.removePlayer(target.getUniqueId());
                send(sender, playerSender, lm, "removed-from-team", "player", target.getName());
            }

            // ─── REMOVEHUNTER ────────────────────────────────────────────────
            case "removehunter", "kickhunter" -> {
                if (!requirePermission(sender, "meowmanhunt.admin", lm)) return true;
                if (args.length < 2) {
                    sender.sendMessage(LanguageManager.colorize("&cUsage: /manhunt removehunter <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    send(sender, playerSender, lm, "player-not-found", "player", args[1]);
                    return true;
                }
                gm.removePlayer(target.getUniqueId());
                send(sender, playerSender, lm, "removed-from-team", "player", target.getName());
            }

            // ─── LIVES ──────────────────────────────────────────────────────
            case "lives", "setlives", "life" -> {
                if (!requirePermission(sender, "meowmanhunt.admin", lm)) return true;
                if (args.length < 2) {
                    sender.sendMessage(LanguageManager.colorize("&cUsage: /manhunt lives <1-10>"));
                    return true;
                }
                try {
                    int lives = Integer.parseInt(args[1]);
                    if (lives < 1) {
                        send(sender, playerSender, lm, "lives-min");
                    } else if (lives > 10) {
                        send(sender, playerSender, lm, "lives-max");
                    } else {
                        plugin.getConfigManager().setHunterLives(lives);
                        send(sender, playerSender, lm, "lives-set", "lives", String.valueOf(lives));
                    }
                } catch (NumberFormatException e) {
                    send(sender, playerSender, lm, "invalid-number");
                }
            }

            // ─── COUNTDOWN ──────────────────────────────────────────────────
            case "countdown", "setcountdown" -> {
                if (!requirePermission(sender, "meowmanhunt.admin", lm)) return true;
                if (args.length < 2) {
                    sender.sendMessage(LanguageManager.colorize("&cUsage: /manhunt countdown <seconds>"));
                    return true;
                }
                try {
                    int sec = Integer.parseInt(args[1]);
                    if (sec < 3) sec = 3;
                    plugin.getConfigManager().setCountdownTime(sec);
                    sender.sendMessage(LanguageManager.colorize("&a✅ Countdown set to &6" + sec + "s&a!"));
                } catch (NumberFormatException e) {
                    send(sender, playerSender, lm, "invalid-number");
                }
            }

            // ─── STATUS ─────────────────────────────────────────────────────
            case "status", "info" -> {
                showGameStatus(sender, playerSender, lm, gm);
            }

            // ─── GLOW ───────────────────────────────────────────────────────
            case "glow" -> {
                if (!requirePermission(sender, "meowmanhunt.admin", lm)) return true;
                if (args.length < 2) {
                    boolean current = plugin.getConfigManager().isGlowEffect();
                    plugin.getConfigManager().setGlowEffect(!current);
                    sender.sendMessage(LanguageManager.colorize("&a✅ Glow effect: " + (!current ? "&aON" : "&cOFF")));
                    return true;
                }
                boolean state = args[1].equalsIgnoreCase("on") || args[1].equals("1") || args[1].equalsIgnoreCase("true");
                plugin.getConfigManager().setGlowEffect(state);
                sender.sendMessage(LanguageManager.colorize("&a✅ Glow effect: " + (state ? "&aON" : "&cOFF")));
            }

            // ─── GIVE ───────────────────────────────────────────────────────
            case "give" -> {
                if (!requirePermission(sender, "meowmanhunt.admin", lm)) return true;
                if (args.length >= 2 && args[1].equalsIgnoreCase("compass")) {
                    // Give compass to all hunters or specified player
                    if (args.length >= 3) {
                        Player target = Bukkit.getPlayer(args[2]);
                        if (target == null) {
                            send(sender, playerSender, lm, "player-not-found", "player", args[2]);
                            return true;
                        }
                        target.getInventory().addItem(new ItemStack(Material.COMPASS));
                        lm.send(target, "give-compass");
                        sender.sendMessage(LanguageManager.colorize("&a✅ Compass given to &e" + target.getName() + "&a!"));
                    } else {
                        int count = 0;
                        for (UUID uuid : gm.getHunters()) {
                            Player hunter = Bukkit.getPlayer(uuid);
                            if (hunter != null) {
                                hunter.getInventory().addItem(new ItemStack(Material.COMPASS));
                                lm.send(hunter, "give-compass");
                                count++;
                            }
                        }
                        sender.sendMessage(LanguageManager.colorize("&a✅ Compass given to &6" + count + " &ahunters!"));
                    }
                } else {
                    sender.sendMessage(LanguageManager.colorize("&cUsage: /manhunt give compass [player]"));
                }
            }

            // ─── LANG ───────────────────────────────────────────────────────
            case "lang", "language" -> {
                if (args.length < 2) {
                    sender.sendMessage(LanguageManager.colorize("&cUsage: /manhunt lang <en|vi>"));
                    return true;
                }
                String lang = args[1].toLowerCase();
                if (!lang.equals("en") && !lang.equals("vi")) {
                    sender.sendMessage(LanguageManager.colorize("&cAvailable languages: &een&c, &evi"));
                    return true;
                }
                if (playerSender != null) {
                    plugin.getLanguageManager().setPlayerLanguage(playerSender.getUniqueId(), lang);
                } else {
                    plugin.getLanguageManager().setDefaultLanguage(lang);
                    plugin.getConfig().set("language", lang);
                    plugin.saveConfig();
                }
                send(sender, playerSender, lm, "language-set");
            }

            // ─── RELOAD ─────────────────────────────────────────────────────
            case "reload", "rl" -> {
                if (!requirePermission(sender, "meowmanhunt.admin", lm)) return true;
                plugin.getConfigManager().reload();
                plugin.getLanguageManager().loadMessages();
                send(sender, playerSender, lm, "reloaded");
            }

            // ─── HELP ───────────────────────────────────────────────────────
            case "help", "?" -> showHelp(sender, lm);

            // ─── UNKNOWN ────────────────────────────────────────────────────
            default -> {
                sender.sendMessage(LanguageManager.colorize(
                        "&c❌ Unknown subcommand. Use &e/manhunt help &cfor help."));
            }
        }

        return true;
    }

    // ===================== TAB COMPLETION =====================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList(
                    "menu", "start", "stop", "reset", "join", "leave",
                    "setrunner", "sethunter", "removerunner", "removehunter",
                    "lives", "countdown", "status", "glow", "give",
                    "lang", "reload", "help"
            ));
            return filterStart(subs, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            return switch (sub) {
                case "join" -> filterStart(Arrays.asList("runner", "hunter", "spectator"), args[1]);
                case "setrunner", "addrunner", "sethunter", "addhunter",
                     "removerunner", "removehunter", "give" ->
                        filterPlayers(args[1]);
                case "lives" -> filterStart(Arrays.asList("1","2","3","4","5","6","7","8","9","10"), args[1]);
                case "countdown" -> filterStart(Arrays.asList("5","10","15","20","30","60"), args[1]);
                case "glow" -> filterStart(Arrays.asList("on","off"), args[1]);
                case "lang", "language" -> filterStart(Arrays.asList("en","vi"), args[1]);
                default -> Collections.emptyList();
            };
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            if (args[1].equalsIgnoreCase("compass")) {
                return filterPlayers(args[2]);
            }
        }

        return Collections.emptyList();
    }

    // ===================== HELPERS =====================

    private void showHelp(CommandSender sender, LanguageManager lm) {
        boolean isPlayer = sender instanceof Player p && p != null;
        Player p = isPlayer ? (Player) sender : null;

        String[] keys = {
                "help-header", "help-menu", "help-start", "help-stop", "help-join",
                "help-leave", "help-setrunner", "help-sethunter", "help-removerunner",
                "help-removehunter", "help-lives", "help-countdown", "help-status",
                "help-reload", "help-lang", "help-give", "help-glow", "help-footer"
        };
        for (String key : keys) {
            sender.sendMessage(LanguageManager.colorize(lm.getMessage(key, p)));
        }
    }

    private void showGameStatus(CommandSender sender, Player playerSender, LanguageManager lm, GameManager gm) {
        sender.sendMessage(LanguageManager.colorize(lm.getMessage("status-header", playerSender)));

        String stateKey = switch (gm.getGameState()) {
            case WAITING -> "state-waiting";
            case COUNTDOWN -> "state-countdown";
            case RUNNING -> "state-running";
            case ENDED -> "state-ended";
        };
        sender.sendMessage(LanguageManager.colorize(
                lm.getMessage("status-state", playerSender,
                        "state", lm.getMessage(stateKey, playerSender))));

        // Runners
        List<String> runnerNames = getNameList(gm.getSpeedrunners());
        sender.sendMessage(LanguageManager.colorize(
                lm.getMessage("status-runners", playerSender,
                        "count", String.valueOf(gm.getSpeedrunners().size()),
                        "list", runnerNames.isEmpty() ? "-" : String.join(", ", runnerNames))));

        // Hunters
        List<String> hunterNames = getNameList(gm.getHunters());
        sender.sendMessage(LanguageManager.colorize(
                lm.getMessage("status-hunters", playerSender,
                        "count", String.valueOf(gm.getHunters().size()),
                        "list", hunterNames.isEmpty() ? "-" : String.join(", ", hunterNames))));

        // Spectators
        List<String> specNames = getNameList(gm.getSpectators());
        sender.sendMessage(LanguageManager.colorize(
                lm.getMessage("status-spectators", playerSender,
                        "count", String.valueOf(gm.getSpectators().size()),
                        "list", specNames.isEmpty() ? "-" : String.join(", ", specNames))));

        sender.sendMessage(LanguageManager.colorize(
                lm.getMessage("status-lives", playerSender,
                        "lives", String.valueOf(plugin.getConfigManager().getHunterLives()))));

        if (gm.isRunning()) {
            long elapsed = (System.currentTimeMillis() - gm.getGameStartTime()) / 1000;
            sender.sendMessage(LanguageManager.colorize(
                    lm.getMessage("status-time", playerSender,
                            "time", GameManager.formatTime(elapsed))));
        }

        sender.sendMessage(LanguageManager.colorize(lm.getMessage("status-footer", playerSender)));
    }

    private List<String> getNameList(Set<UUID> uuids) {
        List<String> names = new ArrayList<>();
        for (UUID uuid : uuids) {
            Player p = Bukkit.getPlayer(uuid);
            names.add(p != null ? p.getName() : "?");
        }
        return names;
    }

    private boolean requirePlayer(CommandSender sender, LanguageManager lm) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(LanguageManager.colorize(lm.getMessage("player-only")));
            return false;
        }
        return true;
    }

    private boolean requirePermission(CommandSender sender, String perm, LanguageManager lm) {
        if (!sender.hasPermission(perm)) {
            Player p = sender instanceof Player pl ? pl : null;
            sender.sendMessage(LanguageManager.colorize(lm.getMessage("no-permission", p)));
            return false;
        }
        return true;
    }

    private void send(CommandSender sender, Player player, LanguageManager lm, String key, String... replacements) {
        if (player != null) {
            lm.send(player, key, replacements);
        } else {
            sender.sendMessage(LanguageManager.colorize(lm.getPrefixedMessage(key, replacements)));
        }
    }

    private List<String> filterStart(List<String> list, String prefix) {
        String lower = prefix.toLowerCase();
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }

    private List<String> filterPlayers(String prefix) {
        String lower = prefix.toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }
}
