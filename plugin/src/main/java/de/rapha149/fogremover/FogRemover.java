package de.rapha149.fogremover;

import de.rapha149.fogremover.version.VersionWrapper;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import static de.rapha149.fogremover.Messages.*;
import static de.rapha149.fogremover.Util.*;

public final class FogRemover extends JavaPlugin {

    private static FogRemover instance;

    @Override
    public void onEnable() {
        instance = this;

        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3].substring(1);
        try {
            WRAPPER = (VersionWrapper) Class.forName(VersionWrapper.class.getPackage().getName() + ".Wrapper" + version).newInstance();
        } catch (IllegalAccessException | InstantiationException exception) {
            throw new IllegalStateException("Failed to load support for server version " + version, exception);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("FogRemover does not support the server version \"" + version + "\"", exception);
        }

        loadMessages();
        loadConfig();

        try {
            Util.registerHandler();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        new FogCommand(getCommand("fog"));
        getLogger().info(getMessage("plugin.enable"));
    }

    public static FogRemover getInstance() {
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
        config.addDefault("default.enabled", true);
        config.addDefault("default.view-distance", 32);
        config.addDefault("individual.enabled", false);
        if (!config.isConfigurationSection("individual.players"))
            config.createSection("individual.players");
        config.options().copyDefaults(true);
        saveConfig();
        Util.checkViewDistances();
    }
}
