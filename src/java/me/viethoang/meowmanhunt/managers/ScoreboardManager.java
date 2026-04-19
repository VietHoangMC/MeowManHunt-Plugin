package me.viethoang.meowmanhunt.managers;

import me.viethoang.meowmanhunt.GameState;
import me.viethoang.meowmanhunt.MeowManhunt;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.*;

public class ScoreboardManager {

    private final MeowManhunt plugin;
    private final Map<UUID, Scoreboard> playerBoards = new HashMap<>();

    public ScoreboardManager(MeowManhunt plugin) {
        this.plugin = plugin;
    }

    public void updateAll() {
        GameState state = plugin.getGameManager().getGameState();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (state == GameState.WAITING) {
                removeScoreboard(p);
            } else {
                updateScoreboard(p);
            }
        }
    }

    public void updateScoreboard(Player player) {
        if (!plugin.getConfigManager().isScoreboardEnabled()) return;

        GameManager gm = plugin.getGameManager();
        LanguageManager lm = plugin.getLanguageManager();

        Scoreboard sb = playerBoards.computeIfAbsent(player.getUniqueId(), k ->
                Bukkit.getScoreboardManager().getNewScoreboard()
        );

        // Clear old objective
        Objective old = sb.getObjective("meowmh");
        if (old != null) old.unregister();

        String title = lm.getMessage("sb-title", player);
        Objective obj = sb.registerNewObjective("meowmh", Criteria.DUMMY,
                LanguageManager.colorize(title));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Build lines
        List<String> lines = buildLines(player, gm, lm);

        // Scores go from high to low (descending order = display order)
        int score = lines.size();
        for (String line : lines) {
            Score s = obj.getScore(ensureUnique(line, sb));
            s.setScore(score--);
        }

        player.setScoreboard(sb);
    }

    private List<String> buildLines(Player player, GameManager gm, LanguageManager lm) {
        List<String> lines = new ArrayList<>();
        UUID uuid = player.getUniqueId();

        lines.add(LanguageManager.colorize("&8&m──────────────"));

        // State line
        String stateKey = switch (gm.getGameState()) {
            case WAITING -> "state-waiting";
            case COUNTDOWN -> "state-countdown";
            case RUNNING -> "state-running";
            case ENDED -> "state-ended";
        };
        String stateName = lm.getMessage(stateKey, player);
        lines.add(LanguageManager.colorize(lm.getMessage("sb-state", player, "state", stateName)));

        lines.add(LanguageManager.colorize("&8&m──────────────"));

        // Runners count
        lines.add(LanguageManager.colorize(lm.getMessage("sb-runners", player,
                "count", String.valueOf(gm.getSpeedrunners().size()))));

        // Hunters count
        lines.add(LanguageManager.colorize(lm.getMessage("sb-hunters", player,
                "count", String.valueOf(gm.getHunters().size()))));

        lines.add(LanguageManager.colorize("&8&m──────────────"));

        // Lives (if hunter)
        if (gm.isHunter(uuid)) {
            int lives = gm.getHunterLivesFor(uuid);
            lines.add(LanguageManager.colorize(lm.getMessage("sb-your-lives", player,
                    "lives", String.valueOf(lives))));
        }

        // Timer
        if (gm.isRunning()) {
            long elapsed = (System.currentTimeMillis() - gm.getGameStartTime()) / 1000;
            lines.add(LanguageManager.colorize(lm.getMessage("sb-time", player,
                    "time", GameManager.formatTime(elapsed))));
        }

        lines.add(LanguageManager.colorize("&8&m──────────────"));
        lines.add(LanguageManager.colorize("&dviethoangmc.page.gd"));

        return lines;
    }

    // Ensure uniqueness for scoreboard entries (they must be unique strings)
    private String ensureUnique(String line, Scoreboard sb) {
        String result = line;
        int counter = 0;
        while (sb.getEntries().contains(result)) {
            result = line + ChatColor.RESET + " ".repeat(++counter);
        }
        // Scoreboard entries have 40-char limit for legacy, trim if needed
        if (result.length() > 40) result = result.substring(0, 40);
        return result;
    }

    public void removeScoreboard(Player player) {
        playerBoards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void removeAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            removeScoreboard(p);
        }
        playerBoards.clear();
    }
}
