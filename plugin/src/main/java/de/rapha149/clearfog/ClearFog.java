package de.rapha149.clearfog;

import de.rapha149.clearfog.Metrics.DrilldownPie;
import de.rapha149.clearfog.Metrics.SimplePie;
import de.rapha149.clearfog.Metrics.SingleLineChart;
import de.rapha149.clearfog.version.VersionWrapper;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static de.rapha149.clearfog.Messages.getMessage;
import static de.rapha149.clearfog.Messages.loadMessages;
import static de.rapha149.clearfog.Util.WRAPPER;
import static de.rapha149.clearfog.Util.config;

public final class ClearFog extends JavaPlugin {

    private static ClearFog instance;

    @Override
    public void onEnable() {
        instance = this;

        String nmsVersion = getNMSVersion();
        try {
            WRAPPER = (VersionWrapper) Class.forName(VersionWrapper.class.getPackage().getName() + ".Wrapper" + nmsVersion).newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new IllegalStateException("Failed to load support for server version \"" + nmsVersion + "\"", e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("ClearFog does not support the server version \"" + nmsVersion + "\"", e);
        }

        loadMessages();
        loadConfig();

        boolean ploudos;
        try {
            ploudos = Files.readString(Path.of("eula.txt")).contains("PloudOS");
        } catch (IOException e) {
            e.printStackTrace();
            ploudos = false;
        }
        boolean finalPloudos = ploudos;

        Metrics metrics = new Metrics(this, 13628);
        metrics.addCustomChart(new SingleLineChart("ploudos_servers", () -> finalPloudos ? 1 : 0));
        metrics.addCustomChart(new SingleLineChart("default_view_distance_enabled", () -> config.getBoolean("default.enabled") ? 1 : 0));
        metrics.addCustomChart(new SingleLineChart("player_specific_view_distance_enabled", () -> config.getBoolean("individual.enabled") ? 1 : 0));
        metrics.addCustomChart(new SingleLineChart("world_specific_view_distance_enabled", () -> config.getBoolean("world.enabled") ? 1 : 0));
        metrics.addCustomChart(new SimplePie("default_view_distance", () -> String.valueOf(config.getInt("default.view-distance"))));
        metrics.addCustomChart(new SimplePie("direct_updates_enabled", () -> String.valueOf(config.getBoolean("direct-view-distance-updates"))));
        metrics.addCustomChart(new DrilldownPie("check_for_updates", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Map<String, Integer> entry = new HashMap<>();
            entry.put(getDescription().getVersion(), 1);
            map.put(String.valueOf(config.getBoolean("check-for-updates")), entry);
            return map;
        }));

        if (config.getBoolean("check-for-updates")) {
            String version = Updates.getAvailableVersion(true);
            if (version == null)
                getLogger().info(getMessage("plugin.up_to_date"));
            else {
                for (String line : getMessage("plugin.outdated").replace("%version%", version)
                        .replace("%url%", Updates.SPIGOT_URL).split("\n")) {
                    getLogger().warning(line);
                }
            }
        }

        try {
            Util.registerHandler();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        new FogCommand(getCommand("fog"));
        getServer().getPluginManager().registerEvents(new Events(), this);
        getLogger().info(getMessage("plugin.enable"));
    }

    public static ClearFog getInstance() {
        return instance;
    }

    @Override
    public void onDisable() {
        try {
            Util.unregisterHandler();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        getLogger().info(getMessage("plugin.disable"));
    }

    private String getNMSVersion() {
        String craftBukkitPackage = Bukkit.getServer().getClass().getPackage().getName();
        if (craftBukkitPackage.contains(".v"))
            return craftBukkitPackage.split("\\.")[3].substring(1);

        // Get NMS Version from the bukkit version
        String bukkitVersion = Bukkit.getBukkitVersion();

        // Try to get NMS Version from online list (https://github.com/Rapha149/NMSVersions)
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("https://raw.githubusercontent.com/Rapha149/NMSVersions/main/nms-versions.json").openConnection();
            conn.setConnectTimeout(10000);
            conn.setRequestMethod("GET");
            conn.connect();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                JSONObject json = new JSONObject(br.lines().collect(Collectors.joining()));
                if (json.has(bukkitVersion))
                    return json.getString(bukkitVersion);
            }
        } catch (IOException e) {
            getLogger().warning("Can't access online NMS versions list, falling back to hardcoded NMS versions. These could be outdated.");
        }

        // separating major and minor versions, example: 1.20.4-R0.1-SNAPSHOT -> major = 20, minor = 4
        final String[] versionNumbers = bukkitVersion.split("-")[0].split("\\.");
        int major = Integer.parseInt(versionNumbers[1]);
        int minor = Integer.parseInt(versionNumbers[2]);

        if (major == 20 && minor >= 5) { // 1.20.5, 1.20.6
            return "1_20_R4";
        } else if (major == 21 && minor <= 1) { // 1.21, 1.21.1
            return "1_21_R1";
        } else if (major == 21 && (minor == 2 || minor == 3)) { // 1.21.2, 1.21.3
            return "1_21_R2";
        }

        throw new IllegalStateException("ClearFog does not support bukkit server version \"" + bukkitVersion + "\"");
    }

    void loadConfig() {
        config = getConfig();
        config.addDefault("check-for-updates", true);
        config.addDefault("direct-view-distance-updates", false);
        config.addDefault("default.enabled", true);
        config.addDefault("default.view-distance", 32);
        config.addDefault("world.enabled", false);
        if (!config.isConfigurationSection("world.worlds"))
            config.createSection("world.worlds");
        config.addDefault("individual.enabled", false);
        if (!config.isConfigurationSection("individual.players"))
            config.createSection("individual.players");
        config.options().copyDefaults(true);
        saveConfig();
        Util.checkViewDistances();
    }
}
