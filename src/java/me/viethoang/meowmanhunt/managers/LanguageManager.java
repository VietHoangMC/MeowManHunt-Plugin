package me.viethoang.meowmanhunt.managers;

import me.viethoang.meowmanhunt.MeowManhunt;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LanguageManager {

    private final MeowManhunt plugin;
    private FileConfiguration enMessages;
    private FileConfiguration viMessages;
    private final Map<UUID, String> playerLanguages = new HashMap<>();
    private String defaultLang;

    public LanguageManager(MeowManhunt plugin) {
        this.plugin = plugin;
        loadMessages();
        this.defaultLang = plugin.getConfigManager().getLanguage();
    }

    public void loadMessages() {
        enMessages = loadLangFile("messages_en.yml");
        viMessages = loadLangFile("messages_vi.yml");
        this.defaultLang = plugin.getConfigManager().getLanguage();
    }

    private FileConfiguration loadLangFile(String filename) {
        File file = new File(plugin.getDataFolder(), filename);
        if (!file.exists()) {
            plugin.saveResource(filename, false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        InputStream defStream = plugin.getResource(filename);
        if (defStream != null) {
            FileConfiguration defConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defStream, StandardCharsets.UTF_8));
            config.setDefaults(defConfig);
        }
        return config;
    }

    public void setPlayerLanguage(UUID uuid, String lang) {
        playerLanguages.put(uuid, lang.toLowerCase());
    }

    public void setDefaultLanguage(String lang) {
        this.defaultLang = lang.toLowerCase();
    }

    public String getPlayerLanguage(UUID uuid) {
        return playerLanguages.getOrDefault(uuid, defaultLang);
    }

    public String getDefaultLanguage() {
        return defaultLang;
    }

    /** Get message for a player in their language */
    public String getMessage(String key, Player player, String... replacements) {
        String lang = player != null ? getPlayerLanguage(player.getUniqueId()) : defaultLang;
        return getMessageByLang(key, lang, replacements);
    }

    /** Get message in a specific language */
    public String getMessageByLang(String key, String lang, String... replacements) {
        FileConfiguration cfg = "vi".equals(lang) ? viMessages : enMessages;
        String msg = cfg.getString(key);
        if (msg == null) msg = "§c[MeowManhunt] Missing: " + key;

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            msg = msg.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return colorize(msg);
    }

    /** Get message in default language */
    public String getMessage(String key, String... replacements) {
        return getMessageByLang(key, defaultLang, replacements);
    }

    /** Prefixed message for a player */
    public String getPrefixedMessage(String key, Player player, String... replacements) {
        return colorize(plugin.getConfigManager().getPrefix()) + getMessage(key, player, replacements);
    }

    public String getPrefixedMessage(String key, String... replacements) {
        return colorize(plugin.getConfigManager().getPrefix()) + getMessage(key, replacements);
    }

    /** Send prefixed message to a player in their language */
    public void send(Player player, String key, String... replacements) {
        String msg = getPrefixedMessage(key, player, replacements);
        for (String line : msg.split("\n")) {
            player.sendMessage(colorize(line));
        }
    }

    /** Send raw (no prefix) message */
    public void sendRaw(Player player, String key, String... replacements) {
        String msg = getMessage(key, player, replacements);
        for (String line : msg.split("\n")) {
            player.sendMessage(colorize(line));
        }
    }

    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
