package de.rapha149.clearfog;

import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

import static de.rapha149.clearfog.Messages.getMessage;
import static de.rapha149.clearfog.Messages.loadMessages;
import static de.rapha149.clearfog.Util.*;

public class FogCommand implements CommandExecutor, TabCompleter {

    private ClearFog plugin;

    public FogCommand(PluginCommand command) {
        plugin = ClearFog.getInstance();
        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("clearfog")) {
            sender.sendMessage(getMessage("no_permission"));
            return true;
        }

        boolean worldfog = alias.equals("worldfog");
        if (worldfog)
            args = (String[]) ArrayUtils.addAll(new String[]{"world"}, args);
        boolean myfog = alias.equals("myfog");
        if (myfog)
            args = (String[]) ArrayUtils.addAll(new String[]{"individual"}, args);

        if (args.length < 1 || !args[0].toLowerCase().matches("reload|directupdates|default|world|individual")) {
            sender.sendMessage(getMessage("syntax").replace("%syntax%", alias + " <reload|directupdates|default|individual>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("clearfog.reload")) {
                    sender.sendMessage(getMessage("no_permission"));
                    break;
                }

                loadMessages();
                plugin.reloadConfig();
                plugin.loadConfig();
                config = plugin.getConfig();
                config.options().copyDefaults(true);
                plugin.saveConfig();
                sender.sendMessage(getMessage("reload"));

                Util.updateViewDistances();
                break;
            case "directupdates": {
                if (!sender.hasPermission("clearfog.directupdates")) {
                    sender.sendMessage(getMessage("no_permission"));
                    break;
                }

                if (args.length < 2 || !args[1].toLowerCase().matches("status|enable|disable")) {
                    sender.sendMessage(getMessage("syntax").replace("%syntax%",
                            alias + " directupdates <status|enable|disable>"));
                    break;
                }

                String arg = args[1].toLowerCase();
                switch (arg) {
                    case "status":
                        if (!sender.hasPermission("clearfog.directupdates.status")) {
                            sender.sendMessage(getMessage("no_permission"));
                            break;
                        }

                        boolean enabled = config.getBoolean("direct-view-distance-updates");
                        sender.sendMessage(getMessage("directupdates.status." + (enabled ? "enabled" : "disabled")));
                        break;
                    case "enable":
                    case "disable":
                        if (!sender.hasPermission("clearfog.directupdates.status")) {
                            sender.sendMessage(getMessage("no_permission"));
                            break;
                        }

                        boolean enable = arg.equals("enable");
                        String messagePrefix = "directupdates." + arg + ".";
                        if (config.getBoolean("direct-view-distance-updates") == enable) {
                            sender.sendMessage(getMessage(messagePrefix + "as_before"));
                            break;
                        }

                        config.set("direct-view-distance-updates", enable);
                        plugin.saveConfig();
                        sender.sendMessage(getMessage(messagePrefix + "success"));

                        if (enable)
                            Util.updateViewDistances();
                        break;
                }
                break;
            }
            case "default": {
                if (!sender.hasPermission("clearfog.default")) {
                    sender.sendMessage(getMessage("no_permission"));
                    break;
                }

                if (args.length < 2 || !args[1].toLowerCase().matches("status|enable|disable|get|set")) {
                    sender.sendMessage(getMessage("syntax").replace("%syntax%",
                            alias + " default <status|enable|disable|get|set>"));
                    break;
                }

                String arg = args[1].toLowerCase();
                switch (arg) {
                    case "status":
                        if (!sender.hasPermission("clearfog.default.status")) {
                            sender.sendMessage(getMessage("no_permission"));
                            break;
                        }

                        boolean enabled = config.getBoolean("default.enabled");
                        sender.sendMessage(getMessage("default.status." + (enabled ? "enabled" : "disabled")));
                        break;
                    case "enable":
                    case "disable":
                        if (!sender.hasPermission("clearfog.default.status")) {
                            sender.sendMessage(getMessage("no_permission"));
                            break;
                        }

                        boolean enable = arg.equals("enable");
                        String messagePrefix = "default." + arg + ".";
                        if (config.getBoolean("default.enabled") == enable) {
                            sender.sendMessage(getMessage(messagePrefix + "as_before"));
                            break;
                        }

                        config.set("default.enabled", enable);
                        plugin.saveConfig();
                        sender.sendMessage(getMessage(messagePrefix + "success"));

                        updateViewDistances();
                        break;
                    case "get":
                    case "set":
                        if (!sender.hasPermission("clearfog.default.values")) {
                            sender.sendMessage(getMessage("no_permission"));
                            break;
                        }

                        if (arg.equals("get")) {
                            sender.sendMessage(getMessage("default.get").replace("%distance%",
                                    String.valueOf(checkViewDistance(config.getInt("default.view-distance")))));
                        } else {
                            if (args.length < 3) {
                                sender.sendMessage(getMessage("syntax").replace("%syntax%",
                                        alias + " default set <View Distance>"));
                                break;
                            }

                            int viewDistance;
                            try {
                                viewDistance = Integer.parseInt(args[2]);
                            } catch (NumberFormatException e) {
                                sender.sendMessage(getMessage("state_number"));
                                break;
                            }

                            if (viewDistance < 1) {
                                sender.sendMessage(getMessage("out_of_bounds"));
                                break;
                            }
                            if (config.getInt("default.view-distance") == viewDistance) {
                                sender.sendMessage(getMessage("default.set.as_before")
                                        .replace("%distance%", String.valueOf(viewDistance)));
                                break;
                            }

                            config.set("default.view-distance", viewDistance);
                            plugin.saveConfig();
                            sender.sendMessage(getMessage("default.set.success")
                                    .replace("%distance%", String.valueOf(viewDistance)));

                            updateViewDistances();
                        }
                        break;
                }
                break;
            }
            case "world": {
                if (!sender.hasPermission("clearfog.world")) {
                    sender.sendMessage(getMessage("no_permission"));
                    break;
                }

                String syntaxPrefix = alias + (!worldfog ? " world " : " ");
                if (args.length < 2 || !args[1].toLowerCase().matches("status|enable|disable|list|get|set|unset")) {
                    sender.sendMessage(getMessage("syntax").replace("%syntax%",
                            syntaxPrefix + "<status|enable|disable|list|get|set|unset>"));
                    break;
                }

                String arg = args[1].toLowerCase();
                switch (arg) {
                    case "status":
                        if (!sender.hasPermission("clearfog.world.status")) {
                            sender.sendMessage(getMessage("no_permission"));
                            break;
                        }

                        boolean enabled = config.getBoolean("world.enabled");
                        sender.sendMessage(getMessage("world.status." + (enabled ? "enabled" : "disabled")));
                        break;
                    case "enable":
                    case "disable": {
                        if (!sender.hasPermission("clearfog.world.status")) {
                            sender.sendMessage(getMessage("no_permission"));
                            break;
                        }

                        boolean enable = arg.equals("enable");
                        String messagePrefix = "world." + arg + ".";
                        if (config.getBoolean("world.enabled") == enable) {
                            sender.sendMessage(getMessage(messagePrefix + "as_before"));
                            break;
                        }

                        config.set("world.enabled", enable);
                        plugin.saveConfig();
                        sender.sendMessage(getMessage(messagePrefix + "success"));

                        updateViewDistances();
                        break;
                    }
                    case "list":
                        if (!config.getBoolean("world.enabled")) {
                            sender.sendMessage(getMessage("world.feature_not_enabled"));
                            break;
                        }

                        if (!sender.hasPermission("clearfog.world.list")) {
                            sender.sendMessage(getMessage("no_permission"));
                            break;
                        }

                        Set<String> worlds = config.getConfigurationSection("world.worlds").getKeys(false);
                        if (worlds.isEmpty()) {
                            sender.sendMessage(getMessage("world.list.nothing"));
                            break;
                        }

                        sender.sendMessage(getMessage("world.list.prefix"));
                        String part = getMessage("world.list.part");
                        worlds.forEach(world -> sender.sendMessage(part.replace("%world%", world)
                                .replace("%distance%", String.valueOf(config.getInt("world.worlds." + world)))));
                        break;
                    case "get":
                    case "set":
                    case "unset":
                        if (!config.getBoolean("world.enabled")) {
                            sender.sendMessage(getMessage("world.feature_not_enabled"));
                            break;
                        }

                        if (!sender.hasPermission("clearfog.world.values")) {
                            sender.sendMessage(getMessage("no_permission"));
                            break;
                        }

                        boolean set = arg.equals("set");
                        String target;
                        if (args.length >= (set ? 4 : 3)) {
                            target = args[set ? 3 : 2];
                            if (Bukkit.getWorld(target) == null) {
                                sender.sendMessage(getMessage("world.not_found").replace("%world%", target));
                                break;
                            }
                        } else if (sender instanceof Player) {
                            target = ((Player) sender).getWorld().getName();
                        } else {
                            sender.sendMessage(getMessage("syntax").replace("%syntax%",
                                    syntaxPrefix + arg + (set ? " <View Distance>" : "") + " <World>"));
                            break;
                        }

                        String key = "world.worlds." + target;
                        String messagePrefix = "world." + arg + ".";
                        if (arg.equals("get")) {
                            if (config.isSet(key)) {
                                sender.sendMessage(getMessage(messagePrefix + "return")
                                        .replace("%distance%", String.valueOf(checkViewDistance(config.getInt(key))))
                                        .replace("%world%", target));
                            } else
                                sender.sendMessage(getMessage(messagePrefix + "not_set").replace("%world%", target));
                        } else if (set) {
                            if (args.length >= 3) {
                                int viewDistance;
                                try {
                                    viewDistance = Integer.parseInt(args[2]);
                                } catch (NumberFormatException e) {
                                    sender.sendMessage(getMessage("state_number"));
                                    break;
                                }

                                if (viewDistance < 1) {
                                    sender.sendMessage(getMessage("out_of_bounds"));
                                    break;
                                }
                                if (config.getInt(key) == viewDistance) {
                                    sender.sendMessage(getMessage(messagePrefix + "as_before")
                                            .replace("%distance%", String.valueOf(viewDistance))
                                            .replace("%world%", target));
                                    break;
                                }

                                config.set(key, viewDistance);
                                plugin.saveConfig();
                                sender.sendMessage(getMessage(messagePrefix + "success")
                                        .replace("%distance%", String.valueOf(viewDistance))
                                        .replace("%world%", target));

                                Util.updateViewDistances(Bukkit.getWorld(target).getPlayers());
                            } else
                                sender.sendMessage(getMessage("syntax").replace("%syntax%",
                                        syntaxPrefix + "set <View Distance> [World]"));
                        } else {
                            if (!config.isSet(key)) {
                                sender.sendMessage(getMessage(messagePrefix + "does_not_exist").replace("%world%", target));
                                break;
                            }

                            config.set(key, null);
                            plugin.saveConfig();
                            sender.sendMessage(getMessage(messagePrefix + "success").replace("%world%", target));

                            Util.updateViewDistances(Bukkit.getWorld(target).getPlayers());
                        }
                        break;
                }
                break;
            }
            case "individual":
                if (!sender.hasPermission("clearfog.individual")) {
                    sender.sendMessage(getMessage("no_permission"));
                    break;
                }

                String syntaxPrefix = alias + (!myfog ? " individual " : " ");
                if (args.length < 2 || !args[1].toLowerCase().matches("status|enable|disable|list|get|set|unset")) {
                    sender.sendMessage(getMessage("syntax").replace("%syntax%",
                            syntaxPrefix + "<status|enable|disable|list|get|set|unset>"));
                    break;
                }

                String arg = args[1].toLowerCase();
                switch (arg) {
                    case "status":
                        if (!sender.hasPermission("clearfog.individual.status")) {
                            sender.sendMessage(getMessage("no_permission"));
                            break;
                        }

                        boolean enabled = config.getBoolean("individual.enabled");
                        sender.sendMessage(getMessage("individual.status." + (enabled ? "enabled" : "disabled")));
                        break;
                    case "enable":
                    case "disable": {
                        if (!sender.hasPermission("clearfog.individual.status")) {
                            sender.sendMessage(getMessage("no_permission"));
                            break;
                        }

                        boolean enable = arg.equals("enable");
                        String messagePrefix = "individual." + arg + ".";
                        if (config.getBoolean("individual.enabled") == enable) {
                            sender.sendMessage(getMessage(messagePrefix + "as_before"));
                            break;
                        }

                        config.set("individual.enabled", enable);
                        plugin.saveConfig();
                        sender.sendMessage(getMessage(messagePrefix + "success"));

                        updateViewDistances();
                        break;
                    }
                    case "list":
                        if (!config.getBoolean("individual.enabled")) {
                            sender.sendMessage(getMessage("individual.feature_not_enabled"));
                            break;
                        }

                        if (!sender.hasPermission("clearfog.individual.list")) {
                            sender.sendMessage(getMessage("no_permission"));
                            break;
                        }

                        Set<String> players = config.getConfigurationSection("individual.players").getKeys(false);
                        if (players.isEmpty()) {
                            sender.sendMessage(getMessage("individual.list.nothing"));
                            break;
                        }

                        sender.sendMessage(getMessage("individual.list.prefix"));
                        String part = getMessage("individual.list.part");
                        players.forEach(uuid -> {
                            OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
                            sender.sendMessage(part.replace("%player%", player.isOnline() || player.hasPlayedBefore() ? player.getName() : uuid)
                                    .replace("%distance%", String.valueOf(config.getInt("individual.players." + uuid))));
                        });
                        break;
                    case "get":
                    case "set":
                    case "unset":
                        if (!config.getBoolean("individual.enabled")) {
                            sender.sendMessage(getMessage("individual.feature_not_enabled"));
                            break;
                        }

                        if (!sender.hasPermission("clearfog.individual.values")) {
                            sender.sendMessage(getMessage("no_permission"));
                            break;
                        }

                        boolean set = arg.equals("set");
                        boolean permOthers = sender.hasPermission("clearfog.individual.values.others");
                        boolean self;
                        OfflinePlayer target;
                        if (args.length >= (set ? 4 : 3) && permOthers) {
                            self = false;
                            String name = args[set ? 3 : 2];
                            target = Bukkit.getOfflinePlayer(name);
                            if (!target.isOnline() && !target.hasPlayedBefore()) {
                                sender.sendMessage(getMessage("player_not_found").replace("%player%", name));
                                break;
                            }
                        } else if (sender instanceof Player) {
                            self = true;
                            target = (Player) sender;
                        } else {
                            sender.sendMessage(getMessage("syntax").replace("%syntax%",
                                    syntaxPrefix + arg + (set ? " <View Distance>" : "") + " <Player>"));
                            break;
                        }

                        String key = "individual.players." + target.getUniqueId();
                        String messagePrefix = "individual." + arg + "." + (self ? "self" : "others") + ".";
                        if (arg.equals("get")) {
                            if (config.isSet(key)) {
                                sender.sendMessage(getMessage(messagePrefix + "return")
                                        .replace("%distance%", String.valueOf(checkViewDistance(config.getInt(key))))
                                        .replace("%player%", target.getName()));
                            } else
                                sender.sendMessage(getMessage(messagePrefix + "not_set").replace("%player%", target.getName()));
                        } else if (set) {
                            if (args.length >= 3) {
                                int viewDistance;
                                try {
                                    viewDistance = Integer.parseInt(args[2]);
                                } catch (NumberFormatException e) {
                                    sender.sendMessage(getMessage("state_number"));
                                    break;
                                }

                                if (viewDistance < 1) {
                                    sender.sendMessage(getMessage("out_of_bounds"));
                                    break;
                                }
                                if (config.getInt(key) == viewDistance) {
                                    sender.sendMessage(getMessage(messagePrefix + "as_before")
                                            .replace("%distance%", String.valueOf(viewDistance))
                                            .replace("%player%", target.getName()));
                                    break;
                                }

                                config.set(key, viewDistance);
                                plugin.saveConfig();
                                sender.sendMessage(getMessage(messagePrefix + "success")
                                        .replace("%distance%", String.valueOf(viewDistance))
                                        .replace("%player%", target.getName()));

                                if (target.isOnline())
                                    updateViewDistance(target.getPlayer());
                            } else
                                sender.sendMessage(getMessage("syntax").replace("%syntax%",
                                        syntaxPrefix + "set <View Distance>" + (permOthers ? " [Player]" : "")));
                        } else {
                            if (!config.isSet(key)) {
                                sender.sendMessage(getMessage(messagePrefix + "does_not_exist").replace("%player%", target.getName()));
                                break;
                            }

                            config.set(key, null);
                            plugin.saveConfig();
                            sender.sendMessage(getMessage(messagePrefix + "success").replace("%player%", target.getName()));

                            if (target.isOnline())
                                updateViewDistance(target.getPlayer());
                        }
                        break;
                }
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (alias.equals("worldfog"))
            args = (String[]) ArrayUtils.addAll(new String[]{"world"}, args);
        if (alias.equals("myfog"))
            args = (String[]) ArrayUtils.addAll(new String[]{"individual"}, args);

        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("clearfog.directupdates"))
                list.add("directupdates");
            if (sender.hasPermission("clearfog.reload"))
                list.add("reload");
            if (sender.hasPermission("clearfog.default"))
                list.add("default");
            if (sender.hasPermission("clearfog.world"))
                list.add("world");
            if (sender.hasPermission("clearfog.individual"))
                list.add("individual");
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("directupdates") &&
                sender.hasPermission("clearfog.directupdates.status")) {
                list.addAll(Arrays.asList("status", "enable", "disable"));
            }
            if (args[0].equalsIgnoreCase("default")) {
                if (sender.hasPermission("clearfog.default.status"))
                    list.addAll(Arrays.asList("status", "enable", "disable"));
                if (sender.hasPermission("clearfog.default.values"))
                    list.addAll(Arrays.asList("get", "set"));
            }
            if (args[0].equalsIgnoreCase("world")) {
                if (sender.hasPermission("clearfog.world.status"))
                    list.addAll(Arrays.asList("status", "enable", "disable"));
                if (sender.hasPermission("clearfog.world.list"))
                    list.add("list");
                if (sender.hasPermission("clearfog.world.values"))
                    list.addAll(Arrays.asList("get", "set", "unset"));
            }
            if (args[0].equalsIgnoreCase("individual")) {
                if (sender.hasPermission("clearfog.individual.status"))
                    list.addAll(Arrays.asList("status", "enable", "disable"));
                if (sender.hasPermission("clearfog.individual.list"))
                    list.add("list");
                if (sender.hasPermission("clearfog.individual.values"))
                    list.addAll(Arrays.asList("get", "set", "unset"));
            }
        }
        if (args.length >= 3) {
            int length = args[1].equalsIgnoreCase("set") ? 4 : 3;
            if (args.length == length && args[0].equalsIgnoreCase("world") &&
                sender.hasPermission("clearfog.world.values")) {
                Bukkit.getWorlds().stream().map(World::getName).forEach(list::add);
            }
            if (args.length == length && args[0].equalsIgnoreCase("individual") &&
                sender.hasPermission("clearfog.individual.values.others")) {
                Arrays.stream(Bukkit.getOfflinePlayers()).map(OfflinePlayer::getName).forEach(list::add);
            }
        }

        String arg = args[args.length - 1].toLowerCase();
        List<String> completions = new ArrayList<>();
        list.stream().filter(s -> s.toLowerCase().startsWith(arg)).forEach(completions::add);
        return completions;
    }
}
