package de.rapha149.clearfog;

import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        boolean myfog = alias.equals("myfog");
        if (myfog)
            args = (String[]) ArrayUtils.addAll(new String[]{"individual"}, args);
        if (args.length < 1 || !args[0].toLowerCase().matches("reload|default|individual")) {
            sender.sendMessage(getMessage("syntax").replace("%syntax%", alias + " <reload|default|individual>"));
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
                try {
                    unregisterHandler();
                    registerHandler();
                    sender.sendMessage(getMessage("reload"));
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                    sender.sendMessage(getMessage("error"));
                }
                break;
            case "default": {
                if (!sender.hasPermission("clearfog.default")) {
                    sender.sendMessage(getMessage("no_permission"));
                    break;
                }

                if (args.length < 2 || !args[1].toLowerCase().matches("enable|disable|get|set")) {
                    sender.sendMessage(getMessage("syntax").replace("%syntax%",
                            alias + " default <enable|disable|get|set>"));
                    break;
                }

                String arg = args[1].toLowerCase();
                switch (arg) {
                    case "enable":
                    case "disable":
                        if (!sender.hasPermission("clearfog.default.toggle")) {
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
                if (args.length < 2 || !args[1].toLowerCase().matches("enable|disable|get|set|unset")) {
                    sender.sendMessage(getMessage("syntax").replace("%syntax%",
                            syntaxPrefix + "<enable|disable|get|set|unset>"));
                    break;
                }

                String arg = args[1].toLowerCase();
                switch (arg) {
                    case "enable":
                    case "disable": {
                        if (!sender.hasPermission("clearfog.individual.toggle")) {
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
                        break;
                    }
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
                            target = Bukkit.getOfflinePlayer(args[set ? 3 : 2]);
                            if (!target.hasPlayedBefore()) {
                                sender.sendMessage(getMessage("player_not_found").replace("%player%", args[set ? 3 : 2]));
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
                        }
                        break;
                }
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (alias.equals("myfog"))
            args = (String[]) ArrayUtils.addAll(new String[]{"individual"}, args);

        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("clearfog.reload"))
                list.add("reload");
            if (sender.hasPermission("clearfog.default"))
                list.add("default");
            if (sender.hasPermission("clearfog.individual"))
                list.add("individual");
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("default")) {
                if (sender.hasPermission("clearfog.default.toggle"))
                    list.addAll(Arrays.asList("enable", "disable"));
                if (sender.hasPermission("clearfog.default.values"))
                    list.addAll(Arrays.asList("get", "set"));
            }
            if (args[0].equalsIgnoreCase("individual")) {
                if (sender.hasPermission("clearfog.individual.toggle"))
                    list.addAll(Arrays.asList("enable", "disable"));
                if (sender.hasPermission("clearfog.individual.values"))
                    list.addAll(Arrays.asList("get", "set", "unset"));
            }
        }
        if (args[0].equalsIgnoreCase("individual") && sender.hasPermission("clearfog.individual.values.others") &&
            args.length == (args[1].equalsIgnoreCase("set") ? 4 : 3)) {
            Arrays.stream(Bukkit.getOfflinePlayers()).map(OfflinePlayer::getName).forEach(list::add);
        }

        String arg = args[args.length - 1].toLowerCase();
        List<String> completions = new ArrayList<>();
        list.stream().filter(s -> s.toLowerCase().startsWith(arg)).forEach(completions::add);
        return completions;
    }
}
