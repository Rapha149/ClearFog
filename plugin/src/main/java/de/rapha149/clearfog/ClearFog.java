package de.rapha149.clearfog;

import de.rapha149.clearfog.Metrics.DrilldownPie;
import de.rapha149.clearfog.Metrics.SimplePie;
import de.rapha149.clearfog.Metrics.SingleLineChart;
import de.rapha149.clearfog.version.VersionWrapper;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static de.rapha149.clearfog.Messages.getMessage;
import static de.rapha149.clearfog.Messages.loadMessages;
import static de.rapha149.clearfog.Util.WRAPPER;
import static de.rapha149.clearfog.Util.config;

public final class ClearFog extends JavaPlugin {

    private static final Map<String, String> VERSIONS = Map.of(
            "1.20.5", "1_20_R4",
            "1.20.6", "1_20_R4",
            "1.21.1", "1_21_R1",
            "1.21.3", "1_21_R2",
            "1.21.4", "1_21_R3",
            "1.21.5", "1_21_R4",
            "1.21.7", "1_21_R5"
    );
    private static final String NEWEST_VERSION = "1_21_R5";

    private static ClearFog instance;

    @Override
    public void onEnable() {
        instance = this;

        String craftBukkitPackage = Bukkit.getServer().getClass().getPackage().getName();
        String nmsVersion = craftBukkitPackage.contains(".v") ? craftBukkitPackage.split("\\.")[3].substring(1) :
                VERSIONS.getOrDefault(Bukkit.getBukkitVersion().split("-")[0], NEWEST_VERSION);
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
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                String version = Updates.getAvailableVersion(true);
                if (version == null)
                    getLogger().info(getMessage("plugin.up_to_date"));
                else {
                    for (String line : getMessage("plugin.outdated").replace("%version%", version)
                            .replace("%url%", Updates.SPIGOT_URL).split("\n")) {
                        getLogger().warning(line);
                    }
                }
            });
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
