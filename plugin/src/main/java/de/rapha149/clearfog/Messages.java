package de.rapha149.clearfog;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class Messages {

    private static File messageFile;
    private static FileConfiguration messageConfig;

    static {
        messageFile = new File(ClearFog.getInstance().getDataFolder(), "messages.yml");
        messageConfig = new YamlConfiguration();
        messageConfig.options().copyDefaults(true);
        messageConfig.addDefault("prefix", "&8[&4ClearFog&8] ");
        messageConfig.addDefault("plugin.enable", "Plugin successfully enabled.");
        messageConfig.addDefault("plugin.disable", "Plugin disabled.");
        messageConfig.addDefault("plugin.up_to_date", "Your version of this plugin is up to date!");
        messageConfig.addDefault("plugin.outdated", "There's a new version available for this plugin: %version%" +
                                                    "\nYou can download it from: %url%");
        messageConfig.addDefault("syntax", "%prefix%&cSyntax error! Please use &7/%syntax%&c.");
        messageConfig.addDefault("no_permission", "%prefix%&cYou do not have enough permissions to perform this action.");
        messageConfig.addDefault("player_not_found", "%prefix%&cThe player &7%player% &ccould not be found.");
        messageConfig.addDefault("state_number", "%prefix%&cPlease state a number.");
        messageConfig.addDefault("out_of_bounds", "%prefix%&cPlease state a number between 2 and 32.");
        messageConfig.addDefault("reload", "%prefix%&7Config was reloaded.");
        messageConfig.addDefault("directupdates.status.enabled", "%prefix%&7Direct updates are currently &aenabled&7.");
        messageConfig.addDefault("directupdates.status.disabled", "%prefix%&7Direct updates are currently &cdisabled&7.");
        messageConfig.addDefault("directupdates.enable.as_before", "%prefix%&6Direct updates are already enabled.");
        messageConfig.addDefault("directupdates.enable.success", "%prefix%&7Direct updates are now &aenabled." +
                                                                 "\\n&6Direct updates will update the view distance directly when it was changed. " +
                                                                 "This is &lnot &6recommeded because it does not look good.");
        messageConfig.addDefault("directupdates.disable.as_before", "%prefix%&6Direct updates are already disabled.");
        messageConfig.addDefault("directupdates.disable.success", "%prefix%&7Direct updates are now &cdisabled.");
        messageConfig.addDefault("default.status.enabled", "%prefix%&7Default view distance is currently &aenabled&7.");
        messageConfig.addDefault("default.status.disabled", "%prefix%&7Default view distance is currently &cdisabled&7.");
        messageConfig.addDefault("default.enable.as_before", "%prefix%&6Default view distance is already enabled.");
        messageConfig.addDefault("default.enable.success", "%prefix%&7Default view distance is now &aenabled.");
        messageConfig.addDefault("default.disable.as_before", "%prefix%&6Default view distance is already disabled.");
        messageConfig.addDefault("default.disable.success", "%prefix%&7Default view distance is now &cdisabled.");
        messageConfig.addDefault("default.set.as_before", "%prefix%&6Default view distance is already set to &7%distance%&7.");
        messageConfig.addDefault("default.set.success", "%prefix%&7Default view distance is now set to &6%distance%&7.");
        messageConfig.addDefault("default.get", "%prefix%&7Default view distance is currently &6%distance%&7.");
        messageConfig.addDefault("individual.feature_not_enabled", "%prefix%&cThis feature is not enabled. You can enable it by using &7/fog individual enable&c.");
        messageConfig.addDefault("individual.status.enabled", "%prefix%&7Player specific view distances are currently &aenabled&7.");
        messageConfig.addDefault("individual.status.disabled", "%prefix%&7Player specific view distances are currently &cdisabled&7.");
        messageConfig.addDefault("individual.enable.as_before", "%prefix%&6Player specific view distances are already enabled.");
        messageConfig.addDefault("individual.enable.success", "%prefix%&7Player specific view distances are now &aenabled.");
        messageConfig.addDefault("individual.disable.as_before", "%prefix%&6Player specific view distances are already disabled.");
        messageConfig.addDefault("individual.disable.success", "%prefix%&7Player specific view distances are now &cdisabled.");
        messageConfig.addDefault("individual.list.nothing", "%prefix%&7There are no player specific view distances.");
        messageConfig.addDefault("individual.list.prefix", "%prefix%&7Player specific view distances:");
        messageConfig.addDefault("individual.list.part", "&7- %player%: &6%distance%");
        messageConfig.addDefault("individual.get.self.return", "%prefix%&7Your individual view distance is &6%distance%&7.");
        messageConfig.addDefault("individual.get.self.not_set", "%prefix%&7Your individual view distance is &6not set&7.");
        messageConfig.addDefault("individual.get.others.return", "%prefix%&7The individual view distance of &5%player% &7is &6%distance%&7.");
        messageConfig.addDefault("individual.get.others.not_set", "%prefix%&7The individual view distance of &5%player% &7is &6not set&7.");
        messageConfig.addDefault("individual.set.self.as_before", "%prefix%&6Your individual view distance is already set to &7%distance%&c.");
        messageConfig.addDefault("individual.set.self.success", "%prefix%&7Your individual view distance was set to &6%distance%&7.");
        messageConfig.addDefault("individual.set.others.as_before", "%prefix%&6The individual view distance of &5%player% &cis already set to &7%distance%&c.");
        messageConfig.addDefault("individual.set.others.success", "%prefix%&7The individual view distance of &5%player% &7was set to &6%distance%&7.");
        messageConfig.addDefault("individual.unset.self.does_not_exist", "%prefix%&cYou do not have an individual view distance.");
        messageConfig.addDefault("individual.unset.self.success", "%prefix%&7Your individual view distance was removed.");
        messageConfig.addDefault("individual.unset.others.does_not_exist", "%prefix%&5%player% &cdoes not have an individual view distance.");
        messageConfig.addDefault("individual.unset.others.success", "%prefix%&7The individual view distance of &5%player% &7was removed.");
    }

    public static void loadMessages() {
        try {
            if (messageFile.exists())
                messageConfig.load(messageFile);
            else
                messageFile.getParentFile().mkdirs();

            messageConfig.getKeys(false).forEach(key -> {
                if (!messageConfig.getDefaults().isSet(key))
                    messageConfig.set(key, null);
            });

            messageConfig.save(messageFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            ClearFog.getInstance().getLogger().severe("Failed to load message config.");
        }
    }

    public static String getMessage(String key) {
        return ChatColor.translateAlternateColorCodes('&', messageConfig.getString(key)
                .replace("\\n", "\n")
                .replace("%prefix%", messageConfig.getString("prefix")));
    }
}
